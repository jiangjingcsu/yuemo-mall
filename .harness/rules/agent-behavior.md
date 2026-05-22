# AI Agent 行为治理规则

> 约束 AI Agent 在 yuemo-mall 项目中的行为边界、工作方式和协作模式。确保 AI 安全、可控、可预测地执行任务。
>
> **层级定位**：本文件是 Rules 层文档，定义 AI Agent 的行为规范（软规范，违反 = 应修复）。
> 与 Constraints 层的关系：Constraints（constraints/ai-boundaries.md）定义硬红线（违反 = 停止执行），本文件定义行为规范。
> 本文件中的条目如涉及硬红线，统一引用 constraints/ai-boundaries.md，不重复定义。

---

## 1. 核心原则

```yaml
最小修改原则:
  - 只修改与任务直接相关的代码
  - 不顺手重构无关代码
  - 不添加任务不需要的抽象
  - 不引入任务不需要的依赖
  - 三个相似行不等同于需要抽象

先分析后修改:
  - 修改前必须阅读相关代码上下文
  - 修改前必须理解模块结构和依赖关系
  - 修改前必须评估影响范围
  - 不确定时先输出分析，等用户确认再修改

安全第一:
  - 涉及数据库结构变更 → 必须确认
  - 涉及 MQ/RPC 接口变更 → 必须确认
  - 涉及配置文件变更 → 必须确认
  - 涉及安全相关代码 → 必须确认

评估优先:
  - 代码修改完成后必须通过评估管线
  - 评估未通过则自动修复（最多 3 次）
  - 不通过评估不标记任务完成

记忆防漂移:
  - 任务开始前读取 .harness/memory/ 相关文件
  - 避免重复犯已知错误
  - 遵循已记录的架构决策

工作流先行:
  - 复杂任务先匹配 .harness/workflows/ 中的工作流
  - 按工作流步骤执行，不跳步
  - 工作流未覆盖时按本文件规范执行
```

---

## 2. 禁止行为

```yaml
禁止:
  # 范围控制
  - 单次任务修改超过 20 个文件
  - 未理解上下文直接修改代码
  - 未阅读模块结构直接生成代码
  - 未分析影响范围直接修改 DTO/Entity

  # 依赖管理
  - 未确认需求就新增依赖（pom.xml/package.json）
  - 未检查是否有现有依赖可复用就引入新库

  # 代码质量
  - 生成无用的注释（"// 新增一个字段"）
  - 添加未使用的 import
  - 保留被注释掉的旧代码
  - 添加任务范围外的"小优化"
  - 返回 Entity 给前端（必须返回 VO）
  - 使用 @Autowired 字段注入（必须构造器注入）
  - DTO/VO 使用普通类（必须用 record）
  - 物理删除数据（必须逻辑删除）
  - @Transactional 不指定 rollbackFor

  # 架构破坏
  - 在 Controller 写业务逻辑
  - 跨模块直接调用 Mapper（cart→product 只读除外）
  - 跨模块直接操作数据库表
  - 往其他模块的数据库表写数据
  - Controller 跨模块调用 Service
  - 引入循环依赖

  # 安全硬红线 → 定义: constraints/ai-boundaries.md §绝对不可以做的事
  分类导航（仅作索引，完整条目见上述定义）:
    - 生产操作红线（DROP/TRUNCATE/无WHERE/Redis FLUSHALL/FLUSHDB/KEYS *等）
    - 代码变更红线（修改主版本号/Flyway已执行脚本/major version等）
    - 架构变更红线（改变通信方式/创建顶级模块/改表结构非Flyway等）
    - 配置变更红线（JWT密钥/加密算法/安全机制/密码强度等）
    - 支付安全红线（余额扣减无事务/回调无签名验证/浮点数等）
    - 部署安全红线（跳过测试/停所有副本/未CI验证镜像等）
  执行前自检: → constraints/ai-boundaries.md §边界检查机制

  # 沟通
  - 对不确定的需求自行假设
  - 跳过用户确认执行破坏性操作
```

---

## 3. 必须行为

```yaml
必须:
  # 上下文理解
  - 任务开始前阅读 CLAUDE.md 和 constraints/ai-boundaries.md
  - 任务开始前阅读 .harness/memory/ 相关文件（避免重复踩坑）
  - 修改模块前阅读 .harness/rules/module-boundary.md
  - 涉及数据库时阅读 .harness/rules/database-governance.md
  - 涉及 Redis 时阅读 .harness/rules/redis-governance.md
  - 涉及 MQ 时阅读 .harness/rules/mq-governance.md
  - 后端编码遵循 .harness/rules/backend-coding.md
  - 前端编码遵循 .harness/rules/frontend-coding.md

  # 代码修改
  - 使用 Edit 工具进行精确修改（而非重写整个文件）
  - 修改后验证编译通过
  - 修改后检查是否有相关测试，若有则运行测试
  - 遵循项目已有的代码风格和模式

  # 验证闭环
  - 后端修改 → mvn compile -pl <module>
  - 前端修改 → npx tsc --noEmit
  - 最多 3 次修复尝试，仍失败则报告用户
  - 不通过验证不标记任务完成
```

---

## 4. 任务规模控制

```yaml
单次任务规模:
  - 简单修复: ≤ 3 文件
  - 中等功能: ≤ 8 文件
  - 大型重构: ≤ 20 文件（需用户确认后执行）

文件统计口径:
  - 包含: .java / .ts / .tsx / .xml / .sql / .yml 等源码和配置文件
  - 不包含: .harness/ 下的治理文档（治理文档修改不计入规模）
  - 测试文件计入规模

超过 20 文件的任务:
  - 必须拆分为多个子任务
  - 每个子任务独立验证
  - 分批向用户报告进度
  - 拆分策略: 按模块边界拆分，不跨模块合并子任务
```

---

## 5. 上下文加载策略

```yaml
按需加载:
  1. 先读 CLAUDE.md（入口）
  2. 根据任务类型加载对应 .harness/rules/
  3. 读取 .harness/memory/ 相关文件（避免重复踩坑）
  4. 根据任务类型加载对应 .harness/docs/
  5. 只读取与任务直接相关的源码文件
  6. 大文件(>500行)只读相关段落
  7. 执行前漂移检测: skills/drift-detection/SKILL.md（详见 execution-engine.md §2）

约束:
  - 单次上下文加载不超过 80K tokens
  - 详细路由策略见 runtime/context-router.md
  - Token 预算不足时按优先级裁剪（constraints > rules > docs > source）

禁止:
  - 一次性加载所有规则和文档（浪费上下文）
  - 通读整个源码文件（除非任务需要）
  - 不读上下文就开始改代码
```

---

## 6. 错误处理

```yaml
遇到编译/运行错误时:
  1. 分析错误根因（不只看表面错误信息）
  2. 搜索项目中相关代码和配置
  3. 修复根因而非症状
  4. 验证修复后重新运行
  5. 3 次无法修复 → 向用户报告：
     - 错误信息
     - 已尝试的修复
     - 建议的下一步

禁止:
  - 忽略错误直接提交
  - 绕过错误（如注释掉失败代码）
  - 不分析根因就盲目尝试修复
```

---

## 7. 沟通规范

```yaml
何时主动沟通:
  - 发现需求不明确或存在歧义
  - 修改方案有多种选择需要决策
  - 修改会影响其他模块或功能
  - 需要修改数据库结构
  - 需要修改 API 接口签名
  - 操作可能影响生产环境
  - 涉及 constraints/ai-boundaries.md §需要用户确认才能做的事 中的任何操作

如何沟通:
  - 简洁描述问题和选项
  - 提供推荐方案和理由
  - 列出各方案的优缺点
  - 等待用户确认后再执行
```

---

## 8. 代码自检清单

AI Agent 完成修改后必须自检：

```yaml
自检:
  引用规则（详细自检项见各领域规则文件）:
    后端编码: → rules/backend-coding.md §24 禁止事项清单
    数据库操作: → rules/database-governance.md §2 SQL编写规范
    Redis操作: → rules/redis-governance.md §5-§6
    MQ操作: → rules/mq-governance.md §4-§6
    安全: → constraints/ai-boundaries.md §绝对不可以做的事

  跨领域必检项（不在单一规则文件中，需在此列出）:
    - [ ] DTO/VO 是否与 Entity 分离
    - [ ] 返回是否为 Result<T> 包装
    - [ ] 异常是否为 BusinessException
    - [ ] 金额是否为 BigDecimal
    - [ ] 没有硬编码的密钥/密码
    - [ ] 敏感数据是否脱敏（手机号/身份证）
```
