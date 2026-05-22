---
name: "refactor-analysis"
description: "分析代码重构的影响范围、风险和收益，输出重构分析报告。作为 refactor.md 工作流的分析子工具使用。"
---

# 重构分析 Skill

对拟重构的代码进行全面影响分析，输出分析报告供 `refactor.md` 工作流使用。
> 本 Skill 是 `workflows/refactor.md` 的**分析阶段子工具**，负责 Step 1-2（目标明确+影响分析），不负责执行/验证/评估。
> 治理规则：`.harness/rules/code-smell-governance.md` `.harness/rules/design-pattern-governance.md` `.harness/rules/architecture-governance.md` `.harness/rules/module-boundary.md` `.harness/rules/ddd-governance.md`

## 与 Workflow 的关系

```yaml
定位: refactor.md 工作流的分析子工具

本 Skill 覆盖:
  - refactor.md Step 1（明确重构目标）→ 本 Skill Step 1-2
  - refactor.md Step 2（影响分析）→ 本 Skill Step 3
  - refactor.md Step 3（制定重构方案）→ 本 Skill Step 5

本 Skill 不覆盖（由 refactor.md 负责）:
  - Step 4: 执行重构
  - Step 5: 验证（编译+测试）
  - Step 6: 自检
  - Step 7: Evaluation 评估
  - Step 8: Memory 写入

联动 Skill:
  - 上游: skills/code-verify/SKILL.md — 先检测坏味道，再进入本 Skill 分析重构方案
  - 下游: workflows/refactor.md — 本 Skill 输出分析报告后，由 Workflow 接管执行
  - 关联: skills/ddd-design/SKILL.md — 贫血→充血模型重构时参考领域设计
```

## 触发条件

- 用户要求重构某个模块/类/方法
- 发现需要消除的技术债务
- 为微服务拆分评估改造范围
- Code Review 发现坏味道需要重构（参考 code-smell-governance.md）
- DDD 改造（贫血模型→充血模型）
- 新增功能时发现现有结构不支持扩展
- 性能优化导致结构调整

## 依赖上下文

### 治理规则

- `.harness/rules/code-smell-governance.md` — 坏味道识别与重构推荐
- `.harness/rules/design-pattern-governance.md` — 设计模式强制使用条件
- `.harness/rules/architecture-governance.md` — 分层架构约束
- `.harness/rules/module-boundary.md` — 模块依赖白名单
- `.harness/rules/ddd-governance.md` — 领域边界规则
- `.harness/rules/domain-modeling.md` — 充血模型规则（贫血→充血重构时必读）

### Memory（强制读取）

- `.harness/memory/decisions.md` — 是否有相关架构决策（避免提出已否决方案）
- `.harness/memory/anti-patterns.md` — 是否有已知反模式（避免重复踩坑）
- `.harness/memory/architecture-history.md` — 架构演进记录

### 被重构代码

```yaml
调用方分析方式:
  - 直接调用: Grep 搜索类名/方法名（静态引用）
  - Spring DI: 搜索 @Autowired / 构造器注入引用
  - MQ 消费: 搜索 topic 名称引用
  - 定时任务: 搜索 @Scheduled 注解方法
  - 配置引用: 搜索 @Value / @ConfigurationProperties 引用

分析深度:
  - 必须: 直接调用方（1 跳）
  - 推荐: 传递调用方（2 跳，当风险等级 ≥ 高时）
  - 可选: 运行时依赖（反射、Spring Bean 名称字符串等）
```

## 分析流程

### Step 1: Memory 检查（强制）

```yaml
读取:
  - memory/decisions.md — 是否有相关架构决策？
    → 如有已否决方案: 在报告中标注"此方案曾被否决，原因: {reason}"
  - memory/anti-patterns.md — 是否有已知反模式？
    → 如有相关条目: 在报告中标注"此区域存在已知反模式: {pattern}"
  - memory/architecture-history.md — 是否有架构演进记录？
    → 如有相关记录: 确认重构方向与架构演进方向一致

原则: 不重复已否决的方案，不踩已知的坑
```

### Step 2: 重构范围界定

```yaml
明确:
  - 重构目标: 要解决什么问题？（参考 code-smell-governance.md 的坏味道分类）
  - 重构范围: 涉及哪些类/方法/文件？
  - 不改变什么: 对外 API / 数据库结构 / 接口签名？
  - 重构类型: 提取方法 / 重命名 / 移动代码 / 设计模式引入 / 贫血→充血 / 模块拆分
```

### Step 3: 影响分析

```yaml
代码引用分析:
  - 被重构的类/方法被哪些代码调用？（Grep 搜索调用方）
  - DTO/Entity 字段被哪些接口使用？
  - Mapper 方法被哪些 Service 使用？
  - MQ 消息体被哪些消费者使用？

模块影响分析:
  - 重构是否跨越模块边界？（参考 module-boundary.md 依赖白名单）
  - 是否影响模块间依赖关系？
  - 是否引入循环依赖？

数据库影响分析:
  - 是否涉及表结构变更？
  - 是否影响现有数据？
  - 是否需要 Flyway 迁移？

Redis 影响分析:
  - 是否涉及 Redis key 结构变更？
  - 是否影响缓存命中率？
  - 是否需要缓存预热或清理？

Enum/常量影响分析:
  - 是否修改枚举值？（影响数据库已存储的整数值）
  - 是否修改常量定义？（影响所有引用处）

定时任务影响分析:
  - 是否影响 @Scheduled 定时任务的触发条件或处理逻辑？
  - 重构期间定时任务是否需要暂停？

前端影响分析:
  - API 返回格式（Result<T> 的 code/message/data）是否变化？
  - DTO 字段增删是否影响前端？
  - 是否需要前后端协同发布？

性能影响分析:
  - 是否可能引入 N+1 查询？
  - 是否将批量操作变为循环单条？
  - 数据库索引是否需要调整？

测试覆盖度评估:
  - 被重构代码的现有测试覆盖情况？
  - 无测试覆盖 → 风险升级一级
  - 部分覆盖 → 标注缺失场景
```

### Step 4: 风险评级

#### 单因素评级

| 级别 | 条件 | 受影响文件数 | 示例 |
|---|---|---|---|
| 低 | 单个类/方法内部重构，无外部引用 | 1-3 | 提取 private 方法、重命名局部变量 |
| 中 | 修改多个同级类，调用方在同一模块 | 4-10 | 重构 ServiceImpl、新增 VO |
| 高 | 修改公开 API/DTO/Entity/Redis key | 4-10 | 改 Controller 签名、改 DTO 字段、改缓存结构 |
| 极高 | 修改数据模型/跨模块接口/MQ 消息体/Enum 值 | 10+ | 改 Entity 字段、改 MQ 消息体、改枚举值 |

#### 组合升级规则

```yaml
风险升级:
  - 2 个"中"风险叠加 → 整体升级为"高"
  - 1 个"高" + 1 个"中" → 整体升级为"极高"
  - 跨模块 + 任何"高"以上 → 整体升级为"极高"
  - 无测试覆盖 + 任何风险 → 风险升级一级

风险降级（需满足全部条件）:
  - 有完整测试覆盖
  - 影响范围限于单模块
  - 不涉及数据库/Redis/MQ 变更
  → 可降一级
```

### Step 5: 方案输出

输出分析报告，供 `refactor.md` 工作流 Step 3（制定重构方案）和 Step 4（执行重构）使用。

```markdown
## 重构分析报告

### 目标
{一句话描述重构目标，关联 code-smell-governance.md 的坏味道分类}

### Memory 检查结果
- 架构决策: {无冲突 / 已否决方案: {reason}}
- 已知反模式: {无 / {pattern}}
- 架构演进方向: {一致 / 需注意: {reason}}

### 重构范围
- 重构类型: {提取方法/重命名/移动/设计模式/贫血→充血/模块拆分}
- 涉及文件: {列表}
- 不改变: {对外 API / 数据库结构 / 接口签名}

### 影响范围
| 维度 | 模块 | 文件 | 影响描述 | 影响程度 |
|---|---|---|---|---|
| 代码引用 | | | | 低/中/高 |
| 模块边界 | | | | 低/中/高 |
| 数据库 | | | | 低/中/高 |
| Redis | | | | 低/中/高 |
| Enum/常量 | | | | 低/中/高 |
| 定时任务 | | | | 低/中/高 |
| 前端 | | | | 低/中/高 |
| 性能 | | | | 低/中/高 |

### 测试覆盖度
- 现有测试: {有/无/部分}
- 缺失场景: {列表}

### 风险等级
{低/中/高/极高}
- 风险因素: {列出各维度风险}
- 组合升级: {是否升级及原因}

### 建议执行步骤
1. {步骤1} — 验证方式
2. {步骤2} — 验证方式
（每步独立 git commit，可单独 revert）

### 回滚方案
- 每步回滚: git checkout <step_files>
- 全部回滚: git reset --hard <重构前 commit>
- 数据库回滚: {Flyway 回滚脚本 / 无需回滚}
- Redis 回滚: {清理新增 key / 恢复旧结构 / 无需回滚}

### 检查清单
- [ ] 不影响对外 API 行为（HTTP 状态码、Result<T> 的 code/message/data 结构不变）
- [ ] 不破坏模块边界（参考 module-boundary.md 依赖白名单）
- [ ] 不引入循环依赖
- [ ] DTO/VO/Entity 分离未被破坏
- [ ] 不改变数据库存储格式（如需变更，需 Flyway 迁移）
- [ ] 不改变 MQ 消息体格式（如需变更，需版本兼容策略）
- [ ] 不改变 Redis key 结构（如需变更，需双写过渡）
- [ ] 不改变 Enum 整数值（如需变更，需数据迁移）
```

## 约束

- 不为了重构而重构（必须有明确目标，关联 code-smell-governance.md 的坏味道）
- 重构不改变可观测行为，具体指：
  - 对外 API 的 HTTP 状态码不变
  - `Result<T>` 的 code/message/data 结构不变
  - 数据库中已存储的数据格式不变
  - MQ 消息体的消费者能正常处理
  - 错误消息文案变化可接受，但错误码不变
- 架构级重构必须先出方案再执行（本 Skill 输出 → refactor.md 执行）
- 不混合重构和新功能（重构提交中不包含新功能代码）
- 跨模块重构需在报告中明确标注，并确认 module-boundary.md 允许
- 涉及数据库/Redis/MQ 变更的重构，必须包含兼容性过渡方案
