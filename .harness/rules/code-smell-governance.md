# 代码坏味道治理规则

> 让 AI Agent 自动识别代码坏味道（Code Smell），发现后主动提示风险、推荐重构方案、推荐设计模式。基于项目实际代码模式制定。
> 实操技能：`.harness/skills/code-verify/SKILL.md` `.harness/skills/refactor-analysis/SKILL.md`

---

## 1. AI 必须识别的坏味道

### 1.1 超长 switch-case（Switch Statements）

```yaml
识别标准:
  传统 switch-case: 分支 ≥ 3
  Java 14+ switch 表达式: 分支 ≥ 3（风险较低但仍建议优化）
检测关键词: switch ( / return switch
风险等级:
  传统 switch-case: HIGH
  switch 表达式: MEDIUM
影响: 违反 OCP，新增 case 需要修改原类，难以测试

项目中已存在:
  MySqlSearchServiceImpl.java — return switch(sortBy) 4 个 case（switch 表达式，建议抽取排序策略 Map）

已修复（正面案例）:
  CartSyncConsumer.java — 原 switch(msg.action()) 5 个 case → 已重构为 CartActionHandler + handlerMap 策略模式

AI 发现后必须:
  - 提示: "检测到 {N} 个 case 的 switch 分发，建议使用策略模式"
  - 推荐: ActionHandler 接口 + HandlerFactory + @Component 注册
  - 输出: 重构方案（包含 Handler 接口、各实现类、Factory）
  - 优先: 生成策略模式代码而非 switch-case
```

### 1.2 if-else 地狱（If-Else Chain）

```yaml
识别标准: 连续 ≥ 3 层 if-else
检测关键词: else if 重复出现
风险等级: HIGH
影响: 可读性差，分支逻辑耦合，难以扩展

AI 发现后必须:
  - 提示: "检测到 {N} 层 if-else 链，建议使用策略模式或提前返回"
  - 推荐: 策略模式（行为分发）/ 提前返回（条件校验）
  - 输出: 重构方案
```

### 1.3 God Service（上帝服务）

```yaml
识别标准（满足任一即触发）:
  - ServiceImpl > 500 行
  - 单个 Service 处理 ≥ 3 种不相关业务
  - 注入 ≥ 5 个 Mapper
  - 方法超过 50 行

检测关键词: 文件行数、@RequiredArgsConstructor 注入数量
风险等级: CRITICAL
影响: 难维护、难测试、难拆分、阻碍微服务化

项目中已存在:
  ProductServiceImpl — 注入 10 个依赖（6 Mapper + 4 Service），严重超标

AI 发现后必须:
  - 提示: "检测到 God Service: {类名}，建议按职责拆分"
  - 推荐: 按业务职责拆分为多个 Service
  - 输出: 拆分方案
```

### 1.4 巨型 Controller

```yaml
识别标准: Controller 方法 > 30 行 或 Controller 包含业务逻辑
风险等级: HIGH
影响: 业务逻辑泄漏到接入层

AI 发现后必须:
  - 将业务逻辑移到 ServiceImpl
  - Controller 只保留：参数校验 + 调用 Service + 返回 Result
```

### 1.5 超长方法（Long Method）

```yaml
识别标准: 单个方法 > 50 行
风险等级: MEDIUM
影响: 难以理解，职责不清

AI 发现后必须:
  - 提取为多个 private 方法
  - 每个方法职责单一
```

### 1.6 Mapper 横飞

```yaml
识别标准:
  - ServiceImpl 注入 ≥ 5 个 Mapper
  - 跨模块注入 Mapper
  - Controller 直接注入 Mapper（分层违规）
风险等级: HIGH
影响: 模块边界模糊，数据层泄漏

项目中已存在:
  ProductController — 直接注入 SearchKeywordMapper，违反 Controller → Service → Mapper 分层

AI 发现后必须:
  - 跨模块 Mapper → 改为调用对方 Service 接口
  - Controller 直接注入 Mapper → 通过 Service 接口封装
  - 多个 Mapper → 评估是否需要拆分 Service
```

### 1.7 重复 SQL

```yaml
识别标准: 相同结构的 SQL 出现在 ≥ 3 处
风险等级: MEDIUM
影响: SQL 变更需要改多处

AI 发现后必须:
  - 提取到 Mapper 方法
  - 使用 LambdaQueryWrapper 封装通用条件
```

### 1.8 魔法值

```yaml
识别标准: 代码中出现字面量数字/字符串
检测关键词: == 0, == 1, == 3（状态值/类型值）
风险等级: MEDIUM
影响: 代码难读，值变更需要全局搜索

示例:
  if(payType == 3)  → 应改为 if(payType == PayType.BALANCE.getValue())
  if(status == 0)   → 应改为 if(status == OrderStatus.PENDING_PAYMENT.getValue())

AI 发现后必须:
  - 建议使用 enum 或常量类替换
```

### 1.9 硬编码配置

```yaml
识别标准: 代码中硬编码 URL、端口、路径、超时值
风险等级: MEDIUM
影响: 环境切换困难

AI 发现后必须:
  - 建议移到 application.yml 配置
  - 使用 @Value 或 @ConfigurationProperties 注入
```

### 1.10 重复 DTO

```yaml
识别标准: 两个 DTO 类字段完全相同
风险等级: LOW
影响: 冗余代码，维护成本翻倍

AI 发现后必须:
  - 合并为共享 DTO
  - 或确认是否有语义差异需要保留两份
```

### 1.11 异常吞没（Swallowed Exception）

```yaml
识别标准:
  - catch 块为空或仅含注释: catch (Exception ignored) {} / catch (Exception e) { /* ignore */ }
  - catch 块仅 log.debug 但不记录异常堆栈: catch (Exception e) { log.debug("msg: {}", param); }
  - catch 块静默返回默认值且调用方无法区分正常与异常: catch (Exception e) { return false; }
风险等级: HIGH
影响: 异常被隐藏，问题难以排查，可能导致数据不一致

项目中已存在:
  ProductServiceImpl — 2 处 catch (Exception ignored) {}，品牌名获取失败完全静默
  MySqlSearchServiceImpl — catch 中 log.debug 未记录异常堆栈 e
  PasswordEncoder — catch 中静默 return false，无法区分密码错误和 BCrypt 异常

AI 发现后必须:
  - 空 catch → 至少添加 log.warn 记录异常
  - log.debug 未记录堆栈 → 补充异常对象: log.warn("描述: {}", param, e)
  - 静默返回默认值 → 区分业务异常和系统异常，或向上抛出
```

---

## 2. AI 行为规范

### 发现坏味道时

```yaml
AI 必须:
  1. 主动识别并标记坏味道
  2. 说明风险等级和影响
  3. 推荐具体的设计模式或重构方案
  4. 在生成新代码时，避免产生坏味道

AI 禁止:
  - 忽略坏味道继续开发
  - 在坏味道基础上堆砌更多代码
  - 声称"先这样，以后再说"（当前不用以后也不会改）
```

### 生成代码前自检

```yaml
AI 在生成代码前必须检查:
  - [ ] 这段代码会产生 switch-case（≥3 case）吗？
  - [ ] 这段代码会产生 if-else 链（≥3 层）吗？
  - [ ] 这个 Service 会超过 500 行吗？
  - [ ] 这个方法会超过 50 行吗？
  - [ ] 这个 Controller 包含业务逻辑吗？
  - [ ] 这段代码使用了魔法值吗？
  - [ ] 这段代码跨模块调 Mapper 了吗？
  - [ ] Controller 中直接注入 Mapper 了吗？
  - [ ] catch 块是否吞没了异常（空 catch / 未记录堆栈 / 静默返回）？

  任一为"是" → 重新设计
```

---

## 3. 坏味道与设计模式映射

| 坏味道 | 推荐设计模式 | 项目中的例子 |
|---|---|---|
| 超长 switch | 策略模式 + 工厂模式 | ✅ CartSyncConsumer → CartActionHandler（已修复） / MySqlSearchServiceImpl switch 表达式（待优化） |
| if-else 地狱 | 策略模式 / 提前返回 | ✅ PaymentServiceImpl → PayTypeHandler（已修复） |
| God Service | 按职责拆分 Service | ProductServiceImpl（10 依赖，待拆分） |
| 状态 if 嵌套 | 状态模式 / 实体方法模式 | ✅ OrderServiceImpl → order.cancel()/pay()/ship()（已修复） |
| 重复校验 | 责任链模式 | 订单创建前置校验 → ValidationChain |
| Mapper 横飞 | 仓储模式 / Service 封装 | ProductController → SearchKeywordMapper（待修复） |
| 异常吞没 | 至少 log.warn + 区分异常类型 | ProductServiceImpl catch(Exception ignored)（待修复） |
| 魔法值 | enum / 常量类 | getStatus()!=1 → 待创建 ProductStatus/SkuStatus/BrandStatus/ReviewStatus 枚举 |

---

## 4. 已有坏味道修复优先级

```yaml
已修复（正面案例参考）:
  - ✅ CartSyncConsumer switch-case → 策略模式（CartActionHandler + handlerMap）
  - ✅ PaymentServiceImpl payType if-else → 策略+工厂（PayTypeHandler + handlerMap）
  - ✅ OrderServiceImpl 状态 if 嵌套 → 实体方法模式（order.cancel()/pay()/ship()）

P0（本次改造修复）:
  - ProductServiceImpl God Service（10 依赖）→ 拆分为 ProductService + ProductQueryService + ProductAdminService
  - ProductServiceImpl 异常吞没（2 处 catch ignored）→ 至少添加 log.warn
  - ProductController 直接注入 SearchKeywordMapper → 通过 SearchService 封装

P1（下次迭代修复）:
  - MySqlSearchServiceImpl switch 表达式 → 抽取排序策略 Map
  - 魔法值替换 → 创建 ProductStatus / SkuStatus / BrandStatus / ReviewStatus 枚举
  - PaymentController handleCallback(1/2) → 使用 PayType 枚举常量
  - MySqlSearchServiceImpl catch 未记录异常堆栈 → 补充 log.debug("...", e)

P2（技术债务跟踪）:
  - ServiceImpl 行数统计，持续监控 God Service
  - 重复代码扫描
```
