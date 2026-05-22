# 模块职责与边界

> AI Agent 理解每个模块的所有权和边界的权威参考。
> 交叉引用: rules/module-boundary.md（模块边界治理规则）、rules/mq-governance.md（MQ Topic 详细规范）

---

## 1. 模块总览

```
yuemo-backend/
├── yuemo-common/
│   ├── common-core/        — Result、BaseEntity、异常、Redis 配置
│   ├── common-security/    — JWT Token 提供者
│   └── common-mybatis/     — MyBatis-Plus 配置、分页插件、自动填充
├── yuemo-modules/
│   ├── yuemo-user/         — 用户注册登录、地址管理、角色
│   ├── yuemo-product/      — 商品/SKU/分类/品牌/评价/库存/规格/标签
│   ├── yuemo-order/        — 订单创建/状态管理/超时处理/退款
│   ├── yuemo-payment/      — 支付单/回调/退款
│   ├── yuemo-cart/         — 购物车(Redis+MySQL)
│   └── yuemo-promotion/    — 优惠券发放/领取/使用
├── yuemo-gateway/          — 认证过滤/限流/熔断/日志
├── yuemo-admin/            — 后台管理接口（商品/优惠券）
└── yuemo-server/           — Spring Boot 入口（聚合所有模块）
```

**当前为单体多模块架构**：单 JAR 部署，模块间直接通过 Spring Bean 调用。

---

## 2. 各模块职责

### 2.1 common-core

| 项目 | 内容 |
|------|------|
| 职责 | 全局基础组件 |
| 拥有数据 | 无 |
| 对外暴露 | `Result<T>`、`BusinessException`、`ResultCode`、`BaseEntity`、`IdGenerator`、`PasswordEncoder` |
| 依赖 | 无内部模块 |
| 禁止 | 写业务逻辑、引用业务模块 |

### 2.2 common-security

| 项目 | 内容 |
|------|------|
| 职责 | JWT Token 生成/校验 |
| 拥有数据 | 无 |
| 对外暴露 | `JwtTokenProvider` |
| 依赖 | common-core |
| 配置 | `jwt.secret`、`jwt.access-token-expiration`(1800s)、`jwt.refresh-token-expiration`(604800s) |

### 2.3 common-mybatis

| 项目 | 内容 |
|------|------|
| 职责 | MyBatis-Plus 全局配置 |
| 拥有数据 | 无 |
| 对外暴露 | `AutoFillMetaObjectHandler`、分页插件 |
| 依赖 | common-core |
| 扫描范围 | `com.yuemo.**.mapper` |

### 2.4 yuemo-user

| 项目 | 内容 |
|------|------|
| 职责 | 用户注册/登录/登出、个人信息、余额管理、收货地址 CRUD、用户角色 |
| 拥有数据 | `yu_user`、`yu_address`、`yu_user_role` |
| 对外 API | `/api/user/register`、`/api/user/login`、`/api/user/logout`、`/api/user/info`(GET+PUT)、`/api/user/balance`、`/api/user/address/*` |
| 暴露服务 | `UserService`（查询用户、扣减/增加余额）、`AddressService` |
| 允许依赖 | common-* |
| 禁止依赖 | 任何业务模块 |
| 禁止 | 操作其他模块的表 |

### 2.5 yuemo-product

| 项目 | 内容 |
|------|------|
| 职责 | 商品 CRUD、SKU 管理、分类/品牌、库存扣减/恢复、评价、搜索、规格模板、标签 |
| 拥有数据 | `yu_product`、`yu_product_sku`、`yu_category`、`yu_brand`、`yu_product_review`、`yu_product_tag`、`yu_product_tag_rel`、`yu_product_spec_template`、`yu_product_spec_value`、`yu_search_keyword` |
| 对外 API | `/api/product/**`、`/api/admin/product/**` |
| 暴露服务 | `ProductService`、`SkuService`、`SearchService`、`ReviewService`、`BrandService` |
| 接收 MQ | `order-stock-preoccupy`（库存预占）、`order-stock-release`（库存释放） |
| 允许依赖 | common-* |
| 禁止依赖 | order、payment、cart、user、promotion |
| 禁止 | 操作订单表、支付表、用户表 |

### 2.6 yuemo-order

| 项目 | 内容 |
|------|------|
| 职责 | 订单创建/取消/发货/收货/删除、超时取消定时任务、退款处理 |
| 拥有数据 | `yu_order`、`yu_order_item`、`yu_order_log` |
| 对外 API | `/api/order/**` |
| 暴露服务 | `OrderService` |
| 发送 MQ | `order-stock-preoccupy`（事务消息）、`order-stock-release`（库存释放） |
| 接收 MQ | `payment-callback`（支付成功通知）、`payment-refund`（退款通知） |
| 允许依赖 | common-*、yuemo-product(Service)、yuemo-cart(Service) |
| 禁止依赖 | yuemo-payment、yuemo-user、yuemo-promotion |
| 禁止 | 操作产品表、支付表、用户表、直接调 ProductMapper |

### 2.7 yuemo-payment

| 项目 | 内容 |
|------|------|
| 职责 | 支付单创建、支付回调处理、退款 |
| 拥有数据 | `yu_payment` |
| 对外 API | `/api/payment/**`（callback 为白名单） |
| 暴露服务 | `PaymentService` |
| 发送 MQ | `payment-callback`（支付成功）、`payment-refund`（退款通知） |
| 允许依赖 | common-*、yuemo-order(Service)、yuemo-user(Service) |
| 禁止依赖 | yuemo-product、yuemo-cart、yuemo-promotion |
| 禁止 | 操作订单表、产品表 |

### 2.8 yuemo-cart

| 项目 | 内容 |
|------|------|
| 职责 | 购物车增删改查（Redis 为主 + MySQL 异步同步） |
| 拥有数据 | `yu_cart_item`、Redis Hash `cart:{userId}` |
| 对外 API | `/api/cart/**` |
| 暴露服务 | `CartService` |
| 发送 MQ | `cart-sync`（Redis→MySQL 同步） |
| 接收 MQ | `cart-sync`（自身消费，落库） |
| 允许依赖 | common-*、yuemo-product(Service/Mapper，只读) |
| 禁止依赖 | order、payment、user、promotion |
| 禁止 | 写其他模块的表 |

### 2.9 yuemo-promotion

| 项目 | 内容 |
|------|------|
| 职责 | 优惠券定义、用户领取、使用/过期管理 |
| 拥有数据 | `yu_coupon`、`yu_user_coupon` |
| 对外 API | `/api/coupon/**` |
| 暴露服务 | `CouponService` |
| 允许依赖 | common-* |
| 禁止依赖 | 任何业务模块 |
| 禁止 | 操作其他模块的表 |
| 规划中 | 优惠券过期定时任务（当前未实现 @Scheduled，过期状态由查询时判断） |

### 2.10 yuemo-gateway

| 项目 | 内容 |
|------|------|
| 职责 | 认证过滤(JWT)、限流(Redis滑动窗口)、熔断(Sentinel)、请求日志 |
| 拥有数据 | 无 |
| 对外暴露 | 无（纯 Filter） |
| 依赖 | common-core、common-security |
| 白名单 | `/api/user/login`、`/api/user/register`、`/api/payment/callback/**`、`/doc.html`、`/webjars/**`、`/actuator/**` |
| 半白名单(GET) | `/api/product/**` |

### 2.11 yuemo-admin

| 项目 | 内容 |
|------|------|
| 职责 | 后台管理（商品/品牌/SKU CRUD、优惠券管理） |
| 对外 API | `/api/admin/product/**`、`/api/admin/coupon/**` |
| 允许依赖 | common-*、yuemo-product、yuemo-promotion |
| 鉴权 | 需要 ROLE_ADMIN（Redis `user:role:{userId}`） |

---

## 3. 模块依赖矩阵

```
                 ┌─────────┐
                 │  user   │ ← 只依赖 common-*
                 └────┬────┘
                      │ (被调用)
         ┌────────────┼────────────┐
         ▼            ▼            ▼
    ┌─────────┐ ┌─────────┐ ┌──────────┐
    │  order  │ │ payment │ │  cart    │
    └────┬────┘ └────┬────┘ └────┬─────┘
         │           │           │
         ▼           │           ▼
    ┌─────────┐      │      ┌──────────┐
    │ product │◄─────┘      │ product  │ (只读 Mapper)
    └─────────┘             └──────────┘

    ┌───────────┐
    │ promotion │ ← 独立，只依赖 common-*
    └───────────┘

    ┌───────────┐                ┌───────────┐
    │  gateway  │ ← common-*    │   admin   │ ← product + promotion
    └───────────┘                └───────────┘
```

---

## 4. 跨模块调用规则

| 规则 | 说明 |
|------|------|
| ✅ 允许 | 模块通过 Service 接口调用其他模块 |
| ✅ 允许 | cart 模块读取 product 的 Mapper（只读商品信息） |
| ✅ 允许 | 通过 RocketMQ 异步通信 |
| ❌ 禁止 | 跨模块直接调用 Mapper（cart→product 除外） |
| ❌ 禁止 | 跨模块直接操作数据库表 |
| ❌ 禁止 | 循环依赖 |
| ❌ 禁止 | Controller 跨模块调用 Service |
| ❌ 禁止 | 往其他模块的数据库表写数据 |
