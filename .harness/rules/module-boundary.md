# 模块边界治理规则

> 约束 AI Agent 不得跨模块污染。每个模块拥有明确的数据主权和依赖边界。
> 实操技能：`.harness/skills/microservice-readiness/SKILL.md`

---

## 1. 模块数据所有权

| 模块 | 拥有表 | 只读访问 |
|---|---|---|
| yuemo-user | `yu_user`、`yu_address`、`yu_user_role` | — |
| yuemo-product | `yu_product`、`yu_product_sku`、`yu_category`、`yu_brand`、`yu_product_review`、`yu_product_tag`、`yu_product_tag_rel`、`yu_search_keyword`、`yu_product_spec_template`、`yu_product_spec_value` | — |
| yuemo-order | `yu_order`、`yu_order_item`、`yu_order_log` | — |
| yuemo-payment | `yu_payment` | — |
| yuemo-cart | `yu_cart_item`、Redis `cart:{userId}` | — |
| yuemo-promotion | `yu_coupon`、`yu_user_coupon` | — |

**每个模块只操作自己拥有的表，任何跨模块写操作都是禁止的。**

---

## 2. 模块依赖白名单

```yaml
yuemo-user:
  允许依赖: [common-core, common-security, common-mybatis]
  禁止依赖: [yuemo-product, yuemo-order, yuemo-payment, yuemo-cart, yuemo-promotion]
  暴露服务: UserService(查询用户、扣减余额), AddressService

yuemo-product:
  允许依赖: [common-core, common-security, common-mybatis]
  禁止依赖: [yuemo-user, yuemo-order, yuemo-payment, yuemo-cart, yuemo-promotion]
  暴露服务: ProductService, SkuService, SearchService, ReviewService, BrandService
  接收MQ: order-stock-preoccupy, order-stock-release

yuemo-order:
  允许依赖: [common-core, common-security, common-mybatis, yuemo-product(Service), yuemo-cart(Service)]
  禁止依赖: [yuemo-user, yuemo-payment, yuemo-promotion]
  暴露服务: OrderService
  发送MQ: order-stock-preoccupy, order-stock-release
  接收MQ: payment-callback, payment-refund

yuemo-payment:
  允许依赖: [common-core, common-security, common-mybatis, yuemo-order(Service), yuemo-user(Service)]
  禁止依赖: [yuemo-product, yuemo-cart, yuemo-promotion]
  暴露服务: PaymentService
  发送MQ: payment-callback, payment-refund

yuemo-cart:
  允许依赖: [common-core, common-security, common-mybatis, yuemo-product(Service)]
  禁止依赖: [yuemo-user, yuemo-order, yuemo-payment, yuemo-promotion]
  暴露服务: CartService
  发送MQ: cart-sync
  接收MQ: cart-sync(自身消费落库)

yuemo-promotion:
  允许依赖: [common-core, common-security, common-mybatis]
  禁止依赖: [yuemo-product, yuemo-order, yuemo-payment, yuemo-cart, yuemo-user]
  暴露服务: CouponService

yuemo-gateway:
  允许依赖: [common-core, common-security]
  禁止依赖: [所有业务模块]
  职责: 认证过滤、限流、熔断（纯 Filter，无业务逻辑）

yuemo-admin:
  允许依赖: [common-core, common-security, common-mybatis, yuemo-product, yuemo-promotion]
  禁止依赖: [yuemo-user, yuemo-order, yuemo-payment, yuemo-cart]
```

---

## 3. 跨模块调用规则

| 规则 | 说明 |
|---|---|
| ✅ 允许 | 模块通过 Service 接口调用其他模块 |
| ✅ 允许 | 通过 RocketMQ 异步通信 |
| ❌ 禁止 | 跨模块直接调用 Mapper |
| ❌ 禁止 | 跨模块直接操作数据库表（INSERT/UPDATE/DELETE） |
| ❌ 禁止 | 循环依赖（A→B→A） |
| ❌ 禁止 | Controller 跨模块调用 Service |
| ❌ 禁止 | 往其他模块的数据库表写数据 |
| ❌ 禁止 | 跨模块直接引用 Entity（应通过 DTO/VO 隔离） |

---

## 4. 已知耦合问题

```yaml
跨模块 Entity 泄露（违反限界上下文隔离，微服务拆分时需修复）:
  - OrderServiceImpl: 直接引用 Product, ProductSku 实体
  - CartServiceImpl: 直接引用 Product, ProductSku 实体
  - PaymentServiceImpl: 直接引用 Order 实体, OrderStatus 枚举

Admin 模块使用 Entity 接收请求参数（违反 API 治理和模块边界）:
  - AdminProductController: @RequestBody ProductSku sku, @RequestBody Brand brand
  - AdminCouponController: @RequestBody Coupon coupon
  应改为: 使用 Admin 模块自己的 DTO，通过 Service 接口传递
```

---

## 5. 新增模块检查清单

当 AI Agent 新增模块或功能时，必须回答：

1. 新代码属于哪个现有模块？如不属于任何模块，是否需要新建模块？
2. 新代码操作哪些数据库表？这些表是否属于该模块？
3. 新代码调用了哪些其他模块？是否在白名单内？
4. 新代码是否引入了循环依赖？
5. 新增 Controller 是否只调用了本模块 Service？
6. 是否存在跨模块直接引用 Entity？（应通过 DTO 隔离）
7. 新增的 MQ 消费者是否实现了幂等？
8. 新增代码是否使用了其他模块的 Entity 作为请求参数？

---

## 6. 模块依赖图

```
  ┌───────────┐  ┌───────────┐  ┌───────────┐
  │   user    │  │  product  │  │ promotion │
  │ (底层模块) │  │ (底层模块) │  │ (独立模块) │
  └─────┬─────┘  └─────┬─────┘  └───────────┘
        │              │
        │         ┌────┴────┐
        │         ▼         ▼
        │   ┌──────────┐  ┌──────────┐
        │   │   cart   │  │  order   │
        │   │→product  │  │→product  │
        │   │ (Service)│  │→cart     │
        │   └──────────┘  └────┬─────┘
        │                     │
        └─────────────────────┼─────┐
                              ▼     ▼
                          ┌──────────┐
                          │ payment  │
                          │→order    │
                          │→user     │
                          └──────────┘

  ┌───────────┐
  │   admin   │ → product, promotion
  └───────────┘

  ┌───────────┐
  │  gateway  │ → common-* only
  └───────────┘
```

---

## 7. Common 模块说明

```yaml
common-core:
  职责: 统一响应(Result/ResultCode/PageResult)、异常体系(BusinessException/GlobalExceptionHandler)、
       基础Entity(BaseEntity)、XSS过滤
  约束: 不依赖任何业务模块，不包含业务逻辑

common-security:
  职责: JWT工具(JwtTokenProvider)、认证常量(AuthRedisKeyConstants)、密码编码
  约束: 不依赖任何业务模块

common-mybatis:
  职责: MyBatis-Plus 自动配置、分页插件、自动填充
  约束: 不依赖任何业务模块
```

---

## 8. 为微服务拆分做准备

当前为单体多模块架构，但代码层面必须保持微服务就绪：

- 模块间通过 Service 接口通信，而非直接调 Mapper
- 数据库表归属清晰，不跨模块 join
- 异步通信统一走 RocketMQ
- 每个模块拥有独立的 `controller`/`service`/`mapper`/`entity` 包

### 拆分优先级建议

```yaml
第一批（可独立拆分，无跨模块同步调用）:
  - yuemo-promotion: 完全独立，不依赖任何业务模块
  - yuemo-product: 底层模块，仅被其他模块调用，不被其他模块依赖

第二批（需先解耦）:
  - yuemo-cart: 需先消除对 product.entity 的引用，改用 DTO
  - yuemo-user: 底层模块，需先提取余额操作为独立服务

第三批（依赖最多，最后拆分）:
  - yuemo-order: 依赖 product + cart，需先改为事件驱动
  - yuemo-payment: 依赖 order + user，需先改为事件驱动

拆分前必须修复:
  - 消除所有跨模块 Entity 引用（改用 DTO）
  - 将同步 Service 调用改为 MQ 事件驱动
  - 共享枚举（如 OrderStatus）提取到 common-core
  - Admin 模块改用 Feign/API 调用替代直接依赖
```
