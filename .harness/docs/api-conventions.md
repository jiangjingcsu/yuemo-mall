# API 约定与模式

> 快速参考文档。详细治理规则见 `rules/api-governance.md`，API 设计技能见 `skills/api-design/SKILL.md`。

## 响应格式

统一使用 `Result<T>` 响应信封：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 业务状态码，200 表示成功 |
| message | string | 提示信息 |
| data | T | 响应数据（泛型），错误时为 null |

```java
Result.success(data)                    // 成功响应
Result.fail(ResultCode.XXX)             // 失败响应（通过 BusinessException 抛出，全局拦截器转 Result）
```

## 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "size": 10,
    "list": []
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| total | long | 总记录数 |
| page | int | 当前页码（从 1 开始） |
| size | int | 每页数量 |
| list | List<T> | 当前页数据列表（VO 类型） |

分页转换：

```java
PageResult.from(iPage.convert(VO::from))           // Service 返回 IPage<Entity>，Controller 中转换
PageResult.from(service.pageXxx(...).convert(VO::from))  // 等价写法
```

## HTTP 方法约定

| 方法 | 用途 | 幂等 | 示例 |
|---|---|---|---|
| GET | 查询资源 | ✅ | `GET /api/product/{id}` |
| POST | 创建资源 / 复杂操作 | ❌ | `POST /api/order/create` |
| PUT | 更新资源 | ✅ | `PUT /api/cart/sku/{skuId}` |
| DELETE | 删除资源 | ✅ | `DELETE /api/cart/sku/{skuId}` |

> **注意**：项目当前未使用 PATCH 方法。部分更新场景（如切换选中状态、设默认地址）使用 PUT。

## URL 路径规范

```
C 端接口:   /api/{module}/{resource}[/{id}][/{action}]
管理端接口: /api/admin/{module}/{resource}[/{id}][/{action}]

示例:
  GET    /api/product/list              — 商品列表
  GET    /api/product/{id}              — 商品详情
  POST   /api/cart/add                  — 加入购物车
  POST   /api/order/create              — 创建订单
  POST   /api/payment/pay               — 发起支付
  GET    /api/admin/product/list        — 管理端商品列表
  POST   /api/admin/product             — 管理端创建商品
  PUT    /api/admin/product/{id}        — 管理端更新商品
```

## 鉴权分层

| 层级 | 说明 | 示例 |
|---|---|---|
| 白名单 | 完全免鉴权 | `/api/user/login`、`/api/user/register`、`/api/payment/callback/**`、`/doc.html` |
| 半白名单 | GET 请求免鉴权，写操作需认证 | `GET /api/product/**`、`GET /api/category/**` |
| 需登录 | 需 JWT Token，通过 `@RequestAttribute("userId")` 获取用户 ID | 大部分 `/api/**` 接口 |
| 需 ADMIN | `/api/admin/**` 额外校验 ADMIN 角色 | 管理端接口 |

## 当前用户获取

- 鉴权接口通过 `@RequestAttribute("userId") Long userId` 获取当前用户 ID
- 由 `GatewayAuthFilter`（`@Order(1)`）JWT 鉴权后通过 `request.setAttribute("userId", userId)` 注入
- Token 从 `Authorization: Bearer <token>` 请求头提取
- 登出后 Token 加入 Redis 黑名单

## 异常处理

- 全局异常通过 `@RestControllerAdvice`（`GlobalExceptionHandler`）统一处理
- 业务异常抛出 `BusinessException`（自定义），HTTP 状态码返回 200，业务错误码在 JSON body 的 `code` 字段
- 参数校验使用 `@Valid` / `@Validated`，校验失败返回 400 + `Result.fail(ResultCode.BAD_REQUEST, message)`
- `@RequestAttribute("userId")` 缺失时返回 401 + `Result.fail(ResultCode.UNAUTHORIZED, "请先登录")`
- 错误响应格式与正常响应一致，code 为对应错误码

## 错误码规范

### 错误码分段

| 码段 | 模块 |
|---|---|
| 1001-1999 | 用户模块 |
| 2001-2999 | 商品模块 |
| 3001-3999 | 订单模块 |
| 4001-4999 | 支付模块 |
| 5001-5999 | 购物车模块 |
| 6001-6999 | 营销模块 |
| 9001-9999 | 系统通用 |

### 已用错误码

| 错误码 | 说明 |
|---|---|
| 1001 | 用户不存在 |
| 1002 | 密码错误 |
| 1003 | 用户已存在 |
| 1004 | Token 已过期 |
| 2001 | 商品不存在 |
| 2002 | 库存不足 |
| 2003 | 分类不存在 |
| 2004 | SKU不存在 |
| 2005 | SKU库存不足 |
| 2006 | 品牌不存在 |
| 3001 | 订单不存在 |
| 3002 | 订单状态异常 |
| 3003 | 订单创建失败 |
| 4001 | 支付失败 |
| 4002 | 支付回调处理异常 |
| 4003 | 支付记录不存在 |
| 4004 | 退款失败 |
| 5001 | 购物车商品不存在 |
| 5002 | 购物车商品数量超限 |
| 6001 | 优惠券不存在 |
| 6002 | 优惠券已过期 |
| 6003 | 优惠券已使用 |

> 新增错误码在对应模块范围内选择，错误码唯一不可重复，定义在 `ResultCode` 枚举中。

## DTO 规范

- 入参使用独立 DTO/Request 类（放在 `dto/` 包），禁止使用 Entity 或 Map 接收参数
- 简单 DTO 使用 Java record，需要校验注解的 DTO 使用 `@Data` 类
- 字段使用 `@NotNull` / `@NotBlank` / `@NotEmpty` / `@Size` 校验
- 金额字段使用 BigDecimal
- 分页参数使用 `@RequestParam` 传入 `page` / `size`，底层使用 MyBatis-Plus `Page`

```java
public record CreateOrderDTO(
    @NotNull Long addressId,
    @NotEmpty List<OrderItemDTO> items,
    String remark
) {}
```

## VO 规范

- 所有接口返回 VO（Java record），禁止返回 Entity
- VO 提供 `static from(Entity)` 工厂方法
- VO 不包含 password/secret 等敏感字段
- VO 不包含 deleted 等 ORM 框架字段
- 部分复杂 VO（如 ProductVO）在 Service 层直接构建，无 `from` 方法

```java
public record OrderVO(Long id, String orderNo, BigDecimal totalAmount, ...) {
    public static OrderVO from(Order order) {
        return new OrderVO(order.getId(), order.getOrderNo(), ...);
    }
}
```

## 回调接口

- 第三方支付回调接口返回 `String`（非 `Result<T>`），因为第三方要求特定格式
- 回调接口在白名单中放行（`/api/payment/callback/**`）
- 必须做签名验证和幂等处理（Redis `setIfAbsent`）

## API 文档

- 使用 Knife4j（OpenAPI 3 规范），访问地址：`/doc.html`
- Controller 类添加 `@Tag` 注解
- 接口方法添加 `@Operation` 注解
- 参数添加 `@Parameter` 注解（可选）
- DTO 字段添加 `@Schema` 注解（可选）
- 生产环境应关闭 `/doc.html` 访问

## 前端请求

- 统一使用 `src/utils/request.ts`（axios 封装）
- **自动注入 JWT Token**：从 `localStorage` 读取 token，添加 `Authorization: Bearer <token>` 请求头
- **自动解包 Result\<T\>**：响应拦截器提取 `data` 字段，调用方直接拿到 `T` 而非 `Result<T>`
- **自动处理 401**：业务码 401 或 HTTP 401 均跳转登录页
- **自动错误提示**：非 200 业务码自动 `message.error()` 提示
- **泛型方法签名**：`request.get<T>()` / `request.post<T>()` / `request.put<T>()` / `request.delete<T>()`

```typescript
const user = await request.get<UserVO>('/api/user/info');
const order = await request.post<OrderVO>('/api/order/create', dto);
```
