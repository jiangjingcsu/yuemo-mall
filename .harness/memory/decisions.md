# 技术决策记录

> 项目重大技术决策及其原因。AI Agent 做新决策前必须先阅读此文件，避免重蹈覆辙。

---

## 决策记录

### D001: 单体多模块架构

```yaml
决策: 单体 JAR + Maven 多模块，非微服务
日期: 项目初期
原因:
  - 团队规模小，微服务运维成本过高
  - 业务尚在验证阶段，不需提前拆分
  - Spring Boot 单 JAR 部署简单可靠
影响:
  - 模块间直接通过 Service 调用，不走 RPC
  - 数据库共享（同一 MySQL 实例）
  - 为微服务做准备：模块边界清晰、通过 MQ 异步通信

AI 注意: 新增代码必须保持模块边界，为未来拆分做准备
```

### D002: MyBatis-Plus 而非 JPA

```yaml
决策: MyBatis-Plus 3.5.9，非 Spring Data JPA
原因:
  - SQL 显式可控，方便优化
  - 团队更熟悉 MyBatis
  - 复杂查询写在 XML 中更清晰
影响:
  - BaseEntity: @Getter + @Setter（公共字段 id/createTime/updateTime/deleted）
  - 子类 Entity: @Data + @EqualsAndHashCode(callSuper = true) + @TableName
  - 核心实体（Order/Payment）采用充血模型，包含领域行为方法（见 D010）
  - Mapper extends BaseMapper<Entity>
  - 逻辑删除自动配置
```

### D003: RocketMQ 而非 RabbitMQ/Kafka

```yaml
决策: Apache RocketMQ
原因:
  - 阿里系技术栈一致性好
  - 事务消息支持（下单→库存预占）
  - 中文社区文档丰富
影响:
  - Topic: {domain}-{event}
  - 消费者幂等是强制要求
  - 事务消息用于订单创建 + 库存预占
```

### D004: Redis Hash 做购物车主存储

```yaml
决策: 购物车以 Redis Hash 为主，MySQL 仅作异步备份
原因:
  - 购物车读写频率高
  - Redis Hash 自然映射 userId→{skuId→item}
  - 最终一致性可接受
影响:
  - 写: Redis → MQ → MySQL（cart-sync）
  - 读: Redis 优先，miss 查 MySQL
  - 清理: 定时任务凌晨 3 点
  - 唯一约束: (user_id, sku_id, deleted)
```

### D005: 逻辑删除统一策略

```yaml
决策: 所有表使用 deleted 字段实现逻辑删除
原因:
  - 数据不丢失，可恢复
  - 业务需要保留历史记录
  - MyBatis-Plus 自动支持
影响:
  - 所有表含 deleted TINYINT(0/1)
  - 唯一键含 deleted 字段
  - 查询自动追加 deleted=0 条件（过滤已删除记录）
```

### D006: Flyway 数据库迁移

```yaml
决策: Flyway 管理数据库版本
原因:
  - SQL 脚本版本化，可追溯
  - 自动执行，无需手动
  - 与 Spring Boot 集成好
规则:
  - 已执行脚本不可修改（checksum）
  - 新变更新建 V{version}__{description}.sql
  - 不可逆操作用存储过程检查
```

### D007: JWT 双 Token + Redis 认证

```yaml
决策: JWT 双 Token（AccessToken 30min + RefreshToken 7天） + Redis 黑名单
原因:
  - 无状态 Token，扩展性好
  - Redis 黑名单实现登出
  - 双 Token 机制：AccessToken 短期使用，RefreshToken 长期有效
影响:
  - GatewayAuthFilter（@Order(1)）拦截所有 /api/**
  - 白名单/半白名单明确列出
  - /api/admin/** 额外校验 ADMIN 角色（Redis user:role:{userId}）
  - Redis token:user:{userId} 存储 AccessToken（TTL 30min）
  - Redis token:blacklist:{token} 存储登出 Token（TTL 30min）

⚠️ 已知缺口:
  - RefreshToken 已生成并返回前端，但后端缺少 /api/user/refresh 端点
  - 需补充：用 refreshToken 换取新 accessToken 的接口

否决方案: Sa-Token（见 D008）
```

### D008: 不采用 Sa-Token

```yaml
决策: 不引入 Sa-Token，维持 JWT + Redis 认证体系
日期: 2026-05
原因:
  - 项目已基于 JWT + Redis 实现完整认证体系（GatewayAuthFilter + Token 黑名单）
  - Sa-Token 引入额外依赖且功能与现有实现重叠
  - Sa-Token 的注解式鉴权（@SaCheckLogin 等）与项目网关层统一鉴权架构不一致
影响:
  - 认证统一在 GatewayAuthFilter 处理，不使用方法级注解
  - 角色校验通过 Redis 缓存（user:role:{userId}）+ 网关层判断
  - AI 禁止引用 Sa-Token 相关 API/配置/注解
```

### D009: 不采用 Elasticsearch

```yaml
决策: 不引入 Elasticsearch，使用 MySQL LIKE + 热词表方案
日期: 2026-05
原因:
  - 当前搜索需求简单（商品名模糊搜索 + 搜索建议）
  - MySQL LIKE + yu_search_keyword 热词表已满足需求
  - ES 运维成本高（独立集群、索引管理、数据同步）
  - 商品数据量级尚未达到 MySQL 搜索瓶颈
影响:
  - 搜索通过 ProductMapper + SearchKeywordMapper 实现
  - 热词统计通过 incrementSearchCount 实现
  - AI 禁止虚构 Elasticsearch 集成（常见幻觉）
状态: 暂时否决，搜索性能瓶颈时重新评估
```

### D010: 核心实体充血模型

```yaml
决策: Order/Payment 等核心实体采用充血模型，包含领域行为方法
日期: 项目初期
原因:
  - 状态转换逻辑内聚在实体中，避免散落在 Service
  - ensureStatus() 守卫方法保证状态机安全
  - 符合 DDD 领域驱动设计理念
影响:
  - Order: pay/ship/confirmReceive/cancel/verifyOwnership/ensureStatus/canDelete
  - Payment: markSuccess/markFailed/markRefunded/verifyOwnership/isPending/isSuccess
  - Service 层调用 Entity 领域方法，不直接 set 状态字段
  - 新增核心实体必须包含领域行为方法（非纯 @Data 贫血模型）
```

### D011: 策略模式用于行为分发

```yaml
决策: 高扩展模块使用 Strategy 模式替代 switch-case/if-else 链
日期: 项目初期
原因:
  - 支付类型可扩展（微信/支付宝/余额，未来可能增加）
  - 策略实现独立，符合开闭原则
  - Spring 自动注入 + Map 路由，无硬编码
影响:
  - PayTypeHandler 接口 + @Component 实现
  - 构造函数收集 List<PayTypeHandler> → Map<payType, handler>
  - 新增支付类型只需添加 @Component 实现，无需修改 Service
  - 其他行为分发场景（如优惠券类型处理）也应使用此模式
```

### D012: 统一 API 响应格式 Result\<T\>

```yaml
决策: 所有接口统一返回 Result<T> 响应信封
日期: 项目初期
原因:
  - 前后端响应格式统一，简化前端拦截器处理
  - 业务错误码与 HTTP 状态码解耦
  - 前端 request.ts 自动解包 Result<T>，调用方拿到 T
影响:
  - 成功: Result.success(data)
  - 失败: Result.fail(ResultCode.XXX)（通过 BusinessException 抛出）
  - 分页: Result<PageResult<T>>（PageResult.from(iPage.convert(VO::from))）
  - 支付回调例外: 返回 String（第三方要求特定格式）
```

### D013: VO 使用 Java record + from() 工厂方法

```yaml
决策: 所有 API 响应使用 Java record VO，提供 static from(Entity) 转换
日期: 项目初期
原因:
  - record 不可变，天然线程安全
  - from() 方法统一 Entity→VO 转换逻辑
  - 隔离内部 Entity 结构与外部 API 契约
影响:
  - VO 不包含 password/secret 等敏感字段
  - VO 不包含 deleted 等 ORM 框架字段
  - Controller 中: 单对象 VO.from(entity)，列表 stream().map(VO::from).toList()
  - 部分复杂 VO（如 ProductVO）在 Service 层直接构建，无 from 方法
```

### D014: 前端技术栈

```yaml
决策: React 19 + Vite 6 + Ant Design 5 + TypeScript
日期: 项目初期
原因:
  - React 生态成熟，社区资源丰富
  - Vite 构建速度快，HMR 体验好
  - Ant Design 5 组件丰富，适合中后台和电商场景
  - TypeScript 类型安全，减少运行时错误
影响:
  - 页面组件放在 src/pages/
  - API 调用使用 src/utils/request.ts（axios 封装，自动解包 Result<T>）
  - 状态管理: React Context + useState（未引入 Redux）
  - 样式: Ant Design token 主题定制 + CSS Modules

  [SUPERSEDED] 状态管理已变更为 Redux Toolkit，见 D016
```

### D016: 前端状态管理升级为 Redux Toolkit

```yaml
决策: 引入 Redux Toolkit (@reduxjs/toolkit ^2.7.0) + react-redux ^9.2.0
日期: 2025-05
原因:
  - 购物车状态需跨组件共享（商品列表页、购物车页、结算页）
  - 用户登录状态需全局持久化
  - React Context + useState 在复杂场景下性能和可维护性不足
  - Redux Toolkit 简化了 Redux 样板代码，与 React 19 兼容
影响:
  - Store 结构: src/stores/index.ts（configureStore）
  - Slice: src/stores/cartSlice.ts（购物车状态）、src/stores/userSlice.ts（用户状态）
  - 取代 D014 中的 React Context + useState 方案
  - D014 状态管理部分标记为 [SUPERSEDED]
```

### D015: Sentinel 流控熔断

```yaml
决策: Sentinel 1.8.9 做流量控制和熔断降级
日期: 项目初期
原因:
  - 保护核心接口不被突发流量压垮
  - 慢调用自动熔断，防止级联故障
  - 轻量级，与 Spring Boot 集成好
影响:
  - 7 个 API 资源配置了 QPS 限制（product 200, cart 100, user/order/coupon 50, payment 30, admin 35）
  - 熔断规则: 慢调用比例 > 50%（RT > 1s）触发 30s 熔断
  - 规则硬编码在 SentinelRuleConfig.java（Dashboard 已部署但未动态配置）
  - 流控返回 429，熔断返回 503
```

### D016: Docker Compose 双副本滚动更新

```yaml
决策: 后端双副本 + 前端单副本，Nginx 负载均衡
日期: 项目初期
原因:
  - 双副本保证零停机部署
  - Docker Compose 简单可靠，适合当前规模
  - Nginx 健康检查 + 滚动更新保证可用性
影响:
  - 后端: yuemo-mall-1:8081 + yuemo-mall-2:8082
  - 前端: frontend-server:8080（Nginx 静态托管）
  - 中间件在基础设施服务器（192.168.1.56）独立运行，非 Docker 管理
  - 部署流程: 逐副本更新 → 健康检查 → 下一个副本
  - 镜像仓库: Harbor（192.168.1.53/yuemo-mall/）
```
