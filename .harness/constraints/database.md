# 数据库安全约束

> 数据库操作红线。所有涉及数据库的 executor 执行前必须检查。
> 适用环境: MySQL 8.0 / InnoDB / utf8mb4
> 详细规则: `.harness/rules/database-governance.md`
> 审查流程: `.harness/skills/sql-review/SKILL.md`

---

## 绝对禁止

```yaml
DDL 禁止:
  - DROP TABLE / DROP DATABASE
  - TRUNCATE TABLE（绕过事务日志、无视逻辑删除、不可回滚）
  - 修改已执行的 Flyway 迁移脚本（checksum 不匹配导致启动失败）
  - 生产环境手动执行 DDL（必须通过 Flyway 迁移）
  - 大表 ALTER（>100万行）不加 ALGORITHM=INSTANT/INPLACE, LOCK=NONE

DML 禁止:
  - DELETE FROM <table> 无 WHERE 条件
  - UPDATE <table> SET ... 无 WHERE 条件
  - 在 for 循环中逐条执行 INSERT/UPDATE/DELETE
  - SELECT *（必须指定字段列表）
  - SQL 中使用 ${} 拼接用户输入
  - WHERE 子句对字段做函数运算（如 WHERE DATE(create_time) = '...'，导致索引失效）
  - INSERT INTO ... SELECT 大表操作（锁源表，需分批或使用导出导入）

事务禁止:
  - 大事务（超过 3 张表或预计耗时 > 2s）
  - 事务中调用远程 HTTP/RPC
  - 事务中 sleep/wait
  - 事务无超时设置（必须按业务分级配置 timeout，禁止无超时）
  # 分级参考:
  #   极短事务(支付/扣款): 1~3s
  #   常规事务(订单创建): 5~10s
  #   批量事务(结算/对账): 30~120s
  #   大批量导入: 不应用事务包全流程，改用分批提交

并发禁止:
  - 并发场景下 SELECT-then-INSERT/UPDATE（竞态条件，改用原子 SQL 或乐观锁）
```

---

## 必须遵守

```yaml
所有表:
  - 含必备字段: id, create_time, update_time, deleted
  - 引擎 InnoDB, 字符集 utf8mb4
  - 逻辑删除: deleted TINYINT（0=正常, 1=删除），不物理删除
  - 唯一键必须包含 deleted 字段（否则逻辑删除后无法再次插入相同业务键）

所有 SQL:
  - #{} 参数化查询
  - UPDATE/DELETE 必须有 WHERE 条件；批量操作必须加 LIMIT，按主键单条操作可不加
  - 分页查询有 LIMIT
  - JOIN 不超过 3 张表
  - 跨模块 SQL 不直接 JOIN（通过 Service 调用获取数据，内存关联）

迁移脚本:
  - 命名: V{version}__{description}.sql
  - DDL 可重复执行（IF NOT EXISTS / IF EXISTS）
  - 不可逆操作使用存储过程检查存在性
```

---

## 索引约束

```yaml
必须建索引的字段:
  - WHERE 条件字段
  - JOIN 关联字段（user_id, order_id, product_id）
  - ORDER BY 排序字段
  - 唯一约束字段

禁止:
  - 低区分度字段单独索引（gender, status 单值占比 > 30%）
  - TEXT/BLOB 普通索引（用 FULLTEXT 或前缀索引）
  - 冗余索引（如已有 idx_a_b，再建 idx_a）
  - 单表超过 5 个索引
```
