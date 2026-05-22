---
name: "domain-modeling"
description: "对商城业务进行领域建模分析，识别实体、值对象、聚合、状态机和领域事件。当需要深入理解业务或梳理业务规则时触发。设计新功能请使用 ddd-design skill。"
---

# 领域建模 Skill

对商城业务进行系统化的领域建模**分析**，产出实体关系、状态机和领域事件图。
> 本 skill 定位：**分析诊断型**（只读），用于理解现有领域。设计新功能或重构请使用 `ddd-design` skill。
> 治理规则：`.harness/rules/domain-modeling.md` `.harness/rules/ddd-governance.md` `.harness/rules/code-smell-governance.md`

## 触发条件

- 需要理解现有业务领域
- 业务规则梳理
- 用户要求输出业务模型
- 评估现有领域模型是否合理

> ⚠️ 设计新功能、重构现有领域 → 使用 `ddd-design` skill

## 依赖上下文

- `.harness/docs/business-domain.md` — 现有领域模型（权威数据源）
- `.harness/docs/module-responsibility.md` — 模块职责
- `.harness/docs/data-flow.md` — 核心数据流
- `.harness/rules/ddd-governance.md` — DDD 治理规则
- `.harness/rules/domain-modeling.md` — 充血模型治理规则
- `.harness/rules/code-smell-governance.md` — 魔法值治理（状态码相关）
- 相关 Entity 和数据库表结构
- `.harness/memory/decisions.md` — 架构决策（领域划分相关决策）
- `.harness/memory/anti-patterns.md` — 已知反模式（贫血模型等）
- `.harness/memory/architecture-history.md` — 架构演进记录

## 建模流程

### Step 0: 前置校验

```yaml
在开始建模前，必须完成以下检查:
  1. 已读取 .harness/docs/business-domain.md，确认当前领域划分
  2. 已读取 .harness/rules/domain-modeling.md，确认充血模型要求
  3. 已确认建模范围（哪个领域/哪些实体）
  4. 已确认本次建模是"分析现有领域"还是"设计新功能"
     - 分析现有 → 继续本 skill
     - 设计新功能 → 切换到 ddd-design skill
  5. 已读取 memory/decisions.md 和 memory/anti-patterns.md，确认无冲突
```

### Step 1: 实体识别

```yaml
方法:
  1. 梳理业务场景中的"名词"（用户、商品、订单...）
  2. 每个名词判断: 是否需要唯一标识？是否有生命周期？
  3. 有 → 实体，无 → 值对象

当前项目核心实体:
  用户域: User, Address
  商品域: Product, ProductSku, Category, Brand, Review
  库存域(商品子域): Product(库存字段), ProductSku(库存字段)
  订单域: Order, OrderItem, OrderLog
  支付域: Payment
  营销域: Coupon, UserCoupon
  购物车域: CartItem
```

### Step 2: 值对象识别

```yaml
方法:
  1. 无独立 ID、通过属性值判断相等
  2. 通常嵌入在实体内
  3. 应设计为不可变

当前项目值对象候选:
  - Address: 省+市+区+详情+邮编（可看作值对象嵌入订单快照）
  - 金额: 数值+币种
  - 规格: specIds + specText
  - 订单快照: 商品名+图片+价格（存在 OrderItem 中但本质是值对象）
```

### Step 3: 聚合边界

```yaml
原则:
  - 聚合根: 外部访问的唯一入口
  - 内部实体: 只能通过聚合根访问
  - 一次事务只修改一个聚合
  - 聚合间通过 ID 引用，不持有对象引用

当前项目聚合:
  User 聚合: User(根) → Address
  Product 聚合: Product(根) → ProductSku, Review
  Order 聚合: Order(根) → OrderItem, OrderLog
  Payment 聚合: Payment(根) → 自包含
  Coupon 聚合: Coupon(根) → UserCoupon
  Cart 聚合: Cart(Redis Hash Key) → CartItem(Redis field)

设计决策备注:
  - Cart 聚合根并非传统 DDD 实体，而是 Redis Hash 的组织方式
    (cart:{userId} 为 Key，skuId 为 field)
    外部通过 userId 访问整个购物车，符合聚合根语义
  - 库存属于商品域子域，库存扣减通过 Product 聚合根操作
    (ProductServiceImpl.updateStock 使用原子 SQL)
```

### Step 4: 状态机

```yaml
对每个有状态的实体，定义状态流转（必须包含状态码、触发条件、附加操作）:

订单状态:
  0-待支付 ──支付──→ 1-已支付 ──发货──→ 2-已发货 ──收货──→ 3-已完成
    │                                          │
    └──取消/30min超时──→ 4-已取消                │
                                                  └──退款──→ 5-已退款

  状态变更触发:
    0→4: 用户取消 / 30min超时 → 释放库存(RMQ order-stock-release)、释放优惠券
    0→1: 支付回调 → 记录 payTime
    1→2: 后台发货 → 填写物流单号、记录 deliveryTime
    2→3: 用户确认收货 → 记录 receiveTime
    3→5: 退款 → 恢复用户余额(MQ payment-refund)、释放库存(RMQ order-stock-release)
    3/4/5→删除: 用户删除 → 逻辑删除(deleted=1)

支付状态:
  0-待支付 → 1-已支付 → 3-已退款
                ↓
           2-支付失败

  状态变更触发:
    0→1(同步): 余额支付成功 → 即时扣余额、记录 payTime、发 MQ 通知订单
    0→1(异步): 第三方支付回调 → 验签、记录 payTime/第三方流水号、发 MQ 通知订单
    0→2: 支付失败 or 余额不足 → 记录失败原因
    1→3: 退款 → 记录 refundNo/refundReason、发 MQ 恢复余额+释放库存

优惠券状态:
  0-未使用 → 1-已使用(关联订单)
               → 2-已过期(超过end_time)

  状态变更触发:
    0→1: 订单支付成功后 → 关联 orderId
    0→2: 定时任务扫描(每天凌晨3点) → 超过 end_time

库存状态(商品子域):
  下单 → 预占库存(RMQ事务消息 order-stock-preoccupy)
       → 支付成功(COMMIT) → 库存实扣
       → 取消/超时(ROLLBACK) → 库存释放(order-stock-release)

  库存操作:
    预占: UPDATE yu_product SET stock = stock - #{quantity} WHERE id = #{productId} AND stock >= #{quantity}
    释放: UPDATE yu_product SET stock = stock + #{quantity} WHERE id = #{productId}
```

### Step 5: 领域事件

```yaml
识别: 聚合根的状态变更需要通知其他领域时 → 领域事件

当前领域事件:
  订单已创建 → 库存预占 (order-stock-preoccupy, RMQ事务消息)
  订单已取消/超时 → 库存释放 (order-stock-release, RMQ普通消息)
  支付已完成 → 订单状态更新 (payment-callback, RMQ普通消息)
  支付已退款 → 库存恢复 (payment-refund, RMQ普通消息)
  购物车已变更 → 数据同步 (cart-sync, RMQ普通消息, 自产自消)

事件规则:
  - 命名: {领域}-{事件}（如 payment-callback）
  - 由聚合根操作产生
  - 通过 RocketMQ 异步发布
  - 事件必须包含聚合根 ID 和事件时间
  - 消费者在 application 层处理
```

### Step 6: 实体行为设计（充血模型）

```yaml
对每个有状态的实体，设计领域行为方法（禁止贫血模型）:

Order 实体行为:
  - pay(payTime): 校验状态=0待支付 → 设为1已支付、记录payTime
  - cancel(): 校验状态=0待支付 → 设为4已取消
  - deliver(deliveryTime, trackingNo): 校验状态=1已支付 → 设为2已发货、记录deliveryTime
  - confirmReceive(receiveTime): 校验状态=2已发货 → 设为3已完成、记录receiveTime
  - canDelete(): 状态=3已完成 或 状态=4已取消

Payment 实体行为:
  - paySuccess(payTime, transactionNo): 校验状态=0待支付 → 设为1已支付
  - payFail(reason): 校验状态=0待支付 → 设为2支付失败
  - refund(refundNo, reason): 校验状态=1已支付 → 设为3已退款

UserCoupon 实体行为:
  - use(orderId): 校验状态=0未使用 → 设为1已使用、关联orderId
  - expire(): 校验状态=0未使用 → 设为2已过期

Product 实体行为(库存相关):
  - 库存扣减/恢复涉及 DB 乐观锁，放在 DomainService(ProductServiceImpl) 更合适
  - 参考: .harness/rules/domain-modeling.md 第4.2节

行为归属原则:
  - 状态校验和转换 → Entity 方法
  - 金额计算 → Entity 或 DomainService
  - 跨聚合编排 → Application Service (ServiceImpl)
  - 跨聚合计算 → DomainService
  - 外部服务调用 → Application Service
```

### Step 7: 验证与合规

```yaml
建模完成后，必须逐项检查:

  1. 新增 Entity 是否包含领域行为？
     - 只有 getter/setter → 重新设计，添加行为方法

  2. 状态流转逻辑在哪里？
     - ServiceImpl 中 → 移到 Entity 或 DomainService

  3. 业务规则在哪里？
     - ServiceImpl 中 → 移到 Entity 方法中

  4. 是否操作了其他聚合的内部实体？
     - 是 → 改为通过聚合根操作或 MQ 通信

  5. 新增功能是否修改了聚合边界？
     - 是 → 评估聚合设计是否合理

  6. 是否产生循环依赖？
     - 是 → 重新设计聚合关系

  7. 一次事务是否修改了多个聚合？
     - 是 → 改用 MQ 最终一致性

  8. 分层依赖方向是否正确？
     - Controller → Service → Mapper（单向，禁止反向）

  9. 状态机是否与代码逻辑一致？
     - 必须检查 ServiceImpl 中的实际状态流转

  10. 领域事件是否与 RocketMQ topic 一一对应？
      - 必须与实际 MQ 配置一致
```

## 输出格式

```markdown
## 领域建模报告

### 实体清单
| 实体 | 所属聚合 | 表名 | 关键属性 | 领域行为 |
|---|---|---|---|---|

### 值对象清单
| 值对象 | 所属实体 | 属性 |
|---|---|---|

### 聚合边界
{聚合名}: {聚合根} → {内部实体} → {值对象}

### 状态机
{实体名}: {状态码}-{状态名} → {状态码}-{状态名}（触发条件 | 附加操作）

### 领域事件
| 事件 | Topic | 触发条件 | 消费者 | 一致性 | 消息类型 |
|---|---|---|---|---|---|

### 领域关系图
{文字描述实体间关系}

### 合规检查
- [ ] Entity 包含领域行为（非贫血模型）
- [ ] 状态流转在 Entity 内部（非 ServiceImpl）
- [ ] 一次事务只修改一个聚合
- [ ] 聚合间通过 ID 引用
- [ ] 模块边界未破坏
- [ ] 不跨领域直接操作数据库
- [ ] 分层依赖方向正确
- [ ] 状态机与代码逻辑一致
- [ ] 领域事件与 MQ topic 对应
```

## 约束

- 建模基于实际数据库表结构，不凭空设计
- 不为建模而建模，只在需要时输出
- 状态机必须标注状态码，与 Entity 字段值对应（禁止魔法值）
- 状态机必须与代码逻辑一致（检查 ServiceImpl）
- 考虑现有模块边界，不跨模块设计聚合
- 领域事件必须与 RocketMQ topic 一一对应
- Entity 必须包含领域行为方法，禁止贫血模型（参考 domain-modeling.md）
- 本 skill 只做分析诊断，不直接生成代码；需要代码实现请切换到 ddd-design skill
