---
name: "cache-design"
description: "为特定业务场景设计完整的缓存方案，包括多级缓存策略、数据一致性保证和失效策略。当需要复杂缓存设计时触发。"
---

# 缓存架构设计 Skill

为具体业务场景设计多级缓存方案，涵盖 Caffeine 本地缓存/Redis/数据库的协同。
> 治理规则：`.harness/rules/redis-governance.md`

## 触发条件

- 需要设计复杂缓存方案
- 需要多级缓存
- 需要缓存预热/刷新策略
- 缓存命中率低需要重新设计

## 依赖上下文

- `.harness/rules/redis-governance.md` — Redis 治理规则
- `.harness/docs/data-flow.md` — 已有缓存流程
- `.harness/skills/redis-design/SKILL.md` — Redis 设计 Skill（简单场景用这个）
- `.harness/memory/decisions.md` — 架构决策（缓存策略相关决策）
- `.harness/memory/anti-patterns.md` — 已知反模式（如 CouponServiceImpl 缓存问题）
- 相关 Service 源码

## 与 redis-design Skill 的边界

```yaml
路由规则:
  redis-design:
    - 单 Key 设计（Token、幂等锁、计数器）
    - 简单 KV 读写
    - 不涉及本地缓存

  cache-design:
    - 多级缓存（Caffeine + Redis）
    - 声明式缓存（@Cacheable/@CacheEvict）
    - 缓存预热/刷新策略
    - 缓存一致性设计
    - 缓存监控与降级

  重叠场景（优先 cache-design）:
    - 优惠券模板缓存（涉及预热 + @Cacheable）
    - 商品详情缓存（涉及 @Cacheable + 一致性）
```

## 与 Workflow 的关系

```yaml
定位: feature-development.md 工作流的缓存设计子工具

本 Skill 覆盖:
  - feature-development.md Step 5（缓存设计）→ 本 Skill 全部步骤

联动 Skill:
  - 上游: skills/ddd-design/SKILL.md — 领域模型确定后，评估缓存需求
  - 下游: skills/redis-design/SKILL.md — 简单 KV 场景委托给此 Skill
  - 关联: skills/performance-review/SKILL.md — 缓存性能问题排查
```

## 缓存设计决策树

```
新功能需要缓存吗？
  ├─ 读多写少 + 允许短暂不一致 → 需要缓存
  │   ├─ 简单查询，单 Key → redis-design Skill
  │   ├─ 查询结果可声明式缓存 → @Cacheable（本 Skill Step 2）
  │   └─ 多级/预热/监控 → 本 Skill 完整流程
  ├─ 写多读少 → 不适合缓存
  ├─ 强一致性要求 → 不适合缓存，直接读 DB
  └─ Redis 主存储场景 → 本 Skill（购物车模式）
```

## 项目已有缓存场景

| 场景 | 缓存模式 | 缓存层 | Key/Cache 名 |
|---|---|---|---|
| 商品详情 | `@Cacheable` 声明式 | Caffeine L1 | `productDetail::{id}` |
| 商品列表 | `@Cacheable` 声明式 | Caffeine L1 | `product::{id}` |
| 评价摘要 | `@Cacheable` 声明式 | Caffeine L1 | `reviewSummary::{productId}` |
| 优惠券列表 | `@Cacheable` 声明式 | Caffeine L1 | `couponPage::{page}:{size}` |
| 购物车 | Redis Hash 主存储 | Redis L2 | `cart:{userId}` |
| Token 缓存 | Redis String | Redis L2 | `token:user:{userId}` |
| Token 黑名单 | Redis String | Redis L2 | `token:blacklist:{token}` |
| 用户角色 | Redis String | Redis L2 | `user:role:{userId}` |
| 支付回调幂等 | Redis String setIfAbsent | Redis L2 | `payment:callback:{paymentNo}` |
| 优惠券领取幂等 | Redis String setIfAbsent | Redis L2 | `coupon:receive:{userId}:{couponId}` |
| 接口限流 | Redis + Lua 脚本 | Redis L2 | `rate:{path}:{userId/ip}` |

## 设计流程

### Step 0: Memory 检查（推荐）

```yaml
读取:
  - memory/decisions.md — 是否有缓存相关架构决策？（如 D004: 购物车 Redis 主存储）
  - memory/anti-patterns.md — 是否有缓存相关反模式？（如 AP005: CouponServiceImpl 缓存问题）
原则: 不重复已有缓存设计，不踩已知的坑
```

### Step 1: 缓存层级选择

| 层级 | 技术 | 访问延迟 | 适用场景 | 项目配置 |
|---|---|---|---|---|
| L1 本地缓存 | Caffeine | < 1ms | 热点配置、字典数据、商品详情 | CacheConfig.java（30min TTL, 1000 上限, recordStats） |
| L2 分布式缓存 | Redis | 1-5ms | 用户数据、业务缓存、主存储 | RedisConfig.java（JSON 序列化） |
| L3 数据库 | MySQL | 5-50ms | 持久化存储 | — |

```yaml
选择原则:
  常驻内存不频繁变更 → L1 Caffeine（通过 @Cacheable）
  共享状态/支持过期/多实例共享 → L2 Redis
  无需缓存 → 直接 L3 MySQL

多级缓存:
  L1 + L2 组合: Caffeine 短 TTL + Redis 长 TTL
  适用: 高频读取 + 允许短暂不一致
  一致性: Redis 更新时通过 Redis Pub/Sub 通知各节点清除 L1
```

### Step 2: 声明式缓存（@Cacheable）

```yaml
适用场景:
  - 查询方法，读多写少
  - 缓存结果为 VO/DTO（禁止缓存 Entity）
  - 单实例部署或允许短暂不一致

使用方式:
  查询: @Cacheable(value = "cacheName", key = "#param", unless = "#result == null")
  更新: @CachePut(value = "cacheName", key = "#id")
  删除: @CacheEvict(value = "cacheName", key = "#id")
  全清: @CacheEvict(value = "cacheName", allEntries = true)

项目配置:
  CacheManager: CaffeineCacheManager（CacheConfig.java）
  默认 TTL: 30 分钟
  默认容量: 1000
  统计: recordStats() 开启

注意事项:
  - @Cacheable 的 key 使用 SpEL 表达式
  - unless 条件避免缓存 null 值（防穿透用布隆过滤器，不用 unless）
  - @CacheEvict 必须在写操作方法上（create/update/delete）
  - 缓存对象必须实现 Serializable（Caffeine 不需要，RedisCacheManager 需要）
  - 多实例部署时 Caffeine 缓存不共享，需要 Redis Pub/Sub 同步
```

### Step 3: 编程式缓存（RedisTemplate）

```yaml
适用场景:
  - @Cacheable 无法表达的复杂逻辑
  - Redis 主存储场景（购物车）
  - 需要精确控制 TTL
  - 需要原子操作（setIfAbsent、increment）

使用方式:
  注入: RedisTemplate<String, Object> redisTemplate
  Value: opsForValue() — Token、计数器、幂等锁
  Hash: opsForHash() — 购物车
  Set: opsForSet() — 幂等标记
  ZSet: opsForZSet() — 排行榜

Key 命名: {module}:{business}:{identifier}
  ✅ coupon:receive:{userId}:{couponId}
  ✅ payment:callback:{paymentNo}
  ✅ cart:{userId}
  ❌ coupon:{couponId}（模块前缀不清晰，与 coupon:receive 混淆）

TTL 规则:
  - 所有 Key 必须设置 TTL（redis-governance.md 强制）
  - Token 类: 30 分钟
  - 幂等锁: 5 分钟
  - 业务缓存: 按场景设定
  - 永不过期: 仅限白名单 Key
```

### Step 4: 一致性设计

```yaml
强一致场景（订单/支付）:
  不适用缓存，直接读 DB
  或：写后同步更新缓存（Cache-Aside 写模式）

最终一致场景（商品信息/用户信息）:
  声明式: @Cacheable + @CacheEvict
    读: 命中返回，miss 查 DB + 写缓存
    写: 更新 DB → @CacheEvict 删除缓存
    TTL 兜底: Caffeine 30min / Redis 按场景
  编程式: 手动 RedisTemplate
    读: get → miss → DB → set
    写: 更新 DB → delete key

Redis 主存储场景（购物车）:
  读写都走 Redis
  MySQL 异步同步（通过 RocketMQ）
  一致性由 MQ 消费保证

多级缓存一致性:
  L1(Caffeine) 短 TTL + L2(Redis) 长 TTL
  更新时: 删除 Redis → 发布 Pub/Sub → 各节点清除 Caffeine
  兜底: Caffeine TTL 到期自动失效
```

### Step 5: 失效策略

```yaml
策略选择:
  主动失效: @CacheEvict 或 RedisTemplate.delete（实时性高）
  被动失效: TTL 过期自动删除（实现简单）
  混合: 主动失效 + TTL 兜底（推荐）

特殊策略:
  缓存预热: 启动时/高峰期前主动加载热点数据
  缓存降级: 缓存失败时降级到 DB（但需限制并发）
  缓存空窗期: 缓存过期但未重建期间的并发保护

防护措施:
  穿透: 布隆过滤器 / 缓存空值（短 TTL）
  击穿: 互斥锁（setIfAbsent）/ 永不过期 + 异步刷新
  雪崩: TTL 加随机偏移 / 多级缓存 / 熔断降级
```

### Step 6: 监控指标

```yaml
Caffeine 监控（recordStats 已开启）:
  - hitRate: 命中率（目标 > 90%）
  - evictionCount: 驱逐次数
  - averageLoadPenalty: 平均加载耗时

Redis 监控:
  - Redis 内存使用率（INFO memory）
  - 缓存命中率（keyspace_hits / keyspace_misses）
  - 大 Key 告警（> 10MB，redis-governance.md）
  - 热点 Key QPS

告警阈值:
  - Caffeine 命中率 < 80% → 检查缓存设计
  - Redis 内存使用率 > 80% → 检查 Key 数量和 TTL
  - 大 Key 出现 → 立即拆分
```

## 反面教材

```yaml
CouponServiceImpl 原始问题（已修复）:
  ❌ redisTemplate.opsForValue().set("coupon:" + coupon.getId(), coupon)
     问题1: 未设置 TTL → 违反 redis-governance.md
     问题2: Key 格式不规范 → coupon:{id} 与 coupon:receive:{userId}:{id} 混淆
     问题3: 缓存 Entity 对象 → 应缓存 VO 或 JSON
     问题4: 无缓存失效策略 → 更新/删除时未清理缓存
     问题5: 预热后从未读取 → 缓存写入无意义

  ✅ 修复后: 使用 @Cacheable + @CacheEvict 声明式缓存
     @Cacheable(value = "couponPage", key = "#page + ':' + #size")
     @CacheEvict(value = "couponTemplate", key = "'all'")

  此案例已记录在 memory/anti-patterns.md (AP005)，后续分析时优先读取 Memory。
```

## 降级方案参考

```java
// RateLimitFilter 中的降级模式
try {
    // Redis 操作
} catch (Exception e) {
    log.warn("[Cache] Redis 不可用，降级到 DB");
    // 降级到数据库查询
}
```

```yaml
降级策略:
  L1 不可用: 自动跳过，直接查 L2/DB
  L2 不可用: 降级到 DB（需限流防雪崩）
  L1 + L2 不可用: DB 限流 + 熔断 + 返回兜底数据
```

## 输出格式

```markdown
## 缓存架构设计

### 缓存层级
| 层级 | 技术 | 数据类型 | TTL | Cache 名/Key |
|---|---|---|---|---|

### 缓存模式
- 声明式: @Cacheable / @CacheEvict
- 编程式: RedisTemplate 操作

### 读写路径
读: {流程}
写: {流程}

### 一致性保证
策略: {强一致/最终一致}
兜底: {TTL/主动清理}

### 防护措施
- 穿透: {方案}
- 击穿: {方案}
- 雪崩: {方案}

### 降级方案
缓存不可用时: {降级策略}
```

## 约束

- 不滥用缓存：写多读少的数据不适合缓存
- 缓存不增加系统复杂度（简单场景用 redis-design Skill）
- 缓存方案必须包含失效和降级策略
- 所有 Redis Key 必须设置 TTL（redis-governance.md 强制）
- 禁止缓存 Entity 对象，缓存 VO/DTO
- @Cacheable 缓存的方法返回值必须为 VO（禁止返回 Entity）
- Key 命名遵循 `{module}:{business}:{identifier}` 格式
- 缓存方案必须与 memory/decisions.md 中的架构决策一致
