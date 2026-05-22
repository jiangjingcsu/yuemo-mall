---
name: "transaction-analysis"
description: "分析业务操作的事务边界、一致性要求和并发安全问题。当涉及跨模块操作、库存扣减或支付流程时触发。"
---

# 事务一致性分析 Skill

分析业务操作中的事务边界、数据一致性保证和并发安全问题。
> 治理规则：`.harness/rules/mq-governance.md` `.harness/rules/database-governance.md`

## 触发条件

- 涉及多个数据库操作的功能开发
- 涉及跨模块数据一致性
- 库存扣减/支付相关代码
- 用户要求分析事务安全性

## 与 Workflow 的关系

```yaml
定位: 多工作流的事务分析子工具

本 Skill 被以下工作流调用:
  - feature-development.md Step 5 — 新功能涉及跨模块操作时
  - bug-fix.md Step 3 — Bug 涉及数据不一致时
  - refactor.md Step 2 — 重构涉及事务边界变更时

联动 Skill:
  - 上游: skills/microservice-readiness/SKILL.md — 微服务拆分时评估事务耦合
  - 下游: skills/sql-review/SKILL.md — SQL 语句审查
  - 关联: skills/root-cause-analysis/SKILL.md — 数据不一致 Bug 排查
```

## 依赖上下文

- `.harness/docs/data-flow.md` — 核心数据流（下单/支付/退款/库存）
- `.harness/docs/business-domain.md` — 订单/支付状态流转
- `.harness/rules/mq-governance.md` — MQ 事务消息
- `.harness/rules/module-boundary.md` — 模块依赖白名单（跨模块事务识别）
- `.harness/rules/domain-modeling.md` — 充血模型规则（Entity 行为影响事务边界）
- `.harness/memory/decisions.md` — 架构决策（事务策略相关决策）
- `.harness/memory/incidents.md` — 事故记录（事务相关事故）
- 相关 ServiceImpl 源码

## 分析流程

### Step 0: Memory 检查（推荐）

```yaml
读取:
  - memory/decisions.md — 是否有事务策略相关决策？
  - memory/incidents.md — 是否有事务相关事故记录？
原则: 不重复已有事务分析，从事故中学习
```

### Step 1: 识别事务边界

```yaml
分析:
  - 操作涉及哪些数据库表？
  - 这些表属于同一个模块还是多个模块？
  - 是否需要同时成功或同时失败？
  - 是否有外部调用（RPC/MQ/HTTP）？

规则:
  - 单模块内多表操作 → 本地事务 @Transactional
  - 跨模块操作 → MQ 最终一致性
  - 本地操作 + MQ 发送 → 事务消息
```

### Step 2: 一致性策略选择

| 场景 | 策略 | 项目实例 |
|---|---|---|
| 单表操作 | 本地事务 | 用户更新个人信息 |
| 单模块多表 | @Transactional | 订单: 创建订单+创建订单项+创建订单日志 |
| 跨模块本地事务（单体架构） | @Transactional + MQ 异步通知 | 余额支付: 扣余额+创建支付单(@Transactional) → payment-callback MQ → 更新订单状态 |
| 跨模块+MQ | RocketMQ 事务消息 | 下单: 创建订单+库存预占 |
| 跨模块异步 | MQ 普通消息+幂等 | 取消: 恢复库存, 支付回调通知 |
| 缓存+DB | Redis 主存储 + MQ 异步落库 | 购物车: Redis→MQ→MySQL |

> ⚠️ **治理标记**: 余额支付的跨模块本地事务在当前单体架构下可接受，但微服务拆分时需治理：
> - PaymentServiceImpl.createPayment() 的 @Transactional 跨 payment + user 模块
> - BalancePayHandler.doPay() 在事务内调用 UserService.deductBalance() + 发送 MQ
> - 拆分后应改为 MQ 事务消息或 Saga 模式

### Step 3: 并发安全分析

```yaml
检查:
  - 是否有并发操作同一资源？（库存扣减、余额扣减）
  - 是否使用了乐观锁/悲观锁？
  - MQ 消费者是否幂等？

当前项目并发安全措施:
  库存扣减: UPDATE yu_product SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}（原子 SQL）
  余额扣减: UPDATE yu_user SET balance = balance - #{amount} WHERE id = #{userId} AND balance >= #{amount}（原子 SQL）
  支付回调: Redis setIfAbsent 幂等（payment:callback:{paymentNo}）
  购物车同步: INSERT ON DUPLICATE KEY UPDATE（原子 upsert）
  领券去重: Redis setIfAbsent（coupon:receive:{userId}:{couponId}）

缺失检查:
  - 是否有未加锁的扣减操作？
  - 是否有未做幂等的 MQ 消费者？
  - 是否有 SELECT-then-UPDATE 竞态条件？
```

### Step 4: 异常回滚分析

```yaml
分析：
  - 操作失败时如何回滚？
  - 回滚是否完整（所有影响都撤销）？
  - 中间状态是否可见？

当前项目回滚机制:
  订单取消: 订单状态→已取消 + MQ 释放库存 + 释放优惠券
  支付失败: 抛 BusinessException，事务自动回滚
  库存预占失败: 事务消息 ROLLBACK，订单不创建

注意:
  - MQ 发送后无法回滚（需补偿操作）
  - Redis 操作无回滚（需设计清理机制）
```

### Step 5: 输出分析报告

```markdown
## 事务分析报告

### 操作流程
{步骤1} → {步骤2} → {步骤3}

### 一致性策略
本地事务: {哪些操作在一个事务中}
MQ 事务消息: {哪些通过事务消息保证}
MQ 普通消息: {哪些通过幂等保证最终一致}

### 并发安全评估
| 资源 | 锁策略 | 安全？ |
|---|---|---|

### 回滚/补偿
| 操作 | 失败时如何回滚 | 完整？ |
|---|---|---|

### 风险评估
- {风险} — {缓解措施}
```

## 约束

- 跨模块操作在单体架构下可用本地事务，但需标记为微服务拆分治理点
- 跨进程/跨服务的操作不能用本地事务
- 事务方法应设置合理超时（@Transactional(timeout = 10)）
- 库存/余额扣减必须有并发保护
- MQ 消费者必须幂等
- 不在事务中调用外部 RPC/HTTP
