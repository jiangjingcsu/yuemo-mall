---
name: "code-verify"
description: "代码修改完成后执行多维度验证，确保编译通过、测试通过、规范合规、安全无虞。当任何代码修改完成后自动触发。"
---

# 代码验证 Skill

代码修改完成后，按层级递进执行多维度验证，确保变更正确、合规、安全。验证不通过则自动修复，最多 3 轮。

## 触发条件

当出现以下场景时，必须激活此 Skill：

- 任何后端 Java/SQL 代码修改完成后
- 任何前端 TypeScript/TSX/CSS 代码修改完成后
- 配置文件（application.yml / docker-compose.yml / nginx.conf 等）修改完成后
- 用户明确要求验证代码正确性

## 验证流程

验证按以下层级递进执行，**前一层失败则不执行后续层**，先修复再继续。

### 第一层：编译验证（必须通过）

确保代码在语法和类型层面无错误。

**后端编译验证：**

```bash
cd yuemo-backend && mvn compile -q
```

| 检查项 | 说明 |
|-------|------|
| 编译错误 | Java 语法错误、类型不匹配、缺少导入等 |
| 依赖问题 | 缺少依赖、版本冲突 |
| 注解处理 | Lombok 生成代码是否正确 |

**前端编译验证：**

```bash
cd yuemo-frontend && npx tsc --noEmit
```

| 检查项 | 说明 |
|-------|------|
| 类型错误 | TypeScript 类型不匹配、缺少类型定义 |
| 严格模式 | strict: true 下的隐式 any、null 引用等 |

### 第二层：构建验证（必须通过）

确保项目可以完整构建打包。

**后端构建验证：**

```bash
cd yuemo-backend && mvn package -DskipTests -q
```

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
| Vite 构建 | 无构建错误、无未解析模块 |
| 产物生成 | dist 目录正常生成 |

### 第三层：测试验证（必须通过）

运行受影响模块的单元测试。

**后端测试验证：**

```bash
cd yuemo-backend && mvn test -pl <修改的模块> -q
```

| 检查项 | 说明 |
|-------|------|
| 单元测试 | 修改模块的测试全部通过 |
| 回归影响 | 修改是否导致其他模块测试失败 |

模块路径映射：

| 业务模块 | Maven 模块路径 |
|---------|--------------|
| 用户 | yuemo-modules/user |
| 商品 | yuemo-modules/product |
| 订单 | yuemo-modules/order |
| 支付 | yuemo-modules/payment |
| 购物车 | yuemo-modules/cart |
| 促销 | yuemo-modules/promotion |
| 网关 | yuemo-gateway |
| 后台管理 | yuemo-admin |
| 公共模块 | yuemo-common |

**前端测试验证：**

```bash
cd yuemo-frontend && npm test -- --watchAll=false
```

若无测试脚本或无相关测试用例，跳过此层并记录"无相关测试覆盖"。

### 第四层：规范合规检查（必须通过）

基于 `.harness/rules/` 下的编码规范，逐项检查修改的代码是否合规。

**后端规范检查清单**（依据 `rules/backend-coding.md`）：

| # | 检查项 | 验证方式 |
|---|--------|---------|
| 1 | Controller 不含业务逻辑 | 人工审查 Controller 方法体 |
| 2 | 依赖注入使用 `@RequiredArgsConstructor`，无 `@Autowired` | grep `@Autowired` 无结果 |
| 3 | DTO/VO 使用 record，非普通类 | 检查新增 DTO/VO 定义 |
| 4 | Entity 继承 BaseEntity，使用 `@Data` + `@EqualsAndHashCode(callSuper = true)` | 检查新增 Entity |
| 5 | 无字符串拼接 SQL，MyBatis 用 `#{}` 非 `${}` | grep `${}` 在 Mapper XML |
| 6 | 逻辑删除，无物理删除 | 检查 delete 方法 |
| 7 | `@Transactional(rollbackFor = Exception.class)` | 检查事务注解 |
| 8 | 错误码在 `ResultCode` 枚举中定义，无硬编码 | 检查新增错误码 |
| 9 | 日志使用 `{}` 占位符，无字符串拼接 | grep `log\.\w+(".*\+"` |
| 10 | 日志不输出敏感数据 | 检查日志内容 |
| 11 | Controller 返回 `Result<T>` | 检查返回类型 |
| 12 | 集合方法返回空集合而非 null | 检查返回语句 |
| 13 | 金额使用 BigDecimal，无 float/double | 检查金额字段类型 |
| 14 | 时间使用 `java.time`，无 `Date`/`SimpleDateFormat` | grep 检查 |
| 15 | 线程池使用 `ThreadPoolExecutor`，无 `Executors.newXxx()` | grep 检查 |

**前端规范检查清单**（依据 `rules/frontend-coding.md`）：

| # | 检查项 | 验证方式 |
|---|--------|---------|
| 1 | 函数组件，无 class 组件 | grep `class.*extends` |
| 2 | Props 定义 `interface Props`，无 `any` | 检查组件定义 |
| 3 | API 调用通过 `request.ts`，无直接 axios | grep `import.*from 'axios'` |
| 4 | 无硬编码 API 地址 | grep `http://` / `https://` |
| 5 | 无 `any` 类型 | grep `: any` / `as any` |
| 6 | 无 `enum`，使用 `as const` 对象 | grep `^enum ` |
| 7 | 列表渲染使用业务 ID 作 key，非 index | 检查 map 渲染 |
| 8 | 无空 catch 吞掉错误 | grep `catch\s*\(\w*\)\s*\{\s*\}` |
| 9 | 无 `dangerouslySetInnerHTML` | grep 检查 |
| 10 | 路径使用 `@/` 别名，无超过 2 层的 `../../` | grep `\.\./\.\./\.\./` |
| 11 | 事件处理函数命名 `handle+动作` | 检查事件绑定 |
| 12 | 布尔变量 `is/has/should` 前缀 | 检查变量命名 |

### 第五层：安全合规检查（必须通过）

基于 `.harness/rules/security.md`，检查修改是否引入安全问题。

| # | 检查项 | 验证方式 |
|---|--------|---------|
| 1 | 无密码/密钥硬编码 | grep `password\s*=` / `secret\s*=` / `key\s*=` |
| 2 | MyBatis 无 `${}` 拼接用户输入 | 检查 Mapper XML |
| 3 | API 响应不含 password 字段 | 检查 VO/响应类 |
| 4 | 日志不输出敏感数据 | 检查日志语句 |
| 5 | 用户输入存储前有转义/校验 | 检查输入处理 |
| 6 | 越权校验：用户只能操作自己的数据 | 检查数据访问逻辑 |
| 7 | 管理接口有 ADMIN 角色校验 | 检查接口鉴权 |
| 8 | 前端无 `dangerouslySetInnerHTML` | grep 检查 |
| 9 | 前端不在 URL 中传递敏感信息 | 检查路由和请求参数 |

### 第六层：API 契约验证（按需）

当修改涉及 API 变更时执行。

| # | 检查项 | 验证方式 |
|---|--------|---------|
| 1 | API 路径遵循 `/api/{module}/...` 或 `/api/admin/...` 格式 | 检查 Controller @RequestMapping |
| 2 | HTTP 方法语义正确（GET 查询 / POST 创建 / PUT 更新 / DELETE 删除） | 检查注解 |
| 3 | 响应格式遵循 `Result<T>` 信封 | 检查返回类型 |
| 4 | 分页响应包含 records/total/size/current | 检查分页接口 |
| 5 | 前端 API 函数与后端接口签名一致 | 对比 api/*.ts 与 Controller |
| 6 | 前端类型定义与后端 VO 字段一致 | 对比 TypeScript 类型与 Java record |

## 验证结果输出

验证完成后，输出结构化结果：

```
🔍 代码验证报告

✅ 第一层：编译验证 — 通过
   - 后端 mvn compile：0 错误
   - 前端 tsc --noEmit：0 错误

✅ 第二层：构建验证 — 通过
   - 后端 mvn package：成功
   - 前端 npm run build：成功

✅ 第三层：测试验证 — 通过
   - yuemo-modules/product：12/12 通过
   - 无回归失败

⚠️ 第四层：规范合规 — 2 项待修复
   - ❌ BE-7: ProductService.getProductDetail 缺少 @Transactional(rollbackFor)
   - ❌ FE-3: ProductList.tsx 使用 index 作为列表 key

✅ 第五层：安全合规 — 通过

⏭️ 第六层：API 契约 — 跳过（无 API 变更）

📊 总结：4/6 层完全通过，2 项规范问题待修复
```

## 自动修复循环

验证失败时的处理流程：

1. **分析错误**：读取错误输出，定位根因
2. **自动修复**：修改代码解决错误
3. **重新验证**：从失败层重新执行验证
4. **循环上限**：同一错误最多修复 3 次

3 次仍失败 → **暂停**，向用户报告：
- 错误的完整信息
- 已尝试的修复方案
- 建议的下一步操作

## 特殊场景处理

| 场景 | 处理方式 |
|------|---------|
| 仅修改配置文件 | 跳过编译和测试层，执行构建验证 + 语法检查 |
| 仅修改 SQL 脚本 | 检查 SQL 语法、命名规范（`yu_` 前缀）、字段类型 |
| 仅修改前端样式 | 执行前端编译 + 构建验证，跳过后端验证 |
| 仅修改文档/注释 | 跳过所有验证层 |
| 新增文件 | 额外检查文件位置、命名规范、包声明 |
| 删除文件 | 检查是否有其他文件引用被删除的类/函数/组件 |

## 约束规则

1. **验证不可跳过**：任何代码修改完成后必须执行验证，不得声称"修改完成"却未验证
2. **逐层递进**：前一层失败不执行后续层，避免浪费时间和掩盖问题
3. **修复后全量验证**：修复代码后，从第一层重新执行，而非仅验证修复点
4. **如实报告**：验证失败必须如实报告，不得隐瞒或跳过
5. **不修改测试来通过验证**：测试失败时修复业务代码，而非修改测试用例使其通过（除非测试本身有误）
