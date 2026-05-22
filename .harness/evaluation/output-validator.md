# 输出验证器

> AI Agent 执行完毕后的即时输出验证。确保输出完整性、编译通过、规则合规。
> 本文件是快速验证入口，综合审查汇总见 review-checklist.md。
> 交叉引用: evaluation/review-checklist.md（综合审查）、rules/tools-permissions.md（修改完成检查清单）

---

## 验证维度

### 1. 文件完整性（权重 20%）

```yaml
检查:
  - [ ] 所有声称创建/修改的文件确实存在
  - [ ] 文件路径与项目结构一致
  - [ ] 文件编码正确（UTF-8 无 BOM）
  - [ ] 文件不为空（> 0 字节）
  - [ ] 无重复文件（同内容不同路径）

验证方式:
  - Glob 验证文件存在
  - Read 抽样验证内容
  - git diff --stat 查看修改范围
```

### 2. 编译/构建验证（权重 30%）

```yaml
后端:
  - [ ] mvn compile -q 通过
  - [ ] 目标模块 mvn test -pl <module> -q 通过
  - [ ] 无新增编译警告（@Deprecated 使用、unchecked 转换等）

前端:
  - [ ] npx tsc --noEmit 通过
  - [ ] npm run build 通过
  - [ ] npm run lint 通过（无新增 ESLint 错误）

失败处理:
  - 编译失败 → 调用 backend-agent 或 frontend-agent 修复
  - 测试失败 → 分析根因，修复代码（非测试）
  - lint 失败 → 修复 lint 错误
  - 3 次失败 → 暂停，向用户报告
```

### 3. 幻觉快速检测（权重 15%）

```yaml
检查:
  - [ ] 引用的类/接口/方法在项目中存在
  - [ ] 引用的配置 key 在 application.yml 中存在
  - [ ] 引用的文件路径在项目中存在
  - [ ] 引用的数据库表名在 Flyway 迁移脚本中存在

验证方式:
  - Grep 搜索引用的类名/方法名
  - Glob 验证文件路径
  - 编译验证: mvn compile -q

详细幻觉检测: evaluation/hallucination-check.md
```

### 4. 规则合规（权重 20%）

```yaml
检查:
  - [ ] 无违背 backend-coding.md / frontend-coding.md
  - [ ] 无违背 architecture-governance.md
  - [ ] 无违背 design-pattern-governance.md
  - [ ] 无违背 domain-modeling.md
  - [ ] 无违背约束层（constraints/）

快速自检:
  - [ ] Controller 无业务逻辑
  - [ ] 无跨模块直接注入 Mapper（cart→product 只读除外）
  - [ ] API 返回 Result<T>，DTO/VO 分离
  - [ ] 金额使用 BigDecimal
  - [ ] SQL 使用 #{} 非 ${}
  - [ ] 无硬编码密钥/密码/Token
  - [ ] 无 switch-case ≥3 分支（业务路由场景）

详细评估:
  - 架构合规: evaluation/architecture-score.md
  - 安全评估: evaluation/security-check.md
  - 代码质量: evaluation/code-quality.md
```

### 5. 业务正确性（权重 15%）

```yaml
检查:
  - [ ] 实现的功能是否与需求一致
  - [ ] 边界情况是否处理（空值、空列表、极值）
  - [ ] 错误场景是否有恰当的错误信息
  - [ ] 是否引入了不必要的复杂度
  - [ ] 支付/金额相关逻辑是否有事务保护
```

---

## 评分

```yaml
总分: 100 分
  ≥ 85: PASS — 输出验证通过
  70-84: WARN — 存在问题但可修复
  < 70: FAIL — 输出不可用
  编译失败: 直接 FAIL

评分说明:
  - 本评分为快速验证评分，不替代子评估文件的独立评分
  - 各子评估文件（code-quality/architecture-score/security-check/hallucination-check/risk-analysis）
    有各自的评分标准和阈值，以子评估文件为准
  - 综合审查汇总见 review-checklist.md
```

## 输出

```yaml
验证报告:
  score: 0-100
  level: PASS|WARN|FAIL
  checks:
    files: { ok: N, fail: N }
    compile: { ok: true|false, details: "..." }
    tests: { ok: true|false, details: "..." }
    lint: { ok: true|false, details: "..." }
    hallucination: { issues: N, details: "..." }
    rules: { violations: [...] }
    business: { issues: [...] }

  next_action:
    PASS → 进入 review-checklist.md 综合审查
    WARN → 修复问题后重新验证
    FAIL → 停止，向用户报告
```

## 与其他评估文件的关系

```yaml
评估流程:
  1. output-validator.md（本文件）— 执行后即时快速验证
  2. 子评估文件（按需执行）:
     - evaluation/code-quality.md — 代码质量详细评估
     - evaluation/architecture-score.md — 架构合规详细评估
     - evaluation/security-check.md — 安全评估
     - evaluation/hallucination-check.md — 幻觉检测详细评估
     - evaluation/risk-analysis.md — 风险分析
  3. evaluation/review-checklist.md — 综合审查汇总

执行策略:
  - 简单任务（≤3 文件）: 仅执行本文件快速验证
  - 中等任务（≤8 文件）: 本文件 + 相关子评估
  - 大型任务（≤20 文件）: 本文件 + 全部子评估 + review-checklist 汇总
```
