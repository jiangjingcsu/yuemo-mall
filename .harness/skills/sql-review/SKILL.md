---
name: "sql-review"
description: "审查 SQL 语句、数据库表设计和索引策略的安全性和性能。当涉及 SQL 编写、表结构变更或数据库迁移时触发。"
---

# SQL 审查 Skill

对 SQL 语句、数据库表设计、索引、迁移脚本进行全面审查，确保安全、性能和规范合规。
> 治理规则：`.harness/rules/database-governance.md`
> 安全红线：`.harness/constraints/database.md`

## 触发条件

- 编写新 SQL 语句（Mapper XML / LambdaQueryWrapper）
- 创建新表或修改表结构（Flyway 迁移）
- 用户要求审查数据库设计
- 性能问题排查中涉及慢查询
- Bug 修复涉及数据不一致或 SQL 异常

## 与 Workflow 的关系

| Workflow | 触发场景 | 调用时机 |
|---|---|---|
| feature-development.md | Step 5 编码实现（数据库迁移 + Mapper 编写） | 实现完成后自检 |
| feature-development.md | Step 7 自检（数据库迁移可重复执行） | 自检清单第 8 项 |
| bug-fix.md | Step 2 根因分析（数据库相关 Bug） | 按 Bug 类型激活 |
| bug-fix.md | Step 5 验证（数据库与缓存检查） | 验证层级第 6 层 |
| refactor.md | 重构涉及表结构变更或 SQL 改写 | 重构方案评审 |

## 依赖上下文

- `.harness/rules/database-governance.md` — 数据库治理规则（权威规则源）
- `.harness/constraints/database.md` — 数据库安全红线（硬约束）
- `.harness/rules/module-boundary.md` — 模块边界（SQL 不可跨模块 JOIN）
- `.harness/skills/domain-modeling/SKILL.md` — 领域建模（Entity-Table 映射验证）
- `yuemo-server/src/main/resources/db/migration/` — 现有迁移脚本
- 相关 Entity 和 Mapper 文件

## 跨 Skill 联动

| 联动 Skill | 触发条件 | 联动方式 |
|---|---|---|
| transaction-analysis | SQL 涉及多表操作、库存扣减、支付流程 | 审查发现多表操作时，转交事务边界分析 |
| performance-review | SQL 存在慢查询、N+1、全表扫描 | Step 2 性能检查发现问题时，转交深度性能审查 |
| domain-modeling | 新建表或修改表结构，需验证 Entity-Table 映射 | Step 1 表设计审查时，验证实体与表的对应关系 |
| cache-design | 高频查询适合引入缓存 | Step 2 发现高频相同条件查询时，建议缓存策略 |

## 审查流程

### Step 0: Memory 检查（强制）

```yaml
审查开始前必须读取:
  1. .harness/memory/decisions.md
     - D002: MyBatis-Plus 而非 JPA（影响 SQL 编写风格）
     - D005: 逻辑删除统一策略（所有查询必须过滤 deleted=0）
     - D006: Flyway 数据库迁移（迁移脚本规则）
  2. .harness/memory/anti-patterns.md
     - AP005: SELECT-then-INSERT 竞态（并发场景禁止此模式）
     - AP006: 唯一键不含 deleted（唯一约束必须含 deleted 字段）
  3. .harness/memory/incidents.md
     - INC001: 购物车重复插入（唯一约束 + 并发竞态）
     - INC002: Flyway 迁移文件 Checksum 不匹配（已执行脚本不可修改）

检查:
  - [ ] 已读取 decisions.md 中与数据库相关的决策
  - [ ] 已读取 anti-patterns.md 中与 SQL 相关的反模式
  - [ ] 已读取 incidents.md 中与数据库相关的事故
  - [ ] 本次审查范围是否涉及历史事故的同类场景
```

### Step 1: 表设计审查

```yaml
基础规范:
  - [ ] 表名使用 yu_ 前缀 + snake_case（如 yu_order_item）
  - [ ] 字段名使用 snake_case（如 user_id, create_time）
  - [ ] 包含必备字段: id(BIGINT AUTO_INCREMENT), create_time, update_time, deleted
  - [ ] 主键为 BIGINT 自增（非 UUID、非雪花ID——DB 层用自增，应用层由 IdGenerator 生成）
  - [ ] 字符集为 utf8mb4
  - [ ] 引擎为 InnoDB
  - [ ] 有合理的表注释（COMMENT）和字段注释
  - [ ] 无物理外键（应用层保证引用完整性）

字段类型:
  - [ ] 金额字段使用 DECIMAL(10,2)（禁止 FLOAT/DOUBLE，精度丢失风险）
  - [ ] 状态/标志位使用 TINYINT
  - [ ] 时间字段使用 DATETIME（禁止 TIMESTAMP，2038 问题）
  - [ ] 短文本(≤256) 使用 VARCHAR(N)
  - [ ] 长文本使用 TEXT（TEXT 不设默认值，不影响排序性能的场景使用）
  - [ ] 图片/JSON 数组使用 VARCHAR(512) 或 TEXT
  - [ ] VARCHAR 长度按实际需要设定，禁止一律 VARCHAR(255)

逻辑删除一致性:
  - [ ] deleted 字段: TINYINT NOT NULL DEFAULT 0
  - [ ] 唯一键包含 deleted 字段（否则逻辑删除后无法再次插入相同业务键）
  - [ ] 业务唯一键（如 order_no）在 deleted=0 范围内唯一

Entity-Table 映射验证:
  - [ ] Entity @TableName 与表名一致
  - [ ] Entity 字段类型与列类型对应（BigDecimal → DECIMAL, LocalDateTime → DATETIME）
  - [ ] Entity 继承 BaseEntity（含 id, createTime, updateTime, deleted）
  - [ ] 新增字段在 Entity 和迁移脚本中同步添加
```

### Step 2: SQL 语句审查

```yaml
安全红线（CRITICAL）:
  - [ ] UPDATE/DELETE 有 WHERE 条件（无 WHERE = 全表操作，禁止）
  - [ ] SELECT 无 WHERE 条件时必须有 LIMIT（防止全表扫描返回海量数据）
  - [ ] 使用 #{} 参数化（禁止 ${} 拼接用户输入，SQL 注入风险）
  - [ ] 生产环境禁止手动执行 DDL（必须通过 Flyway 迁移）

规范检查:
  - [ ] 无 SELECT *（指定字段列表，减少网络传输和内存占用）
  - [ ] WHERE 子句不对字段做函数运算（如 WHERE DATE(create_time) = '2024-01-01' → 改为范围查询）
  - [ ] IN 列表不超过 1000 个值（MySQL 限制，超出分批处理）
  - [ ] 无 for 循环中逐条 SQL（使用 saveBatch/updateBatchById）
  - [ ] 深度 JOIN ≤ 3 张表（超过则拆分查询或冗余字段）

大事务检查:
  - [ ] 单事务操作不超过 3 张表（超过则拆分，改用 MQ 最终一致性）
  - [ ] 单事务预计耗时不超过 2s（长事务持有锁时间长，影响并发）
  - [ ] 事务内不包含外部调用（RPC/HTTP/MQ 发送，外部失败导致事务回滚不可控）
  - [ ] 发现大事务时联动 transaction-analysis skill 深入分析

逻辑删除过滤:
  - [ ] MyBatis-Plus 自动追加 deleted=0（确认 @TableLogic 已配置）
  - [ ] Mapper XML 手写 SQL 必须显式添加 WHERE deleted = 0
  - [ ] 子查询中的表也必须过滤 deleted = 0
  - [ ] 统计查询（COUNT/SUM）必须过滤 deleted = 0
  - [ ] JOIN 关联表都需过滤 deleted = 0

批量操作:
  - [ ] 批量 INSERT 使用 saveBatch()（MyBatis-Plus IService）
  - [ ] 批量 UPDATE 使用 updateBatchById()
  - [ ] 批量操作单次不超过 1000 条（分批提交）
  - [ ] 大批量数据导入使用 LOAD DATA 或分批 INSERT

查询优化:
  - [ ] 子查询 vs JOIN: 优先使用 JOIN，子查询可能导致临时表
  - [ ] UNION vs OR: OR 条件可走索引时用 OR，否则用 UNION ALL（UNION 有去重开销）
  - [ ] 分页查询使用 MyBatis-Plus Page 对象
  - [ ] 深分页优化: WHERE id > lastId LIMIT N（避免 OFFSET 过大）
  - [ ] 避免在 ORDER BY 中使用表达式

N+1 查询识别:
  - [ ] for/while 循环中调 Mapper/Service（传统 N+1）
  - [ ] stream().map() 中调 Mapper/Service（函数式 N+1，同样危险）
  - [ ] stream().collect() 中调 Mapper（如 productIds.stream().collect(toMap(id -> id, id -> mapper.selectById(id)))）
  - [ ] 修复: 批量查询后内存关联（mapper.selectBatchIds(ids) → Map<id, Entity>）

性能检查:
  - [ ] 是否能用 EXPLAIN 分析？（type ≥ range）
  - [ ] 是否使用了索引？（key 列非 NULL）
  - [ ] 大表是否有分页？
  - [ ] 是否存在全表扫描？（type = ALL）
  - [ ] 临时表是否过大？（Using temporary + Using filesort）
```

### Step 3: 索引审查

```yaml
量化阈值:
  - [ ] 单表索引数量 ≤ 5 个（超过需评估是否过度索引，写入性能下降）
  - [ ] 复合索引列数 ≤ 4 列（过宽索引维护成本高，B+Tree 层级增加）
  - [ ] 低区分度阈值: 单值占比 > 30% 的字段禁止单独建索引（如 gender, status）
  - [ ] 索引字段总长度 ≤ 767 bytes（InnoDB 单索引键长度限制，utf8mb4 下约 191 字符）

必须建索引:
  - [ ] WHERE 条件字段有索引
  - [ ] JOIN 关联字段有索引（如 user_id, order_id, product_id）
  - [ ] ORDER BY 排序字段有索引
  - [ ] 唯一约束字段有 UNIQUE KEY

禁止:
  - [ ] 低区分度字段单独索引（gender, deleted 等单值占比 > 30%）
  - [ ] TEXT/BLOB 字段普通索引（需用 FULLTEXT 或前缀索引）
  - [ ] 冗余索引（如已有 idx_a_b，再建 idx_a）
  - [ ] 重复索引（同字段多个索引）

复合索引:
  - [ ] 符合最左前缀原则（查询条件顺序与索引列顺序一致）
  - [ ] 区分度高的列在前（如 user_id 在 create_time 前）
  - [ ] 范围查询列放在最后（如 idx_user_id_create_time，create_time 为范围条件）

命名规范:
  - [ ] 普通索引: idx_{字段名}（如 idx_user_id）
  - [ ] 唯一索引: uk_{字段名}（如 uk_order_no）
  - [ ] 复合索引: idx_{字段1}_{字段2}（如 idx_user_id_create_time）
```

### Step 4: 迁移脚本审查

```yaml
命名与格式:
  - [ ] 脚本命名: V{version}__{description}.sql（双下划线分隔版本和描述）
  - [ ] 版本号递增，无间隙（V1, V2, V3... 不跳号）
  - [ ] 描述使用 snake_case（如 V3__add_coupon_tables.sql）

DDL 安全:
  - [ ] DDL 可重复执行（IF NOT EXISTS / IF EXISTS）
  - [ ] 不可逆操作（DROP COLUMN）使用存储过程检查存在性
  - [ ] 大表变更（> 100 万行）使用 ALGORITHM=INPLACE, LOCK=NONE
  - [ ] 新增列设置合理默认值（避免全表 UPDATE 填充）
  - [ ] 新增唯一约束先验证无重复数据

数据迁移安全:
  - [ ] 数据迁移脚本包含事务控制（BEGIN ... COMMIT）
  - [ ] 大批量数据迁移分批提交（每批 ≤ 1000 行）
  - [ ] 数据迁移前有 SELECT 验证步骤（确认迁移范围）
  - [ ] 迁移后验证数据完整性（COUNT/SUM 对比）

回滚要求:
  - [ ] 每个 DDL 迁移有对应的回滚策略（注释中说明或创建 R 脚本）
  - [ ] 数据迁移有逆向脚本（INSERT → DELETE, UPDATE → 反向 UPDATE）
  - [ ] 回滚脚本已验证可执行

不可变规则:
  - [ ] 未修改已执行的迁移脚本（Flyway checksum 校验）
  - [ ] 新变更创建新的迁移文件
  - [ ] 生产环境禁止使用 flyway repair
```

### Step 5: 跨模块 SQL 检查

```yaml
模块边界:
  - [ ] SQL 不跨模块 JOIN（如订单模块 SQL 不直接 JOIN 商品模块表）
  - [ ] 跨模块数据通过 Service 调用获取，再内存关联
  - [ ] 跨模块数据一致性通过 MQ 异步保证，非本地事务

当前模块划分:
  yuemo-user:     yu_user, yu_address
  yuemo-product:  yu_product, yu_product_sku, yu_category, yu_brand, yu_product_tag, yu_product_tag_rel, yu_review
  yuemo-order:    yu_order, yu_order_item, yu_order_log
  yuemo-payment:  yu_payment
  yuemo-promotion: yu_coupon, yu_user_coupon
  yuemo-cart:     yu_cart_item

违规示例:
  ❌ SELECT o.*, p.name FROM yu_order o JOIN yu_product p ON o.product_id = p.id
     → 订单模块直接 JOIN 商品模块表，违反模块边界
  ✅ 先查 yu_order 获取 product_id 列表，再调 productService.listByIds() 获取商品信息

例外:
  - 订单快照字段（如 order_item 中的 product_name, product_image）属于冗余存储，非 JOIN
  - 跨模块查询必须通过 Service 接口，不直接操作其他模块的 Mapper
```

### Step 6: 存量表合规审查

```yaml
当审查范围包含"全量审查"或"现有表合规"时，必须检查:

唯一键含 deleted 字段:
  当前违反的表（必须通过 Flyway 迁移修复）:
    - yu_user: uk_username(username) → 应改为 uk_username(username, deleted)
    - yu_product_tag: uk_name(name) → 应改为 uk_name(name, deleted)
    - yu_search_keyword: uk_keyword(keyword) → 应改为 uk_keyword(keyword, deleted)
    - yu_product_tag_rel: uk_product_tag(product_id, tag_id) → 应改为 uk_product_tag(product_id, tag_id, deleted)
    - yu_payment: uk_payment_no(payment_no) → 应改为 uk_payment_no(payment_no, deleted)
  已合规:
    - yu_cart_item: uk_user_sku(user_id, sku_id, deleted) ✅
    - yu_order: uk_order_no(order_no) — 订单号全局唯一，逻辑删除后不复用，可不含 deleted

必备字段完整性:
  当前违反的表:
    - yu_product_tag_rel: 缺少 update_time, deleted 字段
  修复: 通过 Flyway 迁移添加缺失字段，并设置合理默认值

索引超标:
  当前违反的表:
    - yu_product: 6 个索引（idx_category, idx_brand, idx_status, idx_name, ft_name_title_desc, PK）
      超过 ≤ 5 个的阈值
      其中 idx_status 在低区分度字段（status 只有 0/1），建议移除
  处理原则:
    - 存量违规不强制立即修复，但新增索引时必须评估总量
    - 新增表严格遵守 ≤ 5 个索引的规则
    - 低区分度字段索引在代码审查中标记为 WARNING

低区分度索引:
  当前违反:
    - yu_product.idx_status: status 只有 0/1，区分度 < 30%
    - yu_brand.idx_status: status 只有 0/1，区分度 < 30%
  处理原则:
    - 如查询总是组合其他高区分度条件使用，可保留为复合索引的一部分
    - 单独的低区分度索引建议移除或合并为复合索引
```

### Step 7: MyBatis-Plus 使用审查

```yaml
LambdaQueryWrapper:
  - [ ] 使用 LambdaQueryWrapper 而非 QueryWrapper（编译期检查字段名，防拼写错误）
  - [ ] 指定 select 字段（.select(Entity::getId, Entity::getName)），避免 SELECT *
  - [ ] 条件构造使用 .eq/.ne/.like/.between 等类型安全方法

动态 SQL 安全:
  - [ ] .last() 仅用于常量拼接（如 .last("LIMIT 100")），禁止拼接用户输入
  - [ ] .last() 拼接 ORDER BY 时，排序字段必须通过 switch/if 硬编码白名单，禁止直接拼接用户参数
  - [ ] .apply() 用于函数条件时（如 FULLTEXT MATCH），参数必须用 {0} 占位符绑定
  - [ ] .having() 条件不拼接用户输入
  - [ ] 所有动态 SQL 片段来源可追溯，无用户输入直接拼入

分页:
  - [ ] 分页查询使用 Page<T> 对象
  - [ ] 禁止手动拼接 LIMIT（使用 MyBatis-Plus 分页插件）
  - [ ] 深分页场景考虑游标分页（WHERE id > lastId ORDER BY id LIMIT N）

批量操作:
  - [ ] 批量 INSERT 使用 IService.saveBatch()（非循环 mapper.insert()）
  - [ ] 批量 UPDATE 使用 IService.updateBatchById()
  - [ ] saveBatch 单次不超过 1000 条（MyBatis-Plus 默认 1000）

逻辑删除:
  - [ ] @TableLogic 注解已配置（全局配置: mybatis-plus.global-config.db-config.logic-delete-field=deleted）
  - [ ] 查询已删除数据时手动清除逻辑删除条件（慎用，需注释说明原因）
  - [ ] 自定义 SQL 中显式添加 deleted = 0 条件
  - [ ] 禁止冗余手动追加 .eq(Entity::getDeleted, false)（MyBatis-Plus 已自动追加，重复添加导致 SQL 冗余且维护混乱）

Mapper XML:
  - [ ] 复杂查询写在 Mapper XML 中（多表关联、动态条件）
  - [ ] XML 中使用 #{} 参数化（禁止 ${} 拼接用户输入）
  - [ ] XML 中 <include> 复用 SQL 片段（避免重复字段列表）
  - [ ] <where> 标签自动处理 AND 前缀（优于手动 WHERE 1=1）
  - [ ] <foreach> 批量操作使用合理的 collection 和 separator

禁止:
  - [ ] 禁止使用 selectAll()（等同于 SELECT *）
  - [ ] 禁止在 Service 层直接操作其他模块的 Mapper
  - [ ] 禁止在 Controller 层直接调用 Mapper
```

### Step 8: 验证与合规

```yaml
审查完成后，必须逐项检查:

  1. CRITICAL 问题是否全部有修复方案？
     - 无修复方案 → 补充具体修复代码或迁移脚本

  2. 大事务检查是否覆盖了所有 @Transactional 方法？
     - 遗漏 → 扫描所有 ServiceImpl 中的 @Transactional

  3. N+1 检查是否覆盖了 stream+mapper 模式？
     - 遗漏 → 搜索 .stream() + mapper.select 模式

  4. 存量表违规是否全部标注？
     - 遗漏 → 对比 V1 迁移脚本中的唯一键定义

  5. .last()/.apply() 使用是否全部审查？
     - 遗漏 → 搜索 .last( 和 .apply( 的所有调用

  6. 逻辑删除过滤是否覆盖了所有查询路径？
     - 遗漏 → 检查 Mapper XML 和自定义 SQL

  7. 审查结果是否与 database-governance.md 对齐？
     - 未对齐 → 补充缺失维度
```

## 审查报告格式

```markdown
## SQL Review Report

### CRITICAL（必须修复，阻塞合并）
| 问题 | 位置 | 规则 | 修复建议 |
|---|---|---|---|
| 缺少 WHERE 条件 | OrderMapper.xml:42 | database.md 红线 | 添加 WHERE 条件 |
| ${} 拼接用户输入 | UserMapper.xml:15 | SQL 注入风险 | 改用 #{} 参数化 |
| 跨模块 JOIN | OrderMapper.xml:28 | module-boundary.md | 改为 Service 调用 |
| 生产手动 DDL | — | database.md 红线 | 通过 Flyway 迁移 |

### WARNING（强烈建议修复）
| 问题 | 位置 | 规则 | 修复建议 |
|---|---|---|---|
| SELECT * | ProductMapper.xml:7 | database-governance.md | 使用 .select() 指定字段 |
| 缺少索引 | yu_order.status | database-governance.md | 添加 idx_status |
| 逻辑删除未过滤 | CartMapper.xml:33 | D005 决策 | 添加 WHERE deleted = 0 |
| 索引数量超限(7个) | yu_product | 量化阈值 ≤ 5 | 评估删除冗余索引 |
| 循环逐条 INSERT | OrderServiceImpl.java:89 | AP005 反模式 | 改用 saveBatch() |
| .last() 拼接动态值 | MySqlSearchServiceImpl.java:76 | 动态SQL安全 | 排序字段用白名单 |
| 冗余手动 deleted | SkuServiceImpl.java:38 | 逻辑删除配置 | 移除手动 .eq(deleted) |

### INFO（建议优化）
| 问题 | 位置 | 规则 | 修复建议 |
|---|---|---|---|
| 缺少 COMMENT | yu_order.total_amount | 表设计规范 | 添加字段注释 |
| VARCHAR(255) 过长 | yu_product.name | 字段类型规范 | 按实际需要缩短 |
| 深分页 OFFSET 过大 | OrderMapper.xml:50 | 查询优化 | 改用游标分页 |

### 存量问题（现有表违规，需通过 Flyway 迁移修复）
| 问题 | 表 | 当前定义 | 应改为 | 优先级 |
|---|---|---|---|---|
| 唯一键缺 deleted | yu_user | uk_username(username) | uk_username(username, deleted) | 中 |
| 唯一键缺 deleted | yu_product_tag | uk_name(name) | uk_name(name, deleted) | 中 |
| 唯一键缺 deleted | yu_search_keyword | uk_keyword(keyword) | uk_keyword(keyword, deleted) | 中 |
| 唯一键缺 deleted | yu_product_tag_rel | uk_product_tag(product_id, tag_id) | uk_product_tag(product_id, tag_id, deleted) | 中 |
| 唯一键缺 deleted | yu_payment | uk_payment_no(payment_no) | uk_payment_no(payment_no, deleted) | 中 |
| 缺少必备字段 | yu_product_tag_rel | 无 update_time, deleted | 添加字段 + 默认值 | 高 |
| 索引超标 | yu_product | 6 个索引 | 移除 idx_status | 低 |
| 低区分度索引 | yu_product.idx_status | status 0/1 | 合并复合索引或移除 | 低 |

### PASS
- 表设计符合规范 ✓
- 索引策略合理 ✓
- 迁移脚本合规 ✓
- MyBatis-Plus 使用规范 ✓
```

## 约束

- CRITICAL 问题必须修复，不修复则 BLOCK
- 生产环境禁止手动执行 DDL，必须通过 Flyway 迁移
- SELECT 无 WHERE 条件视为 CRITICAL（等同全表扫描风险）
- 不修改已执行的 Flyway 迁移脚本（INC002 事故教训）
- 生产环境禁止使用 flyway repair，必须通过新建迁移修复
- 性能优化需基于 EXPLAIN 结果，不盲目加索引
- 跨模块 SQL 必须重构为 Service 调用（module-boundary.md）
- 逻辑删除过滤必须完整覆盖所有查询路径（D005 决策）
- 唯一键必须包含 deleted 字段（AP006 反模式）
- 并发场景禁止 SELECT-then-INSERT/UPDATE 模式（AP005 反模式）
- .last() 禁止拼接用户输入，排序字段必须用白名单（SQL 注入防御）
- 单事务操作不超过 3 张表，超过需联动 transaction-analysis skill
- 存量表违规通过 Flyway 迁移修复，不强制立即修复但新增表必须合规
