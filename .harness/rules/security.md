# 安全边界

> 参照阿里Java安全规约、OWASP Top 10、等保2.0基本要求，结合项目实际安全架构制定。

---

## 1. 认证与授权

| 规则 | 说明 | 项目实现 |
|---|---|---|
| JWT密钥 | ≥256位强随机，禁止硬编码默认值 | `JwtTokenProvider` 通过 `${JWT_SECRET}` 注入 |
| Token过期 | AccessToken 30分钟，RefreshToken 7天 | `accessTokenExpiration=1800` |
| Token黑名单 | 登出加入Redis黑名单，网关拦截器校验 | `GatewayAuthFilter` + Redis |
| 管理员鉴权 | `/api/admin/**` 额外校验ADMIN角色 | `GatewayAuthFilter` 角色校验 |
| 密码存储 | BCrypt单向哈希，禁止明文/MD5/SHA | `PasswordEncoder`（Hutool BCrypt） |
| 登录失败 | 不提示"用户名不存在"还是"密码错误"，统一"用户名或密码错误" | 防止用户名枚举 |

**待加固**：
- JWT Secret 默认值 `yuemo-mall-jwt-secret-key-2024-...` 仅用于开发，生产必须通过环境变量注入强随机密钥
- CORS 配置 `allowedOriginPatterns("*")` + `allowCredentials(true)` 生产环境必须收紧为具体域名

---

## 2. 输入校验与注入防护

| 攻击类型 | 防护措施 | 项目实现 |
|---|---|---|
| SQL注入 | MyBatis参数化查询（`#{}`），禁止`${}`拼接用户输入 | 所有Mapper均用`#{}` |
| XSS | 输入转义+输出编码 | **⚠️ 未实现**，需添加全局XssFilter |
| CSRF | 状态修改操作验证Token/Referer | **⚠️ 未实现**，需添加CSRF防护 |
| 命令注入 | 禁止`Runtime.exec()`拼接用户输入 | — |
| 路径遍历 | 文件路径校验，禁止`../` | — |

**XSS防护要求**：
- 添加全局 `XssFilter` + `XssHttpServletRequestWrapper`，对 `<script>`/`<iframe>`/`onerror` 等危险标签过滤
- 自由文本字段（`nickname`/`remark`/`description`）存储前转义，富文本用白名单标签过滤
- 前端渲染用户内容使用 `textContent` 而非 `innerHTML`，禁止 `dangerouslySetInnerHTML`

---

## 3. 数据脱敏

| 字段类型 | 脱敏规则 | 示例 |
|---|---|---|
| 手机号 | 中间4位掩码 | `138****1234` |
| 身份证 | 保留前3后4位 | `110***********1234` |
| 银行卡 | 保留后4位 | `************1234` |
| 邮箱 | 用户名首字母+`***`+域名 | `t***@example.com` |
| 密码 | 禁止任何场景返回 | API响应中不包含password字段 |

**⚠️ 当前未实现**：需添加 `@JsonSerialize` 自定义脱敏序列化器，对 VO 中敏感字段自动脱敏。

---

## 4. 接口安全

| 规则 | 说明 | 项目实现 |
|---|---|---|
| 限流 | 三层防护：Nginx → Redis滑动窗口 → Sentinel | `RateLimitFilter` + `SentinelRuleConfig` + `nginx.conf` |
| 熔断 | Sentinel熔断降级，慢调用比例>50%触发 | `CircuitBreakerFilter` |
| 越权校验 | 用户只能操作自己的数据（水平越权）；普通用户不能访问管理接口（垂直越权） | `GatewayAuthFilter` 校验角色 |
| 参数校验 | Jakarta Validation + GlobalExceptionHandler | DTO用`@NotBlank`/`@NotNull`等 |
| 幂等性 | 支付/下单等关键操作需防重复提交 | 前端防重复点击 + 后端Token校验 |

**限流配置参考**：

| 接口 | 限流值 | 层级 |
|---|---|---|
| 登录/注册 | 10次/分钟 | Redis滑动窗口 |
| 下单/支付 | 30次/分钟 | Redis滑动窗口 |
| 商品查询 | 200 QPS | Sentinel |
| 后台管理 | 35 QPS | Sentinel |

---

## 5. 传输安全

| 规则 | 说明 |
|---|---|
| HTTPS | 生产环境必须启用，在Nginx层配置SSL证书，强制HTTPS重定向 |
| HSTS | Nginx添加 `add_header Strict-Transport-Security "max-age=31536000"` |
| 数据库连接 | 生产环境启用SSL（当前 `useSSL=false` 仅限开发） |
| 敏感接口 | 登录/支付等接口强制HTTPS，HTTP请求拒绝或重定向 |
| Cookie | 设置 `Secure`/`HttpOnly`/`SameSite=Strict` 属性 |

---

## 6. 日志安全

| 规则 | 说明 |
|---|---|
| 禁止输出 | 密码、Token、密钥、完整手机号、身份证号、银行卡号 |
| 脱敏输出 | 日志中手机号用 `138****1234` 格式 |
| 请求日志 | 记录请求路径、用户ID、IP，不记录请求体中的敏感字段 |
| 异常日志 | 记录异常堆栈，但不包含敏感上下文 |
| 访问审计 | 管理员操作必须记录操作人、操作时间、操作内容 |

---

## 7. 密钥与配置管理

| 规则 | 说明 | 项目实现 |
|---|---|---|
| 环境变量 | 敏感配置通过环境变量注入，不硬编码 | `${JWT_SECRET}`/`${DB_PASS}`/`${REDIS_PASSWORD}` |
| CI/CD变量 | `SSH_PASSWORD`/`DB_PASS`/`JWT_SECRET`/`HARBOR_PASSWORD` 通过GitLab CI/CD Variables管理 | `.gitlab-ci.yml` |
| 默认值 | 敏感配置禁止留默认值，启动时检查 | **⚠️ docker-compose.yml有明文默认密码需清理** |
| 密钥轮换 | JWT密钥定期轮换，轮换时旧Token平滑过渡 | — |
| 代码仓库 | 禁止提交`.env`/密钥文件/证书到Git | `.gitignore` |

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

---

## 10. 禁止事项清单

| # | 禁止项 |
|---|---|
| 1 | 直接修改生产数据库 |
| 2 | 密码/密钥硬编码到代码中 |
| 3 | 删除 `yuemo-backend/sql/` 下的数据库迁移文件 |
| 4 | 未运行测试声称修改完成 |
| 5 | MyBatis中用`${}`拼接用户输入 |
| 6 | 返回密码字段给前端（任何场景） |
| 7 | 日志中输出敏感数据 |
| 8 | `dangerouslySetInnerHTML`（前端） |
| 9 | CORS配置 `allowedOrigins("*")` + `allowCredentials(true)` 用于生产 |
| 10 | 使用MD5/SHA存储密码（必须BCrypt） |
| 11 | `Runtime.exec()`拼接用户输入 |
| 12 | 数据库连接 `useSSL=false` 用于生产 |
