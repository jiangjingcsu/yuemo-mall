# CI/CD 流水线约束

> 流水线配置以 `.gitlab-ci.yml` 和各 `Dockerfile` 为准，本文件仅定义修改约束。

## 修改约束

修改 `.gitlab-ci.yml`、`Dockerfile` 或 `docker-compose.yml` 后，必须检查：

- YAML 语法正确性
- 镜像名称在 CI 变量与 docker-compose.yml 之间一致（当前使用 Harbor 私有仓库）
- 构建路径与项目实际目录结构匹配
- 敏感变量（`SSH_PASSWORD`/`DB_PASS`/`JWT_SECRET`/`HARBOR_PASSWORD`）通过 GitLab CI/CD Variables 管理，禁止硬编码
- docker-compose.yml 中的环境变量默认值不得包含真实密码（应通过部署主机 `.env` 注入）
- SSH 认证使用 sshpass 密码方式（`sshpass -e`），密码从 `SSHPASS` 环境变量读取
- 部署脚本中使用 `${VAR:?error message}` 确保必要变量存在
- 禁止在日志中输出敏感变量
