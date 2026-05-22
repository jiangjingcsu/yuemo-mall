# 已知问题与解决思路

> 当前系统已知的技术债务和改进方向。AI 设计新方案时参考，避免引入更大债务。
> 关联: constraints/payment.md（支付已知风险）、evaluation/security-check.md（安全评估已知技术债务）、memory/incidents.md（历史事故）

---

## 技术债务

### TD001: CartSyncConsumer switch-case

```yaml
类别: 代码质量
状态: 已解决
解决说明: 已重构为 Map<Class, CartActionHandler> 策略路由，6 个 Handler 实现
位置: yuemo-cart/mq/CartSyncConsumer.java → mq/handler/
```

### TD002: PaymentServiceImpl if-else

```yaml
类别: 代码质量
状态: 已解决
解决说明: 已重构为 Map<Integer, PayTypeHandler> 策略路由，3 个 Handler 实现
位置: yuemo-payment/service/impl/PaymentServiceImpl.java → handler/
```

### TD003: 贫血 Entity

```yaml
类别: 架构设计
状态: 已解决
解决说明: Payment/Order 均已充血，含 verifyOwnership/markSuccess/markRefunded/pay/ship/confirmReceive/cancel 等领域方法
残留风险: 仍使用 @Data，外部可绕过领域方法直接 setStatus()；其他模块 Entity（CartItem/Coupon 等）可能仍为贫血模型
位置: yuemo-payment/entity/Payment.java、yuemo-order/entity/Order.java
```

### TD004: XSS 防护缺失

```yaml
类别: 安全漏洞
状态: 已解决
解决说明: XssFilterConfig + XssHttpServletRequestWrapper 已实现，覆盖 <script>/javascript:/事件属性/<iframe>/<object>/<embed>/eval()/expression() 等 8 种 XSS 模式
位置: yuemo-common/common-core/src/main/java/com/yuemo/common/core/xss/
```

### TD005: CORS 配置过宽

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-gateway/config/GatewayWebMvcConfig.java
问题: allowedOriginPatterns("*") + allowCredentials(true)，任意域名可携带 Cookie 发起跨域请求
建议: 生产环境替换为具体前端域名白名单
优先级: P1
预计影响: 减少 CSRF 攻击面
关联约束: constraints/production.md（CORS 通配符 + credentials 列为绝对禁止）
```

### TD006: 魔法值替换

```yaml
类别: 代码质量
状态: 已解决
解决说明: PayType（WECHAT/ALIPAY/BALANCE）、OrderStatus（UNPAID/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDED）枚举已定义，业务代码已使用枚举替代硬编码数字
位置: yuemo-payment/enums/PayType.java、yuemo-order/enums/OrderStatus.java
```

### TD007: 支付回调签名验证为桩实现

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-payment/service/impl/PaymentServiceImpl.java handleCallback()
问题: verifySign() 始终返回 true，任何伪造的回调请求都会被接受
建议: 接入微信/支付宝 SDK 实现真实签名验证
优先级: P0
预计影响: 致命漏洞，伪造回调可导致资金损失
关联约束: constraints/payment.md（支付回调不做签名验证 = 绝对禁止）
关联事故: 无（接入第三方支付前必须修复）
```

### TD008: createPayment 无分布式锁

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-payment/service/impl/PaymentServiceImpl.java createPayment()
问题: DB 无 (order_id, status) 唯一索引保护，仅靠查询幂等存在竞态，高并发下可能创建重复支付单
建议: 添加 Redis 分布式锁或 DB 唯一索引
优先级: P0
预计影响: 重复扣款风险
关联约束: constraints/payment.md（createPayment 无并发保护）
```

### TD009: 退款接口无幂等保护

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-payment/service/impl/PaymentServiceImpl.java refund()
问题: 同一订单可重复发起退款，无 Redis 幂等键或 DB 唯一约束保护
建议: 添加 Redis 幂等键（payment:refund:{orderId}）或 DB 唯一约束
优先级: P0
预计影响: 重复退款导致资金损失
关联约束: constraints/payment.md（退款不做退款单号唯一性校验 = 绝对禁止）
```

### TD010: JWT 密钥默认值可预测

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-common/common-security/utils/JwtTokenProvider.java、docker-compose.yml
问题: JWT 密钥默认值 yuemo-mall-jwt-secret-key-2024-... 为可预测字符串，生产必须通过环境变量 ${JWT_SECRET} 覆盖
建议: 清理 docker-compose.yml 中硬编码默认值，生产强制要求环境变量注入
优先级: P1
预计影响: 签名可被伪造，所有接口鉴权失效
关联事故: INC003（JWT 密钥长度不足）
关联约束: constraints/production.md（docker-compose.yml 明文默认密码 = 绝对禁止）
```

### TD011: DB/Redis 密码明文默认值

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-server/src/main/resources/application.yml、docker-compose.yml
问题: DB_PASS 和 REDIS_PASSWORD 默认值均为 jiangjing，生产必须通过环境变量覆盖
建议: 清理所有明文默认密码，生产强制要求 .env 注入
优先级: P1
预计影响: 密码泄露则数据库/Redis 完全暴露
关联约束: constraints/production.md（docker-compose.yml 明文默认密码 = 绝对禁止）
```

### TD012: docker-compose.yml 保留明文默认密码

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-backend/docker/docker-compose.yml
问题: JWT_SECRET/DB_PASS/REDIS_PASSWORD 均有明文硬编码默认值
建议: 移除所有敏感默认值，通过 .env 文件注入，.env 加入 .gitignore
优先级: P1
预计影响: 代码仓库泄露即全量凭据泄露
关联约束: constraints/production.md、constraints/infra.md
```

### TD013: 退款单号生成碰撞风险

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-payment/service/impl/PaymentServiceImpl.java generateRefundNo()
问题: userId%10000 + IdGenerator.nextId()%1000 组合在高并发下可能碰撞
建议: 使用雪花算法完整 ID 或 DB 唯一索引保底
优先级: P1
预计影响: 退款单号冲突导致退款记录覆盖
关联约束: constraints/payment.md（已知风险）
```

### TD014: BalancePayHandler MQ 发送时机

```yaml
类别: 架构设计
状态: 活跃
位置: yuemo-payment/handler/BalancePayHandler.java doPay()
问题: 余额支付成功后，MQ 在 insert 之前发送，消费者可能读到未持久化的支付单
建议: 将 MQ 发送移至 insert 之后，或使用 RocketMQ 事务消息
优先级: P2
预计影响: 消费端读到脏数据
关联约束: constraints/payment.md（MQ 约束）
```

### TD015: OrderRefundConsumer 消息类型不一致

```yaml
类别: 代码质量
状态: 活跃
位置: yuemo-order/mq/OrderRefundConsumer.java
问题: 声明为 String 类型但生产者发送 RefundMessage 对象，序列化/反序列化可能不一致
建议: 统一消息类型为 RefundMessage
优先级: P2
预计影响: 消息解析失败导致退款库存恢复异常
关联约束: constraints/payment.md（MQ 约束）
```

### TD016: 管理员角色校验依赖 Redis 缓存

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-gateway/filter/GatewayAuthFilter.java
问题: /api/admin/** 角色校验依赖 Redis user:role:{userId} 缓存，TTL 24h，缓存篡改风险
建议: 缩短 TTL 或增加 DB 实时回查机制
优先级: P2
预计影响: 角色变更后 24 小时内不生效，可能越权
```

### TD017: 敏感字段脱敏未实现

```yaml
类别: 安全漏洞
状态: 活跃
位置: 全局 VO 层
问题: rules/security.md §3 定义了手机号/身份证/银行卡/邮箱4类脱敏规则，代码未实现 @JsonSerialize 自定义脱敏序列化器
建议: 添加脱敏注解和序列化器，对 VO 中敏感字段自动脱敏
优先级: P2
预计影响: API 响应暴露完整手机号/邮箱等敏感信息
```

### TD018: UserServiceImpl.updateUser 无归属校验

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-user/service/impl/UserServiceImpl.java updateUser()
问题: 无 verifyOwnership，任意用户可通过修改接口篡改他人信息（越权）
建议: 添加 verifyOwnership(userId) 校验
优先级: P2
预计影响: 水平越权，用户 A 可修改用户 B 的信息
```

### TD019: Token 存储在 localStorage

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-frontend/src/stores/userSlice.ts
问题: Token 存储在 localStorage，XSS 脚本可直接窃取
建议: 迁移至 httpOnly Cookie，或至少使用内存变量 + refreshToken 机制
优先级: P2
预计影响: XSS 攻击可直接获取 Token 冒充用户
```

### TD020: Admin 路由无角色权限守卫

```yaml
类别: 安全漏洞
状态: 活跃
位置: 前端路由配置
问题: Admin 路由仅检查 token 是否存在，未校验用户角色是否为 ADMIN
建议: 添加角色权限守卫，非 ADMIN 用户重定向或提示无权限
优先级: P2
预计影响: 普通用户可直接访问管理后台页面
```

### TD021: 缺少 CSP 策略

```yaml
类别: 安全漏洞
状态: 活跃
位置: Nginx 配置 / 前端 index.html
问题: 未配置 Content-Security-Policy，无法防御 XSS/数据注入/点击劫持
建议: Nginx add_header Content-Security-Policy "..." 配置合理 CSP 策略
优先级: P2
预计影响: 缺少深度防御中的最后一道防线
```

### TD022: MySQL 连接未启用 SSL

```yaml
类别: 安全漏洞
状态: 活跃
位置: yuemo-server/src/main/resources/application.yml
问题: 数据库连接 useSSL=false + sslMode=DISABLED
建议: 生产环境启用 SSL，仅开发环境允许关闭
优先级: P2
预计影响: 数据库通信明文传输，可被中间人窃听
关联约束: constraints/production.md（useSSL=false 用于生产 = 绝对禁止）
```

### TD023: 日志脱敏格式未统一

```yaml
类别: 安全漏洞
状态: 活跃
位置: 全局日志输出
问题: 日志中未统一应用手机号 138****1234 等脱敏格式，可能输出完整敏感信息
建议: 日志框架配置脱敏 Layout，统一敏感字段输出格式
优先级: P2
预计影响: 日志泄露含完整手机号/邮箱
```

### TD024: 管理员操作审计日志未实现

```yaml
类别: 安全漏洞
状态: 活跃
位置: 全局 Admin 接口
问题: 管理员操作（修改/删除/退款）未记录操作人、操作时间、操作内容
建议: 添加 AOP 切面自动记录管理员操作审计日志
优先级: P2
预计影响: 管理员恶意操作无法追溯
```

---

## 活跃债务优先级汇总

```yaml
P0（致命 - 涉及资金安全）:
  - TD007: 支付回调签名验证为桩实现（可伪造回调）
  - TD008: createPayment 无分布式锁（可重复扣款）
  - TD009: 退款接口无幂等保护（可重复退款）

P1（高危 - 涉及鉴权/凭据安全）:
  - TD005: CORS 配置过宽（跨域攻击面）
  - TD010: JWT 密钥默认值可预测
  - TD011: DB/Redis 密码明文默认值
  - TD012: docker-compose.yml 明文默认密码
  - TD013: 退款单号生成碰撞风险

P2（中危 - 涉及越权/信息泄露/代码质量）:
  - TD014: BalancePayHandler MQ 发送时机
  - TD015: OrderRefundConsumer 消息类型不一致
  - TD016: 管理员角色校验依赖 Redis 缓存
  - TD017: 敏感字段脱敏未实现
  - TD018: UserServiceImpl.updateUser 无归属校验
  - TD019: Token 存储在 localStorage
  - TD020: Admin 路由无角色权限守卫
  - TD021: 缺少 CSP 策略
  - TD022: MySQL 连接未启用 SSL
  - TD023: 日志脱敏格式未统一
  - TD024: 管理员操作审计日志未实现
```

---

## 改进方向

```yaml
短期（安全加固 - P0 优先）:
  - 修复 TD007: 支付回调签名验证（接入微信/支付宝 SDK）
  - 修复 TD008: createPayment 添加分布式锁（Redis 或 DB 唯一索引）
  - 修复 TD009: 退款接口添加幂等保护（Redis 幂等键）
  - 修复 TD005: 生产环境 CORS 收紧为具体域名

中期（配置与脱敏 - P1/P2）:
  - 修复 TD010/TD011/TD012: 清理所有明文默认密码/密钥，强制 .env 注入
  - 修复 TD013: 退款单号使用雪花算法完整 ID
  - 修复 TD017/TD023/TD024: 敏感字段脱敏 + 日志脱敏 + 管理员审计日志
  - 修复 TD016: 管理员角色缓存 TTL 缩短或实时回查
  - 修复 TD018: updateUser 添加 verifyOwnership

长期（架构优化 - P2）:
  - 修复 TD003 残留: Entity 去除 @Data 暴露的 setter 方法
  - 修复 TD014: BalancePayHandler MQ 发送时机调整或使用事务消息
  - 修复 TD015: OrderRefundConsumer 统一消息类型
  - 修复 TD019: Token 存储迁移至 httpOnly Cookie
  - 修复 TD020: Admin 路由添加角色权限守卫
  - 修复 TD021: 添加 CSP 策略
  - 修复 TD022: 生产环境 MySQL 启用 SSL
  - 微服务拆分评估
  - 全链路监控
```
