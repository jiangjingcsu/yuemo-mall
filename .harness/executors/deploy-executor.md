# 部署执行器

> 调度 devops-agent 执行部署流程。
> 交叉引用: `constraints/deployment.md`（部署约束）、`constraints/production.md`（生产红线）、`constraints/infra.md`（基础设施约束）、`.gitlab-ci.yml`（CI/CD 配置）

---

## 前置条件

```yaml
CI 强制门禁（必须全部满足）:
  - [ ] CI 编译通过（build:backend / build:frontend）
  - [ ] CI 测试通过（test:backend / test:frontend）
  - [ ] Docker 镜像构建并推送到 Harbor
  - [ ] 用户确认部署

流程规范建议（非 CI 强制，但强烈建议）:
  - [ ] 代码审查通过（无 CRITICAL/HIGH 问题）
  - [ ] 安全审查通过（如涉及敏感代码）
  - [ ] Flyway 迁移脚本已准备（如有数据库变更）
```

## 前置检查

```yaml
检查:
  1. 是否违反 constraints/deployment.md？
  2. 是否违反 constraints/production.md？
  3. 是否违反 constraints/infra.md？
  4. 是否需要在非高峰期部署？
  5. 是否有回滚方案？
  6. 如有数据库变更，Flyway 迁移脚本是否已准备？
```

## 执行步骤

```yaml
后端（2 副本滚动更新，通过 CI Runner SSH 到 192.168.1.55 执行）:
  1. 传输配置文件:
     scp yuemo-backend/docker/docker-compose.yml ${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_PATH}/yuemo-backend/docker/
     tar czf nginx-config.tar.gz -C yuemo-backend/docker nginx/
     scp nginx-config.tar.gz ${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_PATH}/

  2. SSH 到生产服务器:
     ssh ${DEPLOY_USER}@${DEPLOY_HOST}

  3. 解压 Nginx 配置:
     tar xzf ${DEPLOY_PATH}/nginx-config.tar.gz -C ${DEPLOY_PATH}/yuemo-backend/docker/

  4. 创建日志目录:
     mkdir -p ${DEPLOY_PATH}/yuemo-backend/docker/logs/yuemo-mall-1 ${DEPLOY_PATH}/yuemo-backend/docker/logs/yuemo-mall-2

  5. 确保 Docker 网络存在:
     docker network create backend-net 2>/dev/null || true

  6. 登录 Harbor 私有仓库:
     docker login 192.168.1.53 -u ${HARBOR_USERNAME} --password-stdin

  7. 拉取新镜像:
     docker pull 192.168.1.53/yuemo-mall/backend:${CI_COMMIT_SHORT_SHA}

  8. 滚动更新 yuemo-mall-1:
     cd ${DEPLOY_PATH}/yuemo-backend/docker
     IMAGE_TAG=${CI_COMMIT_SHORT_SHA} docker compose up -d --no-deps --force-recreate yuemo-mall-1

  9. 等待 yuemo-mall-1 健康检查通过:
     curl -s -o /dev/null -w '%{http_code}' http://localhost:8081/actuator/health
     期望: HTTP 200，重试 20 次，间隔 5s，总超时 100s
     失败则: docker logs yuemo-mall-1 --tail 30 && exit 1

  10. 滚动更新 yuemo-mall-2:
      IMAGE_TAG=${CI_COMMIT_SHORT_SHA} docker compose up -d --no-deps --force-recreate yuemo-mall-2

  11. 等待 yuemo-mall-2 健康检查通过:
      curl -s -o /dev/null -w '%{http_code}' http://localhost:8082/actuator/health
      期望: HTTP 200，重试 20 次，间隔 5s，总超时 100s
      失败则: docker logs yuemo-mall-2 --tail 30 && exit 1

  12. 核心 API 验证（经前端 Nginx 代理）:
      curl http://localhost:8080/api/product/list

  13. 日志检查:
      docker logs --tail=50 yuemo-mall-1
      docker logs --tail=50 yuemo-mall-2
      无 ERROR 级别日志

  14. 记录部署版本:
      echo '${CI_COMMIT_SHA}  $(date -u +%Y-%m-%dT%H:%M:%SZ)  ${CI_COMMIT_SHORT_SHA}' > ${DEPLOY_PATH}/.backend-version

  15. 清理旧镜像:
      docker image prune -f

前端（单副本，通过 CI Runner SSH 到 192.168.1.55 执行）:
  1. 传输配置文件:
     scp yuemo-frontend/docker/docker-compose.yml ${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_PATH}/yuemo-frontend/docker/

  2. SSH 到生产服务器:
     ssh ${DEPLOY_USER}@${DEPLOY_HOST}

  3. 停止并移除旧容器:
     docker stop frontend-server 2>/dev/null || true
     docker rm frontend-server 2>/dev/null || true

  4. 确保 Docker 网络存在:
     docker network create frontend-net 2>/dev/null || true

  5. 登录 Harbor 私有仓库:
     docker login 192.168.1.53 -u ${HARBOR_USERNAME} --password-stdin

  6. 拉取新镜像:
     docker pull 192.168.1.53/yuemo-mall/frontend:${CI_COMMIT_SHORT_SHA}

  7. 部署:
     cd ${DEPLOY_PATH}/yuemo-frontend/docker
     IMAGE_TAG=${CI_COMMIT_SHORT_SHA} docker compose up -d --force-recreate --remove-orphans

  8. 健康检查（在生产服务器上执行）:
      curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/health
      期望: HTTP 200，重试 10 次，间隔 3s，总超时 30s
      失败则: docker compose logs --tail=20 && exit 1

  9. 首页验证:
     curl http://localhost:8080/

  10. API 代理验证（经前端 Nginx → 后端 Nginx → 后端实例）:
      curl http://localhost:8080/api/product/list

  11. Nginx 日志检查:
      无 5xx 错误

  12. 记录部署版本:
      echo '${CI_COMMIT_SHA}  $(date -u +%Y-%m-%dT%H:%M:%SZ)  ${CI_COMMIT_SHORT_SHA}' > ${DEPLOY_PATH}/.frontend-version

  13. 清理旧镜像:
      docker image prune -f
```

## 评估

```yaml
部署成功:
  后端:
    - [ ] /actuator/health 返回 HTTP 200（两个副本均通过）
    - [ ] Docker 容器健康: docker inspect --format='{{.State.Health.Status}}' <container>
    - [ ] 核心 API 正常响应（经 Nginx 代理）
    - [ ] 日志无 ERROR
    - [ ] 内存/CPU 正常
  前端:
    - [ ] /health 返回 HTTP 200
    - [ ] 首页可访问
    - [ ] API 代理正常: GET /api/product/list 通过 Nginx 代理返回 200
    - [ ] Nginx 日志无 5xx 错误
  通用:
    - [ ] 可观测性指标正常（按 observability/observability-policy.md）

部署失败:
  - 立即回滚到上一版本
  - 收集现场日志: docker logs --tail=100 <container>
  - 记录失败原因
  - 通知相关人员
```

## 回滚

```yaml
回滚触发条件:
  - /actuator/health 返回 DOWN
  - 错误率 > 5%（5 分钟内）
  - 响应时间 > 正常值 3 倍
  - 核心功能不可用

自动回滚（健康检查在重试次数耗尽后仍失败）:
  后端: 逐副本回滚到上一版本镜像
    1. docker pull 192.168.1.53/yuemo-mall/backend:{上一版本SHA}
    2. IMAGE_TAG={上一版本SHA} docker compose up -d --no-deps --force-recreate yuemo-mall-1
    3. 等待健康检查通过
    4. IMAGE_TAG={上一版本SHA} docker compose up -d --no-deps --force-recreate yuemo-mall-2
  前端:
    1. docker pull 192.168.1.53/yuemo-mall/frontend:{上一版本SHA}
    2. IMAGE_TAG={上一版本SHA} docker compose up -d --no-deps --force-recreate frontend-server

手动回滚:
  1. 确认需要回滚，通知用户
  2. 逐副本切换到上一版本镜像（先 pull 再 compose up）
  3. 验证服务恢复（/actuator/health + 核心 API）
  4. 记录回滚原因和时间（写入 .backend-version / .frontend-version）
  5. 修复问题后重新走完整部署流程（禁止跳过步骤）

数据库回滚:
  - Flyway 不支持自动回滚
  - 需提前准备逆向迁移脚本（V{N+1}__rollback_{desc}.sql）
  - 数据库回滚需经用户确认

配置回滚:
  - 恢复上一版本配置文件
  - Nginx 配置变更后须执行 nginx -t 验证
```

## 输出

```yaml
部署完成后输出:
  ## 部署报告

  ### 变更内容
  - 修改的配置文件列表
  - 镜像版本变更（旧 → 新）

  ### 部署结果
  - 后端副本 1: {状态} (健康检查: {结果})
  - 后端副本 2: {状态} (健康检查: {结果})
  - 前端: {状态} (健康检查: {结果})

  ### 验证结果
  - /actuator/health: {状态}
  - 核心 API: {状态}
  - API 代理: {状态}
  - 日志检查: {状态}

  ### 回滚方案
  - 回滚镜像版本: {上一版本 SHA}
  - 回滚命令: {具体命令}
  - 数据库回滚: {需要/不需要，如需要则附逆向迁移脚本}
```

## Agent

```yaml
调度: devops-agent
确认: 所有操作需用户确认
```
