# API 治理规则

> 约束 AI Agent 的接口设计、请求响应格式、错误码规范。基于项目实际 API 模式。
> 实操技能：`.harness/skills/api-design/SKILL.md`

---

## 1. RESTful 规范

### 1.1 URL 设计

```
/api/{module}/{resource}[/{id}][/{action}]

示例:
  GET    /api/product/list                — 商品列表
  GET    /api/product/{id}                — 商品详情
  POST   /api/cart/add                    — 加入购物车
  PUT    /api/cart/sku/{skuId}            — 修改购物车数量
  PUT    /api/cart/sku/{skuId}/select     — 切换购物车选中状态
  DELETE /api/cart/sku/{skuId}            — 删除购物车项
  POST   /api/order/create                — 创建订单
  POST   /api/payment/pay                 — 支付
  POST   /api/payment/refund              — 退款
```

```yaml
规则:
  - URL 全小写，单词间用连字符(-)，不用下划线
  - 资源名用单数名词（/api/product/list 而非 /api/products/list）
  - 动作用 HTTP 方法表示（GET/POST/PUT/DELETE）
  - 复杂操作可用 URL 末尾动词（/create、/pay、/refund）
  - 禁止 URL 中包含动词用于简单 CRUD（/api/product/getById ❌ → GET /api/product/{id} ✅）
```

### 1.2 完整接口清单

```
用户模块 /api/user:
  POST   /api/user/register              — 注册
  POST   /api/user/login                 — 登录
  POST   /api/user/logout                — 登出
  GET    /api/user/info                  — 用户信息
  PUT    /api/user/info                  — 修改用户信息
  GET    /api/user/balance               — 查询余额
  GET    /api/user/address/list          — 地址列表
  GET    /api/user/address/{id}          — 地址详情
  POST   /api/user/address               — 新增地址
  PUT    /api/user/address/{id}          — 修改地址
  DELETE /api/user/address/{id}          — 删除地址
  PUT    /api/user/address/{id}/default  — 设为默认地址

商品模块 /api/product:
  GET    /api/product/list               — 商品列表（分页）
  GET    /api/product/{id}               — 商品详情
  GET    /api/product/category/list      — 分类树
  GET    /api/product/brand/list         — 品牌列表
  GET    /api/product/search/hot         — 热门搜索词
  GET    /api/product/search/suggest     — 搜索建议
  GET    /api/product/{id}/reviews       — 商品评价（分页）
  GET    /api/product/{id}/review-summary — 评价摘要
  POST   /api/product/{id}/review        — 创建评价

购物车模块 /api/cart:
  POST   /api/cart/add                   — 加入购物车
  GET    /api/cart/list                  — 购物车列表
  PUT    /api/cart/sku/{skuId}           — 修改数量
  DELETE /api/cart/sku/{skuId}           — 删除购物车项
  PUT    /api/cart/sku/{skuId}/select    — 切换选中状态
  PUT    /api/cart/select-all            — 全选/取消全选
  DELETE /api/cart/selected              — 清空已选

订单模块 /api/order:
  POST   /api/order/create               — 创建订单
  GET    /api/order/{id}                 — 订单详情
  GET    /api/order/list                 — 订单列表（分页）
  POST   /api/order/cancel/{id}         — 取消订单
  POST   /api/order/ship/{id}           — 发货（管理端）
  POST   /api/order/confirm/{id}        — 确认收货
  DELETE /api/order/{id}                — 删除订单

支付模块 /api/payment:
  POST   /api/payment/pay               — 发起支付
  POST   /api/payment/callback/wechat   — 微信支付回调
  POST   /api/payment/callback/alipay   — 支付宝回调
  GET    /api/payment/{id}              — 支付详情
  GET    /api/payment/list              — 支付记录（分页）
  POST   /api/payment/refund            — 退款

优惠券模块 /api/coupon:
  GET    /api/coupon/list               — 优惠券列表（分页）
  POST   /api/coupon/receive/{couponId} — 领取优惠券
  GET    /api/coupon/my                 — 我的优惠券

管理端 /api/admin:
  POST   /api/admin/product             — 创建商品
  PUT    /api/admin/product/{id}        — 修改商品
  PUT    /api/admin/product/{id}/stock  — 修改库存
  POST   /api/admin/product/sku         — 创建 SKU
  PUT    /api/admin/product/sku/{skuId} — 修改 SKU
  DELETE /api/admin/product/sku/{skuId} — 删除 SKU
  POST   /api/admin/product/brand       — 创建品牌
  PUT    /api/admin/product/brand/{id}  — 修改品牌
  POST   /api/admin/coupon              — 创建优惠券
```

### 1.3 HTTP 方法

| 方法 | 用途 | 幂等 |
|---|---|---|
| GET | 查询 | ✅ |
| POST | 创建/复杂操作 | ❌ |
| PUT | 全量更新 | ✅ |
| DELETE | 删除 | ✅ |

---

## 2. 统一响应格式

### 2.1 Result\<T\>

```java
// common-core 提供，所有接口统一使用
public class Result<T> {
    private final int code;      // 业务状态码
    private final String message; // 提示信息
    private final T data;        // 响应数据
}
```

```yaml
必须:
  - 所有 Controller 方法返回 Result<T> 或其子类型
  - 成功: Result.success(data)
  - 失败: 抛 BusinessException(ResultCode)，由全局异常处理器转 Result
  - 分页: Result<PageResult<T>>，包含 total/page/size/list

禁止:
  - 返回 Map 类型
  - 返回 Entity 类型
  - 返回原始类型（String、Integer 等）——应包装到 Result
  - 自定义响应包装（统一用 Result）
```

### 2.2 分页响应

```java
// 使用 MyBatis-Plus IPage 转 PageResult
public record PageResult<T>(long total, int page, int size, List<T> list) {

    public static <T> PageResult<T> from(IPage<T> iPage) {
        return new PageResult<>(
                iPage.getTotal(),
                (int) iPage.getCurrent(),
                (int) iPage.getSize(),
                iPage.getRecords()
        );
    }
}
```

---

## 3. 错误码规范

### 3.1 HTTP 基础码

```
200: 操作成功（SUCCESS）
400: 请求参数错误（BAD_REQUEST）— 由 GlobalExceptionHandler 自动处理校验异常
401: 未登录或 Token 已过期（UNAUTHORIZED）
403: 无访问权限（FORBIDDEN）
404: 资源不存在（NOT_FOUND）
405: 请求方法不允许（METHOD_NOT_ALLOWED）
409: 数据冲突（CONFLICT）
500: 服务器内部错误（INTERNAL_ERROR）— 由 GlobalExceptionHandler 兜底处理
```

```yaml
规则:
  - HTTP 基础码由 GlobalExceptionHandler 自动使用，业务代码不应直接使用
  - 业务代码统一使用 1xxx-6xxx 段错误码
```

### 3.2 业务错误码分段

```
1001-1999: 用户模块
2001-2999: 商品模块
3001-3999: 订单模块
4001-4999: 支付模块
5001-5999: 购物车模块
6001-6999: 营销模块
9001-9999: 系统通用

当前项目已用错误码:
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
  2007: 规格模板不存在
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
```

```yaml
规则:
  - 错误码唯一，不可重复使用
  - 新增错误码在对应模块范围内选择
  - 错误码定义在 ResultCode 枚举中
  - 前端根据错误码做差异化处理（非 200 即展示 message）
```

### 3.3 BusinessException

```java
// 业务异常统一使用，三种构造方式:

// 1. 仅传 ResultCode（最常用）
throw new BusinessException(ResultCode.SKU_STOCK_INSUFFICIENT);

// 2. 传 ResultCode + 自定义 message（覆盖默认提示）
throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND, "具体原因");

// 3. 直接传 code + message（仅用于需要动态 message 的场景，优先使用方式 1/2）
throw new BusinessException(ResultCode.PAYMENT_FAILED.getCode(), "账户余额不足，当前余额 ¥" + balance);
```

---

## 4. 请求参数

### 4.1 DTO 定义

```yaml
必须:
  - 入参使用独立 DTO/Request 类（放在 dto/ 包）
  - 使用 Java record（简单 DTO）或 @Data 类（需要校验注解）
  - 字段使用 @NotNull/@NotBlank/@NotEmpty 校验
  - 金额用 BigDecimal
  - 分页参数使用 MyBatis-Plus Page

禁止:
  - 使用 Entity 接收请求参数
  - 使用 Map 接收请求参数
  - Controller 中手动校验参数（用 @Valid/@Validated）
```

### 4.2 示例

```java
@PostMapping("/create")
public Result<OrderVO> create(@Valid @RequestBody CreateOrderRequest request) {
    return Result.success(orderService.createOrder(request));
}
```

---

## 5. 接口安全

```yaml
必须:
  - 所有 /api/** 接口经过 GatewayAuthFilter JWT 鉴权
  - 白名单接口明确列出（login、register、callback）
  - 管理接口 /api/admin/** 额外校验 ADMIN 角色
  - 敏感操作记录日志

白名单（无需认证）:
  - POST /api/user/login
  - POST /api/user/register
  - /api/payment/callback/**
  - /doc.html, /webjars/**, /v3/api-docs/**
  - /actuator/health, /actuator/info

半白名单（GET 放行，写操作需认证）:
  - GET /api/product/**
  - GET /api/category/**
```

---

## 6. 异常处理与 HTTP 状态码

```yaml
规则:
  - 业务异常(BusinessException): HTTP 200 + Result.fail(code, message)
    前端通过 Result.code 判断业务是否成功
  - 参数校验异常: HTTP 400 + Result.fail(BAD_REQUEST, 校验信息)
  - 认证异常(MissingRequestValueException): HTTP 401 + Result.fail(UNAUTHORIZED)
  - 未知异常: HTTP 500 + Result.fail(INTERNAL_ERROR)

前端判断逻辑:
  - HTTP 状态码非 2xx → 网络或框架层异常
  - HTTP 200 但 Result.code != 200 → 业务异常，展示 Result.message
  - HTTP 200 且 Result.code == 200 → 成功，读取 Result.data
```

---

## 7. API 文档

```yaml
工具: Knife4j (Swagger 增强)
访问: /doc.html
状态: ⚠️ 待落地 — 当前项目未使用 @Tag/@Operation/@Schema 注解

必须（待落地）:
  - 新增 Controller 添加 @Tag 注解
  - 接口方法添加 @Operation 注解
  - 参数添加 @Parameter 注解（可选）
  - DTO 字段添加 @Schema 注解（可选）

禁止:
  - 文档注解中包含敏感信息
  - 在生产环境暴露 /doc.html（应通过配置控制）
```

---

## 8. 已知技术债务

```yaml
以下代码违反本文档规范，需逐步修复:

  使用 Entity 接收请求参数（应改为 DTO）:
    - UserController.updateInfo: @RequestBody User user
    - AddressController.create: @RequestBody Address address
    - AddressController.update: @RequestBody Address address
    - AdminProductController: @RequestBody ProductSku sku / Brand brand（4处）
    - AdminCouponController.create: @RequestBody Coupon coupon

  使用 Map 接收请求参数（应改为 DTO）:
    - PaymentController.wechatCallback: @RequestBody Map<String, String> params
    - PaymentController.alipayCallback: @RequestBody Map<String, String> params

  返回类型非 Result<T>（支付回调返回原始 String）:
    - PaymentController.wechatCallback: 返回 String
    - PaymentController.alipayCallback: 返回 String
```

---

## 9. 禁止事项汇总

```yaml
禁止:
  - 返回 Map 类型
  - 返回 Entity 类型
  - 自定义 Result 包装（统一用 common-core Result）
  - Controller 中写业务逻辑
  - 使用 Entity 接收请求参数
  - URL 路径中包含动词用于 CRUD
  - 接口不鉴权（除非在白名单中）
  - 错误码重复使用
```
