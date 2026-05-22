# 设计模式治理规则

> 约束 AI Agent 的设计模式使用能力。让 AI 理解何时必须使用策略/工厂/状态/责任链模式，何时禁止 switch-case 和 if-else 链。基于项目实际代码模式制定。
> 实操技能：`.harness/skills/refactor-analysis/SKILL.md`

```yaml
元信息:
  版本: 1.1
  最后更新: 2026-05-22
  更新触发条件:
    - 项目代码完成策略模式重构后（更新"已违反位置"表）
    - 新增高扩展模块时
    - 新增设计模式使用场景时
```

---

## 1. 策略模式（Strategy Pattern）

### 强制使用条件

当满足以下**任一**条件时，必须使用策略模式而非 switch-case/if-else：

```yaml
强制使用:
  - 分支数 ≥ 3（无论是否高扩展模块）
  - 或属于高扩展模块（即使当前仅 2 个分支）
  - action/event/type 字段决定不同处理逻辑
  - 每种行为具有独立、可封装的逻辑
  - 需要在运行时动态选择行为
```

### 项目中待修复位置

| 文件 | 问题 | 应改为 |
|---|---|---|
| `PaymentServiceImpl.verifySign` | `if(payType == PayType.BALANCE.getCode())` 验签逻辑硬编码 | 将 verifySign 职责下沉到 PayTypeHandler 接口 |

### 项目中已修复位置

| 文件 | 原问题 | 当前状态 |
|---|---|---|
| `CartSyncConsumer.java` | `switch(msg.action())` 5 个 case | ✅ 已重构为 CartActionHandler + Map 查表 |
| `PaymentServiceImpl.java` | `if(payType == 3)` 支付方式分派 | ✅ 已重构为 PayTypeHandler + Map 查表 |

### 标准实现模板

AI 遇到 action/event/type 分发时，必须生成以下结构而非 switch-case：

```yaml
Handler 接口设计规范:
  当前项目统一使用 key 返回法:
    - 接口定义: {key类型} {keyMethod}(); void handle({Message} message);
    - Factory: Collectors.toMap(Handler::{keyMethod}, Function.identity())
    - 适用: 路由 key 为枚举/整数的场景（如 CartAction、PayType）
    - 与项目已有代码保持一致（CartActionHandler、PayTypeHandler）
```

```java
// 1. Handler 接口
public interface ActionHandler {
    ActionType action();
    void handle(Message message);
}

// 2. Handler 实现（每种 action 一个独立类）
@Component
public class AddActionHandler implements ActionHandler {
    @Override
    public ActionType action() { return ActionType.ADD; }
    @Override
    public void handle(Message message) { /* 处理逻辑 */ }
}

// 3. HandlerFactory — Spring 自动注入所有 Handler
@Component
public class ActionHandlerFactory {
    private final Map<ActionType, ActionHandler> handlerMap;

    public ActionHandlerFactory(List<ActionHandler> handlers) {
        this.handlerMap = handlers.stream()
            .collect(Collectors.toMap(ActionHandler::action, Function.identity()));
    }

    public ActionHandler get(ActionType action) {
        ActionHandler handler = handlerMap.get(action);
        if (handler == null) throw new BusinessException(ResultCode.UNSUPPORTED_ACTION, "不支持的操作: " + action);
        return handler;
    }
}

// 4. Consumer — 一行调度
actionHandlerFactory.get(msg.action()).handle(msg);
```

### 禁止写法

```java
// 禁止: switch-case 分发
switch (msg.action()) {
    case ADD -> handleAdd(msg);
    case UPDATE -> handleUpdate(msg);
    case REMOVE -> handleRemove(msg);
}

// 禁止: if-else 链分发
if (type.equals("A")) { handleA(); }
else if (type.equals("B")) { handleB(); }
else if (type.equals("C")) { handleC(); }
```

---

## 2. 工厂模式（Factory Pattern）

### 强制使用条件

```yaml
强制使用:
  - 存在动态实现选择（运行时根据参数选择不同实现）
  - 存在多类型处理器（如不同支付方式、不同通知渠道）
  - 创建逻辑需要集中管理
  - 需要隐藏实现细节，只暴露接口
  - 配合策略模式使用（HandlerFactory 本质上就是工厂）
```

### 项目中适用位置

| 场景 | 当前状态 | 建议 |
|---|---|---|
| 支付处理器创建 | ✅ 已使用 PayTypeHandler + Map 查表 | verifySign 职责下沉到 Handler |
| MQ 消息处理器 | ✅ 已使用 CartActionHandler + Map 查表 | 保持当前架构 |
| 通知发送（短信/邮件/推送） | 未实现 | NotificationSenderFactory |

### 标准模板

```java
@Component
public class PayTypeHandlerFactory {
    private final Map<Integer, PayTypeHandler> handlerMap;

    public PayTypeHandlerFactory(List<PayTypeHandler> handlers) {
        this.handlerMap = handlers.stream()
            .collect(Collectors.toMap(PayTypeHandler::payType, Function.identity()));
    }

    public PayTypeHandler get(Integer payType) {
        PayTypeHandler handler = handlerMap.get(payType);
        if (handler == null) throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的支付类型: " + payType);
        return handler;
    }
}
```

---

## 3. 状态模式（State Pattern）

### 强制使用条件

```yaml
强制使用:
  - 存在复杂状态流转（≥4 个状态）
  - 不同状态下行为差异大
  - 状态未来可能增加
  - 状态转换有复杂校验规则

项目中适用:
  - 订单状态: 待支付→已支付→已发货→已完成→已取消（5 个状态，行为差异大）
```

### 当前项目订单状态

```
0-待支付 → 1-已支付 → 2-已发货 → 3-已完成
   ↓
4-已取消
```

```java
// 建议结构（当前项目尚未使用，新增状态逻辑时优先此模式）

// 1. 状态接口 — 仅包含行为判断，next() 返回枚举避免循环依赖
public interface OrderState {
    OrderStatus status();
    boolean canPay();
    boolean canCancel();
    boolean canDeliver();
    boolean canConfirmReceive();
    OrderStatus next(OrderEvent event);
}

// 2. 状态实现 — @Component 无状态单例
@Component
public class PendingPaymentState implements OrderState {
    @Override public OrderStatus status() { return OrderStatus.PENDING_PAYMENT; }
    @Override public boolean canPay() { return true; }
    @Override public boolean canCancel() { return true; }
    @Override public boolean canDeliver() { return false; }
    @Override public OrderStatus next(OrderEvent event) {
        if (event == OrderEvent.PAY) return OrderStatus.PAID;
        if (event == OrderEvent.CANCEL) return OrderStatus.CANCELLED;
        throw new BusinessException(ResultCode.BAD_REQUEST, "待支付状态不支持: " + event);
    }
}

// 3. 状态工厂 — 通过 Spring 自动装配
@Component
public class OrderStateFactory {
    private final Map<OrderStatus, OrderState> stateMap;

    public OrderStateFactory(List<OrderState> states) {
        this.stateMap = states.stream()
            .collect(Collectors.toMap(OrderState::status, Function.identity()));
    }

    public OrderState get(OrderStatus status) {
        OrderState state = stateMap.get(status);
        if (state == null) throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的状态: " + status);
        return state;
    }
}

// 4. 使用 — Entity 中通过 Factory 获取状态实例
OrderState state = orderStateFactory.get(order.getStatus());
if (!state.canPay()) throw new BusinessException(ResultCode.BAD_REQUEST, "当前状态不可支付");
order.setStatus(state.next(OrderEvent.PAY));
```

```yaml
实现要点:
  - 各 State 类为 @Component 无状态单例，仅包含行为判断（canPay/canCancel）
  - next() 返回 OrderStatus 枚举而非 OrderState 实例，避免循环依赖
  - 通过 OrderStateFactory.get(status) 获取状态实例
  - Entity 中通过 Factory 获取状态实例，不直接注入 State 类
```

---

## 4. 责任链模式（Chain of Responsibility）

### 强制使用条件

```yaml
强制使用:
  - 存在多步骤处理流程
  - 多阶段校验（每个校验独立）
  - 多步骤过滤/转换
  - 处理步骤可能动态增减

项目中适用:
  - 订单创建前置校验链（地址校验 → 库存校验 → 金额校验 → 优惠券校验）
  - 请求过滤链（已有 Gateway Filter Chain）
```

### 标准模板

```java
public interface OrderValidationHandler {
    void validate(CreateOrderRequest request, ValidationContext ctx);
    int order(); // 执行顺序
}

@Component
public class StockValidationHandler implements OrderValidationHandler {
    @Override public int order() { return 20; }
    @Override
    public void validate(CreateOrderRequest request, ValidationContext ctx) {
        // 校验库存
    }
}

@Component
public class OrderValidationChain {
    private final List<OrderValidationHandler> handlers;

    public OrderValidationChain(List<OrderValidationHandler> handlers) {
        this.handlers = handlers.stream()
            .sorted(Comparator.comparingInt(OrderValidationHandler::order))
            .toList();
    }

    public void validate(CreateOrderRequest request) {
        ValidationContext ctx = new ValidationContext();
        for (OrderValidationHandler handler : handlers) {
            handler.validate(request, ctx);
            if (ctx.hasError()) break;
        }
    }
}
```

---

## 5. switch-case 治理底线

```yaml
绝对禁止:
  - 核心业务流程中使用 switch-case 做行为分发
  - ≥ 3 个 case 的 switch（必须改为策略模式）
  - action/event/type 字段做 switch 分发
  - 支付逻辑中使用 switch-case
  - MQ 消息路由中使用 switch-case
  - 高扩展模块中使用 switch-case

允许（仅限以下场景）:
  - enum 简单属性转换（如 OrderStatus::getLabel）
  - DTO 字段映射
  - 非扩展性逻辑中的至多 2 个 case
```

---

## 6. AI 生成代码前检查

在生成包含行为分发的代码前，AI 必须自问：

```yaml
1. 分支数 ≥ 3？
   - 是 → 策略模式 + 工厂模式
   - 否 → 继续

2. 属于高扩展模块（支付/MQ事件/状态流转/营销规则/权限/通知）？
   - 是 → 策略模式（即使当前仅 2 个分支）
   - 否 → 允许至多 2 个 if-else

3. 涉及状态流转且状态数 ≥ 4？
   - 是 → 状态模式
   - 否 → 可在 Entity 中用枚举方法处理

4. 新增分支需要修改原有类吗？
   - 是 → 违反 OCP，必须改为策略模式
   - 否 → 设计合规
```

---

## 7. 与现有规则的关联

```yaml
本规则是 architecture-decision.md 的具体展开:
  - architecture-decision.md §3: 高扩展模块清单 → 本规则提供具体模板
  - architecture-decision.md §7: 策略模式使用边界 → 本规则提供实现标准
  - architecture-decision.md §8: 常见决策冲突 → 本规则提供模式组合方案

以下规则与本规则有交叉约束:
  - extensibility-governance.md: 高扩展模块的接口设计和 SPI 机制
  - code-smell-governance.md: 坏味道识别标准与修复优先级
  - ddd-governance.md: 状态模式与 Entity 领域行为的边界

优先级: architecture-decision.md > 本规则 > extensibility-governance.md
```

---

## 8. 模式组合使用指引

```yaml
策略 + 工厂（标配组合）:
  - 策略模式定义 Handler 接口和各实现
  - 工厂模式（HandlerFactory）负责路由到正确 Handler
  - 本项目中所有策略模式必须配套工厂模式

策略 + 状态:
  - 策略模式处理行为分发（如支付方式选择）
  - 状态模式处理状态流转（如订单状态变更）
  - 两者职责分离，不互相替代
  - 例: PayTypeHandler(策略) + OrderState(状态)

责任链 + 策略:
  - 责任链处理多步骤校验流程
  - 链中某个环节如需行为分发，内部使用策略模式
  - 例: OrderValidationChain(责任链) 中某环节使用 CouponTypeHandler(策略)
```

---

## 9. Handler 命名规范

```yaml
接口: {业务领域}Handler（如 PayTypeHandler、CartActionHandler）
实现: {具体类型}Handler（如 WechatPayHandler、BalancePayHandler、AddActionHandler）
工厂: {业务领域}HandlerFactory（如 PayTypeHandlerFactory）
禁止: IHandler 前缀、HandlerImpl 后缀、Handler1/Handler2 编号
```

---

## 10. HandlerFactory 异常处理规范

```yaml
HandlerFactory 异常处理:
  - get() 方法必须处理 handler 为 null 的情况
  - 统一抛出 BusinessException(ResultCode.BAD_REQUEST, "不支持的类型: " + type)
  - 禁止返回 null（调用方需要做 null 检查，增加耦合）
```
