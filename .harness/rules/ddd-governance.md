# DDD 领域驱动设计治理规则

> 约束 AI Agent 的领域建模、聚合设计、服务分层。确保代码向微服务拆分方向演进。
> 实操技能：`.harness/skills/ddd-design/SKILL.md` `.harness/skills/domain-modeling/SKILL.md`

---

## 1. 领域边界

### 1.1 当前领域划分

```
yuemo-mall
├── 用户域 (yuemo-user)     — 注册/登录/个人信息/地址/角色
├── 商品域 (yuemo-product)  — 商品/SKU/分类/品牌/评价/标签/规格/搜索词/库存
├── 订单域 (yuemo-order)    — 订单创建/状态流转/超时处理
├── 支付域 (yuemo-payment)  — 支付单/回调/退款
├── 营销域 (yuemo-promotion) — 优惠券发放/领取/使用
└── 购物车域 (yuemo-cart)   — 购物车 Redis+MySQL
```

### 1.2 模块依赖关系

```
user / product / promotion (底层，无业务模块依赖)
         │
         ├──> cart (依赖 product)
         │
         └──> order (依赖 product + cart)
                  │
                  └──> payment (依赖 order + user)
```

```yaml
规则:
  - 每个领域拥有独立的数据主权（自己的数据库表）
  - 领域间通过 Service 接口或 MQ 事件通信
  - 禁止跨领域直接操作数据库
  - 禁止领域间循环依赖
  - 每个领域应具备独立拆分为微服务的能力
  - 依赖方向必须单向，自下而上
```

---

## 2. 分层职责

```
adapter (Controller)
    │
    ▼
application (Service/ServiceImpl)
    │
    ▼
domain (Entity + 业务逻辑)
    │
    ▼
infrastructure (Mapper + 外部集成)
```

### 2.1 各层职责

| 层 | 当前对应 | 职责 | 禁止 |
|---|---|---|---|
| Adapter | Controller | 协议转换、参数校验、返回 VO | 写业务逻辑、调 Mapper |
| Application | ServiceImpl | 业务编排、事务管理、调领域服务 | 包含领域规则（应在 Entity/DomainService） |
| Domain | Entity + domain service | 核心业务规则、状态校验 | 依赖 Controller、依赖 Mapper |
| Infrastructure | Mapper + Redis + MQ | 数据持久化、缓存、消息 | 包含业务逻辑 |

### 2.2 依赖方向

```
Controller → Service(接口) → ServiceImpl → Mapper
     │            │              │
     ▼            ▼              ▼
   DTO/VO     Domain逻辑      Entity
```

**依赖必须单向，禁止反向依赖。**

---

## 3. 聚合设计

### 3.1 当前聚合根

| 领域 | 聚合根 | 内部实体 |
|---|---|---|
| 用户域 | User | Address, UserRole |
| 商品域 | Product | ProductSku, ProductReview, ProductTagRel |
| 商品域 | Category | — |
| 商品域 | Brand | — |
| 商品域 | ProductTag | — |
| 商品域 | ProductSpecTemplate | ProductSpecValue |
| 商品域 | SearchKeyword | — |
| 订单域 | Order | OrderItem, OrderLog |
| 支付域 | Payment | — |
| 营销域 | Coupon | UserCoupon |
| 购物车域 | CartItem(MySQL) + Redis Hash | — |

### 3.2 聚合规则

```yaml
规则:
  - 聚合根拥有全局唯一标识（ID）
  - 外部只能引用聚合根，不能直接引用内部实体
  - 聚合根负责内部实体的生命周期和一致性
  - 一次事务只修改一个聚合
  - 聚合间通过 ID 引用，不持有对象引用

示例:
  - Order 是聚合根，OrderItem 是内部实体
  - 外部通过 orderId 关联 OrderItem，不直接持有 OrderItem 对象
  - 修改 Order 状态时，OrderLog 作为内部实体一并创建
```

### 3.3 领域模型成熟度评估

```yaml
充血模型（良好实践）:
  - Order: 7个领域方法（pay/ship/confirmReceive/cancel/verifyOwnership/ensureStatus/canDelete）
  - Payment: 5个领域方法（markSuccess/markFailed/markRefunded/verifyOwnership/isPending/isSuccess）

贫血模型（需改进）:
  - Product: 缺少上下架领域方法（如 publish/unpublish）
  - ProductSku: 缺少库存扣减/释放领域方法（如 deductStock/releaseStock）
  - User: 缺少余额变更领域方法（如 deductBalance/rechargeBalance）
  - Coupon: 缺少领取/使用规则校验领域方法（如 receive/use）
  - CartItem: 缺少数量上限校验领域方法
```

---

## 4. 仓储模式

```yaml
当前实现:
  - Mapper extends BaseMapper<Entity>（MyBatis-Plus）
  - 复杂查询在 ServiceImpl 中通过 LambdaQueryWrapper 构建

演进方向:
  - Repository 接口定义在 domain 层
  - Repository 实现在 infrastructure 层
  - domain 层只依赖 Repository 接口（依赖倒置）

当前约束:
  - ServiceImpl 通过 Mapper 访问数据库
  - 禁止 Controller 直接调用 Mapper
  - 复杂 SQL 写在 XML Mapper 中
  - 简单查询用 LambdaQueryWrapper
```

---

## 5. 领域事件

### 5.1 事件清单

| Topic | 生产者 | 消费者 | 消息体 | 发送方式 |
|---|---|---|---|---|
| `order-stock-preoccupy` | OrderServiceImpl | StockPreoccupyConsumer(product) | `Map<Long, Integer>`(skuId→数量) | 事务消息 |
| `order-stock-release` | OrderServiceImpl | StockReleaseConsumer(product) | `Map<Long, Integer>`(skuId→数量) | 普通消息 |
| `payment-callback` | PaymentServiceImpl, BalancePayHandler | PaymentCallbackConsumer(order) | `String`(orderNo) | 普通消息 |
| `payment-refund` | PaymentServiceImpl | OrderRefundConsumer(order) + PaymentRefundConsumer(payment) | `RefundMessage`(orderId,userId,amount) | 普通消息 |
| `cart-sync` | CartServiceImpl | CartSyncConsumer(cart) | `CartSyncMessage`(多态,6种子类型) | 普通消息 |

### 5.2 事件规则

```yaml
规则:
  - 领域事件命名: {领域}-{事件}（如 payment-callback）
  - 事件由聚合根产生
  - 事件消费者在 application 层处理
  - 事件必须包含聚合根 ID 和事件时间

幂等方案:
  - 所有消费者统一使用 Redis setIfAbsent 实现幂等
  - TTL 统一为 10 分钟
  - 库存相关消费者额外使用 fingerprint（基于消息内容 hashCode）
```

### 5.3 特殊机制

```yaml
事务消息（order-stock-preoccupy）:
  - OrderServiceImpl 使用 RocketMQ 事务消息发送库存预占请求
  - OrderStockTransactionListener 实现 executeLocalTransaction/checkLocalTransaction
  - 本地事务：创建订单成功则 COMMIT，失败则 ROLLBACK
  - 确保订单创建与库存扣减的最终一致性

购物车异步落库（cart-sync）:
  - 购物车采用 Redis 先写 + MQ 异步落库 MySQL 的三层架构
  - CartSyncMessage 使用 Jackson 多态序列化（@JsonTypeInfo + @JsonSubTypes）
  - 6 种子类型: ADD/UPDATE/REMOVE/SELECT_ALL/CLEAR_SELECTED/CLEAR_ALL
  - 死信队列: %DLQ%yuemo-cart-consumer，由 CartSyncDeadLetterConsumer 记录日志

双消费者 Topic（payment-refund）:
  - order 模块（yuemo-order-refund-consumer）: 更新订单状态为已退款
  - payment 模块（yuemo-payment-consumer）: 退回用户余额
  - RocketMQ 广播模式，各自消费组独立消费
```

---

## 6. 防腐层

### 6.1 当前现状

```yaml
状态: 单体多模块，模块间存在同步强耦合调用，暂未引入防腐层

跨模块 Service 同步调用:
  - order → cart: CartService.clearSelected()（创建订单后清空购物车）
  - order → product: ProductService.getProductById()（查询商品信息）
  - order → product: SkuService.getSkuById()（查询 SKU 信息）
  - payment → order: OrderService.getOrderByIdAndUserId()（查询订单金额和状态）
  - payment → user: UserService.getBalance/deductBalance()（余额支付）

跨模块 Entity 泄露:
  - order → product.entity.Product, product.entity.ProductSku
  - cart → product.entity.Product, product.entity.ProductSku
  - payment → order.entity.Order, order.enums.OrderStatus
```

### 6.2 演进方向

```yaml
微服务拆分时需引入防腐层（ACL）:
  - 模块间通过 API/事件通信时，需添加防腐层
  - 防腐层负责: 协议转换、数据映射、降级/熔断
  - 外部模型不侵入内部领域模型

具体改进:
  1. 引入 ACL: 为跨模块调用建立 Facade 接口，返回本上下文的 DTO
     示例: order/acl/ProductFacade.java 封装对 ProductService 的调用
  2. 消除 Entity 泄露: 跨模块引用替换为本上下文的 DTO
     示例: OrderServiceImpl 中的 Product → ProductSummaryDTO
  3. 事件替代同步调用:
     - order → cart.clearSelected() → Order 发布 OrderCreatedEvent，Cart 订阅
     - payment → order.getOrderByIdAndUserId() → Order 发布 OrderAwaitingPaymentEvent
  4. 共享枚举提取: OrderStatus 等共享枚举提取到 common-core
```

---

## 7. 已知技术债务

```yaml
跨模块 Entity 泄露（违反限界上下文隔离）:
  - OrderServiceImpl: 直接引用 Product, ProductSku 实体
  - CartServiceImpl: 直接引用 Product, ProductSku 实体
  - PaymentServiceImpl: 直接引用 Order 实体, OrderStatus 枚举

贫血模型 Entity（缺少领域方法）:
  - Product: 缺少上下架、库存校验逻辑
  - ProductSku: 缺少库存扣减/释放逻辑
  - User: 缺少余额变更逻辑
  - Coupon: 缺少领取/使用规则校验
  - CartItem: 缺少数量上限校验

缺少 DDD 标准分层:
  - 无 domain/ 包（领域层聚合根、实体、值对象、领域服务）
  - 无 Repository 抽象层（当前 Mapper 直接暴露给 Service）
```

---

## 8. 新增领域功能检查清单

AI Agent 新增功能时，必须确认：

1. 新功能属于哪个现有领域？是否需要新建领域？
2. 新增的数据库表归属于哪个领域模块？
3. 新增 Entity 的聚合根是什么？内部实体是什么？
4. 聚合间通过什么方式引用（ID 还是对象）？
5. 是否一次事务修改了多个聚合？如果是，需改用 MQ 最终一致性。
6. 新增代码是否遵守了分层依赖方向（Controller → Service → Mapper）？
7. 是否引入了循环依赖？
8. 是否存在跨模块直接引用 Entity？（应通过 DTO 隔离）
9. 新增的领域事件是否包含聚合根 ID 和事件时间？
10. 新增的 MQ 消费者是否实现了幂等？
11. 跨模块调用是否通过 Service 接口而非实现类？
