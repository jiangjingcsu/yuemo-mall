# 生产环境约束

> 生产环境红线。所有 executor 执行前必须检查此约束文件。违反任一约束 = 立即停止。
> 适用环境: Docker Compose 部署 / MySQL 8.0 / Redis 7.x / RocketMQ 5.x
> 总纲摘要: `.harness/constraints/ai-boundaries.md`
> 关联约束: `.harness/constraints/database.md` / `deployment.md` / `infra.md` / `payment.md`

---

## 绝对禁止

```yaml
数据库:
  DDL:
    - DROP TABLE / DROP DATABASE
    - TRUNCATE TABLE（绕过事务日志、无视逻辑删除、不可回滚）
    - ALTER TABLE ... DROP COLUMN（无备份确认）  # DDL 变更管理，非运行时禁止
  DML:
    - DELETE FROM <table> 无 WHERE 条件
    - UPDATE <table> SET ... 无 WHERE 条件
    - SELECT *（必须指定字段列表，防止 OOM 和全表扫描）
    - 全表逻辑删除 UPDATE ... SET deleted=1 无 WHERE（等同物理删除）
  迁移管理:
    - 修改 flyway_schema_history 表
    - 修改已执行的 Flyway 迁移脚本（checksum 不匹配导致启动失败）
  生产数据:
    - 直接执行 UPDATE/DELETE 修改生产数据（需走审核流程，非绝对禁止但需确认）

Redis:
  - FLUSHALL / FLUSHDB
  - KEYS *（生产禁用，用 SCAN）
  - 删除非本项目的 Key
  - 关闭 RDB/AOF 持久化（购物车主存储依赖 Redis，数据丢失不可恢复）

MQ:
  - 清空 RocketMQ 消息队列
  - 修改生产 Consumer Group 配置

部署:
  - 单副本下线（至少保持 1 个副本运行）
  - 直接修改运行中的 docker-compose.yml
  - 同时停止所有副本（导致服务中断）
  - 在生产环境运行未经 CI 验证的镜像
  - 部署后不做健康检查
  - git push --force 到 main/master
  - 在生产服务器执行未知脚本
  - 回滚时删除 Flyway 迁移记录（Flyway 不支持自动回滚）
  - 覆盖已推送的 SHA 版本镜像标签

配置:
  - 修改 JWT 密钥（会导致所有 Token 失效）
  - 修改加密算法
  - 关闭安全机制（CORS、HTTPS/TLS、鉴权）
  - 降低密码加密强度
  - 开启 DEBUG 日志级别到生产
  - CORS 配置 allowedOrigins("*") + allowCredentials(true)（已知安全漏洞）
  - docker-compose.yml 中保留明文默认密码（必须通过 .env 注入）
  - 数据库连接 useSSL=false 用于生产环境
```

---

## 必须确认

```yaml
以下操作不绝对禁止，但必须经用户确认:
  基础设施:
    - 重启服务
    - SSH 登录生产服务器
    - 修改 Nginx/负载均衡配置
    - 修改域名/DNS
    - 修改防火墙规则
    - 更新 SSL 证书
    - 修改 docker-compose.yml
    - 修改 CI/CD 流水线（.gitlab-ci.yml）

  数据库:
    - 数据库迁移执行（Flyway 自动执行，但需确认迁移脚本）
    - 新增数据库表（Flyway 迁移脚本执行）

  业务逻辑:
    - 修改支付/退款相关逻辑
    - 修改模块间 API 接口签名

  配置:
    - 修改环境变量
```

---

## AI Agent 强制行为

```yaml
AI 在生成任何可能影响生产的代码/命令时必须:
  1. 检查是否违反上述禁止项
  2. 违反 → 立即停止，向用户说明原因
  3. 未违反但涉及"必须确认"项 → 向用户确认后再执行
  4. 不确定 → 宁可多确认，不可擅自操作

判定规则（什么情况算"不确定"）:
  - 操作涉及多个约束文件的交叉区域
  - 操作的副作用范围不明确
  - 操作可能影响正在运行的生产服务
  - 约束文件中无明确覆盖的场景
```
