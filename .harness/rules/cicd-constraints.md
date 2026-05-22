# CI/CD 流水线约束

> 流水线配置以 `.gitlab-ci.yml` 和各 `Dockerfile` 为准，本文件仅定义修改约束。
> 交叉引用: constraints/deployment.md（部署流程红线、回滚策略、部署验证）

## 修改约束

修改 `.gitlab-ci.yml`、`Dockerfile` 或 `docker-compose.yml` 后，必须检查：

- YAML 语法正确性
- 镜像名称一致性校验规则：
    CI 变量 `BACKEND_IMAGE` = `${HARBOR_REGISTRY}/${HARBOR_PROJECT}/backend`
    CI 变量 `FRONTEND_IMAGE` = `${HARBOR_REGISTRY}/${HARBOR_PROJECT}/frontend`
    docker-compose.yml 中 `image` 必须与上述变量展开后一致
    当前值: `HARBOR_REGISTRY=192.168.1.53`, `HARBOR_PROJECT=yuemo-mall`
    修改 `HARBOR_REGISTRY` 或 `HARBOR_PROJECT` 时，必须同步更新 docker-compose.yml
- 构建路径约束：
    后端: 上下文 `yuemo-backend/`，Dockerfile 位于 `yuemo-backend/Dockerfile`（默认位置）
    前端: 上下文 `yuemo-frontend/`，Dockerfile 位于 `yuemo-frontend/docker/Dockerfile`（需 `-f` 指定）
    修改目录结构时必须同步更新 `.gitlab-ci.yml` 中的 `cd` 和 `-f` 路径
- 敏感变量（`SSH_PASSWORD` / `HARBOR_USERNAME` / `HARBOR_PASSWORD` / `DB_PASS` / `REDIS_PASSWORD` / `JWT_SECRET`）通过 GitLab CI/CD Variables 管理，禁止硬编码
- docker-compose.yml 中的敏感环境变量禁止设置默认值（当前实现无默认值，需保持），所有敏感值通过部署主机 `.env` 文件注入
- SSH 认证使用 sshpass 密码方式（`sshpass -e`），密码链路：GitLab CI Variable `SSH_PASSWORD` → `SSHPASS` 环境变量 → `sshpass -e` 读取
- 部署脚本中使用 `${VAR:?error message}` 确保必要变量存在（当前实际代码使用 `[ -z "$VAR" ]` 模式，建议后续统一为 `${VAR:?}` 标准写法）
- 禁止在日志中输出敏感变量
- Dockerfile 基础镜像必须使用 Harbor 私有仓库（`192.168.1.53/base-images/`），禁止使用 Docker Hub 公共镜像
- Docker 镜像构建时必须注入溯源 label（`commit` / `branch` / `job`），禁止移除
- 前端构建参数 `API_PROXY_TARGET` 通过 `--build-arg` 注入，构建时写入 Nginx 配置，运行时不可更改，需重新构建镜像才能生效
- 流水线触发规则：所有 Job 限定 `main` 分支，`changes` 路径与模块目录对应（`yuemo-backend/**/*` / `yuemo-frontend/**/*`），新增模块需同步添加 Job
- 部署版本记录：每次部署成功写入 `.backend-version` / `.frontend-version`，格式为 `{CI_COMMIT_SHA}  {UTC时间}  {CI_COMMIT_SHORT_SHA}`，用于回滚溯源，禁止删除或篡改
