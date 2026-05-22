# Feature 开发工作流

> AI Agent 执行功能开发时的标准工作流。确保从需求到代码的工程化交付。

---

## 触发条件

- 用户要求新增功能
- 用户要求实现某个特性
- 用户要求添加 API 接口

---

## 工作流步骤

### Step 1: 上下文加载

```
加载次序:
  1. CLAUDE.md（入口）
  2. .harness/memory/decisions.md（架构决策，避免重蹈覆辙）
  3. .harness/memory/anti-patterns.md（已知反模式，避免重复踩坑）
  4. .harness/docs/project-overview.md（架构全景）
  5. .harness/rules/module-boundary.md（模块边界）
  6. 涉及模块的源码（Controller/Service/Mapper/Entity）
  7. .harness/docs/data-flow.md（相关流程）
```

### Step 2: 需求分析

```yaml
输出:
  - 功能属于哪个模块？
  - 是否需要新建模块？
  - 涉及哪些数据库表？
  - 涉及哪些 Redis Key？
  - 涉及哪些 MQ Topic？
  - 需要调用哪些其他模块的 Service？
  - 是否有类似功能可参考？

检查:
  - 需求是否明确，有无歧义？
  - 是否违反现有架构约束？
  - 是否为未来微服务拆分引入障碍？
```

### Step 3: 技术方案输出

```yaml
方案内容:
  - API 设计（URL、Method、Request/Response）
  - 数据库变更（新表/新字段/迁移脚本）
  - DTO/VO/Entity 设计
  - Service 层逻辑概要
  - MQ 消息设计（如有）
  - Redis 缓存设计（如有）
  - 影响范围评估

输出方式: 直接向用户描述，复杂方案先列要点再逐条确认
```

### Step 4: 任务拆分

```yaml
拆分原则:
  - 先数据层（Entity/Mapper/迁移）
  - 再服务层（Service/ServiceImpl）
  - 最后接口层（Controller/DTO/VO）
  - 每个子任务独立可验证
  - 单子任务不超过 5 个文件

示例:
  1. 创建数据库迁移脚本
  2. 创建 Entity + Mapper
  3. 创建 DTO/VO
  4. 实现 Service + ServiceImpl
  5. 实现 Controller
  6. 运行验证
```

### Step 5: 编码实现

```yaml
按顺序执行:
  1. 数据库迁移（如有）: V{version}__{description}.sql
  2. Entity: @Data @TableName，继承 BaseEntity
  3. Mapper: extends BaseMapper<Entity>，复杂 SQL 写 XML
  4. DTO: Java record 或 @Data 请求类
  5. VO: Java record（不可变）
  6. ServiceImpl: @RequiredArgsConstructor，业务编排
  7. Controller: 协议转换，返回 Result<VO>

编码约束:
  - 遵循 .harness/rules/architecture-governance.md
  - 遵循 .harness/rules/module-boundary.md
  - 遵循 .harness/rules/backend-coding.md（后端）或 frontend-coding.md（前端）
```

### Step 6: 验证

```yaml
后端验证:
  mvn compile -q              # 编译
  mvn test -pl <模块> -q      # 测试

前端验证:
  npx tsc --noEmit           # 类型检查
  npm run build              # 构建

失败处理: 最多 3 次修复尝试，仍失败则报告用户
```

### Step 7: 自检

```yaml
自检清单:
  - [ ] Controller 只做协议转换
  - [ ] ServiceImpl 只调本模块 Mapper
  - [ ] DTO/VO 与 Entity 分离
  - [ ] 返回 Result<T> 包装
  - [ ] 异常用 BusinessException
  - [ ] 新增 Redis Key 有 TTL
  - [ ] MQ 消费者幂等
  - [ ] 数据库迁移可重复执行
  - [ ] 模块依赖在白名单内
```

### Step 8: Evaluation 评估（强制）

```yaml
必须按 evaluation/review-checklist.md 执行综合评估管线:

评估不通过:
  - FAIL 维度 → 修复后重新评估
  - CRITICAL 问题 → BLOCK
  - 3 次仍 FAIL → 暂停，向用户报告
```

### Step 9: Memory 写入

```yaml
任务完成后必须写入 memory/:
  - 如有架构/技术决策 → 追加到 memory/decisions.md
  - 如发现反模式 → 追加到 memory/anti-patterns.md
  - 如涉及架构变更 → 追加到 memory/architecture-history.md

写入规则:
  - 只追加，不修改已有条目
  - 包含时间戳和上下文
  - 按 state-manager.md 规范执行
```

## 约束验证

> 约束检查由 workflow-executor（`.harness/executors/workflow-executor.md`）统一执行。
> 功能开发需额外关注的检查点已在 Step 3（技术方案输出）中覆盖。
> 约束文件清单见 `execution-engine.md` §前置检查 + §约束验证。

## 评估

> 评估管线见 Step 8。本工作流不再重复定义。

## 回滚

```yaml
代码回滚:
  - 编译/测试失败: git checkout <file>
  - 3 次修复仍失败: 还原所有修改，向用户报告

数据库回滚:
  - Flyway 脚本不自动回滚
  - 如需回滚，需创建新的 Flyway 脚本逆向操作

约束违规回滚:
  - 违反 constraints/ 红线 → 立即停止，还原代码
  - 违反 rules/ → 修复代码使其合规
```

---

## 前端功能开发附加步骤

```yaml
前端附加:
  1. API 层: 在 src/api/ 添加类型化的请求函数
  2. Store: 如需全局状态，在 src/stores/ 添加 slice
  3. 页面/组件: 在 src/pages/ 或 src/components/ 实现
  4. 路由: 如需要，在路由配置中注册

前端约束:
  - API 函数使用泛型 request.get<T>() / request.post<T>()
  - 颜色使用 theme.useToken() 而非硬编码色值
  - 性能优化: useMemo/useCallback
  - 遵循 .harness/rules/frontend-coding.md
```
