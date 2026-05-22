# AI Agent 能力边界

> AI Agent 在此项目中的硬性能力边界。越界行为立即终止。
>
> **职责定位**：本文件是 Constraints 层总纲，定义**硬红线**（不可违反、无需解释、违反即停止）。
> 与其他层的关系：
> - **Constraints（本文件）**：硬约束总纲摘要，违反 = 停止执行。类似"宪法"。
> - **Constraints 子文件**：各领域详细硬约束（production / database / payment / deployment / infra），本文件的细化展开。
> - **Rules**（.harness/rules/）：软规范，违反 = 应修复。类似"法律"。
> - **Tools Permissions**（.harness/rules/tools-permissions.md）：工具级权限，定义每个工具的允许/禁止操作。
>
> **冲突仲裁**：当本文件与子约束文件出现分类矛盾时，以子约束文件的精确判定为准。
> Rules 和 Tools Permissions 中涉及硬红线的条目，统一引用本文件，不重复定义。

---

## 绝对不可以做的事

```yaml
生产操作:
  - DROP TABLE / DROP DATABASE / TRUNCATE TABLE（生产环境）
  - DELETE / UPDATE 无 WHERE 条件（生产环境）
  - 执行 Redis FLUSHALL / FLUSHDB / KEYS *
  - 清空 RocketMQ 消息队列
  - 修改生产 Consumer Group 配置
  - 修改 flyway_schema_history 表
  - 单副本下线（至少保持 1 个副本运行）
  - git push --force
  - 删除生产数据卷（docker volume rm）
  - 硬编码服务器 IP / 密码 / 密钥 / Token 到代码中

代码变更:
  - 修改 Spring Boot 主版本（3.2.x）或 Java 版本（17）
  - 修改已执行的 Flyway 迁移脚本
  - 修改已有依赖的 major version（可能引入不兼容变更）
  - 删除代码前未确认引用关系（看似无用但实际在用的代码）

架构变更:
  - 改变模块间通信方式（Service→MQ 或反之）
  - 创建新的顶级模块（除非用户明确要求）
  - 改变数据库表结构（除非通过 Flyway 迁移）
  - 引入新的持久化技术（换数据源类型等）
  - 跨模块直接调用 Mapper 或操作其他模块数据库表

配置变更:
  - 修改 JWT 密钥
  - 修改加密算法
  - 关闭安全机制（CORS、HTTPS、鉴权）
  - CORS allowedOrigins("*") + allowCredentials(true)（安全漏洞，必须配置具体域名）
  - 降低密码加密强度
  - 开启 DEBUG 日志级别到生产

支付安全:
  - 余额扣减无事务保护
  - 余额扣减前不校验余额 >= 扣减金额
  - 支付回调不做签名验证
  - 支付回调不做幂等（必须 Redis setIfAbsent）
  - 退款不做退款单号唯一性校验
  - 在支付流程中使用浮点数（必须 BigDecimal）

部署安全:
  - 跳过测试直接部署
  - 同时停止所有副本（导致服务中断）
  - 在生产环境运行未经 CI 验证的镜像
  - 部署后不做健康检查
  - 回滚时删除数据库迁移（Flyway 不支持回滚）
```

---

## 需要用户确认才能做的事

```yaml
确认后执行:
  - SSH 登录生产服务器
  - 重启生产服务
  - 修改生产环境变量
  - 新增依赖（pom.xml / package.json）
  - 修改 CI/CD 流水线
  - 修改 docker-compose.yml 或 Nginx 配置
  - 修改模块间 API 接口签名
  - 新增数据库表（Flyway 迁移脚本）
  - 修改支付/退款相关逻辑
  - 删除代码文件
  - git push 到远程仓库
  - 修改防火墙规则
  - 更新 SSL 证书
  - 修改域名 / DNS 解析
```

---

## 可以自主做的事

```yaml
自主执行:
  - 在任务范围内新增/修改 .java / .ts / .tsx / .xml 代码文件
  - 在任务范围内新增 .sql 文件（Flyway 迁移脚本，但不执行）
  - 新增 .harness/ 下的治理文档
  - 搜索和分析代码
  - 运行编译/测试命令（mvn compile / npx tsc）
  - 设计技术方案
  - 生成 Flyway 迁移脚本（但不执行）
```

---

## 边界检查机制

```yaml
每次执行前 AI 必须自问:
  1. 这个操作是否在生产环境执行？→ 是 → 停止，请求确认
  2. 这个操作是否涉及数据删除？→ 是 → 停止，检查是否有 WHERE 条件
  3. 这个操作是否涉及版本/架构变更？→ 是 → 停止，请求确认
  4. 这个操作是否涉及支付/资金流转？→ 是 → 停止，请求确认
  5. 这个操作是否涉及敏感数据（密码/密钥/Token/个人信息）？→ 是 → 停止，检查是否安全处理
  6. 这个操作是否跨模块操作？→ 是 → 停止，检查是否违反模块边界
  7. 这个操作是否在能力边界内？→ 不确定 → 停止，请求澄清
```

---

## 文件关系与冲突仲裁

```yaml
本文件是 Constraints 层总纲摘要，各领域详细约束在子文件中:
  - constraints/production.md — 生产环境详细红线
  - constraints/database.md — 数据库安全详细红线
  - constraints/payment.md — 支付安全详细红线
  - constraints/deployment.md — 部署流程详细红线
  - constraints/infra.md — 基础设施详细红线

冲突仲裁规则:
  - 本文件与子约束文件分类矛盾时 → 以子约束文件的精确判定为准
  - 子约束文件之间矛盾时 → 以更严格的判定为准
  - 本文件未覆盖的场景 → 查阅对应子约束文件
```
