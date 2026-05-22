# 反模式记录

> 已识别的反模式。AI 不得重复这些错误模式。
> 状态唯一源（SSOT）: `memory/known-issues.md` — 反模式修复状态以 known-issues.md 为准，本文件提供详细描述和 AI 禁止规则。

---

## 反模式清单

### AP001: switch-case 行为分发

```yaml
位置: CartSyncConsumer.java
问题: switch(msg.action()) 分发 5 种操作
风险: 新增 action 需要修改 Consumer 类（违反 OCP）
状态: 已修复 — 重构为 handlerMap 策略模式（CartActionHandler 接口 + 按 messageType 路由）
修复日期: 2026-05
修复方式:
  - CartSyncConsumer 构造器自动收集所有 CartActionHandler 实现
  - handlerMap.get(msg.getClass()) 替代 switch-case
  - 新增 action 只需添加 Handler 实现类，无需修改 Consumer
AI 禁止: 在类似场景（MQ消息路由、支付方式、状态流转）使用 switch-case
```

### AP002: if-else 支付方式判断

```yaml
位置: PaymentServiceImpl.java
问题: if(payType == 3) 判断余额支付
风险: 新增微信/支付宝支付时需要修改 ServiceImpl
状态: 已修复 — 重构为 PayTypeHandler 策略模式
修复日期: 2026-05
修复方式:
  - PayTypeHandler 接口 + handlerMap 按 payType 路由
  - 3 个实现: WechatPayHandler(1), AlipayPayHandler(2), BalancePayHandler(3)
  - PaymentServiceImpl.createPayment() 通过 handlerMap.get(payType) 分发
AI 禁止: 在支付/营销等高扩展模块使用 if-else 链
```

### AP003: 贫血 Entity

```yaml
位置: 多个模块 Entity
问题: Entity 只有 @Data getter/setter，状态流转逻辑在 ServiceImpl 中
风险: 状态校验分散，容易遗漏
状态: 部分修复 — Order/Payment 已迁移为充血模型，其他 Entity 仍贫血
修复日期: 2026-05（Order/Payment）
已充血:
  - Order.java: verifyOwnership, ensureStatus, pay, ship, confirmReceive, cancel, canDelete
  - Payment.java: verifyOwnership, isPending, isSuccess, markSuccess, markFailed, markRefunded
仍贫血:
  - Product.java, CartItem.java, User.java, Address.java 等
AI 禁止: 新建 Entity 只包含数据字段，行为必须封装在 Entity 或 DomainService 中
```

### AP004: 魔法值

```yaml
位置: 多处
问题: payType == 3, status == 0 等字面量判断
风险: 代码难读，值变更需要全局搜索
状态: 部分修复 — 核心模块已消除，边缘模块仍有残留
修复日期: 2026-05（核心模块）
已消除:
  - PayType 枚举(WECHAT=1, ALIPAY=2, BALANCE=3) + fromCode() 工厂方法
  - PaymentStatus 枚举(PENDING=0, SUCCESS=1, FAILED=2, REFUNDED=3)
  - OrderStatus 枚举(UNPAID=0, PAID=1, SHIPPED=2, COMPLETED=3, CANCELLED=4, REFUNDED=5)
  - Order/Payment 核心模块无字面量数字判断
仍有残留:
  - SkuServiceImpl.java:67 — sku.setStatus(1)，缺少 SkuStatus 枚举
  - UserServiceImpl.java:47 — user.setStatus(0)，缺少 UserStatus 枚举
  - ReviewServiceImpl.java:88 — review.setStatus(1)，缺少 ReviewStatus 枚举
AI 禁止: 使用字面量数字/字符串表示状态/类型
```

### AP005: SELECT-then-INSERT 竞态

```yaml
位置: 购物车加购（已修复）
问题: 先 SELECT 检查是否存在，再 INSERT/UPDATE
风险: 并发重复插入，SQLIntegrityConstraintViolationException
修复: INSERT ... ON DUPLICATE KEY UPDATE（原子 upsert）
状态: 已修复
关联事故: INC001
AI 禁止: 在所有并发场景使用 SELECT-then-UPDATE 模式
```

### AP006: 唯一键不含 deleted

```yaml
位置: 购物车表（已修复）
问题: UNIQUE KEY(user_id, sku_id) 不含 deleted 字段
风险: 逻辑删除后无法再次插入相同的 user_id+sku_id
修复: UNIQUE KEY(user_id, sku_id, deleted)
状态: 已修复
关联事故: INC001
AI 禁止: 创建唯一键时遗漏 deleted 字段
```

### AP007: N+1 查询

```yaml
位置: 多处
问题: 循环内逐条查询数据库，产生 N+1 次查询
风险: 数据库压力大，响应慢，核心链路性能瓶颈
状态: 已识别，待修复
严重:
  - OrderServiceImpl.createOrder: 循环内逐条查询 Product + SKU（2N 次查询）
    已有批量方法 batchGetProductsByIds/batchGetSkusByIds 但未使用
  - MySqlSearchServiceImpl.buildTagMap: 循环内逐个查询标签（N 次查询）
    同文件 buildBrandNameMap 已正确使用批量查询
中等:
  - ProductServiceImpl.enrichProductVO: 循环内逐个查询品牌名
  - ProductServiceImpl.buildSpecGroups: 循环内逐个查询规格值
AI 禁止: 在循环内执行数据库查询，必须使用批量查询（IN 查询或批量方法）
```

### AP008: 异常吞噬

```yaml
位置: 多处
问题: catch 后不记录日志或完全忽略异常
风险: 问题难以排查，错误被静默吞掉
状态: 已识别，待修复
严重:
  - ProductServiceImpl.java:77,101 — catch (Exception ignored) {} 完全吞掉品牌名查询异常
中等:
  - PasswordEncoder.java:17-21 — catch 后静默返回 false，无法区分密码错误和数据损坏
低:
  - GatewayAuthFilter.java:55, CircuitBreakerFilter.java:43 — catch 后 throw RuntimeException 无日志
AI 禁止: 空 catch 块或 catch 后不记录日志（至少 log.warn）
```

### AP009: 循环内逐条写入

```yaml
位置: 多处
问题: 循环内逐条 INSERT/UPDATE，未使用 MyBatis-Plus 批量操作
风险: 数据库往返次数多，性能差
状态: 已识别，待修复
位置清单:
  - OrderServiceImpl:117-120 — 逐条 INSERT OrderItem
  - SkuServiceImpl:58-69 — 逐条 INSERT SKU
  - ProductServiceImpl:318-323 — 逐条 INSERT TagRel
  - AddressServiceImpl:73-76 — 循环内逐条 UPDATE（可用单条 UPDATE WHERE 替代）
AI 禁止: 循环内逐条 INSERT/UPDATE，使用 MyBatis-Plus 批量方法或单条 SQL 替代
```
