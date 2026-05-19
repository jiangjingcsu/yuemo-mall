# 安全边界

## 代码安全

- 所有外部输入必须校验（SQL 注入、XSS、CSRF）
- 密码和密钥通过环境变量注入，不硬编码
- JWT 密钥长度不少于 256 位
- 数据库连接使用 SSL（生产环境）

## 操作安全

- 修改生产配置前，必须向用户确认
- `docker compose down` 操作需用户明确批准
- 远程 SSH 命令需逐条确认
- Git 提交信息必须清晰描述变更内容

## CI/CD 安全

- `SSH_PRIVATE_KEY`、`DB_PASS`、`JWT_SECRET` 通过 GitLab CI/CD Variables 管理
- 禁止在日志中输出敏感变量
- 部署脚本中使用 `${VAR:?error message}` 确保必要变量存在

## 禁止事项

- 禁止直接修改生产数据库
- 禁止将密码/密钥硬编码到代码中
- 禁止删除 `yuemo-backend/sql/` 下的数据库迁移文件
- 禁止在未运行测试的情况下声称修改完成
