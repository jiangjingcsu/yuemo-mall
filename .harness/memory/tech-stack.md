# 技术栈记录

> 当前技术栈及版本。AI 新增依赖时必须参考此文件，避免版本冲突。

---

## 后端

| 技术 | 版本 | 用途 |
|---|---|---|
| Java | 17 | 运行时 |
| Spring Boot | 3.2.12 | 应用框架 |
| MyBatis-Plus | 3.5.9 | ORM |
| MySQL | 8.0 | 数据库 |
| Redis | 7.x | 缓存/分布式锁 |
| RocketMQ | 5.x (rocketmq-spring-boot 2.3.5) | 消息队列 |
| Flyway | 10.x | 数据库迁移 |
| JWT | jjwt 0.12.x | Token 认证 |
| BCrypt | Hutool BCrypt | 密码加密 |
| Knife4j | 4.x | API 文档 |
| Sentinel | 1.8.x | 熔断降级 |
| Maven | 3.8+ | 构建工具 |

## 前端

| 技术 | 版本 | 用途 |
|---|---|---|
| TypeScript | 5.8 | 语言 |
| React | 19 | UI 框架 |
| Vite | 6 | 构建工具 |
| Ant Design | 5 | 组件库 |
| Redux Toolkit | 2 | 状态管理 |
| React Router DOM | 7 | 路由 |
| Axios | 1.x | HTTP 客户端 |

## 基础设施

| 技术 | 用途 |
|---|---|
| Docker Compose | 服务编排 |
| Nginx | 反向代理 + 静态托管 |
| GitLab CI | CI/CD |
| Ubuntu Server | 生产 OS |

---

## 依赖约束

```yaml
禁止:
  - 修改 Spring Boot 主版本（3.2.x）
  - 修改 Java 版本（17）
  - 新增与现有技术栈冲突的依赖
  - 引入未被团队认可的新框架

允许:
  - 新增工具类库（需确认）
  - 更新 patch 版本
  - 新增前端依赖（需确认）
```
