# 术语表

> 月魔商城项目统一术语定义，确保 AI Agent 使用一致的术语。
> 交叉引用: constraints/infra.md（基础设施拓扑）、docs/module-responsibility.md（模块职责）

---

## 业务术语

### 通用

| 术语 | 英文 | 定义 |
|---|---|---|
| 月魔商城 | Yuemo Mall | 项目名称，B2C 电商平台 |
| SKU | Stock Keeping Unit | 库存量单位，商品的最小销售单元 |
| SPU | Standard Product Unit | 标准产品单元，商品的标准化描述 |
| 购物车 | Cart | 用户暂存待购商品的临时容器 |
| 逻辑删除 | Logical Delete | 标记 deleted=1 而非物理删除行 |
| 幂等 | Idempotent | 同一操作执行多次结果相同 |

### 订单状态（OrderStatus）

| 术语 | 枚举值 | code | 定义 |
|---|---|---|---|
| 待支付 | UNPAID | 0 | 订单已创建但未支付 |
| 已支付 | PAID | 1 | 订单已支付，待发货 |
| 已发货 | SHIPPED | 2 | 订单已发货，待收货 |
| 已完成 | COMPLETED | 3 | 订单已完成（确认收货） |
| 已取消 | CANCELLED | 4 | 订单已取消 |
| 已退款 | REFUNDED | 5 | 退款已完成 |

### 支付状态（PaymentStatus）

| 术语 | 枚举值 | code | 定义 |
|---|---|---|---|
| 待支付 | PENDING | 0 | 支付单已创建，等待支付 |
| 支付成功 | SUCCESS | 1 | 支付已完成 |
| 支付失败 | FAILED | 2 | 支付失败 |
| 已退款 | REFUNDED | 3 | 支付已退款 |

### 支付方式（PayType）

| 术语 | 枚举值 | code | 定义 |
|---|---|---|---|
| 微信支付 | WECHAT | 1 | 微信支付（stub，未对接） |
| 支付宝 | ALIPAY | 2 | 支付宝支付（stub，未对接） |
| 平台余额 | BALANCE | 3 | 平台余额支付（已实现） |

### 错误码分段（ResultCode）

| 分段 | 模块 | 示例 |
|---|---|---|
| 200 | 通用成功 | SUCCESS(200) |
| 400-409 | HTTP 通用错误 | BAD_REQUEST(400), UNAUTHORIZED(401), FORBIDDEN(403) |
| 500 | 服务器内部错误 | INTERNAL_ERROR(500) |
| 1xxx | 用户模块 | USER_NOT_FOUND(1001), USER_PASSWORD_ERROR(1002) |
| 2xxx | 商品模块 | PRODUCT_NOT_FOUND(2001), SKU_STOCK_INSUFFICIENT(2005) |
| 3xxx | 订单模块 | ORDER_NOT_FOUND(3001), ORDER_STATUS_ERROR(3002) |
| 4xxx | 支付模块 | PAYMENT_FAILED(4001), REFUND_FAILED(4004) |
| 5xxx | 购物车模块 | CART_ITEM_NOT_FOUND(5001) |
| 6xxx | 促销模块 | COUPON_NOT_FOUND(6001), COUPON_EXPIRED(6002) |

---

## 技术术语

| 术语 | 定义 |
|---|---|
| 充血模型 | Entity 包含领域行为方法（如 Payment: verifyOwnership/markSuccess/markRefunded；Order: pay/cancel/confirmReceive） |
| 贫血模型 | Entity 只有 getter/setter，业务逻辑在 Service 中（禁止） |
| 策略模式 | 用 Handler 接口 + @Component 替代 switch-case 行为分发（如 PayTypeHandler） |
| 聚合根 | DDD 中一组相关对象的入口，如 Order 聚合包含 OrderItem |
| 防腐层 | 隔离外部系统和内部模型的转换层 |
| SPI | Service Provider Interface，用于模块扩展点 |
| 红黑榜 | 模块依赖白名单（允许）与黑名单（禁止） |
| 编排层 | Service 层协调多个领域服务完成业务流程 |
| SQL CAS | 乐观锁模式，UPDATE ... WHERE balance >= amount，防止并发超扣 |
| 事务消息 | RocketMQ 事务消息，保证本地事务与消息发送的原子性 |

---

## 项目特有缩写

| 缩写 | 全称 | 说明 |
|---|---|---|
| yuemo | 月魔 | 项目前缀，包名和模块名使用 |
| DTO | Data Transfer Object | 数据传输对象，用于接收请求 |
| VO | View Object | 视图对象，用于响应 |
| MQ | Message Queue | 消息队列（RocketMQ 5.x） |
| LB | Load Balancer | 负载均衡（Nginx） |
| CI | Continuous Integration | 持续集成（GitLab CI） |
| CD | Continuous Deployment | 持续部署 |
| TDD | Test-Driven Development | 测试驱动开发 |
| OCP | Open/Closed Principle | 开闭原则：对扩展开放，对修改关闭 |
| AAA | Arrange-Act-Assert | 测试结构模式 |

---

## 中间件术语

### MQ Topic

| Topic | 生产者 | 消费者 | 消息体 | 说明 |
|---|---|---|---|---|
| payment-callback | BalancePayHandler / PaymentServiceImpl | PaymentCallbackConsumer（order） | String(orderNo) | 支付成功通知 |
| payment-refund | PaymentServiceImpl | PaymentRefundConsumer（payment）+ OrderRefundConsumer（order） | RefundMessage{orderId, userId, amount} | 退款通知 |
| order-stock-release | OrderServiceImpl | StockReleaseConsumer（product） | Map<Long, Integer>(skuId→quantity) | 库存释放 |

### Redis Key 规范

| Key 模式 | TTL | 用途 |
|---|---|---|
| payment:callback:{paymentNo} | 5 分钟 | 支付回调幂等 |
| mq:consumed:{topic}:{bizId} | 10 分钟 | MQ 消费幂等 |
| cart:{userId} | 7 天 | 购物车缓存 |

---

## 基础设施术语

> 详细拓扑见 constraints/infra.md

| 术语 | 说明 |
|---|---|
| 192.168.1.55 | 生产服务器（Docker Compose 运行后端+前端） |
| 192.168.1.56 | 基础设施服务器（MySQL / Redis / RocketMQ / Sentinel） |
| 192.168.1.53 | gitlab-runner + Harbor（构建执行 + 私有镜像仓库） |
| 192.168.1.54 | GitLab（代码仓库 + CI/CD 调度） |
| yuemo-mall-1 | 后端实例 1 容器（宿主机端口 8081 → 容器 8080） |
| yuemo-mall-2 | 后端实例 2 容器（宿主机端口 8082 → 容器 8080） |
| frontend-server | 前端 Nginx 静态托管容器（端口 8080） |
| MySQL 8.0 | 192.168.1.56:3306 |
| Redis 7.x | 192.168.1.56:6379 |
| RocketMQ 5.x | 192.168.1.56:9876（NameServer） |
| Sentinel | 192.168.1.56:8080（Dashboard） |

---

## 模块术语

### 公共模块（yuemo-common/）

| 模块路径 | 功能 |
|---|---|
| common-core | 核心工具、统一响应（Result<T>）、错误码（ResultCode）、BaseEntity |
| common-security | JWT 认证、Redis Token 管理、密码加密 |
| common-mybatis | MyBatis-Plus 配置、自动填充、逻辑删除 |

### 业务模块（yuemo-modules/）

| 模块路径 | 功能 |
|---|---|
| yuemo-user | 用户模块（登录、注册、用户信息、余额管理） |
| yuemo-product | 商品模块（商品、分类、SKU、品牌、规格、库存） |
| yuemo-order | 订单模块（订单、订单项、订单日志） |
| yuemo-cart | 购物车模块（Redis 缓存 + DB 持久化） |
| yuemo-payment | 支付模块（余额支付、策略模式多支付方式） |
| yuemo-promotion | 促销模块（优惠券） |

### 基础设施模块

| 模块路径 | 功能 |
|---|---|
| yuemo-gateway | API 网关（路由、鉴权、限流、熔断） |
| yuemo-admin | 后台管理 |
| yuemo-server | Spring Boot 启动入口 |

---

## 技术栈版本

| 技术 | 版本 |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.12 |
| MyBatis-Plus | 3.5.9 |
| RocketMQ Starter | 2.3.5 |
| Sentinel | 1.8.9 |
| JJWT | 0.12.6 |
| Knife4j | 4.5.0 |
| Hutool | 5.8.34 |
| MapStruct | 1.6.3 |
