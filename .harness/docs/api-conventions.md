# API 约定与模式

## 响应格式

统一使用以下响应信封：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1700000000000
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 业务状态码，200 表示成功 |
| message | string | 提示信息 |
| data | object/array/null | 响应数据，错误时为 null |
| timestamp | long | 响应时间戳 |

## 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "size": 10,
    "current": 1
  }
}
```

## HTTP 方法约定

| 方法 | 用途 |
|---|---|
| GET | 查询资源 |
| POST | 创建资源 |
| PUT | 完整更新资源 |
| PATCH | 部分更新资源 |
| DELETE | 删除资源 |

## 异常处理

- 全局异常通过 `@RestControllerAdvice` 统一处理
- 业务异常抛出 `BusinessException`（自定义）
- 参数校验使用 `@Valid` / `@Validated`
- 错误响应格式与正常响应一致，code 为对应错误码

## API 文档

- 使用 Knife4j（OpenAPI 3 规范）
- 访问地址：`/doc.html`
- Controller 类和方法添加 `@Tag` / `@Operation` 注解

## 前端请求

- 统一使用 `src/utils/request.ts`（axios 封装）
- 自动注入 JWT token
- 自动处理 401 跳转登录页
- 自动处理通用错误提示
