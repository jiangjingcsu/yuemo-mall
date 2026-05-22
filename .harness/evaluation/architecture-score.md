# 架构合规评估

> 评估代码变更是否符合架构治理规则。
> 治理依据: rules/architecture-governance.md、rules/module-boundary.md、rules/design-pattern-governance.md、rules/extensibility-governance.md、rules/redis-governance.md、rules/api-governance.md、rules/mq-governance.md

---

## 评估维度

### 1. 分层架构合规（权重 30%）

```yaml
检查:
  Controller 层:
    - [ ] 无业务逻辑（只做协议转换、参数校验、调用 Service）
    - [ ] 返回 Result<T>，不返回原始 Entity（第三方回调接口可豁免）
    - [ ] 不使用 HttpServletRequest/Response 作为参数
    - [ ] 不直接调用 Mapper（见 architecture-governance.md 禁止事项）
    - [ ] 不跨模块调用 Service（Controller 只调本模块 Service）
    - [ ] 使用 @RequiredArgsConstructor 注入（禁止 @Autowired 字段注入）
  
  Service 层:
    - [ ] 同模块内可直接调用本模块 Mapper（MyBatis-Plus 标准用法）
    - [ ] 跨模块调用通过 Service 接口或 MQ（禁止跨模块注入 Mapper）
    - [ ] 事务边界明确（@Transactional 在 ServiceImpl 方法上）
    - [ ] 不在事务中调用外部 RPC/HTTP 接口
    - [ ] 不在 for 循环中调用数据库或 MQ 发送

  DTO/VO/Entity 分离:
    - [ ] 入参使用 DTO/Request 对象（不直接接收 Entity）
    - [ ] 出参使用 VO（不直接返回 Entity）
    - [ ] VO 使用 Java record（不可变）

  Entity 层:
    - [ ] 继承 BaseEntity，使用 @TableName 映射
    - [ ] 无 @Autowired / @Component 注解
    - [ ] 不在 Entity 中调用 Service/Mapper
    - [ ] 核心领域 Entity（Order/Payment 等）应包含领域行为方法
    - [ ] 简单 CRUD Entity 允许贫血模型（但新增行为时应优先加到 Entity）

扣分:
  - Controller 含业务逻辑: -15 分
  - Controller 直接调用 Mapper: -15 分
  - Controller 跨模块调用 Service: -10 分
  - Controller 直接接收 Entity 作为 RequestBody: -10 分
  - Service 跨模块注入 Mapper: -20 分
  - Entity 包含 @Autowired/@Component: -10 分
  - 核心领域 Entity（Order/Payment）无行为方法: -10 分
  - 使用 @Autowired 字段注入（应用 @RequiredArgsConstructor）: -5 分
```

### 2. 模块边界合规（权重 25%）

```yaml
检查:
  - [ ] 无禁止的跨模块依赖（见 rules/module-boundary.md 依赖白名单）
  - [ ] 跨模块调用通过 Service 接口（非直接注入 Mapper）
  - [ ] 无循环依赖
  - [ ] 每个模块只操作自己的数据库表（见 rules/module-boundary.md 数据所有权）
  - [ ] 跨模块一致性通过 MQ 异步消息保证（非跨模块事务）

扣分:
  - 违反模块依赖白名单: -25 分（BLOCK）
  - 跨模块直接注入 Mapper: -20 分
  - 跨模块直接操作其他模块数据库表: -20 分
  - 循环依赖: -25 分（BLOCK）
```

### 3. 设计模式合规（权重 20%）

```yaml
检查:
  - [ ] ≥3 分支行为分发使用策略模式（非 switch-case/if-else）
  - [ ] 状态转换使用状态守卫或状态模式（非散落的 if-else 链）
  - [ ] 高扩展模块使用策略模式（见 rules/extensibility-governance.md）
  - [ ] 已有策略模式实现（PayTypeHandler、CartActionHandler）不被退化为 if-else

豁免:
  - 简单值映射的 switch 表达式（如排序字段映射 SQL）不强制策略模式
  - ≤2 分支的条件判断不强制策略模式

扣分:
  - switch-case ≥3 分支（行为分发场景）: -15 分
  - if-else 链 ≥3 条件（策略选择场景）: -10 分
  - 高扩展模块未使用策略模式: -20 分
  - 已有策略模式被退化为 if-else: -15 分
```

### 4. 数据架构合规（权重 15%）

```yaml
检查:
  - [ ] SQL 使用 #{param} 而非 ${param}
  - [ ] 无 SELECT *（指定具体列；MyBatis-Plus 默认全字段映射可接受）
  - [ ] 索引覆盖查询条件（WHERE/JOIN/ORDER BY）
  - [ ] 缓存 Key 符合 {domain}:{biz}:{identifier} 格式（见 rules/redis-governance.md）
  - [ ] MQ 消息有幂等机制（Redis setIfAbsent 或 DB upsert 均可接受）

当前项目 Redis Key 格式（见 rules/redis-governance.md）:
  cart:{userId}                        — 购物车
  token:user:{userId}                  — Token 映射
  token:blacklist:{token}              — Token 黑名单
  user:role:{userId}                   — 角色缓存
  payment:callback:{paymentNo}         — 支付回调幂等
  coupon:receive:{userId}:{couponId}   — 领券去重
  mq:consumed:{topic}:{bizId}          — MQ 消费幂等
  rate:{path}:{userId|ip}              — 限流计数

扣分:
  - ${} 拼接 SQL: -25 分（BLOCK - 安全风险）
  - 无索引的关键查询: -10 分
  - 缓存 Key 不符合项目规范: -5 分
  - MQ 消费者无幂等机制: -10 分
```

### 5. API 设计合规（权重 10%）

```yaml
检查:
  - [ ] URL 使用 RESTful 风格（复数名词，kebab-case）
  - [ ] DTO 和 VO 分离（不暴露 Entity 到 API，见 rules/api-governance.md）
  - [ ] 参数校验使用 @Valid
  - [ ] 分页接口参数命名统一（page/pageSize）
  - [ ] 错误码在正确的分段（见 rules/api-governance.md 错误码规范）

扣分:
  - API 直接返回 Entity（未转 VO）: -10 分
  - API 直接接收 Entity 作为 RequestBody: -10 分
  - 缺少 @Valid 参数校验: -8 分
```

---

## 已知技术债务

```yaml
以下问题存在于当前代码库中，评估新变更时不重复扣分（仅评估增量变更）:

  分层架构:
    - ProductController 直接注入 SearchKeywordMapper（应通过 Service）
    - AdminProductController/AdminCouponController 直接接收 Entity 作为 RequestBody
    - AddressController/UserController 直接接收 Entity 作为 RequestBody
    - PaymentController 回调接口返回 String 而非 Result<T>（第三方规范要求，可豁免）

  领域模型:
    - 19/21 Entity 为贫血模型（仅 Order/Payment 有行为方法）
    - Coupon 缺少 calculate() 行为方法（extensibility-governance.md 要求 CouponTypeHandler）
    - Order 状态流转未使用状态模式（使用 ensureStatus() 守卫，可接受但不够扩展）

  数据架构:
    - Spring Cache Key 命名不规范（productDetail::{id} 等，缺少模块前缀）

  API 设计:
    - Admin 端接口普遍缺少 @Valid 校验
```

---

## 评分

```yaml
总分: 100 分
  ≥ 85: PASS — 架构合规
  70-84: WARN — 存在架构问题，应改进后合并
  < 70: FAIL — 存在严重架构违规（BLOCK）
  含 -25 分项: 直接 FAIL

多模块变更: 取各模块最低分

评分范围:
  仅评估本次变更涉及的文件和模块
  已知技术债务不重复扣分（见上方清单）
  新增违规按扣分规则执行
```

---

## 输出

```yaml
评估报告:
  score: 0-100
  level: PASS|WARN|FAIL
  dimensions:
    layering: 0-30
    boundary: 0-25
    patterns: 0-20
    data: 0-15
    api: 0-10
  critical_issues:
    - rule: <规则文件名>
      location: <文件:行号>
      fix: <修复指引>
  high_issues:
    - rule: <规则文件名>
      location: <文件:行号>
      fix: <修复指引>
  medium_issues:
    - rule: <规则文件名>
      location: <文件:行号>
      suggestion: <改进建议>
  pass_items:
    - <检查项名称> ✓
  next_action: PASS|WARN|FIX
```
