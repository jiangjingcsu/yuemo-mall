---
name: "api-design"
description: "为功能设计 RESTful API 接口，包括 URL、请求参数、响应格式和错误码。当需要新增或修改 API 时触发。"
---

# API 设计 Skill

基于项目现有 API 模式和规范，为新功能设计 RESTful 接口。
> 治理规则：`.harness/rules/api-governance.md` `.harness/docs/api-conventions.md`

## 触发条件

- 新增后端接口
- 修改现有接口签名
- 用户要求设计 API
- 前后端联调前确认接口

## 与 Workflow 的关系

```yaml
定位: feature-development.md 工作流的 API 设计子工具

本 Skill 覆盖:
  - feature-development.md Step 3（API 设计）→ 本 Skill 全部步骤

联动 Skill:
  - 上游: skills/ddd-design/SKILL.md — 领域服务设计完成后，暴露为 API
  - 下游: skills/code-verify/SKILL.md — API 实现后验证契约一致性（第八层）
  - 关联: skills/requirement-breakdown/SKILL.md — 需求拆解时识别 API 变更
```

## 依赖上下文

- `.harness/rules/api-governance.md` — API 治理规则
- `.harness/docs/api-conventions.md` — API 约定
- `.harness/rules/architecture-governance.md` — 架构分层
- `.harness/rules/module-boundary.md` — 模块依赖（API 路径必须与模块对应）
- `.harness/rules/domain-modeling.md` — 充血模型规则（Entity 行为影响 API 设计）
- `.harness/memory/decisions.md` — 架构决策（避免与已否决方案冲突）
- 现有 Controller 源码（参考现有 API 模式）

## 设计流程

### Step 0: Memory 检查（推荐）

```yaml
读取:
  - memory/decisions.md — 是否有 API 相关架构决策？
    → 如有已否决方案: 在设计中标注
原则: 不与已有架构决策冲突
```

### Step 1: URL 设计

```yaml
格式: /api/{module}/{resource}[/{id}][/{action}]

参考:
  GET    /api/product/list          — 商品列表
  GET    /api/product/{id}          — 商品详情
  POST   /api/cart/add              — 加入购物车
  PUT    /api/cart/sku/{skuId}      — 修改购物车
  DELETE /api/cart/sku/{skuId}      — 删除购物车
  POST   /api/order/create          — 创建订单
  POST   /api/payment/pay           — 支付
  POST   /api/payment/refund        — 退款

规则:
  - URL 全小写，连字符分隔
  - 资源名用名词
  - 动作用 HTTP 方法表示（GET/POST/PUT/DELETE）
  - 复杂操作可用末尾动词（/create, /pay, /refund）
  - 列表查询统一用 /list 后缀（如 /api/product/list）
  - 禁止 URL 中包含动词用于简单 CRUD（/api/product/getById ❌ → GET /api/product/{id} ✅）
```

### Step 2: HTTP 方法选择

| 操作 | 方法 | 幂等 | 示例 |
|---|---|---|---|
| 查询列表 | GET | ✅ | `/api/product/list?page=1&size=20` |
| 查询详情 | GET | ✅ | `/api/product/{id}` |
| 创建 | POST | ❌ | `/api/order/create` |
| 全量更新 | PUT | ✅ | `/api/cart/sku/{skuId}` |
| 部分更新 | PATCH | ✅ | `/api/user/{id}/status` |
| 删除 | DELETE | ✅ | `/api/cart/sku/{skuId}` |

### Step 3: 请求参数设计

```yaml
简单参数: @RequestParam（查询）/ @PathVariable（标识）
复杂参数: @RequestBody + DTO/Request 类
当前用户: @RequestAttribute("userId") Long userId（GatewayAuthFilter 鉴权后注入）

分页参数:
  page: 当前页（从 1 开始）
  size: 每页数量
  sort: 排序字段（可选）

DTO 定义规则:
  - 简单 DTO 使用 Java record（如 AddCartRequest、ToggleSelectRequest）
  - 需要校验注解的 DTO 可使用 @Data 类（如 PayRequest、CreateOrderDTO）
  - 命名约定: 输入用 *Request 或 *DTO，放在 dto/ 包
  - 字段使用 @NotNull/@NotBlank/@NotEmpty 校验
  - 金额用 BigDecimal（用 @DecimalMin/@DecimalMax）
  - 自定义校验用 @Constraint
  - 禁止使用 Entity 接收请求参数
  - 禁止使用 Map 接收请求参数
  - Controller 中不手动校验参数（用 @Valid/@Validated）
```

### Step 4: 响应格式设计

```yaml
统一响应: Result<T>

成功:
  Result.success(data)         — 单条数据
  Result.success()             — 无返回数据（Void）
  Result.success(PageResult.from(iPage.convert(VO::from)))  — 分页

分页响应: PageResult<T>（common-core 提供）
  public record PageResult<T>(long total, int page, int size, List<T> list) {}
  转换: PageResult.from(iPage.convert(VO::from))

失败:
  抛 BusinessException(ResultCode.XXX)
  全局异常处理器自动转 Result(code, message, null)

VO 设计:
  - 使用 Java record（不可变）
  - 字段名 camelCase
  - 时间用 LocalDateTime（自动序列化为 ISO 格式）
  - 不包含 password 等敏感字段
  - 不包含 deleted 等 ORM 框架字段
  - 提供 static from(Entity) 工厂方法用于 Entity → VO 转换
  - 放在 vo/ 包

禁止:
  - 返回 Entity 类型（必须转 VO）
  - 返回 Map 类型
  - 返回原始类型（String、Integer 等——应包装到 Result）
  - 自定义响应包装（统一用 Result）
  - VO 中包含 password/secret 等敏感字段
```

### Step 5: 错误码设计

```yaml
错误码段:
  1001-1999: 用户模块
  2001-2999: 商品模块
  3001-3999: 订单模块
  4001-4999: 支付模块
  5001-5999: 购物车模块
  6001-6999: 营销模块
  7001-7999: 网关模块
  8001-8999: 后台管理模块
  9001-9999: 系统通用

当前已用错误码（ResultCode 枚举）:
  1001: 用户不存在
  1002: 密码错误
  1003: 用户已存在
  1004: Token 已过期
  2001: 商品不存在
  2002: 库存不足
  2003: 分类不存在
  2004: SKU不存在
  2005: SKU库存不足
  2006: 品牌不存在
  3001: 订单不存在
  3002: 订单状态异常
  3003: 订单创建失败
  4001: 支付失败
  4002: 支付回调处理异常
  4003: 支付记录不存在
  4004: 退款失败
  5001: 购物车商品不存在
  5002: 购物车商品数量超限
  6001: 优惠券不存在
  6002: 优惠券已过期
  6003: 优惠券已使用

规则:
  - 在 ResultCode 枚举中定义
  - 错误码唯一，不可重复使用
  - 新增错误码在对应模块范围内选择
  - 错误消息对用户友好（中文）
```

### Step 6: 鉴权设计

```yaml
必须鉴权: 所有 /api/** 接口（通过 GatewayAuthFilter JWT 鉴权）

获取当前用户:
  @RequestAttribute("userId") Long userId
  由 GatewayAuthFilter 鉴权后通过 request.setAttribute("userId", userId) 注入

白名单（无需认证）:
  - POST /api/user/login
  - POST /api/user/register
  - /api/payment/callback/**
  - /doc.html, /webjars/**, /actuator/**

半白名单（GET 放行，写操作需认证）:
  - GET /api/product/**

管理员接口:
  /api/admin/** 需要 ROLE_ADMIN
  GatewayAuthFilter 校验 Redis user:role:{userId} == "ADMIN"

回调接口（第三方支付回调）:
  - 返回类型为 String（非 Result<T>），因为第三方要求特定格式
  - 在白名单中放行
  - 必须做签名验证和幂等处理

Token 黑名单:
  登出时加入 Redis key: token:blacklist:{token}，TTL 30 分钟
```

### Step 7: API 文档注解

```yaml
工具: Knife4j (Swagger 增强)
访问: /doc.html

必须:
  - 新增 Controller 添加 @Tag(name = "模块名") 注解
  - 接口方法添加 @Operation(summary = "接口描述") 注解
  - 参数添加 @Parameter 注解（可选）
  - DTO 字段添加 @Schema 注解（可选）

禁止:
  - 文档注解中包含敏感信息
  - 在生产环境暴露 /doc.html（应通过配置控制）
```

## 输出格式

```markdown
## API 设计

### {接口名称}
- URL: {METHOD} /api/{module}/{path}
- 鉴权: {是/否/半白名单/仅ADMIN}
- 当前用户: @RequestAttribute("userId") Long userId（如需）
- 请求参数:
  | 参数 | 类型 | 必填 | 说明 |
- 响应格式: Result<{VO}>
- 分页: Result<PageResult<{VO}>>
- 错误码:
  | 错误码 | 说明 |
- API 文档: @Tag / @Operation 注解
```

## 约束

- 返回类型必须为 Result<T>（第三方回调接口除外）
- VO 必须为 Java record，提供 static from(Entity) 方法
- 禁止返回 Entity 类型（必须转 VO）
- 错误码在 ResultCode 中定义，不可重复
- 不与现有 API 路径冲突
- 遵循项目已有的 URL 命名风格
- 需要当前用户的接口使用 @RequestAttribute("userId")
- 新增 Controller 和方法必须添加 Knife4j 注解
- 新增 API 路径必须与模块对应（/api/{module}/... 中的 module 与 yuemo-modules 下的模块名一致）
- 跨模块 API 调用通过 Service 接口，Controller 不跨模块调 Service
