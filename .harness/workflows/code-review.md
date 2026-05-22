# Code Review 工作流

> AI Agent 执行代码审查时的标准工作流。确保审查系统化、全面、可操作。

---

## 触发条件

- 代码修改完成后
- 用户要求审查代码
- PR 提交前

---

## 审查层次

### 第零层：上下文漂移检测（最先执行）

```yaml
目的: 确保 .harness/ 文档与代码实际状态一致，避免基于过时信息审查
技能: skills/drift-detection/SKILL.md

检测项（结构性，全部执行）:
  - [ ] Maven 模块结构是否与 module-boundary.md 一致？
  - [ ] 数据库表（@TableName）是否与 module-boundary.md 数据所有权一致？
  - [ ] Entity 类是否与 ddd-governance.md 聚合根表一致？
  - [ ] MQ Topic 是否与 ddd-governance.md 事件清单一致？
  - [ ] 跨模块依赖是否与 module-boundary.md 白名单一致？
  - [ ] API 安全白名单是否与 api-governance.md 一致？
  - [ ] 错误码是否与 api-governance.md 错误码表一致？

检测项（语义性，涉及变更的模块执行）:
  - [ ] Entity 领域方法是否与 ddd-governance.md 成熟度评估一致？
  - [ ] 业务流程变更是否同步到对应文档？

结果映射:
  L3 漂移 → CRITICAL（必须修复文档后再审查）
  L2 漂移 → HIGH（需确认，纳入审查报告）
  L1 漂移 → MEDIUM（建议更新，不阻断）
```

### 第一层：安全检查（最高优先级）

```yaml
检查项:
  - [ ] 是否有硬编码的密码/密钥/Token？
  - [ ] SQL 是否使用 #{} 参数化？（禁止 ${} 拼接输入）
  - [ ] 用户输入是否做了校验？
  - [ ] API 是否在白名单/鉴权范围内？
  - [ ] 是否暴露了敏感数据（password、完整手机号）？
  - [ ] 是否引入了 XSS/SQL注入/CSRF 风险？

结果: 发现问题 → CRITICAL，必须修复
```

### 第二层：架构检查

```yaml
检查项:
  - [ ] Controller 是否只做协议转换？（无业务逻辑）
  - [ ] ServiceImpl 是否只调本模块 Mapper？
  - [ ] 是否跨模块直接操作数据库表？
  - [ ] DTO/VO/Entity 是否分离？
  - [ ] 是否引入了循环依赖？
  - [ ] 模块依赖是否在白名单内？
  - [ ] 返回类型是否为 Result<T>？

结果: 发现问题 → HIGH，应修复
参考: .harness/rules/architecture-governance.md, module-boundary.md
```

### 第三层：代码质量

```yaml
检查项:
  - [ ] 方法是否过长（> 50 行）？
  - [ ] 类是否过大（> 800 行）？
  - [ ] 嵌套是否过深（> 4 层）？
  - [ ] 命名是否清晰、一致？
  - [ ] 是否有重复代码？
  - [ ] 是否有未使用的 import？
  - [ ] 是否有注释掉的旧代码？
  - [ ] 异常是否正确处理（不吞异常、不忽略）？
  - [ ] 金额是否使用 BigDecimal？
  - [ ] 分页查询是否有 LIMIT？

结果: 发现问题 → MEDIUM，考虑修复
参考: .harness/rules/backend-coding.md / frontend-coding.md
```

### 第四层：数据库与缓存

```yaml
检查项:
  - [ ] SQL 是否使用 SELECT *？
  - [ ] UPDATE/DELETE 是否有 WHERE 条件？
  - [ ] 表新增字段是否通过 Flyway 迁移？
  - [ ] 新增 Redis Key 是否设置了 TTL？
  - [ ] Redis Key 命名是否符合规范？
  - [ ] MQ 消费者是否幂等？
  - [ ] 索引是否合理？

结果: 发现问题 → MEDIUM-HIGH
参考: .harness/rules/database-governance.md, redis-governance.md, mq-governance.md
```

### 第五层：前端特定检查

```yaml
检查项:
  - [ ] API 调用是否使用泛型 request.get<T>()？
  - [ ] 颜色是否使用 token 变量而非硬编码色值？
  - [ ] 大数据列表是否使用 useMemo？
  - [ ] 事件处理函数是否使用 useCallback？
  - [ ] 是否有类型标注？
  - [ ] 是否使用了语义化 HTML？
  - [ ] React Key 是否正确设置？

结果: 发现问题 → MEDIUM
参考: .harness/rules/frontend-coding.md
```

---

## 审查报告格式

```markdown
## Code Review Report

### CRITICAL（必须修复）
- 描述 + 文件:行号 + 修复建议

### HIGH（应该修复）
- 描述 + 文件:行号 + 修复建议

### MEDIUM（考虑修复）
- 描述 + 文件:行号 + 修复建议

### PASS（通过项）
- 安全检查全部通过 ✓
- 架构规则符合 ✓
```

---

## 审查后处理

```yaml
CRITICAL 问题: 立即修复，修复后重新审查
HIGH 问题: 优先修复，个别可说明豁免理由
MEDIUM 问题: 酌情修复，不阻塞 merge
PASS: 审查通过
```

### 审查后 Evaluation 评估（强制）

```yaml
必须按 evaluation/review-checklist.md 执行综合评估管线:
  重点维度:
  - security-check.md — 安全评分
  - architecture-score.md — 架构评分
  - code-quality.md — 代码质量评分
  - hallucination-check.md — 幻觉检测

输出: 综合审查报告（PASS|WARN|FAIL）

评估不通过:
  - FAIL → 返回修复 → 重新审查
  - 3 轮仍不通过 → 暂停，记录问题到 memory/known-issues.md
```

### 审查后 Memory 写入

```yaml
审查完成后必须写入 memory/:
  - 如发现反模式 → 追加到 memory/anti-patterns.md
  - 如发现已知问题 → 追加到 memory/known-issues.md
  - 如涉及安全漏洞 → 追加到 memory/incidents.md

写入规则:
  - 只追加，不修改已有条目
  - 包含时间戳和上下文
  - 按 state-manager.md 规范执行
```

## 约束验证

> 约束检查由 workflow-executor（`.harness/executors/workflow-executor.md`）统一执行。
> 审查时对照各 rule 文件逐项打分，CRITICAL 问题直接 BLOCK。

## 评估

> 评估管线见上方「审查后 Evaluation 评估」段。本工作流不再重复定义。

## 回滚

```yaml
审查不通过:
  - CRITICAL → 返回修复 → 重新审查
  - HIGH → 建议修复 → 可豁免 → 重新审查
  - 3 轮仍不通过 → 暂停，记录问题到 known-issues.md
```

---

## 自动化审查集成

```yaml
推荐: 每次代码修改后自动触发审查

手动审查: 用户要求时执行
自动审查: 以下情况使用 code-reviewer agent
  - 修改超过 3 个文件
  - 涉及安全敏感代码
  - 涉及架构变更
```
