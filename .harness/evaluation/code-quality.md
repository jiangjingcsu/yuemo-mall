# 代码质量评估

> AI 生成/修改代码后的质量评估清单。
> 交叉引用: `rules/code-smell-governance.md`（坏味道识别）、`rules/backend-coding.md`（后端编码规范）、`rules/frontend-coding.md`（前端编码规范）

---

## 评估维度

### 1. 方法与函数（权重 25%）

```yaml
后端检查:
  - [ ] 方法 ≤ 50 行（与 code-smell-governance.md §1.5 一致）
  - [ ] 参数 ≤ 5 个（超过用 DTO 封装）
  - [ ] 圈复杂度 ≤ 10
  - [ ] 嵌套深度 ≤ 4 层
  - [ ] 单一职责：一个方法只做一件事
  - [ ] 无 switch-case ≥ 3 分支（见 code-smell-governance.md §1.1，应用策略模式）
  - [ ] 无 if-else 链 ≥ 3 条件（见 code-smell-governance.md §1.2，应用策略模式或提前返回）

前端检查:
  - [ ] 组件/函数 ≤ 50 行（复杂组件拆分为子组件）
  - [ ] Hook 函数 ≤ 50 行
  - [ ] 无 any 类型（用 unknown + 类型守卫）
  - [ ] 无 enum（用 as const 对象或联合字面量类型）

扣分:
  - 方法 > 50 行: -5 分/处
  - 方法 > 100 行: -10 分/处
  - switch-case ≥ 3 分支: -10 分/处
  - if-else 链 ≥ 3 条件: -10 分/处
  - 参数 > 7 个: -5 分
  - 嵌套 > 5 层: -10 分
  - 前端使用 any: -3 分/处
```

### 2. 类与文件（权重 20%）

```yaml
后端检查:
  - [ ] 文件 ≤ 800 行
  - [ ] 类 ≤ 500 行（DTO/VO 除外）
  - [ ] 单一职责：一个类一个变更理由
  - [ ] 高内聚：方法使用类的所有字段
  - [ ] 低耦合：依赖接口而非实现
  - [ ] God Service 判定（见 code-smell-governance.md §1.3）:
      > 500 行 + ≥ 3 种不相关业务 + 注入 ≥ 5 Mapper + 方法 > 50 行

前端检查:
  - [ ] 组件文件 ≤ 300 行（超过拆分为子组件）
  - [ ] 单一职责：一个组件一个职责
  - [ ] API 函数在 src/api/ 目录，类型与 API 同文件
  - [ ] Store 按模块拆分 Slice

扣分:
  - 文件 > 800 行: -15 分（强制拆分）
  - God Service: -20 分
  - 循环依赖: -25 分（BLOCK）
  - 前端组件 > 300 行: -10 分
```

### 3. 命名（权重 15%）

```yaml
后端检查:
  - [ ] 类名清晰表达职责（PascalCase）
  - [ ] 方法名清晰表达行为（camelCase + 动词）
  - [ ] 变量名无缩写（userId 非 uid）
  - [ ] 布尔变量 is/has/should/can 前缀
  - [ ] 常量 UPPER_SNAKE_CASE
  - [ ] 无魔法值（见 code-smell-governance.md §1.8，用 enum/常量替换）

前端检查:
  - [ ] 组件文件 PascalCase.tsx
  - [ ] 非组件文件 camelCase.ts
  - [ ] 自定义 Hook use + PascalCase
  - [ ] API 函数对象 xxxApi
  - [ ] Store Slice xxxSlice

扣分:
  - 单字母变量（循环变量除外）: -3 分/个
  - 拼音命名: -5 分/个
  - 误导性命名: -10 分/个
  - 魔法值（字面量数字/字符串）: -3 分/处
```

### 4. 重复代码（权重 15%）

```yaml
后端检查:
  - [ ] 无复制粘贴代码块（>5 行相同）
  - [ ] 公共逻辑已提取
  - [ ] 常量已提取（非硬编码魔法值）
  - [ ] 无重复 SQL（见 code-smell-governance.md §1.7，≥3 处相同结构提取到 Mapper）

前端检查:
  - [ ] 无复制粘贴代码块（>5 行相同）
  - [ ] 公共 UI 逻辑提取为自定义 Hook
  - [ ] 公共 UI 片段提取为组件

扣分:
  - 重复代码块: -10 分/处
  - 魔术数字（非 0/1/-1）: -3 分/处
  - 重复 SQL: -10 分/处
```

### 5. 错误处理（权重 15%）

```yaml
后端检查:
  - [ ] 异常不吞噬（catch 后至少记录日志）
  - [ ] 业务异常使用 BusinessException + ResultCode
  - [ ] 全局异常由 GlobalExceptionHandler 统一处理
  - [ ] 资源正确关闭（try-with-resources）
  - [ ] 无泛化 catch (Exception)（除非有日志记录 + 合理处理）
  - [ ] 错误信息包含足够上下文

前端检查:
  - [ ] 无空 catch 块吞掉错误
  - [ ] API 调用有错误处理（request.ts 拦截器已处理通用错误）
  - [ ] ErrorBoundary 包裹关键模块
  - [ ] 列表为空展示 Empty，加载中展示 Spin

扣分:
  - 吞噬异常（空 catch）: -15 分/处
  - 捕获 Exception 而不处理: -5 分/处
  - 前端空 catch: -10 分/处
```

### 6. 注释与日志（权重 10%）

```yaml
后端检查:
  - [ ] 不添加不必要的注释（代码自解释）
  - [ ] 注释解释 WHY 而非 WHAT
  - [ ] 无注释掉的代码块
  - [ ] 无 TODO/FIXME 遗留（除非有对应 issue）
  - [ ] 日志不输出敏感信息（密码/Token/手机号）
  - [ ] 关键操作有日志记录（MQ 消费/支付/库存变更）

前端检查:
  - [ ] 无 console.log 遗留（调试用除外）
  - [ ] 无注释掉的代码块
  - [ ] 无 TODO/FIXME 遗留

扣分:
  - 注释掉的代码: -5 分/处
  - 冗余注释（与代码完全一致）: -3 分/处
  - 日志输出敏感信息: -15 分/处
  - 前端遗留 console.log: -3 分/处
```

---

## 评分

```yaml
总分: 100 分
  ≥ 85: PASS
  70-84: WARN（可合并，建议改进）
  < 70: FAIL（必须修复）
  含 -25 分项: 直接 FAIL（BLOCK）

维度分数分解:
  method: 0-25
  class: 0-20
  naming: 0-15
  duplication: 0-15
  error_handling: 0-15
  comment_log: 0-10
```

## 输出

```yaml
评估报告:
  score: 0-100
  level: PASS|WARN|FAIL
  dimensions:
    method: 0-25
    class: 0-20
    naming: 0-15
    duplication: 0-15
    error_handling: 0-15
    comment_log: 0-10
  issues:
    - file: <path>
      line: <N>
      severity: BLOCK|WARN|INFO
      dimension: method|class|naming|duplication|error_handling|comment_log
      rule: <规则来源文件，如 code-smell-governance.md §1.5>
      description: <问题描述>
      suggestion: <修复建议>
```
