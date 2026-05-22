# 常见错误与恢复策略

> AI Agent 遇到错误时的排查和恢复参考。优先查阅此文件，再结合 memory/incidents.md 中的历史事故。
> 交叉引用: memory/incidents.md（历史事故）、constraints/deployment.md（部署回滚策略）

---

## 后端错误

| 错误场景 | 恢复策略 |
|---|---|
| Maven 依赖下载失败 | ① 检查网络连通性 ② 检查 `settings.xml` 镜像配置（aliyunmaven=central, aliyun-spring=spring） ③ SNAPSHOT 依赖需确认 `<snapshots><enabled>true</enabled></snapshots>` ④ 尝试 `mvn -U` 强制更新 |
| Maven 编译错误 | ① 检查 Java 版本是否为 17（`java -version`） ② 检查模块依赖顺序（多模块需从根目录编译） ③ 检查是否有未安装的子模块依赖（先 `mvn install -DskipTests`） ④ 单模块编译：`mvn compile -pl <module>` |
| Maven 单元测试失败 | ① 检查数据库连接配置（`DB_HOST`/`DB_PASS`） ② 检查 MyBatis Mapper XML 路径 ③ 检查 Redis 连接是否可用 ④ 单模块跳过测试编译其他模块：`mvn compile -pl <module>` |
| Spring Boot 启动失败 | ① 检查端口 8080 是否被占用 ② 检查环境变量是否完整（`DB_PASS`/`REDIS_PASSWORD`/`JWT_SECRET`/`ROCKETMQ_NS`） ③ 检查 Flyway 迁移是否全部成功 ④ 检查日志 `./logs/yuemo-mall-1/` 或 `./logs/yuemo-mall-2/` |
| Flyway 迁移 Checksum 不匹配 | ① **禁止修改已执行的迁移脚本**（参见 constraints/database.md） ② 还原被修改的脚本为原始版本 ③ 新变更必须新建迁移文件 `V{N+1}__{desc}.sql`（参见 incidents.md INC002） |
| JWT WeakKeyException | ① 检查 `JWT_SECRET` 环境变量是否 ≥256 位 ② 生产环境必须通过环境变量注入强随机密钥 ③ 开发默认值仅用于本地（参见 incidents.md INC003） |
| Actuator 健康检查 DOWN | ① 检查 `/actuator/health` 返回的具体组件状态（db/redis/rocketmq） ② DB 连接异常：检查 `DB_HOST`/`DB_PASS`/网络 ③ Redis 连接异常：检查 `REDIS_HOST`/`REDIS_PASSWORD` ④ RocketMQ 连接异常：检查 `ROCKETMQ_NS`/nameserver 是否可达 |

## 前端错误

| 错误场景 | 恢复策略 |
|---|---|
| npm install 失败 | ① 先尝试 `npm ci`（依赖 package-lock.json，确定性构建） ② 失败则删除 `node_modules` 后重试 `npm ci` ③ 仍失败则 `npm install` ④ **最后手段**才删除 `package-lock.json` 重新生成（会破坏确定性构建） |
| TypeScript 类型错误 | ① 检查 `tsconfig.json` 配置 ② 检查路径别名 `@/*` → `src/*` 与 vite.config.ts 是否一致 ③ 检查依赖版本兼容性 ④ 运行 `npx tsc --noEmit` 定位具体错误 |
| Vite 构建失败 | ① 检查 `vite.config.ts` 配置 ② 检查 import 路径（`@/` 别名是否正确） ③ 检查环境变量 `VITE_*` 是否设置 ④ 检查是否有循环依赖 |
| Vite 开发代理 502 | ① 检查 `vite.config.ts` 中 proxy target（默认 `http://192.168.1.56`）是否可达 ② 本地开发改为 `http://localhost:8081` ③ 检查后端服务是否启动 |

## Docker 错误

| 错误场景 | 恢复策略 |
|---|---|
| Docker build 失败 | ① 检查 Dockerfile 路径和 ARG（前端 `API_PROXY_TARGET`，后端 `JAR_FILE`） ② 检查 COPY 源文件是否存在（后端需先 `mvn package`） ③ 检查私有镜像仓库 `192.168.1.53/base-images/` 是否可达 ④ 检查 `docker login 192.168.1.53` 认证状态 |
| 镜像拉取失败 | ① 检查网络连通性 ② 检查 Harbor 仓库地址 `192.168.1.53/yuemo-mall/` ③ 检查 `docker login` 认证 ④ 检查镜像标签是否存在 |
| 容器启动失败 | ① 检查端口占用（后端 8081/8082，前端 8080） ② 检查环境变量是否完整 ③ 检查 docker-compose external network 是否存在（`backend-net`/`frontend-net`） ④ 查看容器日志 `docker compose logs <service>` |
| 容器健康检查失败 | ① 后端：检查 `/actuator/health` 返回（DB/Redis/MQ 连接状态） ② 前端：检查 nginx 配置是否正确（`wget --spider localhost:80`） ③ 检查容器日志 `docker logs --tail=50 <container>` ④ 检查容器状态 `docker inspect --format='{{.State.Health.Status}}' <container>` |
| 双重 Nginx 代理 400 错误 | ① 检查是否存在前端 Nginx + 外部 Nginx 双重代理 ② 确保前端 Nginx 直接代理到后端，避免双重代理 ③ 检查 proxy_set_header 配置是否完整（参见 incidents.md INC004） |

## CI/CD 错误

| 错误场景 | 恢复策略 |
|---|---|
| CI 流水线卡住 | ① 检查 runner 是否在线（GitLab → Settings → CI/CD → Runners） ② 检查 Docker daemon 是否可用（`docker info`） ③ 检查镜像拉取超时配置 ④ 检查 `changes` 规则是否导致跳过构建 |
| Maven 缓存损坏 | ① 清除 CI 缓存（GitLab → Pipelines → Clear Runner Caches） ② 本地清除 `~/.m2/repository/` 后重新构建 |
| npm 缓存损坏 | ① 清除 CI 缓存 ② 本地删除 `node_modules/` 后 `npm ci` |
| Harbor 镜像推送失败 | ① 检查 `HARBOR_PASSWORD` CI/CD Variable 是否设置 ② 检查 Harbor 服务 `192.168.1.53` 是否可达 ③ 检查镜像标签是否冲突（SHA 标签不可覆盖） |
| 部署 SSH 连接失败 | ① 检查 `SSH_PASSWORD`/`SSHPASS` CI/CD Variable 是否设置 ② 检查目标服务器 `192.168.1.55` 是否可达 ③ 检查 sshpass 是否安装 |
| 部署后健康检查失败 | ① 后端滚动更新：若 yuemo-mall-1 失败，yuemo-mall-2 不会被更新（旧版本仍在运行） ② 查看容器日志 `docker logs --tail=30 <container>` ③ 检查环境变量和端口映射 ④ 回退到上一版本镜像（参见 constraints/deployment.md 回滚策略） ⑤ 前端部署为停机模式，更新期间短暂不可用 |

## 验证失败处理流程

```
验证失败
  ↓
分析错误日志（后端: ./logs/yuemo-mall-{1,2}/, 容器: docker logs --tail=50 <container>）
  ↓
定位根因（编译错误？依赖缺失？配置错误？健康检查异常？）
  ↓
查阅 memory/incidents.md 是否有相似历史事故
  ↓
修复代码（修复根因而非症状）
  ↓
重新验证（最多 3 次）
  ↓
3 次仍失败 → 暂停，向用户报告：错误信息 + 已尝试的修复 + 建议的下一步
```

## 状态恢复

- 每次修改文件后，记录修改清单（哪些文件、改了什么、为什么改）
- 如果任务中断，通过 `git diff` 恢复上下文
- 使用 `git status` 确认当前修改状态
- 部署版本追踪：检查 `.backend-version` / `.frontend-version` 文件确认当前部署版本
- 容器日志持久化：后端日志挂载到宿主机 `./logs/yuemo-mall-1/` 和 `./logs/yuemo-mall-2/`
- 回退到上一版本：使用 SHA 标签 `docker pull 192.168.1.53/yuemo-mall/backend:{上一版本SHA}`
