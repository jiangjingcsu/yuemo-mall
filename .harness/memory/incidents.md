# 历史事故记录

> 记录生产/开发中的重大事故。AI Agent 修复 Bug 时先查阅此文件，避免重复踩坑。
> 交叉引用: anti-patterns.md（通用反模式禁令）、known-issues.md（技术债务）、decisions.md（技术决策）

---

## 事故分类索引

```yaml
并发安全: INC001
数据库迁移: INC002
安全配置: INC003
部署/网络: INC004
```

## 事故清单

### INC001: 购物车重复插入

```yaml
分类: 并发安全
时间: 2026-05
发现方式: 开发阶段测试发现
影响范围: 购物车加购功能，并发场景下重复插入数据
错误: SQLIntegrityConstraintViolationException: Duplicate entry
根因: 
  - 缺少唯一约束
  - SELECT-then-INSERT 竞态条件
  - 逻辑删除 + 唯一约束不含 deleted 字段
修复:
  - 添加 UNIQUE KEY(user_id, sku_id, deleted)
  - INSERT ... ON DUPLICATE KEY UPDATE
  - 删除冗余字段（productName/productImage/price/specText 从 yu_cart_item 移除）
残留风险: 无
关联:
  - anti-patterns: AP005（SELECT-then-INSERT 竞态）、AP006（唯一键不含 deleted）
  - decisions: D004（Redis Hash 购物车）、D005（逻辑删除策略）
  - rules: database-governance.md
教训: 并发场景必须用原子操作，唯一约束必须含 deleted
```

### INC002: Flyway 迁移文件 Checksum 不匹配

```yaml
分类: 数据库迁移
时间: 2026-05
发现方式: CI 部署失败
影响范围: 数据库迁移流程，阻塞部署
错误: Detected failed migration to version 2
根因: 
  - 直接修改了 V1__init_schema.sql
  - V2 迁移失败后残留 flyway_schema_history 记录
修复:
  - 还原 V1 为原始版本
  - 删除失败的 flyway_schema_history 记录
  - V2 改为 idempotent 迁移（存储过程检查存在性）
  - Flyway 配置 baseline-on-migrate: true + baseline-version: 1
残留风险: 无
关联:
  - decisions: D006（Flyway 数据库迁移）
  - constraints: database.md
教训: 已执行的 Flyway 脚本绝对不能修改，新变更必须新建迁移
```

### INC003: JWT 密钥长度不足

```yaml
分类: 安全配置
时间: 2026-05
发现方式: 应用启动失败（WeakKeyException）
影响范围: 认证功能，应用无法启动
错误: WeakKeyException (密钥 < 256 bits)
根因: 默认 JWT 密钥不足 256 位
修复:
  - 生成符合 HS256 要求的 256+ 位密钥
  - 密钥通过环境变量 JWT_SECRET 注入
残留风险: 代码无启动时密钥长度校验，依赖运维正确配置（.env.example 示例值刚好 32 字符）
关联:
  - decisions: D007（JWT + Redis 认证）
  - rules: security.md
  - known-issues: TD010（JwtTokenProvider 启动时密钥长度校验）
教训: 生产密钥不能使用默认值，必须通过环境变量注入；关键配置应有启动时校验
```

### INC004: 双重 Nginx 代理导致 400 错误

```yaml
分类: 部署/网络
时间: 2026-05
发现方式: 前端联调时发现 API 请求 400
影响范围: 前端所有 API 请求
错误: Nginx upstream 返回 400 Bad Request
根因: 
  - 前端容器 nginx 代理 + 后端 nginx 双重代理
  - 前端 proxy_pass 未指定端口，默认 80 到后端 Nginx
  - 后端 Nginx 虚拟主机匹配与前端转发的 Host 头不匹配
  - header 丢失/Host 头不一致
修复:
  - 调整前端 Nginx proxy_pass 配置，正确传递 Host 头
  - 后端 Nginx 配置 proxy_set_header 正确转发
残留风险: 无
关联:
  - constraints: infra.md, deployment.md
  - docs: project-overview.md（部署架构图）
教训: 网络拓扑变更前必须理清代理链路；双重代理需确保每层正确传递 Host/X-Forwarded-For/X-Forwarded-Proto
```

---

## AI 在修复 Bug 前必须检查

```yaml
通用检查:
  1. 此 Bug 是否与历史事故相似？→ 按分类索引快速定位
  2. 修复方案是否解决了根因而非症状？
  3. 修复是否会引入新的并发问题？
  4. 修复是否需要数据库迁移？（走 Flyway）
  5. 修复是否影响其他模块？

分类检查:
  并发安全类:
    - 是否有唯一约束保护？
    - 是否使用了原子操作（INSERT ON DUPLICATE KEY UPDATE / Redis setIfAbsent）？
    - 唯一约束是否包含 deleted 字段？
  数据库迁移类:
    - 是否新建迁移脚本（而非修改已执行的）？
    - 迁移脚本是否幂等（存储过程检查存在性）？
  安全配置类:
    - 关键配置是否有启动时校验？
    - 密钥/密码是否通过环境变量注入？
  部署/网络类:
    - 代理链路是否完整（Host/X-Forwarded-For 传递）？
    - 端口映射是否正确？
```
