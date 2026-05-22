# 代码执行器

> 调度 backend-agent / frontend-agent 执行代码生成和修改任务。
> 通用约束验证（constraints/ 红线、用户确认等）由 workflow-executor 统一执行。
> 评估管线详见 evaluation/review-checklist.md。

---

## 输入

```yaml
输入:
  - 任务描述（功能/Bug/重构）
  - 目标模块
  - 技术方案（来自 architect-agent，可选）
```

## 前置条件

```yaml
必须全部满足:
  - [ ] 任务目标明确（功能点/Bug 描述/重构目标）
  - [ ] 目标模块已确定
  - [ ] 现有代码可编译（mvn compile -q 通过）
  - [ ] 涉及架构变更 → architect-agent 已输出技术方案
  - [ ] 涉及支付/认证模块 → 用户已确认
```

## 前置检查

```yaml
检查:
  1. 是否违反 constraints/ 中的红线？→ 违反则停止
  2. 是否涉及跨模块调用？→ 加载 module-boundary.md 验证
  3. 是否涉及数据库变更？→ 必须先创建 Flyway 脚本
  4. 是否涉及支付？→ 加载 constraints/payment.md，需用户确认
  5. 是否涉及 API 变更？→ 评估前端兼容性
  6. 是否需要新增依赖？→ 需用户确认（ai-boundaries.md）
```

## 上下文加载决策

> 本 executor 仅负责根据代码变更类型决定额外加载哪些治理规则。

```yaml
始终加载:
  - memory/decisions.md — 历史架构决策（避免与已否决方案矛盾）
  - memory/anti-patterns.md — 已知反模式（避免重复踩坑）

按需加载:
  1. 涉及高扩展模块（支付/MQ/营销）？→ 加载 extensibility-governance.md + design-pattern-governance.md
  2. 涉及行为分发（action/event/type 路由）？→ 加载 design-pattern-governance.md（策略模式强制）
  3. 涉及数据库变更？→ 加载 database-governance.md + constraints/database.md
  4. 涉及支付？→ 加载 constraints/payment.md
  5. 涉及缓存/MQ？→ 加载 redis-governance.md / mq-governance.md
  6. 涉及 Entity 修改？→ 加载 domain-modeling.md（充血模型规范）
  7. 涉及跨模块调用？→ 加载 module-boundary.md（模块边界）
  8. 涉及前端修改？→ 加载 frontend-coding.md（前端编码规范）
  9. 涉及 API 设计？→ 加载 api-governance.md（API 规范）
```

## 执行步骤

```yaml
后端代码:
  1. 如涉及数据库 → 先创建 Flyway 迁移脚本（V{version}__{description}.sql）
  2. 创建/修改枚举/常量 → 如需新增状态码、Redis Key 常量等
  3. 创建/修改 Entity → 充血模型，包含领域行为方法
  4. 创建/修改 Mapper → BaseMapper + 复杂查询 XML
  5. 创建/修改 DTO/VO → Java record
  6. 创建/修改 MapStruct Converter → DTO ↔ Entity 转换
  7. 创建/修改 Service → 业务编排，不用 switch-case 分发
  8. 创建/修改 MQ Producer/Consumer → 如涉及异步通信
  9. 创建/修改 Controller → 协议转换，返回 Result<T>
  10. 编译验证: mvn compile -q
  11. 测试验证: mvn test -pl <module> -q

  每步验证（步骤 2-9 每完成一步）:
    - mvn compile -q（确保编译通过）
    - 如修改涉及支付/余额 → 额外检查事务和并发安全

前端代码:
  1. 创建/修改类型定义（src/types/）→ API 响应类型
  2. 创建/修改 API 层（src/api/）→ 类型化泛型 request.get<T>()
  3. 创建/修改 Store（如需）→ Redux slice
  4. 创建/修改组件 → useMemo/useCallback，颜色使用 token 变量
  5. 创建/修改页面 → 组合组件
  6. 类型检查: npx tsc --noEmit
  7. 构建验证: npm run build

  每步验证（步骤 1-5 每完成一步）:
    - npx tsc --noEmit（确保类型正确）
```

## 约束

```yaml
必须:
  - Entity 使用充血模型（包含领域行为方法，非纯 @Data）
  - Controller 只做协议转换，返回 Result<T>
  - Service 不使用 switch-case 分发（用策略模式）
  - DTO/VO 与 Entity 分离（通过 MapStruct 转换）
  - 金额使用 BigDecimal（禁止 float/double）
  - SQL 使用 #{} 参数化（禁止 ${} 拼接用户输入）
  - 跨模块调用通过 Service API 或 MQ（不直接注入其他模块 Mapper）
  - 新增支付方式必须实现 PayTypeHandler 接口
  - 编译通过后才算完成

禁止:
  - 跳过编译验证步骤
  - 直接修改测试代码使其通过（应修复业务代码）
  - 在 Entity 中注入 Service/Mapper
  - Controller 包含业务逻辑
  - API 直接返回 Entity（必须转 VO）
  - 硬编码密钥/密码/Token
  - 修改已执行的 Flyway 迁移脚本
```

## 评估

```yaml
按 evaluation/review-checklist.md 执行评估:
  完整评估: 跨模块变更 / 支付订单用户模块 / 新增 API / 数据库迁移
  简化评估: 单模块内 ≤ 3 文件修改
  最小评估: 纯样式/文案/注释修改

后端快速检查:
  - [ ] 编译通过（mvn compile -q）
  - [ ] 测试通过（mvn test -pl <module> -q）
  - [ ] Controller 无业务逻辑
  - [ ] 无 switch-case 行为分发
  - [ ] Entity 含领域行为（非贫血）
  - [ ] 模块边界未被破坏
  - [ ] DTO/VO 与 Entity 分离
  - [ ] 金额使用 BigDecimal
  - [ ] SQL 使用 #{} 参数化
  - [ ] 异常使用 BusinessException + ResultCode

前端快速检查:
  - [ ] tsc 无类型错误
  - [ ] build 成功
  - [ ] 无硬编码色值（使用 token 变量）
  - [ ] API 调用使用泛型（request.get<T>()）
  - [ ] 组件使用 useMemo/useCallback
```

## 输出

```yaml
代码执行结果:
  - 修改文件列表（含变更类型: 新增/修改/删除）
  - 评估结果（PASS/WARN/FAIL）
  - Flyway 迁移脚本（如有，标注不可回滚风险）
  - 风险提示（如有）
  - Memory 写入（如发现反模式或已知问题）
```

## 回滚

```yaml
编译失败:
  1. 检查错误信息
  2. 修复代码（最多 3 次）
  3. 3 次仍失败 → git checkout <修改的文件> 还原，报告原因

测试失败:
  1. 分析测试失败原因
  2. 修复业务代码（不修改测试代码）
  3. 3 次仍失败 → 还原修改，报告原因

约束违反:
  → 立即停止并报告，不自动回滚（需用户确认）

涉及 Flyway 迁移:
  → Flyway 不支持自动回滚，需提前准备逆向迁移脚本 V{N+1}__rollback_{desc}.sql
  → 回滚时需用户确认
```

## Agent

```yaml
调度: backend-agent（后端）/ frontend-agent（前端）
前置: architect-agent（需架构方案时）
协作: security-agent（涉及支付/认证时）
后置: reviewer-agent（修改完成后审查）
```
