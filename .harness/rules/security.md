# 安全边界

> 参照阿里Java安全规约、OWASP Top 10、等保2.0基本要求，结合项目实际安全架构制定。
> 支付安全硬红线见 `.harness/constraints/payment.md`，数据库操作约束见 `.harness/constraints/database.md`，生产操作红线见 `.harness/constraints/production.md`。

---

## 1. 认证与授权

| 规则 | 说明 | 项目实现 |
|---|---|---|
| JWT密钥 | ≥256位强随机，禁止硬编码默认值 | `JwtTokenProvider` 通过 `${JWT_SECRET}` 注入 |
| Token过期 | AccessToken 30分钟，RefreshToken 7天 | `accessTokenExpiration=1800` |
| Token黑名单 | 登出加入Redis黑名单，网关拦截器校验 | `GatewayAuthFilter` + Redis |
| 管理员鉴权 | `/api/admin/**` 额外校验ADMIN角色 | `GatewayAuthFilter` 角色校验 |
| 密码存储 | BCrypt单向哈希，禁止明文/MD5/SHA | `PasswordEncoder`（Hutool BCrypt） |
| 登录失败 | 统一返回"用户名或密码错误"，禁止区分"用户不存在"和"密码错误" | `ResultCode.USER_LOGIN_FAILED` ✅ |
| 密码字段 | Entity 中 password 字段必须 `@JsonIgnore` | `User.java` ✅ |

**已知违规与待加固**：
- CORS 配置 `allowedOriginPatterns("*")` + `allowCredentials(true)` — **⚠️ 已知违规**（`production.md` 绝对禁止，但代码仍在使用），生产环境必须收紧为具体域名
- JWT Secret 默认值 `yuemo-mall-jwt-secret-key-2024-...` 仅用于开发，生产必须通过环境变量注入强随机密钥
- 管理员角色校验依赖 Redis 缓存（TTL 24h），角色变更后最长需 24 小时生效（TD016）

---

## 2. 输入校验与注入防护

| 攻击类型 | 防护措施 | 项目实现 |
|---|---|---|
| SQL注入 | MyBatis参数化查询（`#{}`），禁止`${}`拼接用户输入 | 所有Mapper均用`#{}` ✅ |
| XSS | 输入过滤+输出编码 | ✅ 已实现 `XssFilterConfig` + `XssHttpServletRequestWrapper`（覆盖 8 种 XSS 模式） |
| CSRF | 状态修改操作验证Token/Referer | ⚠️ 未实现（JWT Bearer Token 认证下风险较低，但非 Cookie 端点仍需防护） |
| 命令注入 | 禁止`Runtime.exec()`拼接用户输入 | — |
| 路径遍历 | 文件路径校验，禁止`../` | — |
| 反序列化 | 禁止反序列化不可信数据（防止RCE） | — |

**XSS 已实现但存在局限**：
- ✅ 已实现 `XssFilterConfig` + `XssHttpServletRequestWrapper`，过滤 `<script>`/`<iframe>`/`onerror`/`<object>`/`<embed>`/`eval()`/`expression()`/`javascript:` 共 8 种模式
- ✅ Nginx 层配置 `X-XSS-Protection: 1; mode=block`
- ⚠️ **仅过滤请求参数和 Header，未过滤 `@RequestBody` JSON 请求体**
- 待改进：添加 JSON 请求体过滤，或使用 OWASP Java HTML Sanitizer

**CSRF 防护要求**（规则已定义，实现待补）：
- 状态修改操作必须验证 Referer 或 CSRF Token
- 当前 JWT Bearer Token 认证下 CSRF 风险较低，但如有基于 Cookie 的端点仍需防护

---

## 3. 数据脱敏

| 字段类型 | 脱敏规则 | 示例 |
|---|---|---|
| 手机号 | 中间4位掩码 | `138****1234` |
| 身份证 | 保留前3后4位 | `110***********1234` |
| 银行卡 | 保留后4位 | `************1234` |
| 邮箱 | 用户名首字母+`***`+域名 | `t***@example.com` |
| 密码 | 禁止任何场景返回 | Entity `@JsonIgnore` + VO 不含 password 字段 |

**⚠️ 当前未实现**：需添加 `@JsonSerialize` 自定义脱敏序列化器，对 VO 中敏感字段自动脱敏。当前 `UserVO.phone`、`AddressVO.receiverPhone` 等字段明文返回。

---

## 4. 接口安全

| 规则 | 说明 | 项目实现 |
|---|---|---|
| 限流 | 三层防护：Nginx → Redis滑动窗口 → Sentinel | `RateLimitFilter` + `SentinelRuleConfig` + `nginx.conf` |
| 熔断 | Sentinel熔断降级，慢调用比例>50%触发 | `CircuitBreakerFilter` |
| 越权校验 | 用户只能操作自己的数据（水平越权）；普通用户不能访问管理接口（垂直越权） | `GatewayAuthFilter` 校验角色 |
| 参数校验 | Jakarta Validation + GlobalExceptionHandler | DTO用`@NotBlank`/`@NotNull`等 |
| 幂等性 | 支付/下单等关键操作需防重复提交 | 前端防重复点击 + 后端Token校验 |

**已知风险**：
- `updateUser` 无归属校验（TD018）— 当前已通过 `!id.equals(currentUserId)` 校验 ✅
- 支付回调白名单 `/api/payment/callback/**` 无鉴权 — 合理（第三方回调无法携带 Token），但**签名验证为桩实现**（TD007），存在伪造回调风险

**限流实际配置**（双层机制，取更严格值生效）：

| 接口 | Redis 滑动窗口 | Sentinel QPS | 生效层 |
|---|---|---|---|
| 登录/注册 | 10次/分钟 | 50 QPS | Redis 先触发 |
| 下单 | 30次/分钟 | 50 QPS | Redis 先触发 |
| 支付 | 10次/分钟 | 30 QPS | Redis 先触发 |
| 商品查询 | 100次/分钟 | 200 QPS | Redis 先触发 |
| 后台管理 | 200次/分钟 | 35 QPS | Sentinel 先触发 |
| 默认 | 100次/分钟 | — | Redis |

---

## 5. 传输安全

| 规则 | 说明 |
|---|---|
| HTTPS | 生产环境必须启用，在Nginx层配置SSL证书，强制HTTPS重定向 |
| HSTS | Nginx添加 `add_header Strict-Transport-Security "max-age=31536000"` |
| 数据库连接 | 生产环境启用SSL（当前 `useSSL=false` + `sslMode=DISABLED` 仅限开发） |
| 敏感接口 | 登录/支付等接口强制HTTPS，HTTP请求拒绝或重定向 |

**Token 存储安全**：
- 当前：JWT Token 存储在浏览器 `localStorage`（⚠️ 存在 XSS 窃取风险，TD019）
- 建议：迁移至 `httpOnly Cookie` + `SameSite=Strict` + `Secure`
- Cookie 属性要求：`Secure` / `HttpOnly` / `SameSite=Strict`

---

## 6. 日志安全

| 规则 | 说明 |
|---|---|
| 禁止输出 | 密码、Token、密钥、完整手机号、身份证号、银行卡号 |
| 脱敏输出 | 日志中手机号用 `138****1234` 格式 |
| 请求日志 | 记录请求路径、用户ID、IP，不记录请求体中的敏感字段 |
| 异常日志 | 记录异常堆栈，但不包含敏感上下文 |
| 访问审计 | 管理员操作必须记录操作人、操作时间、操作内容 |
| 日志级别 | 生产环境禁止开启 DEBUG 日志级别（可能输出敏感调试信息） |

---

## 7. 密钥与配置管理

| 规则 | 说明 | 项目实现 |
|---|---|---|
| 环境变量 | 敏感配置通过环境变量注入，不硬编码 | `${JWT_SECRET}`/`${DB_PASS}`/`${REDIS_PASSWORD}` |
| CI/CD变量 | `SSH_PASSWORD`/`DB_PASS`/`JWT_SECRET`/`HARBOR_PASSWORD` 通过GitLab CI/CD Variables管理 | `.gitlab-ci.yml` |
| 默认值 | 敏感配置禁止留默认值，启动时检查 | **⚠️ docker-compose.yml有明文默认密码需清理** |
| 密钥轮换 | JWT密钥定期轮换，轮换时旧Token平滑过渡 | — |
| 代码仓库 | 禁止提交`.env`/密钥文件/证书到Git | `.gitignore`（根目录已排除 `.env`，⚠️ `yuemo-backend/.gitignore` 未排除 `.env`） |

---

## 8. 依赖安全

| 规则 | 说明 |
|---|---|
| 依赖审计 | 定期 `mvn dependency-check:check` / `npm audit`，高危漏洞必须修复 |
| 版本锁定 | 生产构建锁定依赖版本（`package-lock.json`/Maven BOM） |
| 最小依赖 | 不引入未使用的依赖，减少攻击面 |
| 供应链 | 使用私有Maven仓库和npm镜像，禁止从公共仓库直接拉取 |

---

## 9. 操作安全

| 规则 | 说明 |
|---|---|
| 生产配置 | 修改前必须向用户确认 |
| `docker compose down` | 需用户明确批准 |
| 远程SSH | 逐条确认 |
| 数据库操作 | 禁止直接修改生产数据库；只允许SELECT查询 |
| Git提交 | 信息必须清晰描述变更内容，禁止提交敏感数据 |
| `git push --force` | 未经明确允许禁止使用 |

---

## 10. 禁止事项清单

| # | 禁止项 |
|---|---|
| 1 | 直接修改生产数据库 |
| 2 | 密码/密钥硬编码到代码中 |
| 3 | 删除 `yuemo-server/src/main/resources/db/migration/` 下的数据库迁移文件 |
| 4 | 修改已执行的 Flyway 迁移脚本 |
| 5 | 未运行测试声称修改完成 |
| 6 | MyBatis中用`${}`拼接用户输入 |
| 7 | 返回密码字段给前端（任何场景） |
| 8 | 日志中输出敏感数据 |
| 9 | `dangerouslySetInnerHTML`（前端） |
| 10 | CORS配置 `allowedOrigins("*")` + `allowCredentials(true)` 用于生产 |
| 11 | 使用MD5/SHA存储密码（必须BCrypt） |
| 12 | `Runtime.exec()`拼接用户输入 |
| 13 | 数据库连接 `useSSL=false` 用于生产 |
| 14 | 生产环境开启 DEBUG 日志级别 |
| 15 | 反序列化不可信数据（防止RCE） |
| 16 | 登录接口区分"用户不存在"和"密码错误"（必须统一返回） |

---

## 11. 支付安全

> 硬红线详见 `.harness/constraints/payment.md`，本节为摘要。

| 规则 | 说明 | 状态 |
|---|---|---|
| 余额扣减事务 | 余额扣减必须在事务内，使用 SQL CAS（`WHERE balance >= amount`） | ✅ `userMapper.deductBalance` |
| 余额扣减校验 | 扣减前必须校验余额 >= 扣减金额 | ✅ SQL CAS |
| 支付回调签名验证 | 第三方支付回调必须验证签名 | ⚠️ 桩实现，非余额支付直接拒绝（TD007） |
| 支付回调幂等 | 回调必须幂等，防重复处理 | ⚠️ 未实现（TD008） |
| 退款幂等 | 退款必须做退款单号唯一性校验 | ⚠️ 未实现（TD009） |
| 退款单号碰撞 | 退款流水号需独立生成，防碰撞 | ⚠️ 风险存在（TD013） |
| 金额计算 | 支付流程禁止使用 float/double，必须 BigDecimal | ✅ |
| 跨模块操作 | 禁止跨模块直接操作支付表 | ✅ 通过 Service 接口 |
| 状态变更 | 支付状态变更必须通过 Entity 领域方法 | ✅ `Payment.markSuccess()`/`markFailed()` |
| 分布式锁 | createPayment 需防并发重复创建 | ⚠️ 未实现（TD008） |
| MQ 幂等 | 支付回调 MQ 消费者必须实现 Redis 幂等 | ⚠️ 未实现 |

---

## 12. 缓存安全

> 详见 `.harness/constraints/production.md` Redis 部分。

| 规则 | 说明 |
|---|---|
| 禁止 FLUSHALL/FLUSHDB | 可能导致全部缓存数据丢失，引发数据库雪崩 |
| 禁止 KEYS * | O(N) 复杂度，大 Key 空间下阻塞 Redis |
| 禁止关闭持久化 | 生产环境必须开启 RDB 或 AOF 持久化 |
| Key 隔离 | 禁止删除非本项目的 Redis Key |
| Key 命名 | `yuemo:{module}:{business}:{id}`，防止 Key 冲突 |
| 敏感数据 | 禁止将密码/密钥/完整手机号存入 Redis；Token 存入需设合理 TTL |

---

## 13. 数据库安全

> 详见 `.harness/constraints/database.md` 和 `.harness/constraints/production.md`。

| 规则 | 说明 |
|---|---|
| DDL | 生产环境禁止手动执行 DDL，必须通过 Flyway 迁移 |
| DROP/TRUNCATE | 禁止 `DROP TABLE`/`DROP DATABASE`/`TRUNCATE TABLE` |
| 无 WHERE 的 DML | 禁止 `DELETE`/`UPDATE` 无 `WHERE` 条件 |
| 全表逻辑删除 | 禁止无条件的逻辑删除（等同物理删除） |
| Flyway 历史 | 禁止修改 `flyway_schema_history` 表 |
| 已执行迁移 | 禁止修改已执行的 Flyway 迁移脚本 |
| 大事务 | 禁止超 3 张表或预计耗时 > 2s 的事务 |
| 事务中远程调用 | 禁止事务中调用远程 HTTP/RPC |
| 事务超时 | 事务必须配置 `timeout` |
| 并发竞态 | 禁止 `SELECT-then-INSERT/UPDATE` 竞态，改用原子 SQL 或乐观锁 |
| 唯一键 | 逻辑删除表唯一键必须含 `deleted` 字段 |
| 大表 ALTER | 大表 ALTER 需加 `ALGORITHM`/`LOCK` 参数 |

---

## 14. 消息队列安全

> 详见 `.harness/constraints/production.md` MQ 部分。

| 规则 | 说明 |
|---|---|
| 禁止清空队列 | 禁止清空 RocketMQ 消息队列 |
| Consumer Group | 禁止随意修改生产 Consumer Group 配置 |
| 消费幂等 | MQ 消费者必须实现幂等（Redis 去重键） |
| 消费失败 | 消费失败必须记日志并重试，禁止吞掉异常 |
| 消息发送 | 事务内禁止发送 MQ（应事务提交后发送，或使用事务消息） |

---

## 15. 基础设施安全

> 详见 `.harness/constraints/infra.md`。

| 规则 | 说明 |
|---|---|
| 防火墙 | 禁止关闭防火墙/安全组 |
| 端口暴露 | 禁止将数据库/Redis 端口暴露到公网 |
| 数据卷 | 禁止删除生产数据卷（`docker volume rm`） |
| 镜像验证 | 禁止运行未经 CI 验证的镜像 |
| 副本安全 | 禁止同时停止所有服务副本 |
| 健康检查 | 部署后必须执行健康检查 |
| 资源限制 | Docker Compose 必须配置资源限制和重启策略 |

---

## 16. 前端安全

| 规则 | 说明 | 状态 |
|---|---|---|
| Token 存储 | 禁止长期存储 Token 在 localStorage（XSS 窃取风险） | ⚠️ 当前使用 localStorage（TD019） |
| CSP 策略 | 必须配置 Content-Security-Policy 响应头 | ⚠️ 未配置（TD021） |
| Admin 路由 | 后台管理路由必须有角色权限守卫 | ⚠️ 前端无守卫（TD020） |
| XSS 渲染 | 禁止 `dangerouslySetInnerHTML`，使用 `textContent` | — |
| 敏感展示 | 前端展示手机号/身份证必须脱敏 | ⚠️ 后端未脱敏，前端明文展示 |
