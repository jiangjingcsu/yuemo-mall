# Redis 治理规则

> 约束 AI Agent 的 Redis 使用方式、Key 命名和缓存策略。基于项目实际 Redis 使用模式。
> 实操技能：`.harness/skills/redis-design/SKILL.md` `.harness/skills/cache-design/SKILL.md`

---

## 1. Key 命名规范

### 1.1 命名格式

```
规范格式: {domain}:{biz}:{identifier}

当前项目 Key:
  cart:{userId}              — 购物车（Hash, TTL 7天 + 定时清理）
  token:user:{userId}        — 当前 Token（String, TTL 30min）
  token:blacklist:{token}    — Token 黑名单（String, TTL 30min）
  user:role:{userId}         — 用户角色（String, TTL 24h）
  payment:callback:{paymentNo} — 支付回调幂等锁（String+setIfAbsent, TTL 5min）
  rate:{requestURI}:{userId|ip} — 限流滑动窗口（Sorted Set+Lua, 窗口 60s, 每次请求重置 TTL）
  coupon:receive:{userId}:{couponId} — 领券去重（String+setIfAbsent, TTL 7天）
  mq:consumed:{topic}:{bizId} — MQ 消费幂等锁（String+setIfAbsent, TTL 10min, 失败时删除）

技术债务:
  当前所有 Key 均未使用 yuemo: 前缀（如 cart: 而非 yuemo:cart:）
  新增 Key 必须使用 yuemo: 前缀格式: yuemo:{domain}:{biz}:{identifier}
  现有 Key 应逐步迁移到 yuemo: 前缀格式（需同步修改代码中的常量定义）
```

### 1.2 命名规则

```yaml
规则:
  - 全小写
  - 分隔符使用冒号(:)
  - 禁止使用特殊字符（空格、中文、\n）
  - Key 长度不超过 256 字符
  - 业务含义清晰，见名知义
  - 模块前缀与数据库表对应（cart、order、user 等）
```

---

## 2. 数据结构选择

| 场景 | 数据结构 | 项目实例 |
|---|---|---|
| 用户维度 Hash | Hash | `cart:{userId}` 存储用户购物车所有 SKU |
| 简单 KV | String | `token:user:{userId}`、`token:blacklist:{token}` |
| 滑动窗口限流 | Sorted Set + Lua | `rate:{requestURI}:{userId}` ZADD/ZREMRANGEBYSCORE/ZCARD |
| 幂等锁 | String(setIfAbsent) | `payment:callback:{paymentNo}` |
| MQ 消费幂等 | String(setIfAbsent) | `mq:consumed:payment-callback:{orderNo}` 等多个 Consumer |
| 去重 | String(setIfAbsent) | `coupon:receive:{userId}:{couponId}` |

---

## 3. TTL 策略

```yaml
必须:
  - 所有 Key 必须设置 TTL（包括购物车，TTL 7天 + 定时任务双重保障）
  - Token 类: TTL = Token 有效期（accessToken 30min, refreshToken 7d）
  - 黑名单: TTL = Token 剩余有效期（不超过 30min）
  - 支付回调幂等锁: TTL 5min（覆盖支付宝/微信回调重试周期）
  - 通用幂等锁: 业务操作预计耗时 + 30s 缓冲
  - MQ 消费幂等锁: TTL 10min（覆盖 MQ 重试周期，失败时主动删除 Key 释放锁）
  - 限流滑动窗口: TTL = 窗口长度（60s），每次成功请求时重置
  - 去重: TTL = 业务窗口（领券去重=活动结束时间）
  - 用户角色: TTL 24h（登录时写入，角色变更时主动删除缓存）

禁止:
  - 无限期 Key（除非有配套清理机制）
  - 统一 TTL（不同业务用不同 TTL）
```

---

## 4. 缓存模式

### 4.1 声明式缓存：@Cacheable + Caffeine

```yaml
适用: 读多写少、允许短暂不一致的查询场景
配置: CacheConfig.java → CaffeineCacheManager（30min TTL, 1000 上限, recordStats）
注解:
  @Cacheable: 查询时缓存
  @CacheEvict: 写操作时失效
  @CachePut: 更新缓存

项目实例:
  @Cacheable:
    ProductServiceImpl: @Cacheable(value = "productDetail", key = "#id", unless = "#result == null")
    ReviewServiceImpl: @Cacheable(value = "reviewSummary", key = "#productId")
    CouponServiceImpl: @Cacheable(value = "couponPage", key = "#page + ':' + #size")
  @CacheEvict:
    ProductServiceImpl: @CacheEvict(value = {"product", "productDetail"}, key = "#id")
    SkuServiceImpl: @CacheEvict(value = "product", key = "#sku.productId")
    ReviewServiceImpl: @CacheEvict(value = "reviewSummary", key = "#productId")
    CouponServiceImpl: @CacheEvict(value = "couponTemplate", key = "'all'")
    CouponServiceImpl: @CacheEvict(value = "couponPage", allEntries = true)

约束:
  - @Cacheable 缓存的方法返回值必须为 VO（禁止返回 Entity）
  - @CacheEvict 必须在写操作方法上
  - @Cacheable 应使用 unless = "#result == null" 防止缓存空值（除非业务需要空值缓存防穿透）
  - 多实例部署时 Caffeine 不共享，需 Redis Pub/Sub 同步
```

### 4.2 读模式：Cache-Aside

```
读: 查 Redis → 命中返回 → 未命中查 DB → 写入 Redis → 返回
写: 更新 DB → 删除/更新 Redis
```

### 4.3 购物车模式：Redis 为主 + MySQL 异步同步

```
写: 更新 Redis → 发 MQ(cart-sync) → 异步落 MySQL
读: 查 Redis → 命中返回 → 未命中查 MySQL → 写入 Redis → 返回
```

### 4.4 MQ 消费幂等模式：先锁后做，失败释放

```
模式: setIfAbsent 获取幂等锁 → 执行业务 → 失败时删除锁释放重试机会

步骤:
  1. setIfAbsent(idempotentKey, "1", TTL) 获取幂等锁
  2. 获取失败 → 重复消费，跳过
  3. 获取成功 → 执行业务逻辑
  4. 业务成功 → 正常结束（Key 随 TTL 过期）
  5. 业务失败 → delete(idempotentKey) 释放锁，允许 MQ 重试

项目实例:
  PaymentCallbackConsumer — mq:consumed:payment-callback:{orderNo}
  OrderRefundConsumer — mq:consumed:order-refund:{orderId}
  PaymentRefundConsumer — mq:consumed:payment-refund:{orderId}
  StockPreoccupyConsumer — mq:consumed:stock-preoccupy:{fingerprint}
  StockReleaseConsumer — mq:consumed:stock-release:{fingerprint}

约束:
  - 新增 MQ Consumer 必须实现幂等锁
  - 幂等锁 Key 前缀必须遵循 mq:consumed:{topic}: 格式
  - 业务失败时必须删除幂等锁，否则 MQ 重试无法生效
  - TTL 设为 10min（覆盖 MQ 重试周期）
```

---

## 5. 缓存防护

```yaml
必须:
  - 防缓存穿透: 空值缓存(短 TTL) 或布隆过滤器
  - 防缓存击穿: 热点数据加互斥锁
  - 防缓存雪崩: 随机 TTL（基础 TTL ± 10%）

禁止:
  - 缓存大 Key（单个 Key > 10MB 或 Hash field > 5000）
  - 缓存热 Key（QPS > 10000 的 Key 需拆分）
  - 在 Redis 中存储 JDK 序列化的 Java 对象（应存 JSON）

序列化配置约束:
  当前配置（RedisConfig.java）:
    Key/HashKey: StringRedisSerializer
    Value/HashValue: GenericJackson2JsonRedisSerializer（含 JavaTimeModule）
  禁止:
    - 切换 Value 序列化为 JDK 序列化（会导致 Redis 中存储不可读的二进制数据）
    - 移除 JavaTimeModule（会导致 LocalDateTime 等时间类型序列化失败）
    - 修改 Key 序列化方式（所有 Key 必须使用 StringRedisSerializer）
```

---

## 6. 操作规范

```yaml
必须:
  - 批量操作使用 Pipeline（减少 RTT）
  - 购物车 Hash 使用 HGETALL/HMSET/HINCRBY
  - 复杂逻辑使用 Lua 脚本保证原子性（如限流滑动窗口）
  - RedisTemplate 选择规则:
      业务模块: RedisTemplate<String, Object>（JSON 序列化，适合存储对象）
      网关/Lua 脚本: StringRedisTemplate（纯字符串，Lua 脚本操作需要 String 类型）

禁止:
  硬红线: → constraints/ai-boundaries.md §生产操作（FLUSHALL/FLUSHDB/KEYS *）
  本文件补充:
    - MONITOR 命令（性能影响大）
    - 单个操作阻塞超过 100ms
```

---

## 7. Key 清理策略

| Key 模式 | 清理方式 | 频率 |
|---|---|---|
| `cart:{userId}` | TTL 7天 + 定时任务 `CartCleanupTask` 双重保障 | 每天凌晨 3 点 |
| `token:user:{userId}` | TTL 自动过期 | 30min |
| `token:blacklist:{token}` | TTL 自动过期 | 30min |
| `user:role:{userId}` | TTL 自动过期 + 角色变更时删除 | 24h |
| `rate:{requestURI}:*` | TTL 自动过期（每次成功请求重置） | 60s |
| `payment:callback:*` | TTL 自动过期 | 5min |
| `coupon:receive:*` | TTL 自动过期 | 7天 |
| `mq:consumed:*` | TTL 自动过期 + 业务失败时主动删除 | 10min |

---

## 8. Key 常量化管理

```yaml
必须:
  - 所有 Redis Key 前缀必须定义为 Java 常量，禁止硬编码字符串
  - 常量类组织方式:
    - 认证相关: common-security 模块的 AuthRedisKeyConstants
    - 业务模块: 各模块 constant 包下（如 PaymentRedisKeyConstants）
    - 网关: gateway 模块的 GatewayRedisKeyConstants
  - 常量命名: {BIZ}_KEY_PREFIX，如 TOKEN_USER_KEY_PREFIX = "token:user:"
  - TTL 常量: {BIZ}_TTL_{UNIT}，如 ROLE_TTL_HOURS = 24

禁止:
  - 在代码中直接使用字符串字面量作为 Redis Key
  - 在多个文件中重复定义相同的 Key 前缀
```

当前 Key 常量化状态:

| Key 前缀 | 常量类 | 状态 |
|---|---|---|
| `cart:` | CartConstants.CART_KEY_PREFIX | ✅ 已常量化 |
| `token:user:` | AuthRedisKeyConstants.TOKEN_USER_PREFIX | ✅ 已常量化 |
| `token:blacklist:` | AuthRedisKeyConstants.TOKEN_BLACKLIST_PREFIX | ✅ 已常量化 |
| `user:role:` | AuthRedisKeyConstants.USER_ROLE_PREFIX | ✅ 已常量化 |
| `payment:callback:` | PaymentRedisKeyConstants.PAYMENT_CALLBACK_PREFIX | ✅ 已常量化 |
| `coupon:receive:` | PromotionRedisKeyConstants.COUPON_RECEIVE_PREFIX | ✅ 已常量化 |
| `rate:` | GatewayRedisKeyConstants.RATE_LIMIT_PREFIX | ✅ 已常量化 |
| `mq:consumed:payment-callback:` | PaymentCallbackConsumer.IDEMPOTENT_PREFIX | ⚠️ 未集中常量化（类内 private static final，建议迁移到 OrderRedisKeyConstants） |
| `mq:consumed:order-refund:` | OrderRefundConsumer.IDEMPOTENT_PREFIX | ⚠️ 未集中常量化（类内 private static final，建议迁移到 OrderRedisKeyConstants） |
| `mq:consumed:payment-refund:` | PaymentRefundConsumer.IDEMPOTENT_PREFIX | ⚠️ 未集中常量化（类内 private static final，建议迁移到 PaymentRedisKeyConstants） |
| `mq:consumed:stock-preoccupy:` | StockPreoccupyConsumer.IDEMPOTENT_PREFIX | ⚠️ 未集中常量化（类内 private static final，建议迁移到 ProductRedisKeyConstants） |
| `mq:consumed:stock-release:` | StockReleaseConsumer.IDEMPOTENT_PREFIX | ⚠️ 未集中常量化（类内 private static final，建议迁移到 ProductRedisKeyConstants） |
