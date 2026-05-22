---
name: "ddd-design"
description: "基于 DDD 领域驱动设计方法，为新功能或重构设计领域模型、聚合、实体和值对象。当用户要求设计新功能或重构现有领域时触发。"
---

# DDD 领域设计 Skill

基于项目现有架构和领域划分，为功能设计符合 DDD 原则的领域模型。
> 治理规则：`.harness/rules/ddd-governance.md` `.harness/rules/domain-modeling.md`

## 触发条件

- 用户要求设计新功能
- 用户要求对现有模块进行 DDD 改造
- 用户要求评估领域模型
- 用户要求为微服务拆分做准备
- 用户询问"这个功能应该放在哪个模块？"
- 用户询问"这两个实体应该是一个聚合还是两个？"
- 新增数据库表时的领域归属判断

## 依赖上下文

- `.harness/rules/ddd-governance.md` — DDD 治理规则（领域边界、分层、聚合、事件）
- `.harness/rules/domain-modeling.md` — 领域建模规则（充血模型、贫血模型禁止、领域行为归属）⚠️ 必读
- `.harness/rules/module-boundary.md` — 模块边界（数据所有权、依赖白名单）
- `.harness/docs/business-domain.md` — 业务领域模型（状态机、业务规则）
- `.harness/docs/module-responsibility.md` — 模块职责
- `.harness/constraints/database.md` — 数据库变更约束（索引、表名规范）
- `.harness/skills/api-design/SKILL.md` — API 设计联动（领域服务 → Controller 暴露）

> ⚠️ 注意：`ddd-governance.md` 引用了 `.harness/skills/domain-modeling/SKILL.md`，领域建模实操规则已内嵌于 `domain-modeling.md` 治理规则中。

## 现有聚合现状

> 设计新聚合前，必须了解现有聚合划分和改进方向。

| 领域 | 聚合根 | 内部实体 | 充血模型状态 |
|---|---|---|---|
| 用户域 | User | Address | ⚠️ 待评估 |
| 商品域 | Product | ProductSku, Review | ❌ 贫血（仅字段，无行为） |
| 订单域 | Order | OrderItem, OrderLog | ✅ 已充血（pay/ship/cancel/confirmReceive） |
| 支付域 | Payment | — | ⚠️ 部分充血（markSuccess/markRefunded 已提取，支付方式采用策略模式） |
| 营销域 | Coupon | UserCoupon | ⚠️ 待评估 |
| 购物车域 | Cart(Redis Hash) | CartItem | ⚠️ 待评估 |

> 参考 `domain-modeling.md` 第 4.2 节的聚合评审和改进建议。新实体设计应以 Order 为充血模型参考，而非 Product。

## 设计流程

### Step 1: 识别领域

```yaml
分析:
  - 新功能属于哪个现有领域？（用户/商品/订单/支付/营销/购物车）
  - 是否需要创建新领域？
  - 新领域的数据主权是什么？

领域识别原则:
  - 一个领域有独立的业务含义和数据生命周期
  - 领域内高内聚，领域间低耦合
  - 每个领域应能独立演进

何时创建新领域的决策标准:
  - 拥有独立的数据表集合，且不属于任何现有领域
  - 有独立的状态流转生命周期
  - 与现有领域的业务规则无强内聚关系
  - 未来可能独立部署为微服务
  若不满足以上条件，优先归入现有领域
```

### Step 2: 定义聚合（含事件识别）

```yaml
聚合设计:
  1. 识别聚合根: 拥有全局唯一 ID，外部通过它访问聚合内部
  2. 识别内部实体: 聚合边界内的实体，生命周期由聚合根管理
  3. 识别值对象: 无 ID，通过属性值相等性判断
  4. 识别领域事件: 聚合操作产生的跨域通知

规则:
  - 一次事务只修改一个聚合
  - 聚合间通过 ID 引用
  - 聚合根保证内部一致性
  - 聚合根必须包含领域行为方法（禁止贫血，参考 domain-modeling.md）
  - 外部不能直接引用内部实体

聚合边界判断标准:
  - 两个实体是否必须保证强一致性？是 → 同一聚合
  - 一个实体的生命周期是否完全由另一个管理？是 → 内部实体
  - 两个实体是否需要独立修改？是 → 不同聚合，通过 ID 引用
```

### Step 3: 设计实体

```yaml
实体设计:
  - 实体有唯一标识（ID）
  - 实体继承 BaseEntity
  - 实体映射数据库表（@TableName）
  - 实体放在 entity/ 包
  - ⚠️ 实体必须包含领域行为方法（充血模型），禁止只有 getter/setter

行为设计（参考 domain-modeling.md 第 3 节）:
  - 状态校验和转换 → Entity 方法（如 order.pay()、order.cancel()）
  - 金额计算 → Entity 或 DomainService
  - 判断方法 → Entity（如 order.canDelete()）

输出:
  - 实体类名、表名
  - 核心字段及类型
  - 领域行为方法列表
  - 唯一约束和索引
```

### Step 4: 设计值对象

```yaml
值对象设计:
  - 无 ID，通过所有属性值判断相等
  - 不可变（Java record 天然适合）
  - 可嵌入到实体中

实体 vs 值对象判断标准:
  - 需要独立追踪生命周期 → 实体
  - 只关心属性值是否相等 → 值对象
  - 需要独立修改 → 实体
  - 作为整体替换 → 值对象

当前项目值对象候选:
  - 地址（省/市/区/详情/邮编）
  - 金额+币种
  - 规格组合（specIds + specText）

持久化策略:
  - 值对象嵌入实体字段: 使用 @TableField(typeHandler = JacksonTypeHandler.class) 序列化为 JSON
  - 或将值对象属性展开为实体字段（如 addressProvince、addressCity）
  - 选择依据: 值对象是否需要作为查询条件 → 展开字段；否则 → JSON 序列化
```

### Step 5: 定义领域服务

```yaml
领域服务:
  - 处理跨聚合的业务逻辑
  - 无状态
  - 放在 service/ 包

领域服务 vs 应用服务区分:
  - 领域服务: 包含领域规则/计算逻辑，跨聚合但不含编排
  - 应用服务(ServiceImpl): 编排多个聚合操作、事务管理、调用外部服务

当前项目领域服务示例:
  - ProductServiceImpl.updateStock(): 库存扣减领域逻辑（跨 SKU 的库存计算 + 乐观锁）

当前项目应用服务示例（非领域服务）:
  - OrderServiceImpl.createOrder(): 订单创建编排（创建订单 + 发送库存预占消息 + 清空购物车）
```

### Step 6: 定义领域事件

```yaml
领域事件:
  - 命名: {domain}-{event}（如 order-stock-preoccupy）
  - 由聚合根操作产生
  - 通过 RocketMQ 异步发布
  - 事件必须包含聚合根 ID 和事件时间

当前项目领域事件（完整）:
  - order-stock-preoccupy: 订单创建 → 库存预占（RMQ 事务消息）
  - order-stock-release: 订单取消/超时 → 库存释放
  - payment-callback: 支付成功 → 订单状态更新
  - payment-refund: 退款 → 库存恢复
  - cart-sync: 购物车变更 → MySQL 同步（自产自消）
```

### Step 7: 设计仓储

```yaml
当前实现:
  - Mapper extends BaseMapper<Entity>（MyBatis-Plus）
  - 复杂查询在 ServiceImpl 中通过 LambdaQueryWrapper 构建
  - 禁止 Controller 直接调用 Mapper

设计新聚合时:
  - 新增 Mapper 放在对应模块的 mapper/ 包
  - 简单查询用 LambdaQueryWrapper
  - 复杂 SQL 写在 XML Mapper 中
  - 遵循 constraints/database.md 的表名和索引规范

演进方向（非当前要求）:
  - Repository 接口定义在 domain 层
  - Repository 实现在 infrastructure 层
  - domain 层只依赖 Repository 接口（依赖倒置）
```

### Step 8: 验证与回溯

```yaml
验证检查:
  - 合规检查项全部通过？
  - Entity 是否包含领域行为？（否 → 回到 Step 3）
  - 聚合边界是否合理？（不确定 → 回到 Step 2）
  - 领域事件是否完整？（遗漏 → 回到 Step 6）

回溯规则:
  - Step 3 发现聚合边界问题 → 回到 Step 2
  - Step 5 发现需要新值对象 → 回到 Step 4
  - Step 8 合规不通过 → 根据具体问题回到对应步骤
  - 最多回溯 2 次，仍不通过则输出 WARN 并说明原因
```

## 输出格式

```markdown
## 领域设计

### 领域归属
新功能属于 {现有领域/新领域}

### 聚合设计
- 聚合根: {名称}（{table_name}）
- 内部实体: {列表}
- 值对象: {列表}

### 实体设计
| 实体 | 表名 | 核心字段 | 领域行为方法 | 索引 |

### 值对象设计
| 值对象 | 属性 | 持久化方式 | 嵌入实体 |

### 领域服务
| 服务 | 方法 | 职责 | 跨聚合说明 |

### 领域事件
| 事件 | 触发条件 | 消费者 |

### 聚合交互
{描述聚合间如何通过 ID 引用和事件通信}

### 合规检查
- [ ] 一次事务只修改一个聚合
- [ ] 聚合间通过 ID 引用
- [ ] 模块边界未破坏
- [ ] 不跨领域直接操作数据库
- [ ] Entity 包含领域行为方法（非贫血模型）
- [ ] 状态流转逻辑在 Entity 内部（非 ServiceImpl）
```

## 约束

- 不引入过度抽象（Spring Service 接口+实现是常规模式，不算过度抽象；无实现类的 interface 才是）
- 对于简单 CRUD 场景，可简化聚合设计，但仍需遵守贫血模型禁止规则（Entity 必须有行为方法）
- 领域设计必须与实际数据库表结构一致
- 考虑现有架构模式，不一味套用 DDD 教条
- 新增数据库表和索引需遵循 `constraints/database.md` 规范
