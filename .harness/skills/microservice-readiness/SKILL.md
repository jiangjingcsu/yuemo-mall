---
name: "microservice-readiness"
description: "评估当前单体多模块架构向微服务拆分的就绪度，识别阻碍拆分的耦合点。当讨论微服务拆分时触发。"
---

# 微服务就绪度分析 Skill

评估当前代码向微服务架构拆分的准备程度，识别阻碍拆分的耦合点并给出修复建议。
> 治理规则：`.harness/rules/module-boundary.md` `.harness/rules/ddd-governance.md`
> 联动 skill：`transaction-analysis`（跨模块事务分析）、`refactor-analysis`（重构影响评估）

## 触发条件

- 用户讨论微服务拆分
- 架构评审
- 新增功能前评估是否破坏拆分能力

## 依赖上下文

- `.harness/docs/module-responsibility.md` — 模块职责与边界
- `.harness/rules/module-boundary.md` — 模块依赖规则（含第6节就绪标准）
- `.harness/rules/ddd-governance.md` — DDD 治理
- `.harness/docs/data-flow.md` — 数据流和模块交互
- `.harness/docs/business-domain.md` — 业务领域模型
- 各模块 `pom.xml` — 实际 Maven 依赖声明
- 各模块 ServiceImpl 源码 — 实际跨模块调用
- `.harness/memory/decisions.md` — 架构决策（微服务拆分相关决策）
- `.harness/memory/anti-patterns.md` — 已知反模式（跨模块事务等）
- `.harness/memory/architecture-history.md` — 架构演进记录
- `.harness/memory/incidents.md` — 事故记录（跨模块事务相关事故）

## 分析流程

### Step 0: 前置校验

```yaml
在开始分析前，必须完成以下检查:
  1. 已读取 .harness/rules/module-boundary.md，确认当前模块依赖白名单
  2. 已读取 .harness/docs/data-flow.md，确认跨模块数据流
  3. 已确认分析范围（全模块 or 特定模块）
  4. 已确认本次是"就绪度评估"还是"拆分方案设计"
     - 就绪度评估 → 继续本 skill
     - 拆分方案设计 → 先完成本 skill 评估，再结合 refactor-analysis skill
  5. 已读取 memory/decisions.md，确认微服务拆分相关架构决策
  6. 已读取 memory/anti-patterns.md 和 memory/incidents.md，确认已知问题和事故记录
```

### Step 1: 代码级依赖扫描

```yaml
必须扫描实际代码，不能仅依赖文档推理:

  1. 扫描各模块 pom.xml 中的 Maven 依赖声明
  2. 扫描各模块 ServiceImpl 中的 import 语句，识别实际跨模块调用
  3. 对比 pom 声明与实际 import，识别幽灵依赖（声明了但未使用）
  4. 扫描 @Transactional 方法内的跨模块调用，识别跨模块事务

当前项目实际依赖关系（基于代码验证）:

  yuemo-user:
    pom 声明: common-* only
    实际 import: 无跨模块 import ✅
    被调用方: payment(UserService.deductBalance) ⚠️
    幽灵依赖: 无

  yuemo-product:
    pom 声明: common-* only
    实际 import: 无跨模块 import ✅
    被调用方: order(ProductService/SkuService), cart(ProductService/SkuService) ⚠️
    幽灵依赖: 无

  yuemo-order:
    pom 声明: yuemo-product, yuemo-cart
    实际 import: com.yuemo.product.service.ProductService/SkuService,
                 com.yuemo.product.entity.Product/ProductSku,
                 com.yuemo.cart.service.CartService
    幽灵依赖: 无
    跨模块事务: OrderServiceImpl.createOrder() 在 @Transactional 内
               调用 productService/skuService/cartService 🔴

  yuemo-payment:
    pom 声明: yuemo-order, yuemo-user
    实际 import: com.yuemo.order.service.OrderService,
                 com.yuemo.order.entity.Order,
                 com.yuemo.user.service.UserService
    幽灵依赖: 无 ✅
    跨模块事务: PaymentServiceImpl.createPayment() 在 @Transactional 内
               调用 orderService.getOrderByIdAndUserId() 🔴

  yuemo-cart:
    pom 声明: yuemo-product
    实际 import: com.yuemo.product.service.ProductService/SkuService,
                 com.yuemo.product.entity.Product/ProductSku
    幽灵依赖: 无
    Entity 共享: 直接引用 Product/ProductSku Entity ⚠️

  yuemo-promotion:
    pom 声明: common-* only
    实际 import: 无跨模块 import ✅
    幽灵依赖: 无
```

### Step 2: 模块独立性评估

```yaml
基于 Step 1 的代码扫描结果，对每个模块评估:

  yuemo-user:
    独立数据: yu_user, yu_address ✅
    被依赖: payment 通过 Service 调用 ⚠️
    MQ 通信: 无 MQ 依赖 ✅
    跨模块事务: payment 在事务内调用 UserService.deductBalance() 🔴
    拆分难度: 中等（需处理 payment 调用 + 事务解耦）

  yuemo-product:
    独立数据: 10 张表 ✅
    被依赖: order(Service+Entity), cart(Service+Entity) ⚠️
    Entity 共享: Product/ProductSku 被 order 和 cart 直接引用 ⚠️
    MQ 消费: order-stock-preoccupy, order-stock-release ✅（已走 MQ）
    跨模块事务: order 在事务内调用 productService/skuService 🔴
    拆分难度: 中等（需 API 化 + Entity 解耦 + 事务解耦）

  yuemo-order:
    独立数据: 3 张表 ✅
    依赖模块: product(Service+Entity), cart(Service) ⚠️
    被依赖: payment(Service+Entity) ⚠️
    Entity 共享: Order Entity 被 payment 直接引用 ⚠️
    MQ: 发送 preoccupy/release, 消费 payment-callback ✅
    跨模块事务: createOrder() 在事务内调 3 个外部 Service 🔴🔴
    拆分难度: 高（依赖多 + 跨模块事务严重）

  yuemo-payment:
    独立数据: yu_payment ✅
    依赖模块: order(Service+Entity), user(Service) ⚠️
    Entity 共享: Order Entity 直接引用 ⚠️
    MQ 发送: payment-callback, payment-refund ✅
    跨模块事务: createPayment() 在事务内调 orderService 🔴
    拆分难度: 中等（需处理 order 依赖 + user 依赖 + 事务解耦）

  yuemo-cart:
    独立数据: yu_cart_item + Redis ✅
    依赖模块: product(Service+Entity) ⚠️
    Entity 共享: Product/ProductSku 直接引用 ⚠️
    MQ: cart-sync 自产自消 ✅
    拆分难度: 低（仅需 API 化 + Entity 解耦）

  yuemo-promotion:
    独立数据: yu_coupon, yu_user_coupon ✅
    无模块依赖 ✅
    无跨模块事务 ✅
    拆分难度: 低 ⭐
```

### Step 3: 耦合点识别

```yaml
按严重度从高到低排列:

🔴 跨模块事务（拆分最大障碍）:

  OrderServiceImpl.createOrder():
    位置: yuemo-order/OrderServiceImpl.java
    问题: @Transactional 内调用 productService/skuService/cartService
    影响: 拆分后本地事务无法覆盖远程调用，需分布式事务
    修复: 商品查询改为 API 调用（事务外），
          购物车清理改为 MQ 异步（事务后），
          库存预占已走 RMQ 事务消息 ✅

  PaymentServiceImpl.createPayment():
    位置: yuemo-payment/PaymentServiceImpl.java
    问题: @Transactional 内调用 orderService.getOrderByIdAndUserId()
    影响: 拆分后本地事务无法覆盖远程调用
    修复: 订单状态校验改为 API 调用（事务外），
          或引入本地订单快照表

  PaymentServiceImpl.payByBalance()（最严重）:
    位置: yuemo-payment/PaymentServiceImpl.java
    问题: 一个事务内操作 user(扣余额) + payment(创建支付单) + order(更新状态)
          跨越 3 个模块的数据写操作
    影响: 拆分后需三阶段提交或 Saga 补偿
    修复: 拆分为: 1) 余额扣减(API) 2) 支付单创建(本地事务)
          3) 订单状态更新(MQ payment-callback)

🟡 Entity 共享耦合:

  Product/ProductSku 被 order + cart 直接引用:
    位置: order/CartServiceImpl import com.yuemo.product.entity.*
    问题: 拆分后不应共享 Entity 类，需各自维护 DTO
    影响: 字段变更需多服务同步修改
    修复: order/cart 模块定义本地 ProductDTO/SkuDTO，
          通过 API 调用获取后映射

  Order 被 payment 直接引用:
    位置: payment/PaymentServiceImpl import com.yuemo.order.entity.Order
    问题: 同上
    修复: payment 模块定义本地 OrderDTO

✅ MQ 耦合（正常，无需修复）:

  order → order-stock-preoccupy → product: 事务消息，设计正确
  order → order-stock-release → product: 普通消息，设计正确
  payment → payment-callback → order: 普通消息，设计正确
  payment → payment-refund → PaymentRefundConsumer(恢复余额) + OrderRefundConsumer(释放库存): 普通消息，设计正确
  cart → cart-sync → cart(自消费): 设计正确
```

### Step 4: 拆分路径建议

```yaml
建议拆分顺序（从最独立到最依赖）:

  第一阶段: yuemo-promotion
    前置条件: 无
    改造量: 仅需独立部署 + 暴露 API
    风险: 低

  第二阶段: yuemo-cart
    前置条件:
      - cart → product: Service 调用改为 API
      - cart → product: Entity 共享改为本地 DTO
    改造量: 中等
    风险: 低

  第三阶段: yuemo-user
    前置条件:
      - payment → user: deductBalance 改为 API 调用
      - payment: payByBalance 事务拆分（见 Step 3）
    改造量: 中等
    风险: 中（余额扣减涉及资金）

  第四阶段: yuemo-product
    前置条件:
      - order → product: Service 调用改为 API
      - order → product: Entity 共享改为本地 DTO
      - order: createOrder 事务拆分（见 Step 3）
    改造量: 中等
    风险: 中（库存扣减需保证一致性）

  第五阶段: yuemo-payment
    前置条件:
      - payment → order: Service 调用改为 API
      - payment → order: Entity 共享改为本地 DTO
      - payment → user: deductBalance 改为 API 调用
      - payment: payByBalance 三模块事务拆分
    改造量: 高
    风险: 高（支付涉及资金流转）

  第六阶段: yuemo-order
    前置条件:
      - order → product/cart: 全部改为 API/MQ
      - order: createOrder 事务完全拆分
    改造量: 高
    风险: 高（订单是核心业务，依赖最多）

拆分前必须先解决（阻塞项）:
  1. payByBalance 三模块事务 → 拆分为 API + 本地事务 + MQ
  2. createOrder 跨模块事务 → 商品查询移出事务，购物车清理改为 MQ
  3. createPayment 跨模块事务 → 订单校验移出事务
  4. Entity 共享 → 各模块定义本地 DTO
```

### Step 5: 就绪度评分

```yaml
评分标准（每个维度 0-10 分）:
  10: 完全就绪，可直接拆分
  7-9: 基本就绪，需少量改造
  4-6: 需要中等改造
  1-3: 需要大量改造，耦合严重
  0: 无法拆分

评分维度:
  数据独立性: 模块是否拥有独立数据，不跨模块操作表
  服务独立性: 模块是否通过 Service 接口调用（非 Mapper/Entity 直接引用）
  事务独立性: 模块 @Transactional 内是否包含跨模块调用
  Entity 独立性: 模块是否共享其他模块的 Entity 类
  MQ 通信: 模块间异步通信是否已走 MQ

模块级评分:

  yuemo-promotion:
    数据独立性: 10 | 服务独立性: 10 | 事务独立性: 10 | Entity独立性: 10 | MQ通信: 10
    综合: 10/10 ⭐

  yuemo-cart:
    数据独立性: 10 | 服务独立性: 8 | 事务独立性: 10 | Entity独立性: 6 | MQ通信: 10
    综合: 8.8/10

  yuemo-user:
    数据独立性: 10 | 服务独立性: 10 | 事务独立性: 6 | Entity独立性: 10 | MQ通信: 10
    综合: 9.2/10
    扣分项: payment 在事务内调用 UserService.deductBalance()

  yuemo-product:
    数据独立性: 10 | 服务独立性: 8 | 事务独立性: 4 | Entity独立性: 4 | MQ通信: 9
    综合: 7.0/10
    扣分项: order 在事务内调用 productService/skuService(-6),
            Product/ProductSku Entity 被 order+cart 共享(-6)

  yuemo-payment:
    数据独立性: 10 | 服务独立性: 8 | 事务独立性: 2 | Entity独立性: 6 | MQ通信: 9
    综合: 7.0/10
    扣分项: createPayment 事务内调 orderService(-8),
            payByBalance 三模块事务(-8),
            Order Entity 共享(-4)

  yuemo-order:
    数据独立性: 10 | 服务独立性: 6 | 事务独立性: 1 | Entity独立性: 4 | MQ通信: 8
    综合: 5.8/10
    扣分项: createOrder 事务内调 3 个外部 Service(-9),
            Product/ProductSku Entity 共享(-6),
            依赖 product+cart 两个模块(-4)

总体就绪度: 7.8/10
  - promotion/cart/user 可直接拆分
  - product/payment 需中等改造
  - order 需大量改造（事务解耦是核心难点）
```

### Step 6: 验证与合规

```yaml
分析完成后，必须逐项检查:

  1. pom 声明与实际 import 是否一致？
     - 不一致 → 标记为幽灵依赖，建议清理

  2. 所有 @Transactional 方法是否检查了跨模块调用？
     - 遗漏 → 补充到耦合点清单

  3. Entity 共享是否全部识别？
     - 遗漏 → 扫描所有跨模块 import ...entity.*

  4. 评分是否与耦合点数量一致？
     - 有严重耦合但评分高 → 重新评估

  5. 拆分路径是否考虑了事务依赖？
     - 未考虑 → 重新排序，有跨模块事务的模块放后

  6. 是否与 module-boundary.md 第6节就绪标准对齐？
     - 未对齐 → 补充缺失维度

  7. 是否联动了 transaction-analysis skill？
     - 有跨模块事务 → 引用 transaction-analysis 的分析结果
```

## 输出格式

```markdown
## 微服务就绪度报告

### 代码级依赖验证
| 模块 | pom声明依赖 | 实际import依赖 | 幽灵依赖 |
|---|---|---|---|

### 模块独立性评分
| 模块 | 数据独立 | 服务独立 | 事务独立 | Entity独立 | MQ通信 | 综合 |
|---|---|---|---|---|---|---|
| (0-10) | (0-10) | (0-10) | (0-10) | (0-10) | (0-10) | (0-10) |

### 耦合点清单
| 耦合类型 | 严重度 | 位置（文件:行号） | 影响 | 修复方案 |
|---|---|---|---|---|
| 跨模块事务 | 🔴 | OrderServiceImpl.createOrder():L54 | ... | ... |
| 跨模块事务 | 🔴 | PaymentServiceImpl.payByBalance():Lxx | ... | ... |
| Entity共享 | 🟡 | CartServiceImpl import Product | ... | ... |
| 幽灵依赖 | 🟡 | yuemo-payment/pom.xml:yuemo-user | ... | ... |

### 跨模块事务分析
| 方法 | 事务内跨模块调用 | 涉及模块 | 修复策略 |
|---|---|---|---|

### Entity 共享分析
| Entity | 引用模块 | 修复方案 |
|---|---|---|

### 拆分路径建议
1. {第一阶段}: {模块} — 前置条件: {列表}
2. {第二阶段}: {模块} — 前置条件: {列表}

### 阻塞项
- [ ] {必须修复才能拆分的耦合点}

### 总体就绪度
{X}/10 — {一句话总结}
```

## 约束

- 不做实际拆分（只评估和建议）
- 基于当前代码真实状态评估，必须扫描 pom.xml 和 import 验证，不猜测
- 拆分建议考虑业务影响最小化
- 跨模块事务是拆分最大障碍，必须逐一识别并给出修复方案
- Entity 共享是拆分常见遗漏点，必须扫描所有跨模块 entity import
- 幽灵依赖（pom 声明但未使用）必须识别并建议清理
- 评分必须有明确标准（0-10 分制 + 扣分项），禁止模糊打分
- 有跨模块事务时，应联动 `transaction-analysis` skill 进行深入分析
