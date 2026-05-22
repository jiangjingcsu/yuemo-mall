# AI 幻觉检测

> 检测 AI Agent 生成内容中的幻觉（不存在的 API、错误的路径、虚构的配置）。
> 综合评估入口：`evaluation/review-checklist.md`

---

## 检测维度

### 1. API/方法存在性（权重 30%）

```yaml
检查:
  - [ ] 引用的类/接口在项目中存在
  - [ ] 引用的方法签名与实际一致
  - [ ] 引用的枚举值真实存在
  - [ ] 引用的注解与 Spring Boot 3.2 兼容（Jakarta 命名空间，非 javax）

常见幻觉:
  - 虚构的 Mapper 方法（BaseMapper 中没有的自定义方法）
  - 不存在的枚举常量（如 OrderStatus.PROCESSING 实际不存在，仅有 UNPAID/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDED）
  - 错误的 Spring 注解参数名
  - 项目不存在的依赖库 API
  - 使用 javax.* 命名空间（项目为 Spring Boot 3.2，应使用 jakarta.*）
  - 虚构的 Service 方法（Service 接口中未定义的方法）

项目实际枚举清单:
  - OrderStatus: UNPAID(0), PAID(1), SHIPPED(2), COMPLETED(3), CANCELLED(4), REFUNDED(5)
  - PaymentStatus: PENDING(0), SUCCESS(1), FAILED(2), REFUNDED(3)
  - PayType: WECHAT(1), ALIPAY(2)
  - ProductStatus: OFF_SHELF(0), ON_SHELF(1)
  - UserStatus: NORMAL(0), DISABLED(1)
  - CouponType: THRESHOLD_DISCOUNT(1), DISCOUNT(2), FIXED_DISCOUNT(3)
  - CouponStatus: NOT_STARTED(0), ACTIVE(1), ENDED(2)
  - UserCouponStatus: UNUSED(0), USED(1), EXPIRED(2)

项目自定义 Mapper 方法（仅 13 个，其余为 BaseMapper 默认方法）:
  - UserMapper: deductBalance, addBalance
  - ProductMapper: selectProductById, deductStock, restoreStock
  - ProductSkuMapper: deductStock
  - ProductReviewMapper: selectRatingSummary, selectRatingDistribution
  - ProductTagMapper: selectTagsByProductId
  - SearchKeywordMapper: selectHotKeywords, incrementSearchCount, selectSuggestions
  - CartItemMapper: upsertOnDuplicateKey
  - CouponMapper: incrementReceivedCount

检测方式:
  - Grep 搜索引用的类名
  - 读取依赖的 pom.xml 确认依赖存在
  - 编译验证: mvn compile -q
```

### 2. 配置/属性存在性（权重 25%）

```yaml
检查:
  - [ ] 引用的 application.yml 配置 key 存在
  - [ ] 引用的环境变量名在 Dockerfile 或 docker-compose.yml 中定义
  - [ ] Redis Key 前缀与项目实际使用一致
  - [ ] MQ Topic 名称与项目已定义的一致
  - [ ] 引用的 ResultCode 枚举值真实存在

项目实际 Redis Key 前缀:
  - cart:{userId}
  - token:user:{userId}
  - token:blacklist:{token}
  - user:role:{userId}
  - payment:callback:{paymentNo}
  - rate:{api-path}:{userId|ip}
  - coupon:receive:{userId}:{couponId}
  - mq:consumed:{topic}:{msgId}

项目实际 MQ Topic:
  - order-stock-preoccupy
  - order-stock-release
  - payment-callback
  - payment-refund
  - cart-sync

常见幻觉:
  - 虚构的 spring.xxx 配置项
  - 不存在的 MQ Topic 名称（如 order-status-change 实际不存在）
  - 错误的 Redis Key 格式（如 mall:* 前缀，项目实际不使用 mall: 前缀）
  - 虚构的 Nacos/Apollo 配置中心引用（项目未使用）
  - 虚构的 ResultCode 枚举值
  - 虚构的 Sa-Token 配置（项目未使用 Sa-Token，认证基于 JWT + Redis）

检测方式:
  - Read application.yml 验证配置项
  - Grep Redis Key 常量类（*RedisKeyConstants.java, *Constants.java）
  - Grep MQ Topic 名称（@RocketMQMessageListener 注解）
  - Read ResultCode 枚举确认错误码
```

### 3. 文件路径/模块（权重 20%）

```yaml
检查:
  - [ ] 引用的文件路径在项目中存在
  - [ ] 引用的模块名（如 yuemo-xxx）在 pom.xml 中声明
  - [ ] 引用的包路径与实际目录结构一致
  - [ ] 前端组件路径与项目结构一致

项目实际模块:
  后端（yuemo-backend）:
    - yuemo-server（启动模块）
    - yuemo-gateway（网关）
    - yuemo-admin（后台管理）
    - yuemo-common/common-core
    - yuemo-common/common-mybatis
    - yuemo-common/common-security
    - yuemo-modules/yuemo-user
    - yuemo-modules/yuemo-product
    - yuemo-modules/yuemo-order
    - yuemo-modules/yuemo-cart
    - yuemo-modules/yuemo-payment
    - yuemo-modules/yuemo-promotion
  前端（yuemo-frontend）:
    - src/pages/（页面组件）
    - src/components/（通用组件）
    - src/services/（API 调用）
    - src/utils/（工具函数）

常见幻觉:
  - 虚构的模块名（如 yuemo-inventory、yuemo-logistics 实际不存在）
  - 错误的包路径（如 com.yuemo.xxx 与实际不符）
  - 不存在的文件引用
  - 虚构的 common-xxx 公共模块

检测方式:
  - Glob 验证文件路径
  - Read pom.xml 确认模块
```

### 4. 数据库表/字段（权重 15%）

```yaml
检查:
  - [ ] 引用的表名在 Flyway 迁移脚本或 @TableName 注解中存在
  - [ ] 引用的字段名与 Entity 定义一致
  - [ ] 引用的索引名真实存在
  - [ ] 数据库类型与项目使用一致（MySQL 8.0）

项目实际表清单（20 张，前缀 yu_）:
  yu_user, yu_user_role, yu_address,
  yu_product, yu_category, yu_brand, yu_product_sku,
  yu_product_spec_template, yu_product_spec_value,
  yu_product_tag, yu_product_tag_rel, yu_search_keyword, yu_product_review,
  yu_order, yu_order_item, yu_order_log,
  yu_payment,
  yu_cart_item,
  yu_coupon, yu_user_coupon

常见幻觉:
  - 虚构的表名（如 yu_inventory、yu_logistics 实际不存在）
  - 不存在的字段（如 Order.trackingNumber 实际为 logisticsNo）
  - 错误的字段类型（如 status 用 INT 而非 TINYINT）
  - 虚构的索引名

检测方式:
  - Glob 搜索 Flyway SQL 文件（yuemo-server/src/main/resources/db/migration/）
  - Grep @TableName 注解确认表名
  - Grep Entity 字段定义
```

### 5. 外部服务/依赖（权重 10%）

```yaml
检查:
  - [ ] 引用的外部 API 地址真实存在（非虚构域名）
  - [ ] 引用的第三方服务在项目中有集成（如短信、支付）
  - [ ] 引用的中间件版本与实际一致
  - [ ] 引用的前端库在 package.json 中声明

项目实际依赖版本:
  后端:
    - Spring Boot: 3.2.12
    - Java: 17
    - MyBatis-Plus: 3.5.9
    - MySQL Connector: 8.0.33
    - jjwt: 0.12.6
    - Sentinel: 1.8.9
    - Knife4j: 4.5.0
    - RocketMQ Starter: 2.3.5
  前端:
    - React 19 / Vite 6 / Ant Design 5 / TypeScript

常见幻觉:
  - 虚构的第三方服务集成（如短信、OSS、Elasticsearch 实际未集成）
  - 错误的中间件版本号
  - 引用项目未使用的库（如 Lombok @Builder 在部分场景不可用）
  - 虚构的前端组件库 API

检测方式:
  - 检查 pom.xml 依赖版本
  - 检查 package.json 依赖
  - 检查 application.yml 外部配置
```

---

## 评分

```yaml
每项检测:
  - 无幻觉: 满分
  - 发现幻觉但可自动修复: 扣 50% 该项分数
  - 发现幻觉且无法自动修复: 该项 0 分

总分: 100 分
  ≥ 90: PASS
  70-89: WARN（存在幻觉但已修复或影响小）
  < 70: FAIL（存在严重幻觉，BLOCK）

补充规则:
  - 虚构安全性相关的 API/配置: 直接 FAIL
  - 虚构数据库表/字段: 直接 FAIL
  - 虚构支付/金额相关逻辑: 直接 FAIL
```

## 输出

```yaml
幻觉报告:
  score: 0-100
  level: PASS|WARN|FAIL
  hallucinations:
    - type: API|CONFIG|PATH|DATABASE|EXTERNAL
      claimed: <AI 声称的内容>
      actual: <实际情况>
      severity: CRITICAL|HIGH|MEDIUM|LOW
      fixed: true|false
      detection: <检测方式（Grep/Read/Compile/Glob）>
```
