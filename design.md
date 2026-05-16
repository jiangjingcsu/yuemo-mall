# 月魔商城 — 单体分层架构设计文档

> **版本**: v1.0  
> **日期**: 2026-05-15  
> **架构模式**: 单体代码仓库 + 模块垂直拆分部署  
> **规模定位**: 中小规模（日活万级，QPS < 500）

---

## 目录

1. [设计目标](#1-设计目标)
2. [总体架构](#2-总体架构)
3. [模块拆分设计](#3-模块拆分设计)
4. [数据库设计](#4-数据库设计)
5. [缓存设计](#5-缓存设计)
6. [消息队列设计](#6-消息队列设计)
7. [部署设计](#7-部署设计)
8. [性能与容错](#8-性能与容错)
9. [灰度发布与滚动更新](#9-灰度发布与滚动更新)
10. [监控与日志](#10-监控与日志)
11. [技术选型总览](#11-技术选型总览)
12. [架构图](#12-架构图)

---

## 1. 设计目标

| 目标 | 说明 |
|------|------|
| **单体代码库** | 一个 Git 仓库，Maven 多模块工程，降低代码维护和重构成本 |
| **垂直拆分部署** | 同一 JAR，通过 Spring Profile 控制启用模块，部署到不同服务器组 |
| **模块独立数据库** | 每个业务模块独享 MySQL 实例，天然隔离，互不影响 |
| **高可用** | 每个模块至少 2 实例，Nginx 负载均衡，单实例故障自动摘除 |
| **灰度发布** | 基于 Nginx upstream weight 调整流量比例，支持小范围验证后全量 |
| **滚动更新** | 逐台重启实例，滚动过程中服务不中断 |

---

## 2. 总体架构

### 2.1 核心思路

```
┌─────────────────────────────────────────────────────────────────┐
│                        Nginx (Layer 7)                          │
│               URL 路由 + 负载均衡 + 限流 + 灰度                  │
└──────┬──────┬──────┬──────┬──────┬──────┬───────────────────────┘
       │      │      │      │      │      │
       ▼      ▼      ▼      ▼      ▼      ▼
  ┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐
  │ 用户组 ││ 商品组 ││ 订单组 ││ 支付组 ││购物车组││ 促销组  │
  │ ×2     ││ ×2     ││ ×2     ││ ×2     ││ ×2     ││ ×1     │
  └───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘
      │         │         │         │         │         │
      ▼         ▼         ▼         ▼         ▼         ▼
  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
  │MySQL │ │MySQL │ │MySQL │ │MySQL │ │MySQL │ │MySQL │
  │ 用户  │ │ 商品  │ │ 订单  │ │ 支付  │ │购物车│ │ 促销  │
  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘
      │         │         │         │         │         │
      └─────────┴─────────┴────┬────┴─────────┴─────────┘
                               │
                    ┌──────────▼──────────┐
                    │   Redis Cluster     │
                    │ (共享缓存 + Session) │
                    └─────────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │     RocketMQ        │
                    │   (异步解耦/事务)    │
                    └─────────────────────┘
```

### 2.2 项目结构

```
yuemo-mall/
├── pom.xml                          # 父 POM，依赖版本管理
├── yuemo-common/                    # 公共模块
│   ├── common-core/                 # 基础工具、异常、响应封装
│   ├── common-security/             # 认证鉴权（JWT + Spring Security）
│   └── common-mybatis/              # MyBatis 通用配置、分页插件
├── yuemo-modules/                   # 业务模块
│   ├── yuemo-user/                  # 用户模块
│   ├── yuemo-product/               # 商品模块
│   ├── yuemo-order/                 # 订单模块
│   ├── yuemo-payment/               # 支付模块
│   ├── yuemo-cart/                  # 购物车模块
│   └── yuemo-promotion/             # 优惠券/促销模块
├── yuemo-gateway/                   # 网关层（统一入口 Controller）
├── yuemo-admin/                     # 后台管理（可选独立部署）
├── docker/                          # Dockerfile 和 docker-compose
├── k8s/                             # Kubernetes 部署配置（后续扩展）
└── docs/                            # 文档
```

### 2.3 Profile 设计

通过 Spring Profile 控制当前节点启用的模块：

| Profile | 启用模块 | 说明 |
|---------|----------|------|
| `user` | 用户模块 | 部署到用户服务器组 |
| `product` | 商品模块 | 部署到商品服务器组 |
| `order` | 订单模块 | 部署到订单服务器组 |
| `payment` | 支付模块 | 部署到支付服务器组 |
| `cart` | 购物车模块 | 部署到购物车服务器组 |
| `promotion` | 促销模块 | 部署到促销服务器组 |
| `all` | 全部模块 | 开发/测试环境使用 |

```yaml
# application-user.yml 示例
spring:
  profiles: user
  datasource:
    url: jdbc:mysql://user-db-master:3306/yuemo_user
    username: ${DB_USER}
    password: ${DB_PASS}
  redis:
    cluster:
      nodes: redis1:6379,redis2:6379,redis3:6379
```

---

## 3. 模块拆分设计

### 3.1 用户模块 (yuemo-user)

| 维度 | 说明 |
|------|------|
| **职责** | 注册、登录、Token 签发、用户信息 CRUD、地址管理 |
| **核心 API** | `/api/user/register` `/api/user/login` `/api/user/info` `/api/user/address/*` |
| **拆分理由** | 用户是高频入口，读多写少，独立部署可针对性做缓存优化，登录认证与业务逻辑解耦 |
| **安全要求** | 密码 BCrypt 加密；Token 使用 JWT + 黑名单（Redis）机制 |
| **实例数** | 2 个（中小规模足够，按需扩展至 4 个） |

### 3.2 商品模块 (yuemo-product)

| 维度 | 说明 |
|------|------|
| **职责** | 商品 CRUD、分类管理、SKU 管理、库存查询、商品搜索 |
| **核心 API** | `/api/product/list` `/api/product/{id}` `/api/category/*` `/api/stock/{skuId}` |
| **拆分理由** | 商城入口流量最大，读远大于写，需要大量缓存 + 独立数据库 + 独立服务器抗住流量 |
| **特殊设计** | 商品搜索使用 MySQL FULLTEXT 或 Elasticsearch（后续扩展）；商品详情页静态化 / CDN |
| **实例数** | 2–3 个，可根据大促弹性扩容 |

### 3.3 订单模块 (yuemo-order)

| 维度 | 说明 |
|------|------|
| **职责** | 下单（含库存预占）、订单查询、订单状态流转、订单超时取消 |
| **核心 API** | `/api/order/create` `/api/order/list` `/api/order/{id}` `/api/order/cancel` |
| **拆分理由** | 核心交易链路，写操作多，事务要求高，必须独立数据库保障一致性和隔离性 |
| **关键流程** | 下单 → 预占库存(RocketMQ) → 创建订单 → 通知支付模块 |
| **实例数** | 2 个，高峰期可扩至 4 个 |

### 3.4 支付模块 (yuemo-payment)

| 维度 | 说明 |
|------|------|
| **职责** | 发起支付、支付回调处理、退款、对账 |
| **核心 API** | `/api/payment/pay` `/api/payment/callback/*` `/api/payment/refund` |
| **拆分理由** | 对接第三方（微信/支付宝），逻辑独立，安全等级最高，独立部署方便做安全加固和审计 |
| **安全要求** | 回调验签；幂等处理（防重复扣款）；支付日志完整记录 |
| **实例数** | 2 个 |

### 3.5 购物车模块 (yuemo-cart)

| 维度 | 说明 |
|------|------|
| **职责** | 添加商品到购物车、修改数量、删除、勾选结算 |
| **核心 API** | `/api/cart/add` `/api/cart/list` `/api/cart/update` `/api/cart/delete` |
| **拆分理由** | 写操作频繁（加购、改数量），读操作也频繁（查看购物车），适合用 Redis 做主力存储 + MySQL 异步落盘 |
| **存储策略** | 热数据（最近活跃购物车）存 Redis，冷数据落 MySQL；用户登录后从 MySQL 恢复到 Redis |
| **实例数** | 2 个 |

### 3.6 促销模块 (yuemo-promotion) — 可选

| 维度 | 说明 |
|------|------|
| **职责** | 优惠券发放/领取/核销、满减活动、秒杀活动管理 |
| **核心 API** | `/api/coupon/*` `/api/promotion/*` |
| **拆分理由** | 变更频率高（运营活动），独立部署可随时上线新活动而不影响交易链路 |
| **实例数** | 1–2 个 |

---

## 4. 数据库设计

### 4.1 模块与数据库映射

```
yuemo_user      → MySQL 实例 1 (db_user)     主从 ×2
yuemo_product   → MySQL 实例 2 (db_product)  主从 ×2
yuemo_order     → MySQL 实例 3 (db_order)    主从 ×2
yuemo_payment   → MySQL 实例 4 (db_payment)  主从 ×2
yuemo_cart      → MySQL 实例 5 (db_cart)     单实例 (Redis 为主)
yuemo_promotion → MySQL 实例 6 (db_promotion) 单实例
```

### 4.2 读写分离策略

```
┌──────────────┐        ┌──────────────┐
│   Master     │  Binlog │   Slave      │
│  (读写)      │────────▶│  (只读)       │
└──────────────┘        └──────────────┘
        ▲                       ▲
        │                       │
   ┌────┴────┐            ┌────┴────┐
   │ 写操作   │            │ 读操作   │
   │ INSERT  │            │ SELECT  │
   │ UPDATE  │            │ 查询列表 │
   │ DELETE  │            │ 详情查询 │
   └─────────┘            └─────────┘
```

| 策略项 | 实现方式 |
|--------|----------|
| **读写分离方案** | MyBatis 插件 + 注解 `@Master` / `@Slave` 自动路由数据源 |
| **主从同步延迟处理** | 关键写后立即读走 Master；列表查询走 Slave，容忍 1-3s 延迟 |
| **故障切换** | 主库宕机 → 手动/脚本提升从库为主库（中小规模暂不做自动故障转移） |
| **备份策略** | 每日全量备份 + binlog 增量备份，保留 30 天 |

### 4.3 跨模块事务处理

跨模块事务是单体数据库拆分的核心难题，采用 **最终一致性** 方案：

```
下单流程（跨 订单 + 商品 + 支付 三个数据库）:

  用户请求下单
       │
       ▼
  ① 订单模块：创建订单（状态=待支付）       → 订单库 (本地事务)
       │
       ▼
  ② 发送 RocketMQ 半消息（预占库存）
       │
       ▼
  ③ 商品模块：扣减库存（消费消息）          → 商品库 (本地事务)
       │  成功 → 提交 RocketMQ 消息
       │  失败 → 回滚 RocketMQ 消息
       │         → 订单模块补偿（取消订单）
       ▼
  ④ 订单模块：库存预占成功
       │
       ▼
  ⑤ 支付模块：用户支付                       → 支付库 (本地事务)
       │  超时未支付 → RocketMQ 延迟消息
       │              → 订单模块（取消订单 + 释放库存）
```

**三种跨模块交互模式：**

| 模式 | 适用场景 | 实现方式 |
|------|----------|----------|
| **RocketMQ 事务消息** | 核心交易链路（下单-库存-支付） | 半消息 + 本地事务 + 回查 |
| **本地消息表 + 定时补偿** | 非实时同步（订单状态同步到用户模块） | 本地 outbox 表 + 定时任务扫描发送 |
| **API 同步调用 + 重试** | 实时性要求低的查询 | HTTP 调用 + 指数退避重试 |

---

## 5. 缓存设计

### 5.1 Redis 架构

```
                ┌──────────────────────┐
                │   Redis Cluster      │
                │   3 Master + 3 Slave │
                │   端口: 6379          │
                └──────┬───────┬───────┘
                       │       │
          ┌────────────┼───────┼────────────┐
          │            │       │            │
      session       cache    lock         queue
      (Session)     (业务缓存) (分布式锁)   (简易队列)
```

### 5.2 各模块缓存策略

| 模块 | 缓存内容 | TTL | 策略 |
|------|----------|-----|------|
| **用户** | JWT 黑名单、用户信息、地址列表 | 30min | 登录写缓存，修改主动失效 |
| **商品** | 商品详情、分类树、热销列表 | 10min | Cache-Aside：查询先读缓存，未命中查 DB 并回填；更新时先写 DB 再删缓存 |
| **订单** | 热点订单详情、订单状态 | 5min | 短期缓存防重复查询 |
| **支付** | 支付状态（防重复回调查询） | 1min | 短 TTL，回调后主动更新 |
| **购物车** | 购物车数据（主力存储） | 7天 | Redis 为主存储，异步写 MySQL；用户登录时从 MySQL 恢复 |
| **促销** | 优惠券模板、有效活动列表 | 活动周期 | 活动创建时预热，结束时删除 |

### 5.3 Session 管理

```
方案：Spring Session + Redis 集中存储

登录流程：
  用户登录 → 验证账号密码 → 生成 JWT → JWT 写入 Redis (key: token:xxx)
       │
       ▼
  后续请求 → 网关解析 JWT → 校验 Redis 中是否存在
       │                                │
       │  存在 → 放行                    │  不存在 → 401 (已登出/过期)
       ▼
  登出 → 删除 Redis 中对应 Key

集群共享：所有模块实例连接同一 Redis Cluster
         → Session 天然共享，任一实例重启不影响用户登录态
```

### 5.4 缓存一致性策略

| 场景 | 策略 | 说明 |
|------|------|------|
| **更新商品信息** | 先写 DB → 删除缓存 | 避免并发写导致缓存脏数据 |
| **库存变更** | 先写 DB → 删除商品详情缓存 + 库存缓存 | 库存实时性要求高 |
| **用户信息更新** | 先写 DB → 更新缓存（非删除） | 用户信息读频率极高，更新缓存可避免缓存击穿 |
| **缓存穿透防护** | 布隆过滤器 + 空值短期缓存 | 防恶意查询不存在的数据 |
| **缓存雪崩防护** | TTL + 随机偏移（±20%） | 避免大批 Key 同时过期 |

---

## 6. 消息队列设计

### 6.1 RocketMQ Topic 规划

| Topic | 生产者 | 消费者 | 说明 |
|-------|--------|--------|------|
| `order-stock-preoccupy` | 订单模块 | 商品模块 | 下单预占库存（事务消息） |
| `order-stock-release` | 订单模块 | 商品模块 | 超时取消释放库存 |
| `order-status-change` | 订单模块 | 用户模块、促销模块 | 订单状态变更通知 |
| `payment-callback` | 支付模块 | 订单模块 | 支付回调处理 |
| `cart-clean-expired` | 定时任务 | 购物车模块 | 清理过期购物车 |
| `coupon-use` | 订单模块 | 促销模块 | 优惠券核销 |

### 6.2 消息可靠性保障

| 机制 | 说明 |
|------|------|
| **事务消息** | 下单-库存场景：半消息 → 执行本地事务 → 提交/回滚 |
| **消费重试** | 消费失败自动重试，最大 16 次，间隔递增 |
| **死信队列** | 重试耗尽后进入 DLQ，人工介入处理 |
| **幂等消费** | 消费者通过 `msgId` + Redis 判重，防止重复处理 |

---

## 7. 部署设计

### 7.1 服务器规划（中小规模）

```
┌─────────────────────────────────────────────────────────────────┐
│                        服务器资源规划                             │
├──────────┬──────────────────┬──────────┬──────────┬─────────────┤
│ 服务器    │ 部署内容          │ 实例数   │ 配置      │ 说明         │
├──────────┼──────────────────┼──────────┼──────────┼─────────────┤
│ srv-ngx1 │ Nginx (主)        │ 1        │ 2C4G     │ 主负载均衡    │
│ srv-ngx2 │ Nginx (备)        │ 1        │ 2C4G     │ keepalived   │
├──────────┼──────────────────┼──────────┼──────────┼─────────────┤
│ srv-app1 │ 用户 + 购物车      │ 各1      │ 4C8G     │ 读密集型      │
│ srv-app2 │ 商品 + 促销        │ 各1      │ 4C8G     │ 读密集型      │
│ srv-app3 │ 订单 + 支付        │ 各1      │ 4C8G     │ 写密集型      │
│ srv-app4 │ 用户 + 商品        │ 各1      │ 4C8G     │ 第二实例(HA)  │
│ srv-app5 │ 订单 + 购物车      │ 各1      │ 4C8G     │ 第二实例(HA)  │
├──────────┼──────────────────┼──────────┼──────────┼─────────────┤
│ srv-db1  │ MySQL 用户库       │ 1主1从   │ 4C8G     │ SSD 200G    │
│ srv-db2  │ MySQL 商品库       │ 1主1从   │ 4C8G     │ SSD 200G    │
│ srv-db3  │ MySQL 订单库       │ 1主1从   │ 8C16G    │ SSD 300G    │
│ srv-db4  │ MySQL 支付库       │ 1主1从   │ 4C8G     │ SSD 200G    │
│ srv-db5  │ MySQL 购物车+促销  │ 单实例    │ 2C4G     │ SSD 100G    │
├──────────┼──────────────────┼──────────┼──────────┼─────────────┤
│ srv-cch1 │ Redis (Master1)   │ 1        │ 4C8G     │ 内存 16G    │
│ srv-cch2 │ Redis (Master2)   │ 1        │ 4C8G     │ 内存 16G    │
│ srv-cch3 │ Redis (Master3)   │ 1        │ 4C8G     │ 内存 16G    │
├──────────┼──────────────────┼──────────┼──────────┼─────────────┤
│ srv-mq1  │ RocketMQ NameSrv  │ 1        │ 2C4G     │             │
│ srv-mq2  │ RocketMQ Broker   │ 1主1从   │ 4C8G     │ SSD 100G    │
├──────────┼──────────────────┼──────────┼──────────┼─────────────┤
│ srv-mon1 │ Prometheus       │ 1        │ 2C4G     │ 监控         │
│          │ Grafana           │          │          │             │
├──────────┼──────────────────┼──────────┼──────────┼─────────────┤
│ srv-log1 │ Elasticsearch    │ 1        │ 4C8G     │ 日志收集     │
│          │ Logstash+Kibana   │          │          │             │
└──────────┴──────────────────┴──────────┴──────────┴─────────────┘

总计：约 17–20 台服务器（含虚拟机实例）
     按 4 台物理机虚拟化，或全部使用云服务器（初期约 15 台 ECS）
```

### 7.2 横向扩展策略

```
触发条件：
  ├── CPU 使用率 > 70% 持续 5 分钟 → 告警
  ├── 接口响应时间 P99 > 500ms 持续 3 分钟 → 扩容
  └── QPS 接近当前集群容量 80% → 扩容

扩容步骤（以商品模块为例）：
  ① 准备新服务器，安装 JDK 21, JAR, 配置文件
  ② 启动实例: java -jar --spring.profiles.active=product
  ③ 验证实例健康检查: GET /actuator/health → 200
  ④ 加入 Nginx upstream: 
     upstream product_backend {
         server srv-app2:8080 weight=1;
         server srv-app4:8080 weight=1;
         server srv-new:8080 weight=1;   ← 新实例
     }
  ⑤ nginx -s reload（平滑 reload，不丢连接）
```

### 7.3 Nginx 负载均衡方案

```nginx
# /etc/nginx/conf.d/yuemo-mall.conf

# --- 限流区域定义 ---
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=5r/s;
limit_req_zone $binary_remote_addr zone=order_limit:10m rate=10r/s;
limit_conn_zone $binary_remote_addr zone=conn_limit:10m;

# --- 上游服务器组 ---
upstream user_backend {
    server srv-app1:8080 weight=1 max_fails=3 fail_timeout=30s;
    server srv-app4:8080 weight=1 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

upstream product_backend {
    server srv-app2:8080 weight=1 max_fails=3 fail_timeout=30s;
    server srv-app4:8080 weight=1 max_fails=3 fail_timeout=30s;
    keepalive 64;
}

upstream order_backend {
    server srv-app3:8080 weight=1 max_fails=3 fail_timeout=30s;
    server srv-app5:8080 weight=1 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

upstream payment_backend {
    server srv-app3:8080 weight=1 max_fails=3 fail_timeout=30s;
    server srv-app5:8080 weight=1 max_fails=3 fail_timeout=30s;
    keepalive 16;
}

upstream cart_backend {
    server srv-app1:8080 weight=1 max_fails=3 fail_timeout=30s;
    server srv-app5:8080 weight=1 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

upstream promotion_backend {
    server srv-app2:8080 weight=1 max_fails=3 fail_timeout=30s;
    keepalive 16;
}

server {
    listen 80;
    server_name api.yuemo.com;

    client_max_body_size 10m;
    limit_conn conn_limit 100;

    # --- URL 路由分发 ---
    location /api/user/ {
        limit_req zone=login_limit burst=10 nodelay;
        proxy_pass http://user_backend;
        include proxy_params;
    }

    location /api/product/ {
        proxy_pass http://product_backend;
        include proxy_params;
    }

    location /api/order/ {
        limit_req zone=order_limit burst=20 nodelay;
        proxy_pass http://order_backend;
        include proxy_params;
    }

    location /api/payment/ {
        proxy_pass http://payment_backend;
        include proxy_params;
    }

    location /api/cart/ {
        proxy_pass http://cart_backend;
        include proxy_params;
    }

    location /api/coupon/ {
        proxy_pass http://promotion_backend;
        include proxy_params;
    }

    # 健康检查端点
    location /health {
        return 200 'OK';
        add_header Content-Type text/plain;
    }
}

# proxy_params 公共配置
# include /etc/nginx/proxy_params;
# proxy_set_header Host $host;
# proxy_set_header X-Real-IP $remote_addr;
# proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
# proxy_connect_timeout 5s;
# proxy_read_timeout 30s;
# proxy_send_timeout 30s;
# proxy_http_version 1.1;
# proxy_set_header Connection "";
```

---

## 8. 性能与容错

### 8.1 瓶颈分析

| 模块 | 瓶颈点 | 风险等级 | 应对措施 |
|------|--------|----------|----------|
| **商品模块** | 高并发读（商品列表/详情） | 🔴 高 | Redis 缓存 + CDN 静态化 + 多实例水平扩展 |
| **订单模块** | 高并发写（下单） | 🔴 高 | RocketMQ 削峰 + 库存预占异步化 + 限流 |
| **用户模块** | 登录请求集中爆发 | 🟡 中 | Redis Session + 限流 |
| **支付模块** | 第三方回调延迟 | 🟡 中 | 异步回调处理 + 幂等保障 |
| **购物车模块** | 高频写操作 | 🟡 中 | Redis 主力存储 + 异步落盘 |
| **数据库** | 连接池耗尽 | 🔴 高 | HikariCP 合理配置 + 读写分离 + 慢 SQL 监控 |

### 8.2 资源分配建议

| 组件 | CPU | 内存 | JVM 参数 |
|------|-----|------|----------|
| 用户实例 | 2 Core | 4G | `-Xms2g -Xmx3g -XX:+UseG1GC` |
| 商品实例 | 2 Core | 4G | `-Xms2g -Xmx3g -XX:+UseG1GC` |
| 订单实例 | 2 Core | 4G | `-Xms2g -Xmx3g -XX:+UseG1GC` |
| 支付实例 | 1 Core | 2G | `-Xms1g -Xmx2g -XX:+UseG1GC` |
| 购物车实例 | 1 Core | 3G | `-Xms1g -Xmx2g -XX:+UseG1GC` |

### 8.3 异常隔离策略

```
                      ┌─────────────┐
                      │   Nginx     │
                      │  层：限流    │ ← 第一道防线：限制每个接口的 QPS
                      └──────┬──────┘
                             │
                ┌────────────┼────────────┐
                │            │            │
           ┌────▼────┐ ┌────▼────┐ ┌────▼────┐
           │ 商品模块  │ │ 订单模块  │ │ 用户模块  │
           │ (正常)   │ │ (过载)   │ │ (正常)   │
           └─────────┘ └────┬────┘ └─────────┘
                            │
                     ┌──────▼──────┐
                     │  订单模块    │
                     │  自身限流    │ ← 第二道防线：模块级限流
                     │  (Sentinel)  │
                     └──────┬──────┘
                            │
                     ┌──────▼──────┐
                     │  降级策略    │ ← 第三道防线：优雅降级
                     │  返回"排队中" │
                     └─────────────┘

隔离措施：
  ├── 模块间相互独立部署，单模块故障不影响其他模块
  ├── Sentinel 熔断降级：订单模块 QPS 过高时返回友好提示
  ├── 数据库连接池隔离：每个模块独立的 HikariCP 连接池
  └── 线程池隔离：Tomcat 线程池按模块 Profile 调整
```

### 8.4 容错与高可用

| 场景 | 应对方案 |
|------|----------|
| **Nginx 单点** | keepalived VIP 漂移，主备 Nginx 自动切换 |
| **应用实例宕机** | Nginx `max_fails` + `fail_timeout` 自动摘除 |
| **MySQL 主库宕机** | 手动/脚本将从库提升为主库（预计恢复 5-10 分钟） |
| **Redis 节点宕机** | Cluster 自动故障转移，从节点升级为主节点 |
| **RocketMQ Broker 宕机** | 主从自动切换，生产者自动重连 |
| **级联故障** | Sentinel 熔断 + 核心链路隔离（下单走专用线程池） |

---

## 9. 灰度发布与滚动更新

### 9.1 灰度发布方案

```
┌──────────────────────────────────────────────────────┐
│                    灰度发布流程                        │
├──────────────────────────────────────────────────────┤
│                                                      │
│  ① 部署灰度实例（新版本）                              │
│     └── 启动 1 台新版本实例，注册到 Nginx             │
│                                                      │
│  ② 配置灰度规则                                       │
│     ├── 方式A: Nginx weight 分流                      │
│     │   upstream product_backend {                   │
│     │       server srv-app2:8080 weight=90;  ← 旧版本 │
│     │       server srv-gray:8080 weight=10;  ← 新版本 │
│     │   }                                            │
│     │                                                │
│     └── 方式B: Nginx split_clients (按用户ID)          │
│         split_clients "${cookie_user_id}" $variant {  │
│             10%   gray;      # 10% 用户走灰度         │
│             *     stable;    # 其余走稳定版            │
│         }                                            │
│                                                      │
│  ③ 观察灰度实例 15 分钟                               │
│     ├── 错误率 < 0.1%                                │
│     ├── 接口 RT < 基线上浮 20%                        │
│     └── 无异常日志                                    │
│                                                      │
│  ④ 逐步放量：10% → 30% → 50% → 100%                  │
│     每步观察 10-15 分钟                               │
│                                                      │
│  ⑤ 全量后下线旧版本实例                               │
│                                                      │
│  异常回滚：                                           │
│     └── Nginx 摘除灰度实例 → 重启旧版本 → 分析日志     │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### 9.2 滚动更新方案

```
现有实例: [A(v1)] [B(v1)] [C(v1)]
              ↓
步骤1: 摘除 A → 升级 A → 启动 A(v2) → 加入集群
       [A(v2)] [B(v1)] [C(v1)]    ← 验证 A(v2) 正常
              ↓
步骤2: 摘除 B → 升级 B → 启动 B(v2) → 加入集群
       [A(v2)] [B(v2)] [C(v1)]    ← 验证 B(v2) 正常
              ↓
步骤3: 摘除 C → 升级 C → 启动 C(v2) → 加入集群
       [A(v2)] [B(v2)] [C(v2)]    ← 全部升级完成

关键保障：
  ├── 每步摘除前确保 Nginx 不再分发新请求 (drain 30s)
  ├── 摘除时处理完已接收的请求 (graceful shutdown)
  ├── 始终保持至少 1 个实例在线
  └── 每步之间观察 2-3 分钟，确认无异常
```

### 9.3 Docker 化部署（推荐）

```yaml
# docker-compose.yml (单模块示例)
version: '3.8'
services:
  yuemo-product-1:
    image: yuemo-mall:${VERSION}
    ports:
      - "8081:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=product
      - DB_URL=jdbc:mysql://srv-db2:3306/yuemo_product
      - REDIS_NODES=redis1:6379,redis2:6379,redis3:6379
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3

  yuemo-product-2:
    image: yuemo-mall:${VERSION}
    ports:
      - "8082:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=product
      - DB_URL=jdbc:mysql://srv-db2:3306/yuemo_product
      - REDIS_NODES=redis1:6379,redis2:6379,redis3:6379
    restart: always
```

---

## 10. 监控与日志

### 10.1 监控体系 (Prometheus + Grafana)

```
┌────────────────────────────────────────────────────┐
│                    Grafana 看板                      │
├──────────┬──────────┬──────────┬───────────────────┤
│ JVM 监控  │ 业务监控  │ 中间件监控│ 基础设施监控       │
├──────────┼──────────┼──────────┼───────────────────┤
│ 堆内存    │ QPS      │ Redis    │ CPU               │
│ GC 次数   │ 接口 RT   │ MySQL    │ 内存              │
│ 线程数    │ 错误率    │ RocketMQ │ 磁盘              │
│ 类加载    │ 订单量    │ Nginx    │ 网络              │
└──────────┴──────────┴──────────┴───────────────────┘

告警规则 (Prometheus AlertManager):
  ├── 实例宕机: up == 0 → 立即告警
  ├── 错误率 > 1%: rate(http_server_requests_total{status="5xx"}[5m]) > 0.01
  ├── 接口 RT P99 > 1s: histogram_quantile(0.99, ...) > 1
  ├── JVM 堆使用率 > 85%: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.85
  └── MySQL 慢查询 > 50条/5min
```

### 10.2 日志收集 (ELK)

```
应用实例 (Filebeat) ──→ Logstash ──→ Elasticsearch ──→ Kibana
       │                      │
  logback 输出          解析、过滤、结构化
  JSON 格式             添加模块/实例标签
```

关键日志规范：
- 所有日志输出 JSON 格式，包含 `traceId`, `module`, `instance`, `timestamp`
- 通过 `traceId`（MDC 自动注入）串联一次请求的完整链路
- 日志级别：开发 DEBUG / 生产 INFO / 关键操作 INFO 显式记录

---

## 11. 技术选型总览

| 类别 | 技术 | 版本 | 选择理由 |
|------|------|------|----------|
| **语言** | Java | 21 LTS | 长期支持、虚拟线程、性能与生态成熟 |
| **框架** | Spring Boot | 3.4.5 | 单体项目首选，自动配置、生态齐全 |
| **ORM** | MyBatis-Plus | 3.5.9 | 轻量灵活，须用 `mybatis-plus-spring-boot3-starter`，额外引入 `mybatis-plus-jsqlparser` |
| **安全** | Spring Security + JWT | 6.4.5 | 无状态认证，适合分布式 Session |
| **数据库** | MySQL | 8.0 | 生态成熟、运维简单、InnoDB 事务可靠 |
| **连接池** | HikariCP | 5.1.x | Spring Boot 默认，性能最优 |
| **缓存** | Redis | 7.x Cluster | 高性能、支持持久化、Cluster 高可用 |
| **消息队列** | RocketMQ | 5.x（Starter 2.3.3） | 事务消息、顺序消息，电商场景首选，独立引入 `rocketmq-v5-client-spring-boot-starter` |
| **负载均衡** | Nginx | 1.24+ | 高性能反向代理、路由灵活、社区活跃 |
| **限流熔断** | Sentinel | 1.8.9 | 阿里开源，须用 `sentinel-spring-webmvc-v6x-adapter` 适配 Spring MVC 6.x |
| **监控** | Prometheus + Grafana | — | 云原生标准，指标丰富，看板可定制 |
| **日志** | ELK (Filebeat+Logstash+ES+Kibana) | 8.x | 日志集中收集、全文搜索、可视化分析 |
| **容器化** | Docker + Docker Compose | — | 环境一致性、快速部署、便于滚动更新 |
| **构建** | Maven | 3.9+ | 多模块管理成熟，国内镜像丰富 |

---

## 12. 架构图

### 12.1 完整架构总览

```
                              ┌──────────────┐
                              │   客户端       │
                              │ (Web/iOS/App) │
                              └──────┬───────┘
                                     │ HTTPS
                              ┌──────▼───────┐
                              │  CDN / WAF   │
                              │ (静态资源加速) │
                              └──────┬───────┘
                                     │
                    ┌────────────────┴────────────────┐
                    │                                 │
              ┌─────▼─────┐                   ┌──────▼──────┐
              │ Nginx (主) │◀─── keepalived ──▶│ Nginx (备)  │
              │ srv-ngx1   │    VIP 漂移       │ srv-ngx2    │
              └─────┬─────┘                   └─────────────┘
                    │
    ┌───────┬───────┼───────┬───────┬───────┬───────┐
    │       │       │       │       │       │       │
    ▼       ▼       ▼       ▼       ▼       ▼       ▼
┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐
│用户组 ││商品组 ││订单组 ││支付组 ││购物车 ││促销组 │  ← 应用层
│×2    ││×2    ││×2    ││×2    ││×2    ││×1    │
└──┬───┘└──┬───┘└──┬───┘└──┬───┘└──┬───┘└──┬───┘
   │       │       │       │       │       │
   │       │       │       │       │       │
   ▼       ▼       ▼       ▼       ▼       ▼
┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐
│MySQL ││MySQL ││MySQL ││MySQL ││MySQL ││MySQL │  ← 数据层
│用户库 ││商品库 ││订单库 ││支付库 ││购物车 ││促销库 │
│主+从  ││主+从  ││主+从  ││主+从  ││单实例 ││单实例 │
└──────┘└──────┘└──────┘└──────┘└──────┘└──────┘
   │       │       │       │       │       │
   └───────┴───────┴───┬───┴───────┴───────┘
                       │
          ┌────────────┼────────────┐
          │            │            │
    ┌─────▼────┐ ┌─────▼────┐ ┌────▼─────┐
    │  Redis   │ │ RocketMQ │ │    ELK    │
    │ Cluster  │ │ 集群      │ │ 日志收集  │  ← 基础设施层
    │ 3M+3S    │ │ 1M+1S    │ │          │
    └──────────┘ └──────────┘ └──────────┘
                       │
          ┌────────────▼────────────┐
          │  Prometheus + Grafana   │
          │  监控 + 告警            │  ← 可观测性层
          └─────────────────────────┘
```

### 12.2 下单核心链路

```
  用户                      Nginx                  订单模块            RocketMQ          商品模块         支付模块
  │                          │                       │                  │                │               │
  │  POST /api/order/create  │                       │                  │                │               │
  │─────────────────────────▶│                       │                  │                │               │
  │                          │  路由到订单组           │                  │                │               │
  │                          │──────────────────────▶│                  │                │               │
  │                          │                       │                  │                │               │
  │                          │                       │  ① 参数校验       │                │               │
  │                          │                       │  ② 查询商品信息    │                │               │
  │                          │                       │     (HTTP→商品模块)│                │               │
  │                          │                       │◀─────────────────│                │               │
  │                          │                       │                  │                │               │
  │                          │                       │  ③ 创建订单       │                │               │
  │                          │                       │  (INSERT 订单库)  │                │               │
  │                          │                       │  状态=待支付       │                │               │
  │                          │                       │                  │                │               │
  │                          │                       │  ④ 发送事务消息    │                │               │
  │                          │                       │─────────────────▶│                │               │
  │                          │                       │                  │  预占库存消息   │               │
  │                          │                       │                  │───────────────▶│               │
  │                          │                       │                  │                │  扣减库存       │
  │                          │                       │                  │                │  (UPDATE)      │
  │                          │                       │                  │◀───────────────│               │
  │                          │                       │                  │  成功/失败      │               │
  │                          │                       │◀─────────────────│                │               │
  │                          │                       │                  │                │               │
  │                          │                       │  ⑤ 返回订单号     │                │               │
  │                          │◀──────────────────────│                  │                │               │
  │◀─────────────────────────│                       │                  │                │               │
  │  {orderId: "xxx"}        │                       │                  │                │               │
  │                          │                       │                  │                │               │
  │  (用户去支付)             │                       │                  │                │               │
  │─────────────────────────▶│──────────────────────▶│                  │                │──────────────▶│
  │                          │                       │                  │                │               │ 发起支付
  │                          │                       │                  │                │               │ 调第三方
  │                          │                       │                  │                │               │
  │                          │                       │                  │                │  支付回调      │
  │                          │                       │◀─────────────────│────────────────│───────────────│
  │                          │                       │  更新订单=已支付   │                │               │
  │                          │                       │                  │                │               │
  │                          │                       │  超时30min未支付   │                │               │
  │                          │                       │  RocketMQ延迟消息  │                │               │
  │                          │                       │  → 取消订单       │                │               │
  │                          │                       │  → 释放库存       │                │               │
```

### 12.3 数据流图

```
                     ┌──────────────────┐
                     │    读请求流程      │
                     └──────────────────┘
                           │
                     Nginx 接收请求
                           │
                     URL 路由匹配
                           │
                    转发到应用实例
                           │
                     ┌─────▼─────┐
                     │ 查 Redis   │
                     │ 缓存?      │
                     └──┬────┬───┘
                  命中  │    │  未命中
                        │    │
                   返回数据  │
                            ▼
                     ┌─────────────┐
                     │ 查 MySQL     │
                     │ (Slave 只读) │
                     └──────┬──────┘
                            │
                     ┌──────▼──────┐
                     │ 回填 Redis   │
                     │ TTL + 随机   │
                     └──────┬──────┘
                            │
                         返回数据


                     ┌──────────────────┐
                     │    写请求流程      │
                     └──────────────────┘
                           │
                     Nginx 接收请求
                           │
                     URL 路由匹配
                           │
                    转发到应用实例
                           │
                     ┌─────▼─────┐
                     │ 参数校验    │
                     │ 业务校验    │
                     └─────┬─────┘
                           │
                     ┌─────▼─────┐
                     │ 写 MySQL   │
                     │ (Master)  │
                     └─────┬─────┘
                           │
                     ┌─────▼─────┐
                     │ 删除/更新   │
                     │ Redis 缓存  │
                     └─────┬─────┘
                           │
                     ┌─────▼─────┐
                     │ 异步消息?   │
                     │ (如有需要)  │
                     └─────┬─────┘
                           │
                      返回结果
```

---

## 附录

### A. 启动命令参考

> **前置要求**: JDK 21 + Maven 3.9+

```bash
# 开发环境（全模块）
java -jar yuemo-mall.jar --spring.profiles.active=all

# 生产环境（按模块，JDK 21 推荐使用 ZGC 或 G1GC）
java -jar -Xms2g -Xmx3g -XX:+UseG1GC \
  -Dspring.profiles.active=product \
  -Dserver.port=8080 \
  yuemo-mall.jar

# Docker（基础镜像使用 JDK 21）
docker run -d \
  -e SPRING_PROFILES_ACTIVE=product \
  -e DB_URL=jdbc:mysql://host:3306/yuemo_product \
  -p 8080:8080 \
  yuemo-mall:1.0.0
```

### B. 关键配置项

```yaml
# application.yml 核心配置
spring:
  application:
    name: yuemo-mall
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:all}

# 数据库连接池
spring.datasource:
  hikari:
    minimum-idle: 10
    maximum-pool-size: 50
    connection-timeout: 3000
    idle-timeout: 600000
    max-lifetime: 1800000

# Redis
spring.redis:
  cluster:
    nodes: ${REDIS_NODES:localhost:6379}
  lettuce:
    pool:
      max-active: 50
      max-idle: 20
      min-idle: 10

# RocketMQ（独立 Starter，非 Spring Cloud Alibaba）
rocketmq:
  name-server: ${ROCKETMQ_NS:localhost:9876}
  producer:
    group: yuemo-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 3
  consumer:
    group: yuemo-consumer-group

# Sentinel（独立使用，非 Spring Cloud Alibaba）
sentinel:
  transport:
    dashboard: ${SENTINEL_DASHBOARD:localhost:8080}
  eager: true

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### C. 核心依赖 Maven 坐标（JDK 21 + Spring Boot 3.4.5 精确版本）

```xml
<!-- Spring Boot 父 POM -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.5</version>
</parent>

<!-- MyBatis-Plus（必须用 boot3-starter） -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.9</version>
</dependency>
<!-- MyBatis-Plus 3.5.9 起必须额外引入 jsqlparser -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
    <version>3.5.9</version>
</dependency>

<!-- RocketMQ 5.x 官方 Spring Boot Starter（独立引入，非 SCA） -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-v5-client-spring-boot-starter</artifactId>
    <version>2.3.3</version>
</dependency>

<!-- Sentinel 1.8.9 + Spring MVC 6.x 适配器（独立引入，非 SCA） -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-core</artifactId>
    <version>1.8.9</version>
</dependency>
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-spring-webmvc-v6x-adapter</artifactId>
    <version>1.8.9</version>
</dependency>

<!-- MySQL 8.0 驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Spring Security + JWT（版本由 Spring Boot 父 POM 管理） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
```

> **注意**: 以上 Sentinel 和 RocketMQ 均采用**独立 Starter 方式**引入，不依赖 Spring Cloud Alibaba 全家桶。原因是 Spring Cloud Alibaba 目前存在版本断层——无针对 Spring Boot 3.4.x（Spring Cloud 2024.0.x）的官方发布版（2023.0.1.0 适配 3.2.x，2025.0.0.0 直接跳至 3.5.x）。独立引入可完全绕过此问题。

### D. 后续演进方向

| 阶段 | 目标 | 改动范围 |
|------|------|----------|
| **Phase 1（当前）** | 单体多模块部署，验证业务模型 | — |
| **Phase 2** | 引入 Docker Compose 一键部署 | 部署方式升级 |
| **Phase 3** | 核心模块（订单/支付）拆分为独立服务 | 模块间 HTTP → RPC 通信 |
| **Phase 4** | 全量微服务化 + K8s 编排 | 独立服务 + Service Mesh |
