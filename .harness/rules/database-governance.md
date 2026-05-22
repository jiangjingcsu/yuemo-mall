# 数据库治理规则

> 约束 AI Agent 的数据库设计、SQL 编写和表结构变更。基于项目实际数据库模式（MySQL 8.0 / InnoDB / utf8mb4）。
>
> **层级定位**：本文件是 Rules 层文档，定义数据库设计、SQL 编写和表结构变更的治理规范（软规范，违反 = 应修复）。
> 与 Constraints 层的关系：Constraints（constraints/database.md）定义数据库操作硬红线（违反 = 停止执行），本文件定义详细规范。
> 实操技能：`.harness/skills/sql-review/SKILL.md`

---

## 1. 表设计规范

### 1.1 必备字段

```yaml
所有表必须包含:
  id: BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键'
  create_time: DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
  update_time: DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
  deleted: TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'

主键: BIGINT 自增（@TableId(type = IdType.AUTO)，纯数据库自增，非 UUID/雪花ID）
业务编号: 使用 IdGenerator 生成（如 payment_no、refund_no），非主键用途

注释要求:
  - 所有表必须有 COMMENT 说明用途
  - 所有字段必须有 COMMENT 说明含义
  - 状态/枚举字段注释需列出可选值（如 COMMENT '状态: 0-禁用, 1-启用'）
```

### 1.2 命名规范

| 对象 | 规范 | 示例 |
|---|---|---|
| 表名 | `yu_` 前缀 + 下划线小写 | `yu_order`、`yu_order_item` |
| 字段名 | 下划线小写 | `user_id`、`create_time` |
| 索引名 | `idx_` 前缀(普通)、`uk_` 前缀(唯一) | `idx_user_id`、`uk_order_no` |
| 主键 | 统一用 `id` | `PRIMARY KEY (id)` |
| 外键 | 禁止使用物理外键 | 应用层保证引用完整性 |

### 1.3 字段类型

| 场景 | 类型 |
|---|---|
| 主键/外键 | `BIGINT` |
| 短文本(≤256) | `VARCHAR(N)`，N 按实际需要设定，禁止一律 VARCHAR(255) |
| 长文本 | `TEXT` |
| 金额 | `DECIMAL(10,2)` |
| 状态/标志位 | `TINYINT` |
| 时间 | `DATETIME`（禁止 `TIMESTAMP` 有 2038 问题） |
| 图片/JSON数组 | `VARCHAR(512)` 或 `TEXT` |

---

## 2. SQL 编写规范

### 2.1 禁止事项

```yaml
禁止:
  硬红线（违反即停止）: → constraints/database.md §绝对禁止
  本文件补充的规范级禁止:
    - 深度 JOIN（超过 3 张表）
    - 无 LIMIT 的分页查询
    - WHERE 子句中对字段进行函数运算（如 WHERE DATE(create_time) = '2024-01-01'）
    - 在 for 循环中逐条 INSERT/UPDATE（使用批量操作）
    - 跨模块 SQL JOIN（如订单模块 SQL 不直接 JOIN 商品模块表，通过 Service 调用获取数据）
    - INSERT INTO ... SELECT 大表操作（锁源表，需分批或使用导出导入）
    - N+1 查询（循环中逐条调 Mapper/Service，改用批量查询后内存关联）
```

### 2.2 强制要求

```yaml
必须:
  - 所有 SQL 使用 #{} 参数化查询
  - 分页查询使用 MyBatis-Plus Page 对象
  - 分页响应统一使用 PageResult<T>（common-core 提供）
    MyBatis-Plus Page 字段: total/current/size/records
    PageResult record 字段: total/page/size/list
    Service 层需做 Page → PageResult 转换（PageResult.of(page, voList)）
  - 批量操作使用 saveBatch()/updateBatchByIds()
  - 批量操作单次不超过 1000 条（saveBatch/updateBatchById 默认 1000）
  - IN 列表不超过 1000 个值（超出分批处理）
  - 唯一约束用 UNIQUE KEY，包含 deleted 字段以支持逻辑删除
  - 索引建立在区分度高的字段上
  - 复合索引遵循最左前缀原则
  - EXPLAIN 分析查询计划（type 至少达到 range 级别）
  - 事务按业务分级配置 timeout（禁止无超时设置）
    极短事务(支付/扣款): 1~3s
    常规事务(订单创建): 5~10s
    批量事务(结算/对账): 30~120s
  - 深分页优化: WHERE id > lastId LIMIT N（避免 OFFSET 过大导致性能劣化）
```

---

## 3. 逻辑删除

项目统一使用 MyBatis-Plus 逻辑删除：

```yaml
逻辑删除字段: deleted (DB: TINYINT, Java: Boolean, 0=false/正常, 1=true/删除)
类型映射: BaseEntity 中 deleted 为 Boolean 类型，MyBatis-Plus 自动处理 Boolean↔TINYINT 映射
配置: mybatis-plus.global-config.db-config.logic-delete-field=deleted
      mybatis-plus.global-config.db-config.logic-delete-value=1
      mybatis-plus.global-config.db-config.logic-not-delete-value=0

注意事项:
  - 唯一键必须包含 deleted 字段，否则逻辑删除后无法再次插入相同业务键
  - 查询时 MyBatis-Plus 自动追加 deleted=0 条件
  - 需要查询已删除数据时，手动清除逻辑删除条件（慎用，需注释说明原因）
  - 禁止冗余手动追加 .eq(Entity::getDeleted, false)（MyBatis-Plus 已自动追加，重复添加导致 SQL 冗余）
```

---

## 4. 索引策略

```yaml
必须建索引:
  - WHERE 条件字段
  - JOIN 关联字段（如 user_id、order_id、product_id）
  - ORDER BY 排序字段
  - 唯一约束字段（使用 UNIQUE KEY）

禁止:
  - 在低区分度字段建单独索引（如 gender、status 单值占比 > 30%）
  - 在 TEXT/BLOB 字段建普通索引（需用 FULLTEXT）
  - 冗余索引（如已有 idx_a_b，再建 idx_a）
  - 超过 5 个索引的表（评估是否过度索引）
```

---

## 5. 数据库变更管理

```yaml
迁移工具: Flyway
脚本位置: yuemo-server/src/main/resources/db/migration/
命名规范: V{version}__{description}.sql

规则:
  - 已执行的迁移文件禁止修改（会导致 checksum 不匹配）
  - 新变更必须创建新的迁移文件
  - 所有 DDL 必须可重复执行（使用 IF NOT EXISTS / IF EXISTS）
  - 不可逆操作（DROP COLUMN）先用存储过程检查存在性
  - 大表 ALTER（>100万行）必须指定 ALGORITHM=INSTANT/INPLACE, LOCK=NONE
  - 新增列设置合理默认值（避免全表 UPDATE 填充）
  - 新增唯一约束前先验证无重复数据
  - 生产环境禁止使用 flyway repair，必须通过新建迁移修复
```

---

## 6. 数据安全

```yaml
必须:
  - 密码字段使用 BCrypt 存储（yu_user.password）
  - 手机号脱敏展示 138****1234
  - API 响应不返回 password 字段
  - 敏感操作记录日志（订单状态变更 → yu_order_log）

禁止:
  - 明文存储密码、密钥、Token
  - 日志中输出用户密码、完整手机号
  - 直接拼接用户输入到 SQL 字符串
```
