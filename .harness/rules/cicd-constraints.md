# CI/CD 流水线约束

## 流水线阶段

```
build → docker → deploy（3 阶段）
```

## 镜像名一致性

| CI 阶段构建的镜像 | docker-compose.yml 中的镜像 | docker tag 命令 |
|---|---|---|
| `yuemo-mall-backend:${SHA}` | `yuemo-mall:latest` | `docker tag yuemo-mall-backend:${SHA} yuemo-mall:latest` |
| `yuemo-mall-frontend:${SHA}` | `yuemo-mall-frontend:latest` | `docker tag yuemo-mall-frontend:${SHA} yuemo-mall-frontend:latest` |

## 构建路径

| Job | 工作目录 | Dockerfile 路径 |
|---|---|---|
| `docker:backend` | `yuemo-backend/` | `yuemo-backend/Dockerfile` |
| `docker:frontend` | `yuemo-frontend/docker/` | `yuemo-frontend/docker/Dockerfile` |

## 超时建议

| 阶段 | 建议超时 | 理由 |
|---|---|---|
| build:backend | 30 分钟 | Maven 首次下载依赖较慢 |
| build:frontend | 15 分钟 | npm ci + vite build |
| docker:backend | 10 分钟 | JRE 镜像 + JAR 复制 |
| docker:frontend | 5 分钟 | Nginx 镜像 + 静态文件复制 |
| deploy:* | 15 分钟 | 含健康检查等待时间 |

## 修改约束

修改 `.gitlab-ci.yml` 或 `Dockerfile` 后，必须检查：
- YAML 语法正确性
- 镜像名称在 CI 和 docker-compose.yml 之间一致
- 构建路径与项目实际目录结构匹配
- `SSH_PASSWORD`、`DB_PASS`、`JWT_SECRET` 通过 GitLab CI/CD Variables 管理
- SSH 认证使用 sshpass 密码方式（`sshpass -e`），密码从 `SSHPASS` 环境变量读取
- 禁止在日志中输出敏感变量
- 部署脚本中使用 `${VAR:?error message}` 确保必要变量存在
