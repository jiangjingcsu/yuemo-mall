# 中间件使用指南

> 月魔商城项目使用的中间件配置和使用规范。
> 详细治理规则：`rules/database-governance.md`、`rules/redis-governance.md`、`rules/mq-governance.md`
> 红线约束：`constraints/database.md`、`constraints/infra.md`

---

## 1. MySQL 8.0

```yaml
版本: 8.0.33（mysql-connector-j）
驱动: com.mysql.cj.jdbc.Driver
连接: jdbc:mysql://${DB_HOST:192.168.1.56}:3306/yuemo_mall?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
ORM: MyBatis-Plus 3.5.9（mybatis-plus-spring-boot3-starter）
迁移: Flyway 10.10.0（Spring Boot 3.2.12 管理）

表规范:
  - 表前缀: yu_（如 yu_order, yu_cart_item）
  - 必备字段: id, create_time, update_time, deleted
  - 主键策略: BIGINT 自增（应用层 IdGenerator 生成业务 ID）
  - 逻辑删除: deleted TINYINT NOT NULL DEFAULT 0（MyBatis-Plus 全局配置）
  - 唯一索引: 必须包含 deleted 字段

连接池: HikariCP
  minimum-idle: 5
  maximum-pool-size: 20
  idle-timeout: 300000 (5分钟)
  connection-timeout: 15000 (15秒)
  max-lifetime: 1800000 (30分钟)
  pool-name: YuemoHikariPool
```

## 2. Redis 7.x

```yaml
版本: 7.x
连接: ${REDIS_HOST:192.168.1.56}:${REDIS_PORT:6379}
客户端: Spring Data Redis + Lettuce
序列化: Key=StringRedisSerializer, Value=GenericJackson2JsonRedisSerializer

Key 规范:
  格式: {biz}:{identifier}
  示例:
    - cart:{userId}                          （购物车，Hash 结构，TTL 7天）
    - token:user:{userId}                    （当前 AccessToken，String，TTL 30min）
    - token:blacklist:{token}                （Token 黑名单，String，TTL 30min）
    - user:role:{userId}                     （用户角色，String，TTL 24h）
    - payment:callback:{paymentNo}           （支付回调幂等锁，setIfAbsent，TTL 5min）
    - rate:{api-path}:{userId|ip}            （限流计数，Lua 滑动窗口，TTL 60s）
    - coupon:receive:{userId}:{couponId}     （领券去重，setIfAbsent，TTL 7天）
    - mq:consumed:{topic}:{msgId}            （MQ 消费幂等，setIfAbsent，TTL 10min）

TTL 策略:
  - AccessToken: 30 分钟
  - Token 黑名单: 30 分钟（= AccessToken 剩余有效期）
  - 用户角色: 24 小时
  - 支付回调幂等: 5 分钟
  - 限流计数: 60 秒
  - 购物车: 7 天（+ 定时任务 CartCleanupTask 双重保障）
  - 领券去重: 7 天
  - MQ 消费幂等: 10 分钟

Spring Cache:
  type: redis
  time-to-live: 600000 (10分钟)
  声明式缓存: @Cacheable + Caffeine（30min TTL, 1000 上限）

禁止:
  - KEYS * 命令（用 SCAN 替代）
  - FLUSHALL / FLUSHDB 命令（生产环境）
  - 大 Key（> 10MB 或 Hash field > 5000）
  - 在 Redis 中存储序列化的 Java 对象（应存 JSON）
  - 无限期 Key（除非有配套清理机制）
```

## 3. RocketMQ 5.x

```yaml
版本: rocketmq-spring-boot-starter 2.3.5
Name Server: ${ROCKETMQ_NS:192.168.1.56:9876}
Producer Group: yuemo-producer-group
send-message-timeout: 3000ms

Topic 列表:
  - order-stock-preoccupy  （订单库存预占，事务消息）
  - order-stock-release    （订单库存释放）
  - payment-callback       （支付成功回调）
  - payment-refund         （退款通知）
  - cart-sync              （购物车 Redis→MySQL 同步）

Consumer Group:
  - yuemo-order-consumer                  ← payment-callback
  - yuemo-order-refund-consumer           ← payment-refund
  - yuemo-product-consumer                ← order-stock-preoccupy
  - yuemo-product-stock-release-consumer  ← order-stock-release
  - yuemo-cart-consumer                   ← cart-sync
  - yuemo-cart-consumer-dlq               ← %DLQ%yuemo-cart-consumer（死信）
  - yuemo-payment-consumer                ← payment-refund

消息规范:
  - 消息体使用 Java record（不可变）
  - JSON 序列化（Jackson）
  - 消息体不超过 1MB
  - 事务消息: order-stock-preoccupy 使用 sendMessageInTransaction

消费规范:
  - 必须实现幂等消费（Redis setIfAbsent 或 DB UNIQUE KEY）
  - 消费失败需记录日志
  - 重试次数: 默认 16 次（间隔递增 10s→2h）
  - 死信队列: 超过重试次数自动转入 %DLQ%{ConsumerGroup}
  - 消费耗时 < 5s
```

## 4. Nginx

```yaml
前端 Nginx（192.168.1.55:8080）:
  用途: SPA 静态服务 + API 反向代理
  监听: 80
  根目录: /usr/share/nginx/html
  SPA 路由: try_files $uri $uri/ /index.html
  API 代理: /api/ → http://192.168.1.56（超时 60s）
  文档代理: /doc.html, /v3/api-docs, /webjars/ → http://192.168.1.56
  静态资源缓存: js/css/png/jpg 等 30 天
  Gzip: 开启（最小 1024 字节）
  健康检查: /health

后端 Nginx（192.168.1.56）:
  用途: 负载均衡 + 限流
  Worker: auto, worker_connections: 1024
  负载均衡算法: 轮询（weight=1）
  上游:
    - yuemo-mall-1:8080 (max_fails=3, fail_timeout=30s)
    - yuemo-mall-2:8080 (max_fails=3, fail_timeout=30s)
    - keepalive: 64
  限流区域:
    - login_limit: 5r/s per IP
    - order_limit: 10r/s per IP
  client_max_body_size: 10m
  日志格式: JSON（含 request_time, upstream_response_time）
  健康检查: /actuator/health
```

## 5. Sentinel 1.8.9

```yaml
版本: 1.8.9（sentinel-core + sentinel-spring-webmvc-v6x-adapter）
用途: 流量控制、熔断降级
Dashboard: ${SENTINEL_DASHBOARD:192.168.1.56:8080}

流控规则:
  - api-user:     50 QPS
  - api-product:  200 QPS
  - api-order:    50 QPS
  - api-payment:  30 QPS
  - api-cart:     100 QPS
  - api-coupon:   50 QPS
  - api-admin:    35 QPS

熔断规则（所有资源）:
  - 策略: 慢调用比例
  - 最大 RT: 1000ms
  - 慢调用比例阈值: 0.5（> 50% 触发熔断）
  - 熔断时长: 30s
  - 最小请求数: 10

熔断响应:
  - FlowException: 429 "请求过于频繁，请稍后再试"
  - DegradeException: 503 "服务暂时不可用，请稍后再试"
```

## 6. JWT (jjwt 0.12.6)

```yaml
库: io.jsonwebtoken:jjwt-api 0.12.6
算法: HMAC-SHA（Keys.hmacShaKeyFor）
密钥: 环境变量 JWT_SECRET（≥ 256 bits / 32 字符）

双 Token 机制:
  Access Token:
    过期: 30 分钟（1800s）
    Claims: { sub: userId, username, iat, exp }
    存储: Redis token:user:{userId}（TTL 30min）
  Refresh Token:
    过期: 7 天（604800s）
    Claims: { sub: userId, type: "refresh", iat, exp }

鉴权流程:
  1. 前端请求携带 Authorization: Bearer <accessToken>
  2. GatewayAuthFilter（@Order(1)）解析 JWT，提取 userId/username
  3. 检查 Redis token:blacklist:{token}（登出后加入黑名单）
  4. 通过 request.setAttribute("userId", userId) 注入
  5. /api/admin/** 额外校验 Redis user:role:{userId} 是否为 ADMIN

Token 结构:
  Header: { "alg": "HS256" }
  Payload: { "sub": userId, "username": "xxx", "iat": timestamp, "exp": timestamp }
  Signature: HMAC-SHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
```

## 7. Knife4j 4.5.0

```yaml
版本: 4.5.0（knife4j-openapi3-jakarta-spring-boot-starter）
用途: API 文档生成（OpenAPI 3 规范）

访问: /doc.html
注解: @Tag（类级）, @Operation（方法级）, @Parameter, @Schema
白名单: /doc.html, /webjars/**, /v3/api-docs/**
生产环境: 应关闭 /doc.html 访问
```

## 8. Docker Compose

```yaml
后端服务（192.168.1.55）:
  yuemo-mall-1:
    镜像: 192.168.1.53/yuemo-mall/backend:latest
    端口: 8081:8080
    基础镜像: eclipse-temurin:17-jre-alpine
    JVM: -XX:+UseZGC -XX:MaxRAMPercentage=75.0
    健康检查: /actuator/health（30s 间隔）
    重启策略: always
  yuemo-mall-2:
    镜像: 192.168.1.53/yuemo-mall/backend:latest
    端口: 8082:8080
    配置同上

前端服务（192.168.1.55）:
  frontend-server:
    镜像: 192.168.1.53/yuemo-mall/frontend:latest
    端口: 8080:80
    基础镜像: nginx:stable-alpine
    健康检查: /（30s 间隔）

网络:
  backend-net: external（后端服务 + 后端 Nginx 通信）
  frontend-net: external（前端服务通信）

基础设施服务（192.168.1.56，非 Docker Compose 管理）:
  MySQL 8.0:     192.168.1.56:3306
  Redis 7.x:     192.168.1.56:6379
  RocketMQ 5.x:  192.168.1.56:9876
  Sentinel:      192.168.1.56:8080（Dashboard）
  后端 Nginx:    192.168.1.56（负载均衡，非 Docker 部署）

镜像仓库:
  Harbor: 192.168.1.53/yuemo-mall/
    - backend:  192.168.1.53/yuemo-mall/backend
    - frontend: 192.168.1.53/yuemo-mall/frontend
```

---

## 中间件版本锁定

```yaml
禁止修改的版本:
  - Spring Boot: 3.2.12
  - Java: 17
  - MyBatis-Plus: 3.5.9
  - MySQL Connector: 8.0.33
  - jjwt: 0.12.6
  - Sentinel: 1.8.9
  - Knife4j: 4.5.0
  - RocketMQ Starter: 2.3.5

可协商修改:
  - 补丁版本升级（如 3.2.12 → 3.2.15）
  - 新增中间件（需架构评审）
```
