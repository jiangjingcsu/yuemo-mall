# 审查执行器

> 调度 reviewer-agent 和 security-agent 执行代码审查。
> 通用约束验证由 workflow-executor 统一执行。
> 评估管线详见 evaluation/review-checklist.md。
> Agent 定义详见 agents/reviewer-agent.md（5 层审查 + 协作规则 + 审查后处理）。

---

## 输入

```yaml
输入:
  - 修改的文件列表
  - 修改的内容（diff）
  - 修改的类型（功能/Bug/重构）
```

## 前置条件

```yaml
必须全部满足:
  - [ ] 修改文件列表完整
  - [ ] diff 内容可获取
  - [ ] 代码已编译通过（mvn compile -q / npx tsc --noEmit）
  - [ ] 涉及支付/认证模块 → 用户已确认变更
```

## 前置检查

```yaml
检查:
  1. 是否违反 constraints/ 中的红线？→ 违反则 BLOCK
  2. 是否涉及安全敏感代码（认证/支付/用户数据）？→ 激活 security-agent
  3. 是否涉及架构变更？→ 激活 architect-agent
  4. 是否涉及数据库变更？→ 加载 database-governance.md + constraints/database.md
  5. 是否涉及支付？→ 加载 constraints/payment.md
  6. 是否涉及前端修改？→ 加载 frontend-coding.md
```

## 上下文加载决策

> 本 executor 仅负责根据审查范围决定额外加载哪些治理规则。

```yaml
始终加载:
  - memory/decisions.md — 历史架构决策（审查建议不与已否决方案矛盾）
  - memory/anti-patterns.md — 已知反模式（审查时识别重复踩坑）
  - evaluation/review-checklist.md — 综合审查清单

按需加载:
  1. 涉及安全敏感代码？→ 加载 rules/security.md + evaluation/security-check.md
  2. 涉及架构变更？→ 加载 rules/architecture-governance.md + rules/module-boundary.md + evaluation/architecture-score.md
  3. 涉及数据库变更？→ 加载 rules/database-governance.md + constraints/database.md
  4. 涉及支付？→ 加载 constraints/payment.md
  5. 涉及缓存/MQ？→ 加载 rules/redis-governance.md / rules/mq-governance.md
  6. 涉及前端修改？→ 加载 rules/frontend-coding.md
  7. 涉及代码质量深度评估？→ 加载 evaluation/code-quality.md
  8. 涉及幻觉检测？→ 加载 evaluation/hallucination-check.md
  9. 涉及风险分析？→ 加载 evaluation/risk-analysis.md
```

## 审查范围判定

```yaml
完整审查（5 层全执行）:
  - 跨模块变更
  - 支付/订单/用户模块变更
  - 架构变更
  - 新增 API 接口

简化审查（L1 + L2 + L3）:
  - 单模块内 ≤ 3 文件修改
  - 不涉及支付/订单/用户模块
  - 不涉及数据库/缓存/MQ 变更

最小审查（L1 + L3）:
  - 纯前端样式/文案修改
  - 注释修改

降级时必须记录跳过的层次和原因
```

## 执行步骤

```yaml
1. reviewer-agent 执行 5 层审查（按 workflows/code-review.md）:
   L1: 安全检查（最高优先级）:
     - 硬编码密码/密钥/Token
     - SQL 参数化（#{} vs ${}）
     - 用户输入校验
     - API 鉴权范围
     - 敏感数据暴露
     - XSS/SQL注入/CSRF 风险
     → 发现问题: CRITICAL（BLOCK），转交 security-agent 深度审查
     → 引用: rules/security.md, constraints/payment.md

   L2: 架构检查:
     - Controller 只做协议转换
     - ServiceImpl 只调本模块 Mapper
     - 跨模块直接操作数据库表
     - DTO/VO/Entity 分离
     - 循环依赖
     - 模块依赖白名单
     - 返回类型 Result<T>
     → 发现问题: HIGH，应修复
     → 引用: architecture-governance.md, module-boundary.md

   L3: 代码质量:
     - 方法过长（> 50 行）/ 类过大（> 800 行）
     - 嵌套过深（> 4 层）
     - 命名清晰一致
     - 重复代码
     - 异常处理
     - 金额 BigDecimal
     → 发现问题: MEDIUM，考虑修复
     → 引用: backend-coding.md / frontend-coding.md, code-smell-governance.md

   L4: 数据库与缓存（按需）:
     - SELECT * / UPDATE/DELETE WHERE 条件
     - Flyway 迁移合规
     - Redis Key TTL / 命名规范
     - MQ 消费者幂等
     → 发现问题: MEDIUM-HIGH
     → 引用: database-governance.md, redis-governance.md, mq-governance.md

   L5: 前端特定检查（按需）:
     - API 调用泛型 request.get<T>()
     - 颜色 token 变量
     - useMemo/useCallback
     - 类型标注
     → 发现问题: MEDIUM
     → 引用: frontend-coding.md

2. security-agent 深度审查（如 L1 发现 CRITICAL 或涉及安全敏感代码）:
   - 认证/鉴权逻辑深度审查
   - 输入安全（参数校验/SQL注入/XSS）
   - 数据安全（敏感数据/越权访问）
   - 支付安全（BigDecimal/幂等/事务/签名验证）
   - 前端安全（dangerouslySetInnerHTML/Token 存储）
   → 引用: evaluation/security-check.md

3. 执行 Evaluation 评估管线（按 evaluation/review-checklist.md）:
   a. output-validator.md — 编译/构建验证（基础门禁）
   b. hallucination-check.md — 幻觉检测（基础门禁）
   c. security-check.md — 安全评分
   d. architecture-score.md — 架构评分
   e. code-quality.md — 代码质量评分
   f. risk-analysis.md — 风险分析
   → 输出综合评估报告（PASS/WARN/FAIL）

4. Memory 写入:
   - 发现反模式 → 追加到 memory/anti-patterns.md
   - 发现已知问题 → 追加到 memory/known-issues.md
   - 涉及安全漏洞 → 追加到 memory/incidents.md
   - 涉及架构决策 → 追加到 memory/decisions.md

5. 输出审查报告:
   - CRITICAL: 必须修复（BLOCK）
   - HIGH: 应该修复
   - MEDIUM: 考虑修复
   - PASS: 通过项
   - 综合评估: PASS/WARN/FAIL
   - 下一步: COMMIT/FIX/REVIEW
```

## 约束

```yaml
必须:
  - 5 层审查逐层执行，L1 不可跳过
  - 发现 CRITICAL 问题必须标记为 BLOCK
  - 审查报告引用具体规则文件和行号
  - 审查后执行 Evaluation 评估管线
  - 审查后执行 Memory 写入
  - L1 发现 CRITICAL 安全问题转交 security-agent
  - 降级时记录跳过的层次和原因

禁止:
  - 直接修改审查的代码
  - 跳过 L1 安全检查层
  - 审查不引用具体规则依据
  - 审查后跳过 Evaluation 评估
  - 审查后跳过 Memory 写入
  - 在审查报告中使用模糊描述（必须给出文件:行号）
```

## 评估

```yaml
按 evaluation/review-checklist.md 综合判定:
  PASS:
    - 所有维度达到各自 PASS 阈值
    - 无 CRITICAL 问题
    - 风险等级 LOW
    → 代码可提交

  WARN:
    - 存在 HIGH/MEDIUM 问题但无 CRITICAL
    - 风险等级 MEDIUM
    → 建议修复后提交，用户确认后可跳过

  FAIL:
    - 存在 CRITICAL 问题
    - 任一评估维度 FAIL
    - 风险等级 HIGH/CRITICAL
    → BLOCK，必须修复
```

## 输出

```yaml
审查报告:
  status: PASS|WARN|FAIL
  summary: <一句话总结>
  evaluation_level: FULL|SIMPLIFIED|MINIMAL
  scores:
    code_quality: { score: N, level: PASS|WARN|FAIL }
    architecture: { score: N, level: PASS|WARN|FAIL }
    hallucination: { score: N, level: PASS|WARN|FAIL }
    security: { score: N, level: PASS|WARN|FAIL }
    risk: { score: N, level: LOW|MEDIUM|HIGH|CRITICAL }
    output: { score: N, level: PASS|WARN|FAIL }

  critical_issues:  # BLOCK 项
    - issue: <描述>
      location: <文件:行号>
      rule: <规则文件名>
      fix: <修复指引>

  high_issues:  # 应该修复
    - issue: <描述>
      location: <文件:行号>
      rule: <规则文件名>
      fix: <修复指引>

  medium_issues:  # 考虑修复
    - issue: <描述>
      location: <文件:行号>
      rule: <规则文件名>
      suggestion: <改进建议>

  pass_items:  # 通过项
    - <检查项名称> ✓

  next_action: COMMIT|FIX|REVIEW
```

## 回滚

```yaml
审查不通过（FAIL）:
  → 返回给 code-executor 修复
  → CRITICAL 问题修复后必须重新走完整 5 层审查
  → HIGH/MEDIUM 问题修复后可只审查受影响的层次

3 轮审查仍不通过:
  → 暂停，记录问题到 memory/known-issues.md
  → 向用户报告，等待决策

审查通过但 WARN:
  → 建议修复，用户确认后可提交
  → 记录豁免理由
```

## Agent

```yaml
调度: reviewer-agent（5 层审查）
前置: code-executor（代码修改完成）
协作:
  - security-agent（L1 发现 CRITICAL 或涉及安全敏感代码时）
  - architect-agent（L2 发现架构层面问题时）
后置: 评估结果写入 memory/
```
