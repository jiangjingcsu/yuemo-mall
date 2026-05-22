---
name: "root-cause-analysis"
description: "系统性分析 Bug 根因，从错误信息追溯到代码调用链、数据状态和配置，确认根因后再制定修复方案。当需要定位 Bug 根因、排查运行时异常或调查线上事故时触发。"
---

# 根因分析 Skill

对 Bug 进行系统性根因分析，从现象追溯到根因，确认后再进入修复阶段。
> 治理规则：`.harness/memory/anti-patterns.md` `.harness/memory/incidents.md`
> 被引用：`.harness/workflows/bug-fix.md` Step 2

## 触发条件

- 用户报告 Bug 需要定位根因
- CI/CD 流水线失败需要排查
- 运行时异常/错误日志需要分析
- 线上事故调查
- 性能问题排查（配合 `performance-review`）

## 依赖上下文

- `.harness/memory/anti-patterns.md` — 已知反模式（AP001-AP006）
- `.harness/memory/incidents.md` — 类似事故记录（INC001-INC004）
- `.harness/memory/known-issues.md` — 已知问题（TD001-TD006）
- `.harness/memory/decisions.md` — 架构决策（D001-D007）
- 错误日志 / 堆栈信息 / 异常截图
- 相关模块源码（Controller → Service → Mapper）

## 项目异常处理架构

> 分析 Bug 前必须理解项目的异常处理链，否则会误判异常类型。

```yaml
异常处理链:
  Controller 抛异常
    ├─ BusinessException → GlobalExceptionHandler → Result.fail(code, message) → HTTP 200
    │   注意: 业务异常返回 HTTP 200（非 4xx/5xx），通过 code 区分
    │   构造: throw new BusinessException(ResultCode.XXX) 或 throw new BusinessException(ResultCode.XXX, "自定义消息")
    │
    ├─ MethodArgumentNotValidException → GlobalExceptionHandler → Result.fail(BAD_REQUEST) → HTTP 400
    │   触发: @Valid/@Validated 校验失败
    │
    ├─ MissingRequestValueException → GlobalExceptionHandler → Result.fail(UNAUTHORIZED) → HTTP 401
    │   触发: @RequestAttribute("userId") 缺失（即未登录/Token 无效）
    │
    └─ 其他 Exception → GlobalExceptionHandler → Result.fail(INTERNAL_ERROR) → HTTP 500
        触发: NullPointerException、ClassCastException 等系统异常

网关层异常（独立处理链）:
  GatewayExceptionHandler 处理:
    - Token 过期/黑名单 → HTTP 401
    - 限流 → HTTP 429
    - 熔断 → HTTP 503
    - 路由不存在 → HTTP 404

分析时注意:
  - HTTP 200 + code != 200 → 业务异常，看 ResultCode 定位
  - HTTP 400 → 参数校验失败，看 DTO 注解
  - HTTP 401 → 鉴权失败，看 GatewayAuthFilter 白名单
  - HTTP 500 → 系统异常，看后端日志堆栈
  - HTTP 429/503 → 网关层限流/熔断，看 RateLimitFilter/CircuitBreakerFilter
```

## 快速定位路径

> 常见 Bug 可通过快捷路径快速定位，无需走完整 5 步流程。

```yaml
编译失败:
  → 直接看错误行号和依赖版本
  → 检查 pom.xml 版本冲突

BusinessException:
  → 直接看 ResultCode → 全局搜索 throw new BusinessException(ResultCode.XXX) → 定位 throw 位置

NullPointerException:
  → 直接看堆栈行号 → 检查链式调用 get() 是否判空

参数校验失败 (HTTP 400):
  → 直接看 DTO @NotBlank/@NotNull 注解 → 检查请求参数

401 未授权:
  → 直接看 GatewayAuthFilter 白名单配置 → 检查 Token 是否过期/黑名单

唯一约束冲突:
  → 检查唯一键是否包含 deleted 字段（AP006）
  → 检查是否存在 SELECT-then-INSERT 竞态（AP005）

数据不一致:
  → 检查 Redis 缓存与 DB 一致性（D004: 购物车 Redis 为主）
  → 检查逻辑删除是否正确过滤（D005: deleted 字段）
  → 检查 MQ 消费者是否重复消费导致数据重复/不一致（查找消费者中是否有幂等 Key 处理逻辑）
```

## 分析流程

### Step 1: 信息收集与分类

```yaml
收集:
  - Bug 现象: 报错信息、异常行为、截图
  - 复现步骤: 稳定复现 / 偶现 / 仅生产
  - 影响范围: 模块、用户群体、环境（dev/staging/prod）
  - 相关日志: 异常堆栈、操作序列、时间线

Bug 分类（决定后续分析路径）:
  | 类型 | 优先级 | 分析侧重 | 辅助 Skill |
  |---|---|---|---|
  | 编译失败 | 最高 | 依赖版本、语法兼容性 | — |
  | 运行时异常 | 高 | 堆栈追溯、空指针、类型转换 | — |
  | 数据不一致 | 高 | 事务边界、并发竞态、缓存一致性 | sql-review / transaction-analysis |
  | 功能不符合预期 | 中 | 业务逻辑、状态流转、条件分支 | — |
  | 性能问题 | 中 | 慢查询、N+1、缓存缺失 | performance-review |
  | UI 显示问题 | 低 | 接口数据格式、前端状态、路由 | — |

Memory 检查（强制）:
  - 读取 .harness/memory/decisions.md — 是否有相关架构决策
  - 读取 .harness/memory/anti-patterns.md — 是否有已知反模式
  - 读取 .harness/memory/incidents.md — 是否有类似事故记录
  - 读取 .harness/memory/known-issues.md — 是否有已知技术债务
```

### Step 2: 堆栈与调用链追溯

```yaml
从错误信息出发，逐层追溯:

1. 异常类/行号定位:
   - 读取堆栈信息，定位异常抛出位置
   - 区分: BusinessException（看 ResultCode）vs 系统异常（看堆栈行号）
   - 区分: 业务层异常（GlobalExceptionHandler）vs 网关层异常（GatewayExceptionHandler）

2. 调用链追溯（Controller → Service → Mapper）:
   - 从异常位置向上追溯调用方
   - 检查每一层的参数传递和返回值处理
   - 关注: null 传递、类型转换、集合操作

3. 数据流追踪:
   - 请求参数 → Controller 接收 → Service 处理 → Mapper 查询 → 数据库
   - 响应: 数据库 → Entity → VO(from 方法) → 前端
   - 检查每一步的数据转换是否正确（Entity → VO 是否遗漏字段）
```

### Step 3: 多维度排查

```yaml
按 Bug 分类选择排查维度:

代码维度:
  - 空指针: 未做 null 检查、Optional 使用不当、链式调用中断
  - 逻辑错误: 条件分支遗漏、状态判断错误、边界未处理
  - 类型问题: 类型转换异常、泛型擦除、序列化/反序列化

数据维度:
  - 数据库记录状态（SELECT 确认，禁止直接 DELETE/UPDATE）
  - Redis 缓存与数据库一致性（D004: 购物车 Redis 为主存储）
  - 逻辑删除字段（deleted）是否正确过滤（D005: 唯一键含 deleted）

并发维度（→ transaction-analysis）:
  - 是否有竞态条件（AP005: SELECT-then-INSERT/UPDATE 竞态）
  - 是否缺少幂等处理
  - 是否缺少锁保护

配置维度:
  - application.yml 配置值
  - 环境变量是否缺失
  - Feature Flag 状态

依赖维度:
  - pom.xml / package.json 版本冲突
  - 依赖缺失或版本不兼容

前端维度:
  - 接口数据格式变更: VO 字段名/类型变更但前端未同步
  - Redux 状态不一致: selector 返回过期数据
  - 路由问题: React Router 状态丢失、参数解析错误
  - 请求拦截器: 401 自动跳转登录页、Token 刷新失败
  - Ant Design 组件: Table 数据源格式、Form 字段绑定
  - 请求参数: 前端传参名称与后端 DTO 字段不匹配
```

### Step 4: 根因确认

```yaml
确认方法:
  - 根因是否能解释所有现象？（不能只解释部分）
  - 修复根因是否能消除 Bug？（不是掩盖症状）
  - 是否有其他触发路径？（同一根因可能有多种触发方式）

常见根因模式库（关联项目 memory）:
  | 根因类型 | 典型表现 | 项目实际案例 |
  |---|---|---|
  | 空指针 | NPE 堆栈 | 链式 get() 未判空、Optional.get() 未检查 |
  | 数据库 | 唯一约束冲突、数据缺失 | AP006: 唯一键不含 deleted、INC001: 购物车重复插入 |
  | 并发 | 偶现数据不一致 | AP005: SELECT-then-INSERT 竞态、INC001 |
  | 配置 | 特定环境失败 | INC003: JWT 密钥长度不足、INC004: 双重 Nginx 代理 |
  | 边界 | 特定输入失败 | 空列表、0 金额、超长字符串、特殊字符 |
  | 缓存 | 数据不一致 | D004: 购物车 Redis 与 MySQL 不一致 |
  | 序列化 | 字段丢失/类型错误 | Jackson 忽略字段、日期格式不一致、枚举映射 |
  | 权限 | 越权/无权限 | Token 过期、角色校验遗漏、跨用户数据访问 |
  | 前端 | UI 显示异常 | VO 字段变更前端未同步、Redux state 不一致 |
```

### Step 5: 输出分析报告

```markdown
## 根因分析报告

### Bug 描述
{一句话描述现象}

### Bug 分类
{编译失败/运行时异常/数据不一致/功能不符合预期/性能问题/UI 显示问题}

### 根因
{一句话描述根因}

### 分析路径
{异常位置} → {调用链} → {根因位置}

### 根因类型
{空指针/数据库/并发/配置/边界/缓存/序列化/权限/前端}

### 影响范围
- 受影响模块: {模块名}
- 受影响功能: {功能点}
- 受影响用户: {用户群体}

### 关联 Memory
- 反模式: {AP00X}（如有）
- 历史事故: {INC00X}（如有）
- 架构决策: {D00X}（如有）

### 初步修复方向
{1-2 句描述修复方向，具体修复方案由 bug-fix 工作流 Step 3 制定}

### 关联 Skill（如需深入分析）
- 数据库相关 → sql-review
- 并发/事务相关 → transaction-analysis
- 性能相关 → performance-review
- 代码验证（可选） → code-verify
```

## 按类型深入分析

当 Step 3 排查指向特定领域时，激活对应 Skill 做深入分析：

| 根因指向 | 激活 Skill | 说明 |
|---------|-----------|------|
| 数据库查询/表设计 | `sql-review` | 审查 SQL 语句、索引、迁移脚本 |
| 事务/并发/一致性 | `transaction-analysis` | 分析事务边界、并发安全、回滚机制 |
| 性能瓶颈 | `performance-review` | 分析慢查询、N+1、缓存缺失 |
| 代码验证（可选） | `code-verify` | 快速验证 API 路径/方法是否存在 |

## 分析失败回退策略

```yaml
当排查了所有维度仍无法定位根因时:
  1. 添加更多日志: 在关键路径添加 log.info/debug，复现后分析
  2. 本地断点调试: 在 IDE 中设断点，逐步跟踪数据流
  3. 数据快照对比: 对比正常/异常数据状态（SELECT 查询，禁止修改）
  4. 请求链路追踪: 检查 Gateway → Service → Mapper 完整链路
  5. 环境差异对比: 对比 dev/staging/prod 配置和数据差异
  6. 向用户请求更多信息: 复现步骤、操作录屏、网络请求截图

最多 3 轮排查，仍无法定位 → 向用户报告已知信息和待确认假设
```

## 约束

- 不自行假设 Bug 原因，对不明确的问题先请求更多信息
- 先定位根因再修复，不治标不治本
- 数据库排查只读（SELECT），不在生产执行 DELETE/UPDATE
- 根因必须能解释所有现象，否则继续排查
- 分析过程记录关键发现，便于后续事故复盘
- Memory 检查是强制的，不可跳过
- 分析报告必须包含初步修复方向，不可只输出分析结果
