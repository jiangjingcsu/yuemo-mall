# 部署约束

> 部署流程红线。所有部署相关操作必须遵守。
> 交叉引用: constraints/production.md（生产红线）、constraints/infra.md（基础设施约束）
>
> **职责边界**：本文件关注部署**流程**（构建→推送→部署→验证→回滚），infra.md 关注基础设施**拓扑**（服务器/网络/Docker Compose 结构）。两者在"单副本下线"等场景有交叉，以本文件为流程权威，infra.md 为拓扑权威。

---

## 部署流程

```yaml
标准部署流程:
  1. CI 编译 + 测试通过（test:backend / test:frontend）
  2. 构建 Docker 镜像
  3. 推送镜像到 Harbor Registry（192.168.1.53/yuemo-mall/）
  4. 滚动更新（后端逐副本更新，至少保持 1 个副本存活）
  5. 健康检查验证（/actuator/health 返回 HTTP 200）
  6. 核心 API 验证
  7. 日志检查

禁止跳过: 编译 → 测试 → 镜像构建 → 滚动更新 → 健康检查
```

---

## 部署方式

```yaml
后端（2 副本滚动更新）:
  方式: 逐副本更新，禁止同时更新所有副本
  步骤:
    1. docker compose pull yuemo-mall-1
    2. IMAGE_TAG={版本} docker compose up -d --no-deps --force-recreate yuemo-mall-1
    3. 等待健康检查通过（/actuator/health 返回 HTTP 200）
    4. docker compose pull yuemo-mall-2
    5. IMAGE_TAG={版本} docker compose up -d --no-deps --force-recreate yuemo-mall-2
    6. 等待健康检查通过
  禁止: docker compose up -d --force-recreate（不指定服务名时会同时重建所有副本）

前端（单副本）:
  方式: 直接替换
  注意: 更新期间前端短暂不可用（可接受），应避免高峰期操作
  步骤:
    1. docker compose pull frontend-server
    2. IMAGE_TAG={版本} docker compose up -d --no-deps --force-recreate frontend-server
    3. 等待健康检查通过（/health 返回 200）
```

---

## 镜像版本管理

```yaml
标签策略:
  - {CI_COMMIT_SHORT_SHA}: 不可变版本标签，禁止覆盖已推送的 SHA 标签
  - latest: 指向最新部署版本，每次部署自动更新

镜像仓库: 192.168.1.53/yuemo-mall/
  - backend: 192.168.1.53/yuemo-mall/backend
  - frontend: 192.168.1.53/yuemo-mall/frontend

回滚时: 使用上一版本的 SHA 标签拉取镜像
  示例: docker pull 192.168.1.53/yuemo-mall/backend:abc1234
```

---

## 绝对禁止

```yaml
禁止:
  - 跳过测试直接部署
  - 同时停止所有副本（导致服务中断）
  - 在生产环境运行未经 CI 验证的镜像
  - 部署后不做健康检查
  - 回滚时删除 Flyway 迁移记录（Flyway 不支持自动回滚，需提前准备逆向迁移脚本 V{N+1}__rollback_{desc}.sql）
  - 手动在生产服务器修改 JAR/配置文件
  - 覆盖已推送的 SHA 版本镜像标签
  - 直接修改运行中的 docker-compose.yml（参见 constraints/production.md）
  - 单副本下线（至少保持 1 个副本运行，参见 constraints/production.md）
```

---

## 部署验证

```yaml
部署后必须验证:
  后端:
    - 健康检查端点: GET /actuator/health 返回 UP（含 DB/Redis/MQ 连接状态）
    - 核心 API: GET /api/product/list 返回 200（半白名单，无需认证）
    - 日志无 ERROR 级别异常: docker logs --tail=50 <container>
    - Docker 容器健康: docker inspect --format='{{.State.Health.Status}}' <container>
    - 内存/CPU 使用正常
  前端:
    - 健康检查端点: GET /health 返回 200（Nginx 静态返回）
    - 首页可访问: GET / 返回 200
    - API 代理正常: GET /api/product/list 通过 Nginx 代理返回 200
    - Nginx 日志无 5xx 错误

健康检查参数:
  后端: 重试 20 次，间隔 5s，总超时 100s
  前端: 重试 10 次，间隔 3s，总超时 30s
```

---

## 部署窗口

```yaml
建议:
  - 避免业务高峰期部署（具体时段由业务方定义）
  - 数据库迁移必须在低峰期执行
  - 前端单副本更新期间短暂不可用，需提前通知

紧急部署:
  - 紧急修复可豁免部署窗口限制
  - 但仍须遵守滚动更新 + 健康检查约束
  - 部署后必须补记部署原因和审批记录
```

---

## 回滚策略

```yaml
回滚触发条件:
  - /actuator/health 返回 DOWN
  - 错误率 > 5%（5 分钟内）
  - 响应时间 > 正常值 3 倍
  - 核心功能不可用

回滚步骤（后端）:
  1. 确认需要回滚，通知用户
  2. 逐副本回滚:
     docker pull 192.168.1.53/yuemo-mall/backend:{上一版本SHA}
     IMAGE_TAG={上一版本SHA} docker compose up -d --no-deps yuemo-mall-1
     等待健康检查通过
     IMAGE_TAG={上一版本SHA} docker compose up -d --no-deps yuemo-mall-2
  3. 验证服务恢复

回滚步骤（前端）:
  1. docker pull 192.168.1.53/yuemo-mall/frontend:{上一版本SHA}
  2. IMAGE_TAG={上一版本SHA} docker compose up -d --no-deps frontend-server
  3. 验证首页可访问

数据库回滚:
  - Flyway 不支持自动回滚
  - 需提前准备逆向迁移脚本（V{N+1}__rollback_{desc}.sql）
  - 数据库回滚需经用户确认

配置回滚:
  - 恢复上一版本配置文件
  - Nginx 配置变更后须执行 nginx -t 验证

回滚后必须:
  - 验证服务恢复（/actuator/health + 核心 API）
  - 记录回滚原因和时间（写入 .backend-version / .frontend-version）
  - 修复问题后重新部署
  - 通知相关人员
```

---

## 部署失败处理

```yaml
自动回滚:
  触发: 健康检查在重试次数耗尽后仍失败
  动作: 自动回滚到上一版本镜像

手动回滚:
  触发: 错误率/响应时间超标、核心功能不可用
  动作: 人工确认后执行回滚

失败后:
  - 收集现场日志: docker logs --tail=100 <container>
  - 记录失败原因
  - 通知相关人员
  - 修复后重新走完整部署流程（禁止跳过步骤）
```
