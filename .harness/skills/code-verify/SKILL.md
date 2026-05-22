---
name: "code-verify"
description: "代码修改完成后执行多维度验证，确保编译通过、测试通过、规范合规、安全无虞。当任何代码修改完成后自动触发。"
---

# 代码验证 Skill

代码修改完成后，按层级递进执行多维度验证，确保变更正确、合规、安全。验证不通过则自动修复，最多 3 轮。
> 治理规则：`.harness/rules/code-smell-governance.md` `.harness/rules/domain-modeling.md` `.harness/evaluation/` `.harness/memory/anti-patterns.md`

## 触发条件与变更分级

当出现以下场景时，必须激活此 Skill：

- 任何后端 Java/SQL 代码修改完成后
- 任何前端 TypeScript/TSX/CSS 代码修改完成后
- 配置文件（application.yml / docker-compose.yml / nginx.conf 等）修改完成后
- 用户明确要求验证代码正确性

### 变更分级

根据变更影响范围，确定验证执行的层级，避免轻微变更触发全量验证：

| 变更级别 | 触发条件 | 执行验证层 | 示例 |
|---------|---------|-----------|------|
| L1-轻微 | 仅注释/格式/import/拼写修改 | Layer 1 + 2 | 修正注释、调整 import 顺序 |
| L2-一般 | 业务逻辑修改、新增方法/类 | Layer 1-5 | 新增 Service 方法、修改 DTO 字段 |
| L3-重大 | 新增/变更 API、数据库变更、支付相关、跨模块修改 | Layer 1-8 | 新增接口、Flyway 迁移、支付流程修改 |

**判定规则**：
- 涉及支付模块（yuemo-payment）的任何修改默认 L3
- 涉及跨模块修改默认 L3
- 修改 Entity/数据库表结构默认 L3
- 不确定时按高一级处理

## 与 Workflow 的关系

```yaml
定位: 多工作流的验证子工具

本 Skill 被以下工作流调用:
  - feature-development.md Step 6 — 新功能开发完成后验证
  - bug-fix.md Step 5 — Bug 修复后验证
  - refactor.md Step 5 — 重构后验证

联动 Skill:
  - 上游: skills/refactor-analysis/SKILL.md — 验证发现需要重构时，进入重构分析
  - 关联: skills/ddd-design/SKILL.md — 领域模型合规性参考
```

## 验证流程

验证按以下层级递进执行，**前一层失败则不执行后续层**，先修复再继续。

### 第一层：编译验证（必须通过）

确保代码在语法和类型层面无错误。

**后端编译验证：**

```bash
cd yuemo-backend && mvn compile -pl <修改的模块> -am -q
```

> `-pl` 指定修改的模块，`-am` 同时编译依赖模块（also-make）。若修改了 common 模块或不确定影响范围，则执行全量 `mvn compile -q`。

| 检查项 | 说明 |
|-------|------|
| 编译错误 | Java 语法错误、类型不匹配、缺少导入等 |
| 依赖问题 | 缺少依赖、版本冲突 |
| 注解处理 | Lombok 生成代码是否正确 |
| MapStruct | Mapper 接口转换方法签名是否匹配 source/target 类型 |

**前端编译验证：**

```bash
cd yuemo-frontend && npm run lint
```

| 检查项 | 说明 |
|-------|------|
| ESLint 规则 | 代码风格、潜在错误、React hooks 规则 |
| TypeScript 类型 | 严格模式下的类型错误（由 ESLint + @typescript-eslint 插件覆盖） |

> 前端 `npm run build`（即 `tsc -b && vite build`）已在第二层覆盖完整类型检查和构建，第一层使用 `npm run lint` 做快速语法和规则检查，避免与第二层冗余。

### 第二层：构建验证（必须通过）

确保项目可以完整构建打包。

**后端构建验证：**

```bash
cd yuemo-backend && mvn package -pl <修改的模块> -am -DskipTests -q
```

> 同第一层，优先增量构建；修改 common 模块时执行全量 `mvn package -DskipTests -q`。

| 检查项 | 说明 |
|-------|------|
| 打包成功 | Spring Boot fat jar 生成 |
| 资源文件 | application.yml / mapper XML 等资源正确打包 |

**前端构建验证：**

```bash
cd yuemo-frontend && npm run build
```

| 检查项 | 说明 |
|-------|------|
| TypeScript 编译 | `tsc -b` 类型检查通过 |
| Vite 构建 | 无构建错误、无未解析模块 |
| 产物生成 | dist 目录正常生成 |

### 第三层：测试验证（必须通过）

运行受影响模块的单元测试。

**后端测试验证：**

```bash
cd yuemo-backend && mvn test -pl <修改的模块> -am -q
```

| 检查项 | 说明 |
|-------|------|
| 单元测试 | 修改模块的测试全部通过 |
| 回归影响 | 修改是否导致依赖模块测试失败（`-am` 确保依赖模块也参与） |

模块路径映射：

| 业务模块 | Maven 模块路径 | artifactId |
|---------|--------------|------------|
| 用户 | yuemo-modules/yuemo-user | yuemo-user |
| 商品 | yuemo-modules/yuemo-product | yuemo-product |
| 订单 | yuemo-modules/yuemo-order | yuemo-order |
| 支付 | yuemo-modules/yuemo-payment | yuemo-payment |
| 购物车 | yuemo-modules/yuemo-cart | yuemo-cart |
| 促销 | yuemo-modules/yuemo-promotion | yuemo-promotion |
| 网关 | yuemo-gateway | yuemo-gateway |
| 后台管理 | yuemo-admin | yuemo-admin |
| 公共核心 | yuemo-common/common-core | common-core |
| 公共安全 | yuemo-common/common-security | common-security |
| 公共MyBatis | yuemo-common/common-mybatis | common-mybatis |
| 启动入口 | yuemo-server | yuemo-server |

> `-pl` 参数使用 artifactId 或相对路径均可。修改 common 模块时，建议执行全量测试 `mvn test -q`，因为所有业务模块都依赖 common。

**前端测试验证：**

```bash
cd yuemo-frontend && npm test -- --watchAll=false
```

> ⚠️ 当前前端项目未配置 `test` 脚本（package.json 中无 test 命令），此步骤跳过并记录"前端无测试覆盖"。待前端测试框架就绪后启用。

若无测试脚本或无相关测试用例，跳过此层并记录"无相关测试覆盖"。

### 第四层：架构治理检查（必须通过）

基于新增的治理规则，逐项检查架构合规。

| # | 检查项 | 参考规则 | 验证方式 |
|---|--------|---------|---------|
| 1 | Controller 不直接调 Mapper | architecture-governance.md | grep `Mapper\|mapper` 在 Controller 文件 |
| 2 | ServiceImpl 不调其他模块 Mapper（cart→product 只读除外） | module-boundary.md | 检查 import 语句是否引入其他模块的 Mapper |
| 3 | 不跨模块直接操作数据库表 | module-boundary.md | 检查 Mapper XML 的 namespace 是否属于本模块 |
| 4 | DTO/VO/Entity 严格分离，Entity 非贫血模型 | architecture-governance.md | 检查 Entity 是否含业务方法（非纯 getter/setter）（参考 domain-modeling.md 充血模型规则） |
| 5 | 未引入模块循环依赖 | module-boundary.md | 检查 pom.xml 依赖链 |
| 6 | 新增模块依赖在白名单内 | module-boundary.md | 检查新增 dependency |
| 7 | 无 switch-case 行为分发（AP001） | anti-patterns.md | grep `switch` 在 MQ 消费者/支付/状态流转代码 |
| 8 | 无 if-else 链支付方式判断（AP002） | anti-patterns.md | 检查支付模块是否使用策略模式 |
| 9 | 无魔法值（AP004） | anti-patterns.md | 检查状态/类型字段的字面量使用 |

### 第五层：规范合规检查（必须通过）

基于 `.harness/rules/` 下的编码规范，逐项检查修改的代码是否合规。

**后端规范检查清单**（依据 `rules/backend-coding.md`）：

| # | 检查项 | 验证方式 | 类型 |
|---|--------|---------|------|
| 1 | Controller 不含业务逻辑 | 审查 Controller 方法体：无复杂计算/条件分支/事务注解 | 🔍 人工 |
| 2 | 依赖注入使用 `@RequiredArgsConstructor`，无 `@Autowired` | grep `@Autowired` 在 `*.java` 无结果 | 🤖 自动 |
| 3 | DTO/VO 使用 record，非普通类 | 检查新增 DTO/VO 定义是否使用 `record` 关键字 | 🤖 自动 |
| 4 | Entity 继承 BaseEntity，使用 `@Data` + `@EqualsAndHashCode(callSuper = true)` | 检查新增 Entity 定义 | 🤖 自动 |
| 5 | 无字符串拼接 SQL，MyBatis 用 `#{}` 非 `${}` | grep `\$\{` 在 `*Mapper.xml` 无结果 | 🤖 自动 |
| 6 | 逻辑删除，无物理删除 | 检查 delete 方法使用 `@TableLogic` 或逻辑删除字段 | 🔍 人工 |
| 7 | `@Transactional(rollbackFor = Exception.class)` | grep `@Transactional` 检查是否缺少 `rollbackFor` | 🤖 自动 |
| 8 | 错误码在 `ResultCode` 枚举中定义，无硬编码 | 检查新增错误码是否在枚举中注册 | 🔍 人工 |
| 9 | 日志使用 `{}` 占位符，无字符串拼接 | grep `log\.\w+\("[^"]*\+` 在 `*.java` 无结果 | 🤖 自动 |
| 10 | 日志不输出敏感数据 | 检查日志内容不含 password/token/secret | 🔍 人工 |
| 11 | Controller 返回 `Result<T>` | 检查 Controller 方法返回类型 | 🤖 自动 |
| 12 | 集合方法返回空集合而非 null | 检查返回语句使用 `Collections.emptyList()` / `List.of()` | 🔍 人工 |
| 13 | 金额使用 BigDecimal，无 float/double | 检查金额字段类型 | 🤖 自动 |
| 14 | 时间使用 `java.time`，无 `Date`/`SimpleDateFormat` | grep `java\.util\.Date\|SimpleDateFormat` 在 `*.java` 无结果 | 🤖 自动 |
| 15 | 线程池使用 `ThreadPoolExecutor`，无 `Executors.newXxx()` | grep `Executors\.new` 在 `*.java` 无结果 | 🤖 自动 |
| 16 | MapStruct Mapper 接口使用 `@Mapper(componentModel = "spring")` | 检查新增 MapStruct Mapper 定义 | 🤖 自动 |

**前端规范检查清单**（依据 `rules/frontend-coding.md`）：

| # | 检查项 | 验证方式 | 类型 |
|---|--------|---------|------|
| 1 | 函数组件，无 class 组件 | grep `class.*extends` 在 `*.tsx` 无结果 | 🤖 自动 |
| 2 | Props 定义 `interface Props`，无 `any` | 检查组件 Props 定义 | 🔍 人工 |
| 3 | API 调用通过 `request.ts`，无直接 axios | grep `import.*from\s+['"]axios['"]` 在 `*.ts` `*.tsx` 无结果 | 🤖 自动 |
| 4 | 无硬编码 API 地址 | grep `https?://` 在 `*.ts` `*.tsx`（排除注释和常量定义文件） | 🤖 自动 |
| 5 | 无 `any` 类型 | grep `:\s*any\b\|as\s+any\b` 在 `*.ts` `*.tsx` 无结果 | 🤖 自动 |
| 6 | 无 `enum`，使用 `as const` 对象 | grep `^enum\s` 在 `*.ts` 无结果 | 🤖 自动 |
| 7 | 列表渲染使用业务 ID 作 key，非 index | 检查 `.map()` 渲染的 key 属性 | 🔍 人工 |
| 8 | 无空 catch 吞掉错误 | grep `catch\s*\(\w*\)\s*\{\s*\}` 在 `*.ts` `*.tsx` 无结果 | 🤖 自动 |
| 9 | 无 `dangerouslySetInnerHTML` | grep `dangerouslySetInnerHTML` 在 `*.tsx` 无结果 | 🤖 自动 |
| 10 | 路径使用 `@/` 别名，无超过 2 层的 `../../` | grep `\.\./\.\./\.\./` 在 `*.ts` `*.tsx` 无结果 | 🤖 自动 |
| 11 | 事件处理函数命名 `handle+动作` | 检查事件绑定函数命名 | 🔍 人工 |
| 12 | 布尔变量 `is/has/should` 前缀 | 检查布尔变量命名 | 🔍 人工 |

> 类型说明：🤖 自动 = 可通过 grep/编译器自动检测；🔍 人工 = 需 AI 或人工审查代码逻辑

### 第六层：数据库与缓存检查（必须通过）

基于数据库/Redis/MQ 治理规则，检查数据层合规。

| # | 检查项 | 参考规则 | 验证方式 |
|---|--------|---------|---------|
| 1 | 新增表含必备字段（id/create_time/update_time/deleted） | database-governance.md | 检查 Flyway 迁移脚本 DDL |
| 2 | SQL 使用 #{} 参数化，无 SELECT * | database-governance.md | grep `SELECT\s+\*` 和 `\$\{` 在 `*Mapper.xml` |
| 3 | 新增 Redis Key 符合命名规范且有 TTL | redis-governance.md | 检查 RedisTemplate 操作的 Key 前缀和 expire 设置 |
| 4 | MQ 消费者支持幂等 | mq-governance.md | grep `@RocketMQMessageListener` 找到消费者类，检查是否使用 `RedisTemplate.opsForValue().setIfAbsent()` 做幂等去重，Key 格式是否为 `mq:consumed:{topic}:{业务唯一标识}`，删除幂等 Key 的策略是否合理 |
| 5 | MQ Topic 命名符合规范（`{domain}-{event}`） | mq-governance.md | 检查 Topic 常量定义 |
| 6 | Flyway 迁移脚本命名规范（`V{version}__{desc}.sql`） | decisions.md D006 | 检查新增 SQL 文件名格式 |
| 7 | 未修改已执行的 Flyway 脚本 | incidents.md INC002 | 检查 git diff 是否包含 `db/migration/` 下已有文件的修改 |
| 8 | 唯一键包含 deleted 字段 | anti-patterns.md AP006 | 检查新增唯一索引是否含 deleted 列 |

### 第七层：安全合规检查（必须通过）

基于 `.harness/rules/security.md`，检查修改是否引入安全问题。

| # | 检查项 | 验证方式 | 类型 |
|---|--------|---------|------|
| 1 | 无密码/密钥硬编码 | grep `password\s*=\s*["']` / `secret\s*=\s*["']` 在 `*.java` `*.yml`（排除配置模板和占位符） | 🤖 自动 |
| 2 | MyBatis 无 `${}` 拼接用户输入 | 检查 Mapper XML 中 `${}` 的上下文（排序字段等安全用法除外） | 🔍 人工 |
| 3 | API 响应不含 password 字段 | 检查 VO/响应类字段列表 | 🤖 自动 |
| 4 | 日志不输出敏感数据 | 检查日志语句不含 password/token/secret/手机号 | 🔍 人工 |
| 5 | 用户输入存储前有转义/校验 | 检查 Controller `@Valid` 注解和 Service 输入处理 | 🔍 人工 |
| 6 | 越权校验：用户只能操作自己的数据 | 检查数据访问逻辑是否校验 userId 归属 | 🔍 人工 |
| 7 | 管理接口有 ADMIN 角色校验 | 检查 `/api/admin/**` 接口鉴权注解 | 🤖 自动 |
| 8 | 前端无 `dangerouslySetInnerHTML` | grep `dangerouslySetInnerHTML` 在 `*.tsx` 无结果 | 🤖 自动 |
| 9 | 前端不在 URL 中传递敏感信息 | 检查路由和请求参数不含 token/password | 🔍 人工 |
| 10 | JWT 密钥长度 >= 256 bits | 检查配置中 JWT 密钥长度（参考 INC003） | 🤖 自动 |
| 11 | XSS 防护：用户输入经转义后输出 | 检查是否使用 XssHttpServletRequestWrapper（参考 TD004） | 🔍 人工 |

### 第八层：API 契约验证（按需）

当修改涉及 API 变更时执行。

| # | 检查项 | 验证方式 |
|---|--------|---------|
| 1 | API 路径遵循 `/api/{module}/...` 或 `/api/admin/...` 格式 | 检查 Controller @RequestMapping |
| 2 | HTTP 方法语义正确（GET 查询 / POST 创建 / PUT 更新 / DELETE 删除） | 检查注解 |
| 3 | 响应格式遵循 `Result<T>` 信封 | 检查返回类型 |
| 4 | 分页响应包含 records/total/size/current | 检查分页接口 |
| 5 | 前端 API 函数与后端接口签名一致 | 对比 api/*.ts 与 Controller |
| 6 | 前端类型定义与后端 VO 字段一致 | 对比 TypeScript 类型与 Java record |
| 7 | Knife4j 注解与实际接口一致（@ApiOperation / @Schema） | 检查注解是否描述正确 |

## 验证结果输出

验证完成后，输出结构化结果：

```
代码验证报告

变更级别：L2-一般
执行层级：Layer 1-5

✅ 第一层：编译验证 — 通过
   - 后端 mvn compile -pl yuemo-order -am：0 错误
   - 前端 npm run lint：0 错误，0 警告

✅ 第二层：构建验证 — 通过
   - 后端 mvn package -pl yuemo-order -am -DskipTests：成功
   - 前端 npm run build：成功

✅ 第三层：测试验证 — 通过
   - 后端 mvn test -pl yuemo-order -am：0 失败
   - 前端：无测试覆盖（跳过）

✅ 第四层：架构治理 — 通过
✅ 第五层：规范合规 — 通过

⏭️ 第六层：数据库与缓存 — 跳过（变更级别 L2）
⏭️ 第七层：安全合规 — 跳过（变更级别 L2）
⏭️ 第八层：API 契约 — 跳过（无 API 变更）

总结：5/5 层通过（L2 级别），0 项待修复
```

## 自动修复循环

验证失败时的处理流程：

1. **分析错误**：读取错误输出，定位根因
2. **自动修复**：修改代码解决错误
3. **重新验证**：从失败层的前一层重新执行验证（而非从第一层全量重验）
4. **循环上限**：同一错误最多修复 3 次

> **重新验证起点规则**：修复后从失败层的前一层开始验证。例如 Layer 5 失败，修复后从 Layer 4 开始。这样既确保前序层未被修复影响，又避免全量重验的时间浪费。但以下情况必须从 Layer 1 全量重验：
> - 修改了 common 模块（影响所有模块）
> - 修改了 pom.xml 依赖（可能影响编译）
> - 修改了配置文件（可能影响构建）

3 次仍失败 → **暂停**，向用户报告：
- 错误的完整信息
- 已尝试的修复方案
- 建议的下一步操作

## 与 Evaluation 管线的关系

本 Skill 与 `.harness/evaluation/` 管线互补，分工如下：

| 维度 | 本 Skill | Evaluation 管线 |
|------|---------|----------------|
| 定位 | 代码修改后的**即时验证** | 任务完成后的**综合评估** |
| 触发时机 | 每次代码修改后自动触发 | 任务交付前执行 |
| 检查范围 | 修改涉及的文件和模块 | 全项目架构和代码质量 |
| 代码质量指标（方法行数/圈复杂度） | 不检查 | code-quality.md 检查 |
| AI 幻觉检测 | 不检查 | hallucination-check.md 检查 |
| 风险评估 | 不检查 | risk-analysis.md 检查 |
| 设计模式合规 | 仅检查反模式（AP001-AP004） | design-pattern-governance.md 全面检查 |
| 领域模型合规 | 仅检查 Entity 非贫血（Layer 4 #4） | domain-modeling.md 全面检查 |

> 本 Skill 通过后，仍需在任务交付前执行 Evaluation 管线做综合评估。

## 特殊场景处理

| 场景 | 处理方式 |
|------|---------|
| 仅修改配置文件 | 跳过编译和测试层，执行构建验证 + 语法检查 |
| 仅修改 SQL 脚本 | 检查 SQL 语法、命名规范（`yu_` 前缀）、字段类型 + Flyway 规范（Layer 6 #6-#8） |
| 仅修改前端样式 | 执行前端 lint + 构建验证，跳过后端验证 |
| 仅修改文档/注释 | 跳过所有验证层 |
| 新增文件 | 额外检查文件位置、命名规范、包声明 |
| 删除文件 | 检查是否有其他文件引用被删除的类/函数/组件 |
| 修改支付模块 | 强制 L3 级别，全量验证 + Evaluation 管线 |

## 约束规则

1. **验证不可跳过**：任何代码修改完成后必须执行验证，不得声称"修改完成"却未验证
2. **逐层递进**：前一层失败不执行后续层，避免浪费时间和掩盖问题
3. **修复后定向重验**：修复代码后，从失败层的前一层重新执行验证；修改 common/pom/配置时从 Layer 1 全量重验
4. **如实报告**：验证失败必须如实报告，不得隐瞒或跳过
5. **不修改测试来通过验证**：测试失败时修复业务代码，而非修改测试用例使其通过（除非测试本身有误）
6. **变更分级必须判定**：每次验证前必须先判定变更级别，按级别执行对应层级
7. **验证发现架构/设计问题时，应建议使用 refactor-analysis Skill 进行深入分析**
