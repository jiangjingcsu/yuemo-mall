# 综合审查清单

> 所有 Executor 执行完毕后，汇总各维度评估结果的综合审查清单。
> 交叉引用: 各子评估文件（code-quality.md / architecture-score.md / hallucination-check.md / security-check.md / risk-analysis.md / output-validator.md）
> 约束依据: constraints/（所有红线）

---

## 评估执行流程

```yaml
1. 按变更范围确定评估级别:
   完整评估（6 维度全执行）:
     - 跨模块变更
     - 支付/订单/用户模块变更
     - 新增 API 接口
     - 数据库迁移
     - 架构变更

   简化评估（安全 + 输出验证 + 风险分析）:
     - 单模块内 ≤ 3 文件修改
     - 不涉及支付/订单/用户模块
     - 不涉及数据库迁移
     - 不涉及 API 变更

   最小评估（仅输出验证）:
     - 纯前端样式/文案修改
     - 注释修改
     - 配置文件修改（非生产）

2. 执行子评估（按顺序）:
   a. output-validator.md — 编译/构建/测试验证（基础门禁，不通过则终止）
   b. hallucination-check.md — 幻觉检测（基础门禁，不通过则终止）
   c. security-check.md — 安全评估
   d. architecture-score.md — 架构合规
   e. code-quality.md — 代码质量
   f. risk-analysis.md — 风险分析

3. 汇总到本清单，输出综合审查报告
```

---

## 审查汇总

### 评估结果汇总表

```yaml
维度汇总:
  代码质量（code-quality.md）:       ___ / 100  [PASS|WARN|FAIL]
  架构合规（architecture-score.md）:  ___ / 100  [PASS|WARN|FAIL]
  幻觉检测（hallucination-check.md）: ___ / 100  [PASS|WARN|FAIL]
  安全评估（security-check.md）:      ___ / 100  [PASS|WARN|FAIL]
  风险分析（risk-analysis.md）:       ___ / 100  [LOW|MEDIUM|HIGH|CRITICAL]
  输出验证（output-validator.md）:    ___ / 100  [PASS|WARN|FAIL]

综合判定: [PASS|WARN|FAIL]
```

### 综合判定规则

```yaml
PASS（通过，可提交）:
  - 所有维度达到各自 PASS 阈值
  - 无 FAIL 维度
  - 无 CRITICAL 级别问题
  - 风险等级 LOW

  各维度 PASS 阈值（以子评估文件为准）:
    代码质量: ≥ 85
    架构合规: ≥ 85
    幻觉检测: ≥ 90
    安全评估: ≥ 85
    输出验证: ≥ 85
    风险分析: LOW

WARN（告警，建议修复后提交）:
  - 存在 WARN 维度但无 FAIL
  - 存在 HIGH 级别问题但有豁免理由
  - 风险等级 MEDIUM

FAIL（不通过，BLOCK）:
  - 任一维度 FAIL
  - 存在 CRITICAL 级别问题
  - 存在 BLOCK 项
  - 编译/测试失败
  - 风险等级 HIGH 或 CRITICAL
```

### 评估结果流转

```yaml
PASS:
  → 允许提交
  → 写入 memory（如有反模式/已知问题）

WARN:
  → 建议修复后重新评估
  → 修复后重新执行受影响的子评估
  → 用户明确确认后可跳过修复直接提交（需记录豁免理由）

FAIL:
  → BLOCK，必须修复
  → 修复后重新执行完整评估
  → 3 轮仍 FAIL → 暂停，记录到 memory/known-issues.md，向用户报告
```

---

## 快速检查清单

### 代码级

```yaml
结构与大小:
  - [ ] 方法 ≤ 50 行（Service 层 ≤ 80 行），文件 ≤ 800 行
  - [ ] 参数 ≤ 5 个（超过用 DTO 封装）
  - [ ] 嵌套深度 ≤ 4 层

命名:
  - [ ] 类名 PascalCase，方法名 camelCase + 动词
  - [ ] 变量名无缩写（userId 非 uid），无拼音命名

设计模式:
  - [ ] 无 switch-case ≥ 3 分支（业务路由场景，用策略模式）
  - [ ] 无 if-else 链 ≥ 3 条件（策略选择场景）

领域模型:
  - [ ] Entity 包含领域行为方法（非纯 @Data 贫血模型）
  - [ ] Controller 无业务逻辑（只做协议转换 + 调用 Service）

安全:
  - [ ] 无硬编码密钥/密码/Token
  - [ ] SQL 使用 #{param} 而非 ${param}
  - [ ] 金额使用 BigDecimal（禁止 float/double）

错误处理:
  - [ ] 异常不吞噬（catch 后至少记录日志）
  - [ ] 用户可见错误使用 BusinessException + ResultCode

重复与冗余:
  - [ ] 无复制粘贴代码块（>5 行相同）
  - [ ] 无注释掉的代码块
  - [ ] 无未使用的 import
```

### 架构级

```yaml
分层:
  - [ ] Controller 只做协议转换，返回 Result<T>
  - [ ] Service 不直接操作其他模块 Mapper
  - [ ] Entity 不调用 Service/Mapper

模块边界:
  - [ ] 无跨模块直接注入 Mapper
  - [ ] 无循环依赖
  - [ ] 跨模块调用通过 Service API 或 MQ

数据架构:
  - [ ] 缓存 Key 符合项目实际格式（{module}:{biz}:{id}，如 cart:{userId}、payment:callback:{paymentNo}）
  - [ ] MQ 消息有幂等处理（mq:consumed:{topic}:{bizId}）
  - [ ] 事务边界正确（@Transactional 在 Service 方法上）

API 设计:
  - [ ] DTO 和 VO 分离（不暴露 Entity 到 API）
  - [ ] URL 使用 RESTful 风格（复数名词，kebab-case）
  - [ ] 参数校验使用 @Valid
  - [ ] 错误码在正确分段（1xxx 用户/2xxx 商品/3xxx 订单/4xxx 支付/5xxx 购物车/6xxx 促销）

支付专项:
  - [ ] 支付回调有签名验证 + 幂等（Redis setIfAbsent + DB 状态校验）
  - [ ] 余额扣减在事务内 + SQL CAS 并发保护
  - [ ] 支付状态变更通过 Entity 领域方法（markSuccess/markRefunded）
```

### 流程级

```yaml
构建验证:
  - [ ] 后端 mvn compile -q 通过
  - [ ] 前端 npx tsc --noEmit 通过
  - [ ] 前端 npm run build 通过

测试验证:
  - [ ] 后端 mvn test -pl <module> 通过
  - [ ] 新增代码有对应测试

安全验证:
  - [ ] 无新增安全漏洞
  - [ ] 不违反 constraints/ 红线

质量验证:
  - [ ] 无 AI 幻觉（虚构的 API/配置/路径/类名/方法名）
  - [ ] 无新增 ESLint 错误

风险验证:
  - [ ] 风险可控（有回滚方案）
  - [ ] 涉及 Flyway 迁移脚本 → 确认不可回滚风险
  - [ ] 涉及 MQ 消息格式变更 → 确认消费端兼容
```

---

## 输出格式

```yaml
综合审查报告:
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

  critical_issues:  # BLOCK 项，必须修复
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
