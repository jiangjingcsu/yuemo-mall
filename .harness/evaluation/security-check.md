# 安全评估

> AI 生成/修改代码后的安全评估清单。
> 安全规则依据: rules/security.md（10 维度）、constraints/production.md（生产红线）、constraints/infra.md、constraints/payment.md

---

## 评估维度

### 1. 认证与鉴权（权重 15%）

```yaml
检查:
  - [ ] 敏感接口经 GatewayAuthFilter 鉴权（白名单外的接口需 Bearer Token）
  - [ ] /api/admin/** 路径有 ADMIN 角色校验（GatewayAuthFilter + Redis 角色缓存）
  - [ ] 方法级权限使用 @PreAuthorize 注解（如有细粒度控制需求）
  - [ ] JWT 密钥通过环境变量注入（禁止可预测的默认值用于生产）
  - [ ] Token 不在 URL 中传递（使用 Authorization Header）
  - [ ] 无硬编码的密码/密钥/Token（包括 application.yml 和 docker-compose.yml 默认值）
  - [ ] 密码使用 BCrypt 加密（非 MD5/SHA，见 PasswordEncoder）
  - [ ] 登出时 Token 加入 Redis 黑名单
  - [ ] 登录失败信息统一（不区分"用户名不存在"还是"密码错误"）

扣分:
  - 敏感接口无鉴权: -25 分（BLOCK）
  - 硬编码密钥/密码（含可预测默认值）: -25 分（BLOCK）
  - 明文密码存储: -25 分（BLOCK）
  - Admin 路径无角色校验: -20 分
  - 登录失败信息泄露用户名枚举: -10 分
```

### 2. 输入安全（权重 15%）

```yaml
检查:
  - [ ] Controller 参数使用 @Valid 校验
  - [ ] 字符串字段有 @NotBlank/@Size 限制
  - [ ] 数值字段有 @NotNull/@Min/@Max 限制
  - [ ] SQL 参数使用 #{param}（非 ${param}）
  - [ ] 分页参数有最大值限制（pageSize ≤ 100）
  - [ ] 全局 XSS 过滤器（XssFilter + XssHttpServletRequestWrapper）
  - [ ] 文件上传有类型/大小限制
  - [ ] 文件路径校验，禁止 ../ 路径遍历
  - [ ] 禁止 Runtime.exec() 拼接用户输入（命令注入防护）
  - [ ] CSRF 防护（Bearer Token 方式天然抗 CSRF，表单提交需额外防护）

扣分:
  - ${} 拼接 SQL: -25 分（BLOCK - SQL 注入）
  - Runtime.exec() 拼接用户输入: -25 分（BLOCK - 命令注入）
  - 无输入校验: -15 分
  - 文件上传无限制: -20 分
  - 缺少全局 XSS 过滤器: -10 分
  - 文件路径未校验路径遍历: -15 分
```

### 3. 数据安全与脱敏（权重 10%）

```yaml
检查:
  - [ ] 敏感数据（手机号/密码/身份证/银行卡）不在日志中输出
  - [ ] API 响应不暴露数据库错误信息（GlobalExceptionHandler 返回通用错误码）
  - [ ] 异常信息不泄露内部实现细节
  - [ ] 用户数据访问有归属校验（verifyOwnership）
  - [ ] 敏感字段脱敏输出（手机号 138****1234、邮箱 t***@example.com、身份证前3后4、银行卡后4位）
  - [ ] API 响应不包含 password 字段
  - [ ] 无批量数据导出无限制

扣分:
  - 越权数据访问: -25 分（BLOCK）
  - 响应包含 password 字段: -25 分（BLOCK）
  - 日志输出密码/完整手机号: -20 分
  - 响应暴露 SQL 错误: -15 分
  - 敏感字段未脱敏: -10 分
```

### 4. 接口安全（权重 10%）

```yaml
检查:
  - [ ] 登录/注册接口有限流（10次/分钟，Redis 滑动窗口）
  - [ ] 支付/下单接口有限流（30次/分钟）
  - [ ] Sentinel 熔断降级配置正确（慢调用比例>50%触发）
  - [ ] 关键操作有幂等保护（支付/下单防重复提交）
  - [ ] MQ 消费者有 Redis 幂等键（mq:consumed:{topic}:{bizId}）

扣分:
  - 关键操作无幂等保护: -20 分
  - 登录/支付接口无限流: -15 分
  - MQ 消费者无幂等: -15 分
```

### 5. 支付安全（权重 15%）

```yaml
检查（仅支付模块）:
  - [ ] 金额使用 BigDecimal（非 float/double，Entity/DTO/VO/MQ 消息体均不例外）
  - [ ] 支付回调有签名验证（非直接返回 true 的桩实现）
  - [ ] 支付回调有幂等处理（Redis setIfAbsent + DB 状态校验双重保护）
  - [ ] 余额扣减使用 SQL CAS（WHERE balance >= #{amount}，禁止先查后扣）
  - [ ] 余额扣减在事务内（@Transactional(rollbackFor = Exception.class)）
  - [ ] 退款有唯一退款号 + 幂等保护（Redis 幂等键或 DB 唯一约束）
  - [ ] 支付状态变更通过 Entity 领域方法（markSuccess/markRefunded，禁止直接 setStatus）
  - [ ] 支付单归属校验（verifyOwnership(userId)）
  - [ ] createPayment 有并发保护（Redis 分布式锁或 DB 唯一索引）
  - [ ] 禁止跨模块直接操作支付表（必须通过 Service/MQ 通信）
  - [ ] 新增支付方式必须实现 PayTypeHandler 接口

扣分:
  - 金额用 float/double: -25 分（BLOCK）
  - 支付回调签名验证未实现（返回 true）: -25 分（BLOCK）
  - 支付回调无幂等: -25 分（BLOCK）
  - 余额扣减无事务: -25 分（BLOCK）
  - 退款无幂等保护: -25 分（BLOCK）
  - 跨模块直接操作支付表: -25 分（BLOCK）
  - 余额扣减无 CAS/乐观锁: -20 分
  - createPayment 无并发保护: -20 分
  - 支付状态变更绕过 Entity 领域方法: -20 分
```

### 6. 传输与配置安全（权重 10%）

```yaml
检查:
  - [ ] 生产环境启用 HTTPS（Nginx SSL + 强制重定向）
  - [ ] 数据库连接生产环境启用 SSL（禁止 useSSL=false）
  - [ ] CORS 配置不含 allowedOrigins("*") + allowCredentials(true)
  - [ ] Nginx 安全响应头（X-Frame-Options、X-Content-Type-Options、HSTS、CSP）
  - [ ] docker-compose.yml 敏感变量通过 .env 注入（无明文默认密码）
  - [ ] JWT 密钥、DB 密码、Redis 密码通过环境变量注入（无可预测默认值）
  - [ ] .env / 密钥文件 / 证书在 .gitignore 中排除
  - [ ] Cookie 设置 Secure/HttpOnly/SameSite=Strict 属性
  - [ ] 敏感接口（登录/支付）强制 HTTPS
  - [ ] CI/CD 变量（SSH_PASSWORD/DB_PASS/JWT_SECRET/HARBOR_PASSWORD）通过 GitLab CI/CD Variables 管理

扣分:
  - CORS 通配符 + credentials: -25 分（BLOCK）
  - docker-compose 明文默认密码: -25 分（BLOCK）
  - 生产 useSSL=false: -25 分（BLOCK）
  - 敏感接口未强制 HTTPS: -20 分
  - Nginx 缺少安全头: -10 分
  - Cookie 缺少安全属性: -10 分
```

### 7. 日志安全（权重 5%）

```yaml
检查:
  - [ ] 日志中不输出密码、Token、密钥、完整手机号、身份证号、银行卡号
  - [ ] 日志中手机号使用脱敏格式（138****1234）
  - [ ] 请求日志记录路径、用户ID、IP，不记录请求体中的敏感字段
  - [ ] 异常日志记录堆栈但不包含敏感上下文
  - [ ] 管理员操作记录操作人、操作时间、操作内容（访问审计）
  - [ ] 生产环境禁止 DEBUG 日志级别

扣分:
  - 日志输出密码/Token/密钥: -25 分（BLOCK）
  - 管理员操作无审计日志: -15 分
  - 日志中输出完整手机号/身份证: -10 分
  - 生产环境 DEBUG 日志: -15 分
```

### 8. 前端安全（权重 5%）

```yaml
检查:
  - [ ] 无未 sanitize 的 dangerouslySetInnerHTML（必须用 DOMPurify）
  - [ ] Token 存储方式安全（httpOnly Cookie 优于 localStorage）
  - [ ] 无硬编码的 API Key/Secret
  - [ ] Admin 路由有角色权限守卫（非仅检查 token 存在）
  - [ ] 用户输入渲染前已转义（textContent 而非 innerHTML）

扣分:
  - 未 sanitize 的 dangerouslySetInnerHTML: -25 分（BLOCK - XSS）
  - 前端硬编码密钥: -25 分（BLOCK）
  - Admin 路由无角色校验: -20 分
  - Token 存储在 localStorage（已知风险，非 BLOCK）: -5 分
```

### 9. 依赖安全（权重 5%）

```yaml
检查:
  - [ ] 后端无高危漏洞依赖（mvn dependency-check:check）
  - [ ] 前端无高危漏洞依赖（npm audit）
  - [ ] 依赖版本锁定（package-lock.json / Maven BOM）
  - [ ] 无未使用的多余依赖（最小依赖原则）
  - [ ] 使用私有 Maven 仓库和 npm 镜像（供应链安全）

扣分:
  - 高危漏洞依赖未修复: -25 分（BLOCK）
  - 中危漏洞依赖未修复: -10 分
  - 依赖版本未锁定: -10 分
```

### 10. 操作安全（权重 10%）

```yaml
检查:
  数据库操作:
    - [ ] 无 DROP TABLE / DROP DATABASE
    - [ ] 无 TRUNCATE TABLE
    - [ ] 无无 WHERE 条件的 DELETE / UPDATE
    - [ ] 无 SELECT *（必须指定字段列表）
    - [ ] 无修改 flyway_schema_history 或已执行的 Flyway 迁移脚本
  Redis 操作:
    - [ ] 无 FLUSHALL / FLUSHDB
    - [ ] 无 KEYS *（生产禁用，用 SCAN）
    - [ ] 无关闭 RDB/AOF 持久化
  MQ 操作:
    - [ ] 无清空 RocketMQ 消息队列
    - [ ] 无修改生产 Consumer Group 配置
  部署操作:
    - [ ] 无单副本下线（至少保持 1 个副本运行）
    - [ ] 无运行未经 CI 验证的镜像
    - [ ] 无 git push --force 到 main/master
    - [ ] 部署后有健康检查
  配置操作:
    - [ ] 无直接修改 JWT 密钥（会导致所有 Token 失效）
    - [ ] 无修改加密算法
    - [ ] 无关闭安全机制（CORS/HTTPS/鉴权）

扣分:
  - DROP TABLE / TRUNCATE / 无 WHERE 的 DELETE/UPDATE: -25 分（BLOCK）
  - FLUSHALL / FLUSHDB / KEYS *: -25 分（BLOCK）
  - git push --force: -25 分（BLOCK）
  - 单副本下线 / 未验证镜像: -25 分（BLOCK）
  - 修改 JWT 密钥 / 加密算法: -25 分（BLOCK）
  - SELECT *: -10 分
  - 部署后无健康检查: -10 分
```

---

## 已知技术债务

```yaml
以下安全问题存在于当前代码库中，评估新变更时不重复扣分（仅评估增量变更）:

  认证与鉴权:
    - 无方法级权限注解（权限控制在网关层和 Service 层）
    - 管理员角色校验依赖 Redis 缓存（TTL 24h，缓存篡改风险）

  输入安全:
    - CSRF 防护未实现（Bearer Token 方式天然抗 CSRF，风险较低）

  数据安全:
    - 敏感字段脱敏未实现（rules/security.md 标注为待实现）

  接口安全:
    - MQ 消费者幂等键 TTL 配置需验证（10 分钟是否足够）

  支付安全:
    - 支付回调路径在白名单中（/api/payment/callback/** 无需认证）
    - createPayment 无分布式锁（高并发下可能创建重复支付单）
    - 退款接口无幂等保护（可重复退款）
    - 退款单号生成碰撞风险（userId%10000 + IdGenerator.nextId()%1000）
    - BalancePayHandler 在 insert 之前发送 MQ（消费者可能读到未持久化的支付单）
    - OrderRefundConsumer 消息类型不一致（声明为 String 但生产者发送 RefundMessage）

  传输与配置:
    - MySQL 连接 useSSL=false + sslMode=DISABLED
    - CORS 配置 allowedOriginPatterns("*") + allowCredentials(true)

  日志安全:
    - 日志脱敏格式未统一实现
    - 管理员操作审计日志未实现

  前端:
    - Token 存储在 localStorage

## 已修复项（2025-05-22）

```yaml
以下安全问题已修复，不再作为技术债务:

  认证与鉴权:
    - ✓ JWT 密钥默认值已移除，强制环境变量注入（application.yml: jwt.secret=${JWT_SECRET}）
    - ✓ DB 密码/Redis 密码默认值已移除，通过 .env 注入（docker/.env.example）

  输入安全:
    - ✓ 全局 XSS 过滤器已实现（XssFilterConfig + XssHttpServletRequestWrapper）

  数据安全:
    - ✓ UserServiceImpl.updateUser 已添加归属校验（currentUserId 参数 + equals 检查）

  支付安全:
    - ✓ 支付回调签名验证已修复（非余额支付类型拒绝回调，记录错误日志）

  传输与配置:
    - ✓ docker-compose.yml 敏感变量已移除默认值，通过 .env 注入
    - ✓ 前端 Nginx 已添加安全响应头（X-Frame-Options、X-Content-Type-Options、X-XSS-Protection、Referrer-Policy、Permissions-Policy、CSP）
    - ✓ 后端 Nginx 已添加安全响应头（同上 + Swagger CSP 宽松策略）
    - ✓ HSTS 已预配置（注释状态，启用 HTTPS 后取消注释即可）

  前端:
    - ✓ ProductDetail.tsx dangerouslySetInnerHTML 已添加 DOMPurify 消毒
    - ✓ App.tsx 已添加 AdminRoute 角色权限守卫（JWT payload.role === 'ADMIN'）
    - ✓ Nginx CSP 策略已配置（前端 + 后端）
```

---

## 评分

```yaml
总分: 100 分
  ≥ 90: PASS
  80-89: WARN（低风险问题）
  < 80: FAIL（BLOCK）
  含 BLOCK 项: 直接 FAIL

评分范围:
  仅评估本次变更涉及的文件和模块
  已知技术债务不重复扣分（见上方清单）
  新增安全违规按扣分规则执行
```

---

## 输出

```yaml
安全评估报告:
  score: 0-100
  level: PASS|WARN|FAIL
  dimensions:
    auth: 0-15
    input: 0-15
    data_masking: 0-10
    api_security: 0-10
    payment: 0-15
    transport_config: 0-10
    logging: 0-5
    frontend: 0-5
    dependency: 0-5
    operations: 0-10
  critical_issues:
    - category: AUTH|INPUT|DATA|API|PAYMENT|TRANSPORT|LOGGING|FRONTEND|DEPENDENCY|OPERATIONS
      description: <问题描述>
      location: <文件:行号>
      fix: <修复建议>
  high_issues:
    - category: AUTH|INPUT|DATA|API|PAYMENT|TRANSPORT|LOGGING|FRONTEND|DEPENDENCY|OPERATIONS
      description: <问题描述>
      location: <文件:行号>
      fix: <修复建议>
  medium_issues:
    - category: AUTH|INPUT|DATA|API|PAYMENT|TRANSPORT|LOGGING|FRONTEND|DEPENDENCY|OPERATIONS
      description: <问题描述>
      location: <文件:行号>
      suggestion: <改进建议>
  pass_items:
    - <检查项名称> ✓
  next_action: PASS|WARN|FIX
```
