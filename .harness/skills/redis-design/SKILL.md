---
name: "redis-design"
description: "为功能设计缓存方案，包括缓存决策、Key 命名、数据结构选择、TTL 策略、缓存模式和防护设计。当需要新增或修改 Redis 缓存时触发。"
---

# Redis 缓存设计 Skill

基于项目现有 Redis 使用模式和大厂最佳实践，为新功能设计合理的缓存方案。
> 治理规则：`.harness/rules/redis-governance.md`、`.harness/memory/anti-patterns.md`

## 触发条件

- 新增需要缓存的查询
- 需要分布式锁/幂等锁
- 需要计数器/限流
- 用户要求设计缓存方案
- 评估是否需要引入缓存

## 与 Workflow 的关系

```yaml
所属工作流: feature-development.md
触发阶段: Step 3（技术方案输出）— 当需求涉及缓存设计时
输入: Step 2 的需求分析结果（模块归属、读写频率、一致性要求）
输出: 缓存设计方案，作为技术方案的一部分
后续: Step 5（编码实现）— 按方案实现缓存操作代码
自检: Step 7 — 确认新增 Key 有 TTL、无大 Key/热 Key
```

## 与 cache-design Skill 的边界

```yaml
路由规则:
  redis-design（本 Skill）:
    - 缓存决策（是否需要缓存、L1/L2 选择）
    - 单 Key 设计（Token、幂等锁、计数器）
    - 简单 KV 读写
    - L1 本地缓存（@Cacheable + Caffeine）
    - L2 Redis 分布式缓存
    - Redis 主存储模式（购物车）

  cache-design:
    - 多级缓存编排（Caffeine + Redis 联动）
    - 缓存预热/刷新策略
    - 缓存一致性设计（跨级失效同步）
    - 缓存监控与降级

  重叠场景（优先 cache-design）:
    - 优惠券模板缓存（涉及预热 + @Cacheable）
    - 商品详情缓存（涉及 @Cacheable + 一致性）
```

## 跨 Skill 链接

| 关联 Skill | 关系 | 交互方式 |
|---|---|---|
| cache-design | 上层：复杂缓存场景路由到 cache-design | cache-design 引用本 Skill 做简单场景，本 Skill 遇到多级缓存需求时上交 |
| performance-review | 下游：Redis 性能问题（慢查询、大 Key、热 Key）触发性能审查 | 本 Skill 输出方案后，如存在性能风险，应提示触发 performance-review |
| code-verify | 下游：验证管线 Layer 6 Redis 检查项 | 本 Skill 输出方案后，编码实现需通过 code-verify 的 Redis 检查 |

## 依赖上下文

- `.harness/rules/redis-governance.md` — Redis 治理规则
- `.harness/rules/module-boundary.md` — 模块边界（Redis Key 必须归属特定模块）
- `.harness/docs/data-flow.md` — 数据流（含 Redis 使用位置）
- `.harness/memory/decisions.md` — 架构决策（避免重蹈覆辙）
- `.harness/memory/anti-patterns.md` — 反模式记录（避免重复踩坑）
- `.harness/skills/code-verify/SKILL.md` — 验证管线（Layer 6 Redis 检查项）
- 相关 Service 源码（现有 Redis 使用方式）

## 设计流程

### Step 0: 缓存决策判断

```yaml
决策树:
  QPS < 100 且数据量小 → 不需要缓存，直接查 DB
  QPS 100-1000 → 考虑 Caffeine 本地缓存（@Cacheable）
  QPS > 1000 或多实例部署 → Redis 分布式缓存
  需要分布式协调（锁/限流/去重）→ Redis
  需要持久化 + 高频写入 → Redis 为主存储 + MQ 异步落 DB

输出:
  - 是否需要缓存
  - 选择的缓存层级（L1 本地 / L2 分布式 / Redis 主存储）
  - 不需要缓存时直接结束，无需后续步骤
```

### Step 1: Memory 检查

```yaml
必须先读取:
  - .harness/memory/decisions.md — 检查是否已有相关架构决策
    - D004: Redis Hash 做购物车主存储（Redis 主存储模式适用场景）
    - D007: JWT + Redis 认证（Token/黑名单 Key 设计参考）
  - .harness/memory/anti-patterns.md — 检查是否有相关反模式
    - AP005: SELECT-then-INSERT 竞态（Redis 幂等锁可解决类似问题）

检查要点:
  - 是否与已有决策冲突？
  - 是否在重复踩已知反模式的坑？
  - 是否有可复用的现有设计模式？
```

### Step 2: 需求分析

```yaml
明确:
  - 缓存什么数据？（用户信息/商品列表/购物车/Token/锁）
  - 读写频率？（读多写少/写多读少/均衡）
  - 一致性要求？（强一致/最终一致/允许短暂不一致）
  - 数据大小？（单条大小/总量/增长趋势）
  - 过期策略？（自然过期/定时清理/永不过期+手动维护）
  - 所属模块？（Key 前缀必须与模块归属一致，见 module-boundary.md）

强一致性判断:
  ❌ 以下场景禁止使用 Redis 缓存:
    - 订单状态（支付状态、发货状态等需强一致）
    - 库存扣减（需事务保证，Redis 仅做预占）
    - 账户余额变更（需数据库事务保证）
    - 任何涉及资金/法律合规的数据
    - 金额数据（仅 DB 为准）
  ✅ 以下场景适合 Redis:
    - 读多写少 + 允许短暂不一致
    - 高频访问 + 可接受 TTL 窗口内不一致
    - Redis 主存储 + 异步落库（如购物车）
  ❌ 以下数据禁止缓存:
    - 密码
    - 完整手机号
```

### Step 3: Key 设计

```yaml
格式: {domain}:{biz}:{identifier}

参考当前项目 Key 模式:
  cart:{userId}                     — Hash, TTL 7天 + 定时清理
  token:user:{userId}               — String, TTL 30min
  token:blacklist:{token}           — String, TTL 30min
  user:role:{userId}                — String, TTL 24h（登录时写入，角色变更时删除）
  payment:callback:{paymentNo}      — String(setIfAbsent), TTL 5min
  rate:{api-path}:{userId|ip}       — String+Lua, TTL 60s
  coupon:receive:{userId}:{couponId} — String(setIfAbsent), TTL 7天

规则:
  - Key 长度 ≤ 256 字符
  - 全小写，冒号分隔
  - 业务含义明确，见名知义
  - 模块前缀与 module-boundary.md 中的模块归属一致
    ✅ coupon:receive:{userId}:{couponId} — 模块前缀 coupon 明确
    ✅ payment:callback:{paymentNo} — 模块前缀 payment 明确
    ❌ coupon:{couponId} — 模块前缀不清晰，与 coupon:receive 混淆
  - 所有新增 Key 必须定义为 Java 常量，禁止硬编码字符串
  - 新增 Key 不得与现有 Key 前缀冲突

Key 常量类组织:
  认证相关 Key（token:blacklist、token:user、user:role）→ common-security 模块的 AuthRedisKeyConstants
  业务模块 Key → 各模块的 constant 包下:
    - PaymentRedisKeyConstants（yuemo-payment）
    - PromotionRedisKeyConstants（yuemo-promotion）
  网关 Key → gateway 模块的 GatewayRedisKeyConstants

模块前缀对照:
  cart:     — yuemo-cart 模块
  token:    — common-security 模块
  user:     — yuemo-user 模块
  payment:  — yuemo-payment 模块
  rate:     — yuemo-gateway 模块
  coupon:   — yuemo-promotion 模块
  product:  — yuemo-product 模块
  order:    — yuemo-order 模块
```

### Step 4: 数据结构选择

| 场景 | 结构 | 说明 |
|---|---|---|
| 用户维度键值集合 | Hash | 如购物车：userId 下多个 skuId→JSON |
| 简单 KV | String | Token、黑名单、角色 |
| 计数器/限流 | ZSet + Lua | 限流滑动窗口（Lua 脚本保证原子性） |
| 幂等锁/去重 | String(setIfAbsent) | 支付回调幂等、领券去重（原子操作 set+expire 合一） |
| 排行榜/热门 | ZSet | 热门搜索、销量排行 |

去重场景统一使用 String+setIfAbsent，不使用 Set（setIfAbsent 原子操作更高效，且 set+expire 合一）

### Step 5: TTL 设计

```yaml
TTL 策略:
  Token 缓存: Token 有效时长（30min）
  Token 黑名单: Token 剩余时长（不超过 30min）
  用户角色: 24h（登录时写入，角色变更时主动删除缓存）
  支付回调幂等锁: 5min（支付回调重试窗口长，5min 覆盖支付宝/微信回调重试周期）
  通用幂等锁: 业务操作预计耗时 + 30s 缓冲
  缓存数据: 5min～30min，根据数据更新频率
  限流: 窗口时长（60s）
  去重: 业务窗口（领券去重=活动结束时间）
  热点数据: 基础 TTL × (0.9～1.1) 随机防雪崩
  购物车: TTL 7天 + 定时清理双重保障（TTL 兜底防内存泄漏，定时任务精确清理过期数据）

禁止:
  - 不设 TTL（购物车也必须设 TTL，由 TTL + 定时任务双重保障）
  - 统一 TTL（不同业务不同 TTL）
```

### Step 6: 缓存模式选择

```yaml
L1 本地缓存（@Cacheable + Caffeine）:
  适用: 读多写少、QPS < 1000、允许短暂不一致
  读: Caffeine → miss → DB → 写 Caffeine → 返回
  写: 更新 DB → @CacheEvict 失效 Caffeine
  配置: CacheConfig.java → 30min TTL, 1000 上限, recordStats
  约束:
    - 返回值必须为 VO（禁止 Entity）
    - 多实例部署时需 Redis Pub/Sub 同步
  项目实例: ProductServiceImpl, ReviewServiceImpl, CouponServiceImpl

L2 分布式缓存（Cache-Aside）:
  适用: 读多写少、QPS > 1000、多实例部署
  读: Redis → miss → DB → 写 Redis → 返回
  写: 更新 DB → 删除 Redis（或更新 Redis）

Redis 主存储（购物车模式）:
  适用: 高频写入 + 最终一致性
  写: Redis → MQ → 异步落 DB
  读: Redis → miss → DB → 写 Redis → 返回
  清理: TTL 7天 + 定时任务双重保障
  序列化: DTO(record) → Jackson → JSON String

写穿透:
  只写 Redis + 异步落 DB，适合高频写入 + 最终一致性
```

### Step 7: 防护设计

```yaml
必须设计:
  - 缓存穿透防护: 空值缓存（TTL 60s）或布隆过滤器
  - 缓存击穿防护: 热点数据互斥锁
  - 缓存雪崩防护: 随机 TTL（±10%）
  - Redis 降级: Redis 不可用时降级到 DB 查询（限流场景降级放行）
  - 大 Key 防护: 单个 Key 不超过 10MB，Hash field 不超过 5000
  - 热 Key 防护: QPS > 10000 的 Key 需拆分或本地缓存

大 Key 防护（redis-governance.md 强制）:
  定义:
    - String 类型: value > 10MB
    - Hash 类型: field 数量 > 5000
    - Collection 类型: 元素数量 > 5000
  预防:
    - 设计阶段评估单 Key 数据量上限
    - Hash 按 userId 拆分，避免一个 Hash 存全量数据
    - 大集合拆分为多个小 Key（如 order:items:{userId}:{page}）
    - 购物车限制 MAX_CART_ITEMS 防止 Hash 膨胀
  检测:
    - redis-cli --bigkeys 定期扫描
    - HLEN/SCARD 检查集合大小
  处理:
    - 发现大 Key → 立即拆分
    - 拆分方案需评估对现有读写逻辑的影响

热 Key 防护:
  定义: QPS > 10000 的 Key（redis-governance.md）
  预防:
    - 设计阶段评估 Key 访问频率
    - 避免全量数据集中在单个 Key
  处理:
    - 本地缓存: 极端热 Key 加 Caffeine L1 缓存（短 TTL，1-5min）
    - Key 拆分: 将热 Key 拆分为多个子 Key，客户端随机读取
      例: hot:data → hot:data:0, hot:data:1, ..., hot:data:N
    - 读写分离: 热读走从节点
  路由:
    - 需要本地缓存的热 Key → 转交 cache-design Skill（多级缓存方案）
```

### Step 8: 序列化规范

```yaml
规范:
  - Redis Value 使用 JSON 序列化（GenericJackson2JsonRedisSerializer）
  - 禁止存储 Java 原生序列化对象
  - 存储 DTO 使用 record 类型，通过 Jackson 序列化为 JSON
  - 自定义序列化参考: CartSerializer.java（CartItemRedis record → JSON）
  - RedisConfig 已配置:
    - Key/HashKey: StringRedisSerializer
    - Value/HashValue: GenericJackson2JsonRedisSerializer
    - JavaTimeModule 支持 java.time 类型

反序列化注意:
  - GenericJackson2JsonRedisSerializer 会在 JSON 中写入 @class 类型信息
  - 反序列化时需要对应类在 classpath 中
  - 如需跨模块读取，确保 DTO 类在 common 模块或调用方可访问
  - 时间字段使用 LocalDateTime，序列化为 ISO-8601 格式
```

### Step 9: 批量操作与 Lua 脚本

```yaml
批量操作:
  - 多个 Redis 命令必须使用 Pipeline（减少 RTT）
  - 示例: 购物车全选操作应使用 Pipeline 批量 HSET，而非逐条执行

Lua 脚本:
  - 使用场景: 需要原子性的复合操作（限流、分布式锁、条件更新）
  - 编写规范:
    - 使用 KEYS[1..N] 和 ARGV[1..N] 传参，禁止拼接
    - 脚本必须设置合理的超时时间
    - 复杂脚本需编写单元测试
  - 项目实例: RateLimitFilter.java 滑动窗口限流脚本
```

### Step 10: 数据一致性保证

```yaml
方案选择:
  先更新 DB 再删除缓存（推荐）:
    - 一致性: 最终一致
    - 复杂度: 低
    - 适用: 大多数场景
    - 风险: 极短窗口内可能读到旧数据

  延迟双删:
    - 一致性: 较强
    - 复杂度: 中
    - 适用: 一致性要求高的场景
    - 实现: 删缓存 → 更新 DB → 延迟 500ms → 再删缓存

  基于 MQ 异步删除:
    - 一致性: 最终一致
    - 复杂度: 高
    - 适用: 分布式场景
    - 实现: 更新 DB → 发 MQ → 消费者删缓存

购物车模式一致性:
  - Redis 为主存储，MQ 异步落 DB
  - Redis 不可用时降级到 DB
  - 定时任务兜底清理过期数据
```

## 输出格式

```markdown
## 缓存设计方案

### 缓存决策
- 是否需要缓存: {是/否}
- 缓存层级: {L1 本地 / L2 分布式 / Redis 主存储 / 不需要}
- 决策依据: {QPS、部署模式、一致性要求等}

### Key 设计
| Key | 类型 | TTL | 序列化 | Key 常量 | 说明 |
|---|---|---|---|---|---|
| {domain}:{biz}:{id} | String/Hash/... | 30min | JSON(GenericJackson2Json) | XxxRedisKeyConstants.KEY_XXX | ... |

### 缓存模式
{描述读写路径}

### 数据一致性
{描述如何保证缓存与 DB 一致}

### 防护措施
- 穿透: {方案}
- 击穿: {方案}
- 雪崩: {方案}
- 大 Key: {评估 + 预防方案}
- 热 Key: {评估 + 预防方案}
- Redis 降级: {降级方案}

### 紧急清理命令
redis-cli DEL {key}
redis-cli SCAN 0 MATCH {prefix}:* COUNT 100
redis-cli --bigkeys

### 兼容性检查
- [ ] Key 格式符合 {domain}:{biz}:{identifier} 规范
- [ ] Key 已定义为 Java 常量，无硬编码字符串
- [ ] Key 模块前缀与 module-boundary.md 归属一致
- [ ] Key 不与现有 Key 前缀冲突
- [ ] TTL 合理且已设置（包括购物车等主存储模式）
- [ ] 不会产生大 Key（value < 10MB, Hash field < 5000）
- [ ] 不会产生热 Key（QPS < 10000，否则需拆分或加本地缓存）
- [ ] 序列化格式为 JSON，非 Java 序列化
- [ ] 不用于强一致性数据（订单状态/库存/余额/金额等）
- [ ] 不缓存敏感数据（密码、完整手机号）
- [ ] Redis 降级方案已设计
```

## 约束

- Key 命名必须与现有项目风格一致，格式为 `{domain}:{biz}:{identifier}`
- Key 模块前缀必须与 module-boundary.md 中的模块归属一致
- 新增 Key 必须定义为 Java 常量，禁止硬编码字符串
- 新增 Key 不得与现有 Key 前缀冲突
- 不要过度设计（简单 String 能解决的不上复杂结构）
- 考虑清理：如何清理过期/无用数据
- 禁止用 Redis 缓存强一致性数据（订单状态、库存扣减、账户余额等）
- 金额数据不得缓存在 Redis（仅 DB 为准）
- 敏感数据（密码、完整手机号）不得缓存在 Redis
- 禁止产生大 Key（value > 10MB 或 Hash field > 5000），设计阶段必须评估
- 禁止产生热 Key（QPS > 10000），否则必须拆分或加本地缓存
- 所有 Key 必须设置 TTL（包括购物车，TTL + 定时清理双重保障）
- 支付回调幂等锁 TTL 不低于 5min
- 所有存入 Redis 的数据使用 JSON 序列化（GenericJackson2JsonRedisSerializer），禁止 Java 序列化
- 禁止缓存 Entity 对象，缓存 VO/DTO
- Redis 为主存储的模式（如购物车）需经架构评审
