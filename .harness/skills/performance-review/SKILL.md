---
name: "performance-review"
description: "审查代码的性能问题，识别慢查询、N+1查询、缓存缺失、并发安全、大事务、不合理的资源使用。当涉及性能优化或发现响应缓慢时触发。"
---

# 性能审查 Skill

对代码进行性能层面的审查，识别潜在瓶颈和优化机会。
> 治理规则：`.harness/evaluation/code-quality.md` `.harness/rules/database-governance.md`

## 触发条件

- 用户反馈系统响应慢
- 新增涉及大量数据操作的功能
- 代码中包含循环/批量操作
- 用户要求性能优化
- 代码审查时发现性能隐患
- 重构工作流中需要性能基线评估
- Bug 修复中涉及性能类问题（慢查询、超时、OOM）

## 与 Workflow 的关系

本 Skill 是以下 Workflow 的子工具，由 Workflow 在特定步骤激活：

| Workflow | 激活时机 | 激活步骤 |
|---|---|---|
| `.harness/workflows/refactor.md` | Step 2 影响分析 — 评估重构前性能基线；Step 7 Evaluation — 验证重构后性能未退化 | Step 2 / Step 7 |
| `.harness/workflows/bug-fix.md` | Step 1 Bug 分类为"性能问题"时激活；Step 2 根因分析中按类型激活 | Step 1 / Step 2 |

独立使用时，按本 Skill 自身流程执行；被 Workflow 调用时，仅执行 Workflow 指定的步骤子集。

## 依赖上下文

### 治理规则

- `.harness/rules/database-governance.md` — SQL 性能规范
- `.harness/rules/redis-governance.md` — 缓存策略
- `.harness/rules/domain-modeling.md` — 领域建模规则（Entity 行为影响查询模式：贫血模型导致 ServiceImpl 中散落多次查询，充血模型可将关联查询内聚）
- `.harness/rules/module-boundary.md` — 模块边界规则（跨模块 Service 调用是性能风险点：每次跨模块调用 = 一次远程调用的模拟，未来微服务拆分后变为网络开销）

### 子 Skill（深度审查时激活）

| 子 Skill | 激活条件 | 审查深度 |
|---|---|---|
| `.harness/skills/sql-review/SKILL.md` | Step 1 发现慢查询/索引问题 | SQL 语句、表设计、索引策略全量审查 |
| `.harness/skills/transaction-analysis/SKILL.md` | Step 3 发现事务/并发问题 | 事务边界、隔离级别、锁竞争深度分析 |
| `.harness/skills/cache-design/SKILL.md` | Step 2 发现缓存缺失/设计问题 | 多级缓存方案、一致性策略、预热刷新设计 |
| `.harness/skills/redis-design/SKILL.md` | Step 2 发现 Redis 单 Key 问题 | Key 设计、TTL、数据结构选择 |

### 跨 Skill 联动

| 审查发现 | 联动 Skill | 联动原因 |
|---|---|---|
| 慢查询 / 索引缺失 / 全表扫描 | sql-review | 需要完整的 SQL + 索引审查 |
| 缓存缺失 / 缓存穿透 / 击穿 / 雪崩 | cache-design | 需要设计完整缓存方案 |
| Redis Key 设计不合理 / 大 Key / 热点 Key | redis-design | 需要 Redis 专项设计 |
| 事务内远程调用 / 先 select 后 update | transaction-analysis | 需要事务边界和并发安全深度分析 |
| 跨模块 Service 循环调用 / 跨模块 JOIN | module-boundary | 跨模块调用是性能风险，也是架构违规 |
| Entity 贫血导致多次查询 / ServiceImpl 散落查询 | domain-modeling | 贫血模型导致查询散落，充血模型可内聚 |

## 优先级排序

```yaml
审查发现按以下优先级排序:
  P0-数据正确性: 并发超卖/超扣（必须立即修复）
  P1-可用性: 大事务含远程调用/第三方调用（可能导致雪崩）
  P2-性能: N+1 查询/循环逐条写入/缓存缺失（影响响应时间）
  P3-体验: 前端懒加载/代码分割/串行请求（影响用户体验）
  P4-优化: 连接池调优/索引优化/缓存命中率（锦上添花）
```

## 项目已知性能问题

> 以下问题已在代码中确认存在，审查时应优先关注是否恶化或新增同类问题。

### 后端已知问题

| 问题 | 位置 | 优先级 | 状态 |
|---|---|---|---|
| N+1 远程调用 | OrderServiceImpl.createOrder() 循环内调 productService/skuService | P2 | 待修复 |
| 循环逐条 INSERT | OrderServiceImpl.createOrder() orderItemMapper.insert() | P2 | 待修复 |
| 循环逐条 INSERT | ProductServiceImpl.createProduct() tagRelMapper.insert() | P2 | 待修复 |
| 循环逐条 INSERT | SkuServiceImpl.createSkus() skuMapper.insert() | P2 | 待修复 |
| 循环逐条 UPDATE | AddressServiceImpl.setDefault() addressMapper.updateById() | P2 | 待修复 |
| 大事务含远程调用 | OrderServiceImpl.createOrder() 事务内跨服务调用 | P1 | 待修复 |
| 大事务含第三方调用 | PaymentServiceImpl.createPayment() 事务内调支付网关 | P1 | 待修复 |
| 库存扣减无并发保护 | ProductServiceImpl.updateStock() 先 select 后 update 无锁 | P0 | 已修复 → 原子SQL |
| SKU 库存扣减无并发保护 | SkuServiceImpl.updateSkuStock() 先 select 后 update 无锁 | P0 | 已修复 → 原子SQL |
| 余额扣减无并发保护 | UserServiceImpl.deductBalance() 先 select 后 update 无锁 | P0 | 已修复 → 原子SQL |
| Redis 循环写入 | CartServiceImpl.selectAll() 循环 hashOps.put() 未用 Pipeline | P2 | 待修复 |
| 项目未使用 saveBatch | 所有批量操作均为逐条写入 | P2 | 待修复 |

### 前端已知问题

| 问题 | 说明 | 优先级 | 状态 |
|---|---|---|---|
| 无图片懒加载 | 商城图片密集，所有图片同步加载 | P3 | 待修复 |
| 无路由级代码分割 | 未使用 React.lazy/Suspense | P3 | 待修复 |
| 无虚拟滚动 | 商品/订单长列表无虚拟化 | P3 | 待修复 |
| 串行请求 | 仅 ProductDetail 用了 Promise.all，其余串行 | P3 | 待修复 |

## 审查流程

### Step 0: Memory 检查（强制）

> 审查开始前必须读取 Memory 文件，避免重复踩坑和忽略已知问题。

```yaml
必须读取:
  - .harness/memory/decisions.md — 架构决策（如 D001 单体多模块影响跨模块调用模式）
  - .harness/memory/anti-patterns.md — 已知反模式（如 AP005 SELECT-then-INSERT 竞态）
  - .harness/memory/known-issues.md — 已知技术债务（如 TD003 贫血 Entity 导致查询散落）

读取目的:
  - decisions.md: 理解架构约束（单体多模块 → 跨模块调用不走 RPC 但仍需关注性能）
  - anti-patterns.md: 避免重复报告已知反模式，关注是否新增同类问题
  - known-issues.md: 已知问题是否恶化，是否有新增同类问题

输出: 简要记录与本次审查相关的 Memory 条目编号
```

### Step 1: 数据库查询审查

> 深度审查引用 `.harness/skills/sql-review/SKILL.md`

#### 1.1 慢查询检测

```yaml
量化阈值:
  慢查询定义: 执行时间 > 500ms（项目标准，非 MySQL 默认 10s）
  全表扫描: EXPLAIN type = ALL 且 rows > 1000
  索引未命中: EXPLAIN type = ALL / index_merge（需确认是否合理）
  临时表: EXPLAIN Extra 包含 Using temporary
  文件排序: EXPLAIN Extra 包含 Using filesort 且 rows > 500

EXPLAIN type 要求（从优到劣）:
  system > const > eq_ref > ref > range > index > ALL
  最低要求: ref（等值查询）/ range（范围查询）
  不可接受: ALL（全表扫描，除非表行数 < 100）

检测方法:
  1. 识别所有 Mapper 方法和 LambdaQueryWrapper 调用
  2. 对涉及大表（> 1000 行）的查询，要求提供 EXPLAIN 结果
  3. 关注 WHERE 条件字段是否有对应索引
  4. 关注 ORDER BY / GROUP BY 字段是否利用索引排序
```

#### 1.2 N+1 查询检测

```yaml
检测方法:
  1. 搜索 for / forEach / stream().map() 循环体
  2. 检查循环体内是否调用了 Mapper 方法或 Service 查询方法
  3. 检查是否存在"先查主表列表，再循环查子表"模式

N+1 判定标准:
  - 循环内调用 Mapper.selectXxx() / Service.getById() → 确认 N+1
  - 循环内调用 Service.listByIds() 但每次只传 1 个 ID → 确认 N+1
  - 循环内调用远程 Service → 确认 N+1（且更严重，未来微服务化为网络开销）

项目实际 N+1 模式:
  改前:
    for (CreateOrderDTO.OrderItemDTO itemDTO : dto.getItems()) {
        Product product = productService.getProductById(itemDTO.getProductId());
        ProductSku sku = skuService.getSkuById(skuId);
    }
  改后:
    List<Long> productIds = dto.getItems().stream().map(CreateOrderDTO.OrderItemDTO::getProductId).toList();
    Map<Long, Product> productMap = productService.listByIds(productIds).stream()
        .collect(Collectors.toMap(Product::getId, p -> p));
```

#### 1.3 MyBatis-Plus 特有检查

```yaml
检查项:
  - [ ] 循环逐条 INSERT/UPDATE → 是否改用 saveBatch()/updateBatchById()？
  - [ ] 分页查询是否使用 Page 对象？
  - [ ] LambdaQueryWrapper 是否指定了 select 字段（避免 SELECT *）？
  - [ ] @Cacheable 注解的方法返回值是否为 VO（禁止缓存 Entity）？
  - [ ] 逻辑删除字段(deleted)是否导致索引失效？（唯一键含 deleted，查询条件含 deleted）

优化建议:
  - N+1 → 批量查询 listByIds() + 内存 Map 关联
  - SELECT * → LambdaQueryWrapper.select()
  - 循环逐条 INSERT → saveBatch()（batchSize 建议 500）
  - 循环逐条 UPDATE → updateBatchById() 或单条 SQL 批量更新
```

#### 1.4 领域建模对查询的影响

```yaml
依赖: .harness/rules/domain-modeling.md

贫血模型导致的性能问题:
  - Entity 无行为 → ServiceImpl 中散落多次查询（先查状态再操作）
  - 状态校验在 ServiceImpl → 多次 if-else 分支各自查询
  - 跨聚合操作在 ServiceImpl → 循环内跨模块 Service 调用

充血模型的性能优势:
  - Entity 内聚行为 → 单次查询后内存中完成状态判断
  - 聚合根控制内部一致性 → 减少跨聚合查询
  - DomainService 处理跨聚合 → 显式标记性能关注点

审查关注:
  - ServiceImpl 中是否存在"查了又查"同一实体的不同字段？
  - 是否因贫血模型导致同一业务流程中多次查询同一张表？
```

### Step 2: 缓存审查

> 深度审查引用 `.harness/skills/cache-design/SKILL.md`（多级缓存场景）和 `.harness/skills/redis-design/SKILL.md`（单 Key 场景）

#### 2.1 声明式缓存检查

```yaml
检查项:
  - [ ] 高频读取数据是否使用 @Cacheable？
  - [ ] 写操作是否配置了 @CacheEvict？
  - [ ] @Cacheable 缓存的返回值是否为 VO（禁止缓存 Entity）？
  - [ ] @Cacheable 的 key 是否合理（避免缓存穿透）？
  - [ ] @Cacheable 的 unless 条件是否正确（避免缓存空值导致穿透）？

项目缓存场景:
  商品详情: @Cacheable(key = "'product:detail:' + #id") → 高频读取，适合缓存
  优惠券模板: @Cacheable(key = "'coupon:template:' + #id") → 高频读取，适合缓存
  用户信息: 当前未缓存 → 可考虑 @Cacheable
```

#### 2.2 编程式缓存检查

```yaml
检查项:
  - [ ] 缓存 TTL 是否设置？（redis-governance.md 强制）
  - [ ] 是否有防穿透措施？（缓存空值 + 短 TTL / 布隆过滤器）
  - [ ] 是否有防击穿措施？（互斥锁 / 逻辑过期 / 预热）
  - [ ] 是否有防雪崩措施？（TTL 加随机偏移量）
  - [ ] 是否有热点 Key 风险？（单 Key QPS > 5000）
  - [ ] 是否有大 Key 风险？（Value > 10KB 需关注，> 1MB 必须拆分）
  - [ ] Redis 批量操作是否使用了 Pipeline？

项目实际缓存问题:
  ❌ CartServiceImpl.selectAll(): 循环 hashOps.put() 未用 Pipeline
     修复: 使用 hashOps.putAll() 或 Redis Pipeline

  ❌ CouponServiceImpl.receiveCoupon(): 事务内混合 Redis 操作 + DB 读写
     修复: Redis 幂等检查移到事务外

缓存设计决策树（引用 cache-design Skill）:
  读多写少 + 允许短暂不一致 → 需要缓存
  读少写多 → 不需要缓存
  强一致性要求 → 不适合 Redis 缓存，考虑本地缓存 + DB
```

#### 2.3 购物车 Redis 专项

```yaml
架构决策 D004: Redis Hash 做购物车主存储
  写: Redis → MQ → MySQL（cart-sync）
  读: Redis 优先，miss 查 MySQL
  清理: 定时任务凌晨 3 点

审查关注:
  - [ ] 购物车 Hash 操作是否使用 Pipeline？
  - [ ] 购物车同步 MQ 是否有积压风险？
  - [ ] 购物车 miss 回查 MySQL 是否有 N+1？
  - [ ] 购物车清理任务是否影响在线用户？
```

### Step 3: 事务与并发审查

> 深度审查引用 `.harness/skills/transaction-analysis/SKILL.md`

#### 3.1 事务边界检查

```yaml
量化阈值:
  大事务定义: 事务执行时间 > 2s 或 操作表数 > 3
  事务超时: @Transactional(timeout = ?) 是否设置？（建议 5s）
  连接占用: 事务持有 DB 连接时间 > 3s 即为风险

检查项:
  - [ ] 事务内是否包含远程调用（跨服务 HTTP/RPC）？
  - [ ] 事务内是否包含第三方调用（支付网关/短信/对象存储）？
  - [ ] 事务内是否包含 Redis 操作？
  - [ ] 事务内是否包含 MQ 发送？
  - [ ] 事务是否超过 3 张表操作？
  - [ ] 事务预计耗时是否 > 2s？

项目实际事务反模式:
  ❌ OrderServiceImpl.createOrder():
     事务内: for 循环远程调用 + for 循环逐条 insert + cartService 远程调用
     风险: 远程调用失败导致长事务回滚，连接池耗尽
     修复: 远程调用移到事务外，事务内只做本地 DB 操作

  ❌ PaymentServiceImpl.createPayment():
     事务内: orderService 远程调用 + 第三方支付调用
     风险: 支付网关超时导致事务长时间持有 DB 连接
     修复: 第三方调用移到事务外，事务内只记录支付单

  ❌ CouponServiceImpl.receiveCoupon():
     事务内混合 Redis 操作 + DB 读写
     风险: Redis 操作失败导致事务回滚
     修复: Redis 幂等检查移到事务外
```

#### 3.2 并发安全检查

```yaml
检查项:
  - [ ] 是否存在"先 select 后 update"模式？（AP005 反模式）
  - [ ] 库存/余额扣减是否有并发保护？
  - [ ] 优惠券领取是否有幂等保护？
  - [ ] 是否需要乐观锁（version 字段）？
  - [ ] 是否需要分布式锁（setIfAbsent）？

项目实际并发问题:
  ❌ ProductServiceImpl.updateStock():
     已修复 → CAS 原子更新

  ❌ SkuServiceImpl.updateSkuStock():
     已修复 → CAS 原子更新

  ❌ UserServiceImpl.deductBalance():
     已修复 → CAS 原子更新

优化建议:
  - 远程调用/第三方调用 → 移到事务外，事务内只做本地 DB 操作
  - 先 select 后 update → 改为 CAS 原子更新（UPDATE ... WHERE stock >= qty）
  - 事务内 Redis → 移到事务外，Redis 操作不依赖事务回滚
  - 事务内 MQ → 使用事务消息或事务提交后发送（@TransactionalEventListener）
```

#### 3.3 跨模块调用的事务风险

```yaml
依赖: .harness/rules/module-boundary.md

跨模块 Service 调用的事务风险:
  - 当前单体架构下跨模块调用 = 本地方法调用，但事务传播行为需注意
  - 未来微服务化后 = 网络调用，事务内跨服务调用 = 分布式事务
  - 审查关注: 事务内调用其他模块 Service 时，被调用方是否也开启了事务？
  - 传播行为: 默认 REQUIRED，可能意外加入外层事务导致长事务

模块依赖白名单中的性能风险点:
  - yuemo-order → yuemo-product(Service): createOrder 中循环调用 productService
  - yuemo-order → yuemo-cart(Service): createOrder 中调用 cartService
  - yuemo-payment → yuemo-order(Service): createPayment 中调用 orderService
  - yuemo-payment → yuemo-user(Service): createPayment 中调用 userService
```

### Step 4: 循环与批量操作审查

```yaml
检查项:
  - [ ] for 循环中是否有数据库调用？
  - [ ] for 循环中是否有 MQ 发送？
  - [ ] for 循环中是否有 Redis 操作？
  - [ ] 批量操作是否使用了 batch 方法？
  - [ ] Redis 批量操作是否使用了 Pipeline？

项目实际循环问题:
  ❌ OrderServiceImpl: for 循环 orderItemMapper.insert(item)
  ❌ ProductServiceImpl: for 循环 tagRelMapper.insert(rel)
  ❌ SkuServiceImpl: for 循环 skuMapper.insert(sku)
  ❌ AddressServiceImpl: for 循环 addressMapper.updateById(addr)
  ❌ CartServiceImpl: for 循环 hashOps.put()

优化建议:
  - 循环逐条 INSERT → saveBatch()（MyBatis-Plus IService，batchSize 建议 500）
  - 循环逐条 UPDATE → updateBatchById() 或单条 SQL 批量更新
  - 循环 Redis 写入 → Pipeline 或 putAll()
  - 循环发 MQ → 批量发送
```

### Step 5: 连接池与线程池审查

```yaml
连接池检查（HikariCP）:
  - [ ] maximum-pool-size 是否合理？
      默认: 10
      建议公式: CPU 核数 * 2 + 磁盘数
      当前项目建议: 20（4 核 + 2 磁盘 * 2 + 余量）
  - [ ] minimum-idle 是否合理？
      建议: 与 maximum-pool-size 一致，避免连接创建抖动
  - [ ] connection-timeout 是否合理？
      默认: 30s
      建议: 10s（超过 10s 获取不到连接说明池已耗尽，应排查而非等待）
  - [ ] idle-timeout 是否合理？
      默认: 600s（10min）
      建议: 300s（5min，减少空闲连接占用）
  - [ ] max-lifetime 是否合理？
      默认: 1800s（30min）
      建议: 1800s（需小于 MySQL wait_timeout，默认 8h）
  - [ ] 连接池监控指标是否暴露？
      建议: HikariCP metrics 接入 Prometheus

Redis 连接池检查（Lettuce）:
  - [ ] max-active 是否合理？
      建议: 50（Redis 操作通常很快，不需要太多连接）
  - [ ] max-idle 是否合理？
      建议: 与 max-active 一致
  - [ ] min-idle 是否合理？
      建议: 10
  - [ ] max-wait 是否合理？
      建议: 3s

线程池检查:
  - [ ] @Async 方法是否有自定义线程池？
      默认 SimpleAsyncTaskExecutor 不复用线程，必须替换
      建议: 配置自定义 ThreadPoolTaskExecutor
  - [ ] ThreadPoolExecutor 核心参数是否合理？
      corePoolSize: CPU 密集型 = CPU 核数 + 1，IO 密集型 = CPU 核数 * 2
      maxPoolSize: corePoolSize * 2
      queueCapacity: 500（避免无界队列导致 OOM）
      rejectedHandler: CallerRunsPolicy（降级而非丢弃）
  - [ ] RocketMQ 消费者线程数是否合理？
      consumeThreadMin: 20
      consumeThreadMax: 50
  - [ ] 定时任务线程池大小是否合理？
      默认单线程，多 @Scheduled 任务会互相阻塞
      建议: 配置 SchedulingConfigurer 指定线程池

Tomcat 线程池:
  - [ ] max-threads 是否合理？默认 200
  - [ ] accept-count 是否合理？默认 100
  - [ ] connection-timeout 是否合理？默认 20s

MyBatis-Plus 批量操作配置:
  - [ ] 是否开启了批量操作支持？（需配置 sqlSessionTemplate）
  - [ ] 分页插件是否配置？（PaginationInnerInterceptor）
  - [ ] 是否有慢查询日志配置？
```

### Step 6: 序列化与网络审查

```yaml
JSON 序列化检查:
  - [ ] 是否使用 Jackson？（Spring Boot 默认，确认无 Gson/Fastjson 混用）
  - [ ] 是否有不必要的序列化字段？（@JsonIgnore 忽略密码等敏感字段）
  - [ ] 日期序列化格式是否统一？（建议 yyyy-MM-dd HH:mm:ss，避免时间戳）
  - [ ] 是否有循环引用导致序列化死循环？（Entity 双向关联）
  - [ ] 枚举序列化是否正确？（建议 @JsonFormat(shape = Shape.STRING)）

API Payload 检查:
  - [ ] 单个 API 响应体是否 > 100KB？（大响应需分页或字段裁剪）
  - [ ] 列表 API 是否返回了不必要的字段？（VO 字段裁剪）
  - [ ] 批量操作 API 的请求体是否 > 1MB？（需限制批量大小）
  - [ ] 文件上传/下载是否使用流式处理？（避免全量加载到内存）
  - [ ] 是否启用 HTTP 压缩？（gzip/brotli，Nginx 层配置）

网络层检查:
  - [ ] 是否有不必要的跨模块同步调用？（可改 MQ 异步）
  - [ ] 外部 API 调用是否设置了超时？（RestTemplate/WebClient timeout）
  - [ ] 外部 API 调用是否有重试策略？（建议指数退避 + 最大 3 次）
  - [ ] 是否有同步调用链路过长？（A→B→C→D，建议扁平化）

项目实际序列化/网络问题:
  - OrderServiceImpl.createOrder() 跨模块同步调用链: Controller → OrderService → ProductService + SkuService + CartService
  - PaymentServiceImpl.createPayment() 调用第三方支付网关无超时配置
```

### Step 7: 前端性能审查

```yaml
技术栈: React 19 + Vite 6 + Ant Design 5 + Redux Toolkit

#### 7.1 React 19 特有性能模式

React 19 编译器（React Compiler）:
  - [ ] 是否过度使用 useMemo/useCallback？
      React 19 编译器自动优化 memoization，手动 memo 可能不必要
      例外: 跨组件传递的回调 + 引用相等性检查场景仍需 useCallback
  - [ ] 是否过度使用 React.memo？
      React 19 编译器自动处理大部分 re-render 优化
      例外: 重渲染成本极高的组件（大列表、复杂图表）仍需 memo
  - [ ] 是否使用了 use() Hook 处理异步数据？
      React 19 新增 use() Hook，可替代 useEffect + useState 获取数据
      配合 Suspense 实现声明式加载状态

代码分割:
  - [ ] 路由级是否使用 React.lazy + Suspense？
      每个路由页面独立 chunk，首屏只加载当前路由
  - [ ] 大型组件是否使用动态 import()？
      如富文本编辑器、图表库等重型依赖按需加载
  - [ ] Suspense fallback 是否合理？
      避免闪烁（loading 时间 < 200ms 时不显示 skeleton）

#### 7.2 Ant Design 5 特有性能模式

Table 组件:
  - [ ] 大数据量是否启用虚拟滚动？（scroll.y + virtual）
      阈值: 行数 > 100 时必须启用
  - [ ] 是否使用 rowKey？（避免 diff 算法全量对比）
  - [ ] 列渲染是否使用 shouldCellUpdate？（避免不必要的单元格重渲染）
  - [ ] 是否使用 columns.render 返回轻量 JSX？（避免内联函数创建组件）

Form 组件:
  - [ ] 是否使用 shouldUpdate 精确控制渲染范围？
      默认任一字段变化重渲染整个 Form
  - [ ] 大表单是否拆分为独立 Form.Item 组？
  - [ ] 是否使用 Form.useWatch 替代 Form.Item 的 dependencies？

Select 组件:
  - [ ] 大数据量是否使用虚拟滚动？（> 100 选项）
  - [ ] 远程搜索是否做了防抖？（debounce 300ms）
  - [ ] 是否使用 filterOption={false} 关闭前端过滤？（远程搜索时）

#### 7.3 图片与资源

  - [ ] 图片是否懒加载？（<img loading="lazy">）
      商城图片密集，首屏外图片必须懒加载
  - [ ] 是否使用 srcset 响应式图片？（不同屏幕尺寸加载不同分辨率）
  - [ ] 是否使用 WebP/AVIF 等现代图片格式？（体积减少 30-50%）
  - [ ] 图片是否有 width/height 属性？（避免 CLS 布局偏移）
  - [ ] 是否有过大的依赖包？（用 rollup-plugin-visualizer 分析）
      阈值: 单个 chunk > 200KB 需关注

#### 7.4 请求优化

  - [ ] 可并行的 API 请求是否使用 Promise.all()？
      项目现状: 仅 ProductDetail 使用 Promise.all，其余串行
  - [ ] 是否有不必要的重复请求？（Redux selector 去重）
  - [ ] 列表页是否实现了无限滚动/分页加载？
  - [ ] 请求是否有防抖/节流？（搜索框 debounce 300ms）
  - [ ] 是否使用 SWR/React Query 缓存请求结果？（减少重复请求）

#### 7.5 构建优化

  - [ ] Vite 是否配置了 manualChunks 分割策略？
      建议: react/vue → vendor, antd → ui, 业务代码 → app
  - [ ] 是否有 bundle 分析工具？（rollup-plugin-visualizer）
  - [ ] 是否启用了 gzip/brotli 压缩？（Nginx 层）
  - [ ] 静态资源是否配置了 CDN？（Vite base 配置）
  - [ ] 是否配置了合理的缓存策略？（hash 文件名 + 长期缓存）

项目实际前端问题:
  ❌ 无图片懒加载（商城图片密集，所有图片同步加载）
  ❌ 无 React.lazy/Suspense 代码分割
  ❌ 无虚拟滚动（商品/订单长列表）
  ❌ 仅 ProductDetail 使用 Promise.all，其余串行请求
```

## 输出格式

### 有问题时的输出

```markdown
## 性能审查报告

**审查范围**: [模块/文件列表]
**审查时间**: [时间]
**Memory 关联**: [相关 decisions/anti-patterns/known-issues 编号]

### CRITICAL — 数据正确性（必须立即修复）
| 问题 | 位置 | 影响 | 修复建议 | 关联 |
|---|---|---|---|---|
| 并发超卖 | ProductServiceImpl.updateStock() | 并发扣库存导致超卖 | CAS 原子更新 | AP005 |

### WARNING — 可用性/性能（影响响应时间或可能导致雪崩）
| 严重度 | 问题 | 位置 | 影响 | 修复建议 | 关联 |
|---|---|---|---|---|---|
| P1 | 大事务含远程调用 | OrderServiceImpl.createOrder() | 远程调用失败导致长事务回滚 | 远程调用移到事务外 | — |
| P2 | N+1 查询 | OrderServiceImpl.createOrder() | 循环内调 productService | listByIds + Map 关联 | — |
| P2 | 循环逐条 INSERT | SkuServiceImpl.createSkus() | 批量创建 SKU 慢 | saveBatch() | — |

### INFO — 体验/优化（影响用户体验或锦上添花）
| 严重度 | 问题 | 位置 | 影响 | 修复建议 | 关联 |
|---|---|---|---|---|---|
| P3 | 无图片懒加载 | 商品列表页 | 首屏加载慢 | img loading="lazy" | — |
| P4 | 连接池未调优 | application.yml | 默认配置可能不足 | 按公式调整 | — |

### 跨 Skill 联动建议
| 问题 | 建议激活的 Skill | 原因 |
|---|---|---|
| 慢查询 + 索引缺失 | sql-review | 需要完整 SQL + 索引审查 |
| 缓存缺失 | cache-design | 需要设计完整缓存方案 |

### 汇总
| 严重度 | 优先级 | 数量 | 关键问题 |
|---|---|---|---|
| CRITICAL | P0 | N | ... |
| WARNING | P1 | N | ... |
| WARNING | P2 | N | ... |
| INFO | P3 | N | ... |
| INFO | P4 | N | ... |
```

### 无问题时的输出

```markdown
## 性能审查报告

**审查范围**: [模块/文件列表]
**审查时间**: [时间]
**Memory 关联**: [相关 decisions/anti-patterns/known-issues 编号]

### 审查结果: ✅ 未发现性能问题

已检查以下维度，均未发现异常:

| 检查维度 | 结果 | 备注 |
|---|---|---|
| 数据库查询 | ✅ 通过 | 无 N+1、无慢查询、索引合理 |
| 缓存 | ✅ 通过 | 高频数据已缓存、TTL 合理 |
| 事务与并发 | ✅ 通过 | 事务边界合理、无并发风险 |
| 循环与批量 | ✅ 通过 | 无循环内 DB/Redis 调用 |
| 连接池与线程池 | ✅ 通过 | 配置合理 |
| 序列化与网络 | ✅ 通过 | Payload 合理、无冗余序列化 |
| 前端性能 | ✅ 通过 | 代码分割、懒加载均已实现 |

### 潜在关注点（当前无风险，但需持续观察）
| 关注点 | 说明 | 观察指标 |
|---|---|---|
| ... | ... | ... |
```

## 约束

```yaml
必须遵守:
  - 鼓励基于测量（EXPLAIN/profile）而非猜测
  - 先修复 CRITICAL(P0)，后处理 WARNING(P1/P2)，最后处理 INFO(P3/P4)
  - 并发安全问题（P0）优先于所有性能优化
  - 大事务含远程调用（P1）必须修复，否则可能导致雪崩
  - 引用子 Skill 做深度审查，本 Skill 不重复定义
  - 审查前必须读取 Memory 文件（Step 0）

禁止:
  - 不追求极致性能而牺牲可读性
  - 不过早优化（无测量数据支撑的优化是浪费）
  - 不忽略已知瓶颈（Memory 中记录的问题必须复查）
  - 不在无 EXPLAIN 证据时断言"需要加索引"
  - 不在无压测数据时断言"需要调大连接池"

平衡原则:
  - 不过早优化，但也不忽视已知问题
  - 已知瓶颈（Memory 记录）必须修复，未知瓶颈需测量后决定
  - 优化必须可度量：优化前有基线数据，优化后有对比数据
```
