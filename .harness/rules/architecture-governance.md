# 架构治理规则

> 约束 AI Agent 不得破坏系统分层架构。所有代码修改必须在正确的架构层内进行。
> 实操技能：`.harness/skills/api-design/SKILL.md`

---

## 1. 分层架构定义

```
Controller → Service(接口) → ServiceImpl → Mapper → DB
     ↕            ↕               ↕
  DTO/VO       Result<T>      Entity(BaseEntity)
```

| 层 | 职责 | 禁止 |
|---|---|---|
| Controller | 参数校验、调用 Service、返回 `Result<T>` | 写业务逻辑、调 Mapper、直接返回 Entity、Entity 作为入参 |
| Service 接口 | 定义业务契约（纯手写接口，不继承 `IService`） | 包含实现代码 |
| ServiceImpl | 业务编排、事务管理，直接实现 Service 接口 | 调其他模块 Mapper、在事务中发送普通 MQ 消息 |
| Mapper | `extends BaseMapper<Entity>`，复杂 SQL 写 XML | 被 Controller 直接调用 |
| Entity | 继承 `BaseEntity`，`@TableName` 映射 | 返回给前端、作为 Controller 入参、包含基础设施逻辑（直接调 Mapper/Redis/MQ，参见 ddd-governance.md） |

---

## 2. 禁止事项

```yaml
禁止:
  - Controller 直接调用 Mapper
  - Controller 编写业务逻辑（if/else 分支、金额计算、状态判断等）
  - Service 跨模块直接操作其他模块的 Mapper（cart→product Mapper 只读除外）
  - 跨模块直接访问数据库表（每个模块只操作自己的表）
  - Entity 直接返回给前端（必须转 VO）
  - Entity 作为 Controller 入参（必须用 DTO/Request 接收，防止前端篡改 id/createTime/deleted 等字段）
  - Entity 包含基础设施逻辑（直接调 Mapper/Redis/MQ，领域行为应写在 Entity 中，参见 ddd-governance.md）
  - DTO/Entity 混用（入参用 DTO/Request，返回用 VO）
  - for 循环中调用数据库查询（使用批量查询 + Map 关联）
  - for 循环中逐条插入/更新（使用 MyBatis-Plus 批量操作 saveBatch/updateBatchById 或 XML 批量 SQL）
  - for 循环中调用 RPC/MQ 发送
  - Controller 通过 @Autowired 注入其他模块的 Service
  - 在 static 方法中使用 Spring Bean（会导致 null pointer）
  - 在 @Transactional 方法中使用 RocketMQTemplate.convertAndSend() 发送普通消息（事务回滚后消息无法撤回）
```

---

## 3. 强制要求

```yaml
必须:
  - Controller 只负责协议转换：接收 DTO/Request → 调 Service → 返回 Result<VO>
  - ServiceImpl 直接实现 Service 接口，手动注入所需 Mapper（不继承 MyBatis-Plus ServiceImpl）
  - ServiceImpl 负责业务编排，通过构造器注入依赖
  - 所有接口统一返回 Result<T>（common-core 提供）
  - 所有业务异常统一抛 BusinessException（使用 ResultCode 枚举）
  - 所有写操作必须幂等（支付回调、消息消费、库存扣减）
  - 使用 @RequiredArgsConstructor 进行依赖注入（禁止 @Autowired 字段注入）
  - DTO/VO 使用 Java record（输入 DTO 可保留 @Data 用于校验注解，新增 DTO/Request 统一使用 record，已有 @Data 类逐步迁移）
  - MQ 消息体必须使用 Java record 定义（禁止使用 String/Map 传递业务数据），放在 mq/ 包下以 XxxMessage 命名
  - Entity 使用 @Data + @TableName，继承 BaseEntity
  - 事务注解 @Transactional(rollbackFor = Exception.class) 加在 ServiceImpl 方法上
  - ServiceImpl 方法操作 2 张及以上表时必须加 @Transactional
  - 事务内发消息必须使用事务消息（sendMessageInTransaction）或将消息发送移到事务提交后（@TransactionalEventListener）
```

---

## 4. 模块间调用规则

| 调用方式 | 允许 | 说明 |
|---|---|---|
| 通过 Service 接口调用 | ✅ | `orderService.createOrder()` |
| 通过 MQ 异步通信 | ✅ | `order-stock-preoccupy`、`payment-callback` |
| 直接调其他模块 Mapper | ❌ | cart→product Mapper(只读) 除外 |
| 直接操作其他模块数据库表 | ❌ | 每个模块只操作自己的表 |
| Controller 跨模块调 Service | ❌ | Controller 只调本模块 Service |
| admin Controller 调其他模块 Service | ⚠️ 临时允许 | **边界债务**：当前单体架构下允许，后续逐步收敛到 Facade/Application API，为微服务拆分做准备 |

---

## 5. DTO/VO/Entity 分离

```
请求 → Request/DTO（入参，可含校验注解）
响应 → VO（出参，Java record 不可变）
持久化 → Entity（继承 BaseEntity，不暴露到 Controller 层）
```

| 类型 | 位置 | 注解 | 用途 |
|---|---|---|---|
| Request/DTO | `dto/` 包 | `@Data` 或 record | 接收请求参数（新增统一使用 record） |
| VO | `vo/` 包 | Java record | 返回给前端 |
| Entity | `entity/` 包 | `@Data` `@TableName` | 数据库映射，禁止作为 Controller 入参 |
| 消息体 | `mq/` 包 | Java record | MQ 消息传输，以 `XxxMessage` 命名，禁止使用 String/Map |

---

## 6. 事务边界

- 事务在单个 ServiceImpl 方法内，不跨模块
- 跨模块一致性通过 RocketMQ 事务消息/异步消息保证
- 禁止大事务（单事务操作超过 3 张表需拆分）
- 禁止在事务中调用外部 RPC/HTTP 接口
- 禁止在事务中 sleep/wait
- 禁止在事务中使用 `convertAndSend` 发送普通 MQ 消息（事务回滚后消息无法撤回）
- 事务内发消息必须使用事务消息（`sendMessageInTransaction`）或将消息发送移到事务提交后（`@TransactionalEventListener`）

---

## 7. 幂等性规范

| 场景 | 幂等策略 | 说明 |
|---|---|---|
| 支付回调 | Redis `setIfAbsent` + DB 状态判断 | 双重保障，Redis TTL ≥ 5 分钟 |
| MQ 消息消费 | Redis `setIfAbsent` + DB 唯一索引/状态判断 | 禁止仅依赖 Redis TTL，必须有 DB 兜底 |
| 库存扣减 | 业务唯一键（如 `orderId + skuId`）作为幂等键 | 禁止使用 `hashCode` 作为 fingerprint（存在碰撞风险） |
| 优惠券领取 | Redis `setIfAbsent` + DB 唯一索引 | 防止重复领取 |

```yaml
必须:
  - 所有 MQ Consumer 必须实现幂等（无例外）
  - Redis 幂等键 TTL 必须大于消息最大重试周期
  - 幂等键使用业务唯一标识（orderId、paymentNo 等），禁止使用 hashCode
  - 消费失败时必须删除 Redis 幂等键，允许重试

禁止:
  - MQ Consumer 仅依赖 Redis TTL 做幂等（必须有 DB 兜底）
  - 使用 hashCode/fingerprint 作为幂等键
  - MQ Consumer 不实现幂等处理
```

---

## 8. Redis 使用规范

```yaml
必须:
  - Key 命名格式：模块:业务:标识（如 payment:callback:{paymentNo}）
  - Key 常量统一管理：每个模块提供 XxxRedisKeyConstants 类
  - 幂等键前缀统一：mq:consumed:{topic}:{bizId}
  - 设置合理的 TTL，避免 Key 永不过期

禁止:
  - 在代码中硬编码 Redis Key 字符串
  - 不设 TTL 的 Key 写入（除非有明确业务理由）
```
