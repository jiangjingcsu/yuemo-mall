# 项目演进计划

> 项目未来演进方向和路线图。AI Agent 设计新功能时必须考虑演进方向，不为未来拆分制造障碍。
> 关联: `memory/architecture-history.md`（历史阶段 Phase 1~4）、`memory/decisions.md`（技术决策）、`memory/anti-patterns.md`（反模式）、`memory/known-issues.md`（技术债务）
> 约束关联: `constraints/production.md`、`constraints/database.md`、`constraints/payment.md`

---

## 当前阶段

```yaml
当前: Phase 4 — 企业级升级（已完成）
  说明: Phase 1~4 历史详见 memory/architecture-history.md
  下一阶段: Phase 5 — 代码质量提升
```

---

## 演进路线图

### Phase 间前置依赖

```
Phase 5（代码质量提升）
  ├── 消除反模式和技术债务
  └── 依赖: Phase 4 治理层已建立（constraints/evaluation/memory 就绪）
        ↓
Phase 6（微服务就绪）
  ├── 模块边界加固、通信标准化
  └── 依赖: Phase 5 反模式已消除（有反模式时拆分更危险）
        ↓
Phase 7（生产就绪）
  ├── 监控、灾备、安全审计
  └── 依赖: Phase 6 微服务就绪后独立监控才有意义
```

### 里程碑（条件触发）

```yaml
Phase 5 完成标志:
  - AP001~AP006 全部状态为 [FIXED]
  - 测试覆盖率 ≥ 80%
  - XSS 过滤器已部署
  - CORS 配置已收紧
  - payment.md 已知高风险项已修复

Phase 6 完成标志:
  - 跨模块 Mapper 直接调用全部消除（cart→product 只读除外，微服务拆分时按计划改为 API）
  - 跨模块事务 100% 走 MQ 最终一致性
  - 所有模块 API 接口已版本化
  - 数据库按模块分离访问已验证

Phase 7 完成标志:
  - Prometheus + Grafana 全链路监控上线
  - 灾备演练通过（RPO/RTO 达标）
  - 安全审计无 CRITICAL 问题
  - 自动化压测报告通过
```

---

### Phase 5: 代码质量提升（下一阶段）

```yaml
目标: 消除已识别的反模式和技术债务

P0（安全/资金风险，必须修复）:
  - payment.md 已知高风险项:
      · 签名验证桩实现 → 接入真实 SDK 验签
      · createPayment 无分布式锁 → 添加 Redis 锁或 DB 唯一索引
      · 退款接口无幂等保护 → 添加 Redis 幂等键或 DB 唯一约束
  - 测试覆盖率: 当前基线待测量 → 目标 80%

P1（架构反模式）:
  - CartSyncConsumer switch→策略模式重构（AP001）
  - PaymentServiceImpl if-else→策略+工厂重构（AP002）
  - Entity 充血模型改造（AP003，订单状态流转）
  - XSS 过滤器（TD004）

P2（代码质量）:
  - 魔法值枚举化（AP004 / TD006）
  - CORS 配置收紧（TD005）
```

---

### Phase 6: 微服务就绪

```yaml
目标: 完成微服务拆分的所有准备，但不实际拆分
前置: Phase 5 反模式已消除 + payment.md 高风险项已修复

模块边界:
  - 消除 cart→product Mapper 直接读取（当前为 module-boundary.md 允许的例外，微服务拆分时改为 API 调用）
  - 所有跨模块事务改为 MQ 最终一致性
  - 数据库表按模块分离访问（先在同一 MySQL 实例内按模块隔离 schema，拆分时再分实例）

通信标准化:
  - 模块间 Service 调用改为明确 API 接口
  - 引入 API 版本管理
  - MQ 消费者幂等统一方案（所有消费者使用 Redis 幂等 mq:consumed:{topic}:{bizId}）

支付模块微服务前置:
  - payment.md 已知风险全部修复后方可拆分
  - 支付回调幂等方案统一（当前 payment 有、cart 无）
```

---

### Phase 7: 生产就绪

```yaml
目标: 生产级别的可用性和安全性

全链路监控:
  - Prometheus + Grafana（与现有 Sentinel 互补：Sentinel 负责熔断降级，Prometheus 负责指标采集和可视化）
  - 分布式链路追踪
  - 日志中心化
  - 核心业务指标告警

灾备:
  - RPO ≤ 1 小时 / RTO ≤ 4 小时
  - 每日增量备份 + 每周全量备份
  - 故障切换演练
  - 数据库主从切换演练

安全审计:
  - 代码审计（OWASP Top 10 覆盖）
  - 依赖审计（CVE 扫描 + 版本升级）
  - 渗透测试（认证/支付/API 接口）
  - 合规审计（数据保护/日志留存）

性能:
  - 自动化压测（核心 API + 支付链路）
  - 性能回归检测
  - CDN 加速（静态资源）

前端演进:
  - 性能优化: 代码分割 / 懒加载 / 图片优化 / 包体积控制
  - 用户体验: 骨架屏 / 离线缓存 / PWA
  - 可维护性: 组件库升级 / 类型安全 / E2E 测试
  - 适配: 移动端响应式 / 无障碍化
```

---

## 架构原则（面向演进）

```yaml
AI 设计新功能时必须:
  1. 保持模块边界清晰（不跨模块操作数据库）
  2. 模块间通过 Service 接口通信（不直接调 Mapper）
  3. 跨模块一致性走 MQ（不在本地事务中跨模块操作）
  4. 每个模块具备独立拆分能力（独立的数据/Service/API）
  5. 不引入循环依赖（当前无循环依赖，保持）
  6. 遵循 constraints/ 红线（production / database / payment / deployment / infra / ai-boundaries）

前端架构原则:
  1. API 调用层按模块拆分（src/api/{module}/）
  2. 页面组件按业务模块组织（pages/{module}/）
  3. 状态管理按模块拆分（stores/{module}Slice）
  4. 公共组件保持无业务依赖（components/ 不引用 pages/ 或 stores/）

AI 禁止:
  - 为短期方便而破坏模块边界
  - 在 Controller 中写业务（拆分时需要重写）
  - 直接调其他模块的 Mapper（拆分时需要改 RPC）
```
