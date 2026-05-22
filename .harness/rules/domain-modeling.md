# 领域建模治理规则

> 增强 AI Agent 的领域建模能力。避免过程式编程和贫血模型，强制面向对象设计。这是对 `ddd-governance.md` 的补充，聚焦于 AI 生成代码时的行为约束。
> 实操技能：`.harness/skills/domain-modeling/SKILL.md` `.harness/skills/ddd-design/SKILL.md`

---

## 1. 核心原则

```yaml
领域建模优先:
  - 先理解业务领域，再写代码
  - 先设计聚合和实体关系，再写 Service
  - 行为属于对象（Entity/DomainService），而非 ServiceImpl

禁止过程式编程:
  - 禁止: 所有逻辑堆积在 ServiceImpl，Entity 只是数据容器
  - 禁止: DTO 承担业务逻辑
  - 禁止: 在 ServiceImpl 中用 if-else 判断实体状态
```

---

## 2. 贫血模型 vs 充血模型

### 禁止：贫血模型

```java
// 禁止: Entity 只是数据容器，业务逻辑全在 ServiceImpl
@Data
@TableName("yu_order")
public class Order extends BaseEntity {
    private String orderNo;
    private Integer status;
    private BigDecimal totalAmount;
    // 只有 getter/setter，没有任何行为
}
```

```java
// ServiceImpl 中到处是这样判断（项目中不存在此写法，但 AI 生成时须避免）
if (order.getStatus() == 0) { // 魔法值 + 贫血（禁止）
    order.setStatus(1);        // 直接改状态，无校验（禁止）
    order.setPayTime(now);
}
```

### 要求：充血模型

```java
// 要求: Entity 包含领域行为（与项目实际代码一致）
@Data
@TableName("yu_order")
public class Order extends BaseEntity {
    private String orderNo;
    private Integer status;
    private BigDecimal totalAmount;

    // 通用方法: 状态前置校验
    public void ensureStatus(OrderStatus expected) {
        if (this.status == null || this.status != expected.getCode()) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
    }

    // 通用方法: 所有权验证
    public void verifyOwnership(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    // 领域行为: 支付
    public void pay() {
        ensureStatus(OrderStatus.UNPAID);
        this.status = OrderStatus.PAID.getCode();
        this.payTime = LocalDateTime.now();
    }

    // 领域行为: 发货
    public void ship(String logisticsCompany, String logisticsNo) {
        ensureStatus(OrderStatus.PAID);
        this.status = OrderStatus.SHIPPED.getCode();
        this.logisticsCompany = logisticsCompany;
        this.logisticsNo = logisticsNo;
        this.deliveryTime = LocalDateTime.now();
    }

    // 领域行为: 确认收货
    public void confirmReceive() {
        ensureStatus(OrderStatus.SHIPPED);
        this.status = OrderStatus.COMPLETED.getCode();
        this.receiveTime = LocalDateTime.now();
    }

    // 领域行为: 取消
    public void cancel() {
        ensureStatus(OrderStatus.UNPAID);
        this.status = OrderStatus.CANCELLED.getCode();
    }

    // 领域行为: 判断是否可删除
    public boolean canDelete() {
        return this.status != null
            && (this.status == OrderStatus.COMPLETED.getCode()
            || this.status == OrderStatus.CANCELLED.getCode());
    }
}
```

---

## 3. 领域逻辑归属

| 逻辑类型 | 归属 | 示例 |
|---|---|---|
| 状态校验和转换 | Entity | `order.pay()` / `order.cancel()` |
| 金额计算 | Entity 或 DomainService | `orderItem.calculateSubtotal()` |
| 跨聚合编排 | Application Service (ServiceImpl) | 下单流程：创建订单 + 占用库存 |
| 跨聚合计算 | DomainService | 订单金额 = SUM(orderItem) + 优惠券计算 |
| 外部服务调用 | Application Service | 调用其他模块 Service |
| 数据持久化 | Mapper / Repository | 查询和保存 |

### 领域服务 vs 应用服务

| 维度 | DomainService | Application Service (ServiceImpl) |
|---|---|---|
| 定位 | 跨聚合的业务逻辑，无状态 | 业务编排，事务管理 |
| 调用方 | 被 ServiceImpl 调用 | 被 Controller 调用 |
| 依赖 | 只依赖 Entity 和值对象 | 可依赖 Mapper、其他 Service、MQ |
| 命名 | `XxxDomainService` | `XxxServiceImpl` |
| 事务 | 不管理事务 | 管理 @Transactional |

```yaml
何时引入 DomainService:
  - 跨聚合计算（如订单金额 = SUM(orderItem) + 优惠券计算）
  - 跨聚合业务规则（如库存扣减涉及乐观锁，不适合放在 Entity 中）
  - 当前项目: 暂无 DomainService，跨聚合逻辑直接在 ServiceImpl 中编排
  - 演进: 当 ServiceImpl 中跨聚合逻辑超过 3 处时，提取为 DomainService
```

---

## 4. 聚合设计强制规则

### 4.1 聚合根职责

```yaml
聚合根必须:
  - 拥有全局唯一标识（ID）
  - 控制内部实体的一致性
  - 提供领域行为方法（非 CRUD 式的 setXxx）
  - 保护内部实体不被外部直接引用

聚合根禁止:
  - 只有 getter/setter，没有行为（贫血）
  - 直接暴露内部实体集合（应返回不可变视图）
```

### 4.2 当前项目聚合评审

```yaml
Order 聚合（yuemo-order）:
  当前: Entity 已充血，包含 pay()/ship()/confirmReceive()/cancel()/canDelete() 等领域行为
  遗漏: refundOrder() 仍直接 setStatus()，缺少 Order.refund() 方法
  改进: 补充 Order.refund() 领域方法，消除 ServiceImpl 中的直接状态修改

Payment 聚合（yuemo-payment）:
  当前: Entity 已充血，包含 markSuccess()/markFailed()/markRefunded()/isPending()/isSuccess() 等领域行为
  完成: 状态变更逻辑已封装在 Entity 中
  改进: 方法命名可考虑统一为更简短的 pay()/refund()/fail()（非紧急）

Product 聚合（yuemo-product）:
  当前: Product + ProductSku 均为贫血模型，无领域行为
  问题:
    - ProductStatus 枚举已存在但未引用，状态判断使用硬编码魔法数字
    - SkuServiceImpl.syncProductAggregate() 反向修改 Product 聚合根，违反聚合边界
    - ProductServiceImpl 注入 6 个 Mapper，直接操作内部实体（TagRel/SpecTemplate/SpecValue）
  改进:
    - Product 添加 onSale()/offSale() 等状态行为方法
    - 库存扣减涉及 DB 乐观锁，放在 DomainService 可接受
    - SkuServiceImpl 修改 Product 应通过 ProductService 接口调用

CartItem 聚合（yuemo-cart）:
  当前: 纯贫血模型，无领域行为
  问题: 购物车逻辑全在 CartServiceImpl 中，数量校验/选中状态变更无封装
  改进: CartItem 添加 updateQuantity()/toggleSelect() 等方法
  备注: 购物车以 Redis 为主存储，Entity 行为收益有限，优先级低

Coupon 聚合（yuemo-promotion）:
  当前: 纯贫血模型，无领域行为
  问题: 状态和类型使用硬编码整数，无枚举引用
  改进: Coupon 添加 activate()/expire()/canReceive() 等方法，引入 CouponStatus/CouponType 枚举

User 聚合（yuemo-user）:
  当前: 纯贫血模型，无领域行为
  问题: 余额扣减在 PaymentServiceImpl 中直接 setBalance()，无校验
  改进: User 添加 deductBalance()/addBalance() 方法，封装余额变更规则

Address 聚合（yuemo-user）:
  当前: 纯贫血模型，无领域行为
  改进: Address 添加 setAsDefault() 方法（当前循环更新在 ServiceImpl 中）
  优先级: 低
```

### 4.3 聚合边界维护

```yaml
聚合边界规则:
  - 内部实体的生命周期由聚合根统一管理，禁止 Service 层直接操作内部实体的 Mapper
  - 禁止跨聚合直接修改另一个聚合根的状态（应通过 Service 接口调用）
  - 聚合间只通过 ID 引用，不持有其他聚合根的对象引用

当前已知违规:
  - OrderServiceImpl 直接操作 OrderItemMapper（应通过 Order 聚合根管理）
  - CouponServiceImpl 直接操作 UserCouponMapper
  - UserServiceImpl 直接操作 UserRoleMapper
  - ProductServiceImpl 直接操作 TagRelMapper/SpecTemplateMapper/SpecValueMapper
  - SkuServiceImpl.syncProductAggregate() 反向修改 Product 聚合根

演进方向:
  - 短期: 内部实体操作至少通过本模块 Service 接口，而非直接 Mapper
  - 中期: 聚合根提供管理内部实体的方法（如 Order.addItem()）
  - 长期: 引入 Repository 模式，聚合根统一持久化
```

---

## 5. AI 生成代码前检查

### 领域建模检查

```yaml
AI 在生成代码前必须检查:
  1. 新增 Entity 是否包含领域行为？
     - 只有 getter/setter → 重新设计，添加行为方法

  2. 状态流转逻辑在哪里？
     - ServiceImpl 中 → 移到 Entity 或 DomainService

  3. 业务规则在哪里？
     - ServiceImpl 中 → 移到 Entity 方法中

  4. 是否操作了其他聚合的内部实体？
     - 是 → 改为通过聚合根操作或 MQ 通信

  5. 新增功能是否修改了聚合边界？
     - 是 → 评估聚合设计是否合理

  6. 是否产生循环依赖？
     - 是 → 重新设计聚合关系

  7. 是否存在直接 setStatus() 绕过 Entity 领域方法的情况？
     - 是 → 必须使用 Entity 的领域行为方法，缺失则先补充
```

---

## 6. 领域建模步骤

AI 在生成代码时，必须按以下顺序进行：

```yaml
1. 识别领域概念:
   - 阅读 .harness/docs/business-domain.md
   - 确认属于哪个现有领域

2. 设计聚合:
   - 识别聚合根和内部实体
   - 定义聚合边界

3. 设计实体行为:
   - 每个实体有哪些行为方法？
   - 状态如何流转？
   - 需要哪些校验？

4. 设计领域服务:
   - 哪些逻辑跨聚合？（放在 DomainService）
   - 哪些逻辑是聚合内部？（放在 Entity）

5. 设计应用服务:
   - ServiceImpl 只做编排
   - 不包含领域规则

6. 验证:
   - Entity 是否只有数据？（是 → 贫血模型，重新设计）
   - 状态校验是否在 Entity 内部？（否 → 重新设计）
```

---

## 7. 与现有规则的关联

```yaml
本规则是对以下规则的补充:
  - ddd-governance.md: DDD 分层架构
  - architecture-decision.md: 架构决策（switch-case 治理）
  - design-pattern-governance.md: 设计模式（状态模式）
  - architecture-governance.md: 分层约束

联动的禁止事项:
  - 禁止 DTO 包含业务逻辑（共同约束）
  - 禁止 ServiceImpl 包含领域规则（共同约束）
  - 禁止 Entity 只是数据结构（本规则重点）
  - 禁止使用魔法值表示状态（code-smell-governance 约束）

与 ddd-design SKILL 的状态同步:
  - 本文档的聚合评审（第4.2节）应与 .harness/skills/ddd-design/SKILL.md 中的"现有聚合现状"保持一致
  - 当 Entity 充血模型改进后，须同步更新两处文档
  - 当前一致状态: Order 已充血, Payment 已充血, Product 贫血

贫血模型 = 过程式编程 = 面向过程 = 不是面向对象
充血模型 = 行为+数据在一起 = 面向对象 = 可扩展
```

---

## 8. 值对象规范

```yaml
值对象识别:
  - 无独立 ID，通过属性值判等
  - 不可变（Java record 天然适合）
  - 示例：金额（BigDecimal 封装）、地址（省/市/区/详情组合）、规格描述

当前项目:
  - 无领域层值对象
  - CartItemRedis (record + withXxx 模式) 最接近值对象概念，但放在 dto 包

规范:
  - 值对象使用 Java record 定义
  - 放在 entity/ 包下的 valueobject/ 子包中
  - 需要持久化时：简单值对象展开为 Entity 字段，复杂值对象 JSON 序列化
  - 短期：不强制引入值对象，但新增领域概念优先考虑 record
```

---

## 9. 渐进式充血策略

```yaml
优先级:
  P0 - 核心聚合（已有领域行为，补全遗漏）:
    - Order: 补充 refund() 方法
    - Payment: 已完成，方法命名可优化

  P1 - 高业务价值聚合:
    - Product: 添加 onSale()/offSale()，引入 ProductStatus 枚举
    - Coupon: 添加 activate()/expire()/canReceive()，引入 CouponStatus 枚举

  P2 - 辅助聚合:
    - User: 添加 deductBalance()/addBalance()
    - CartItem: 添加 updateQuantity()/toggleSelect()（优先级低，Redis 为主存储）
    - Address: 添加 setAsDefault()（优先级低）

迁移原则:
  - 新增功能必须充血（新 Entity 必须有领域行为）
  - 修改现有 Entity 时顺便充血（Boy Scout Rule）
  - 不做大规模重构（避免一次性改动过多）
  - ServiceImpl 中的状态判断逻辑是充血迁移的首要目标
```
