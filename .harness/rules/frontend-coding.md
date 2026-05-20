# 前端编码规范（TypeScript / React 19 / Vite 6）

> 参照阿里/腾讯/字节前端规约，结合项目实际代码模式提炼。

---

## 1. 技术栈

| 类别 | 选型 | 版本锁定 |
|---|---|---|
| 框架 | React + TypeScript | React 19 / TS 5.8 |
| 构建 | Vite | 6.x |
| UI | Ant Design | 5.x |
| 状态 | Redux Toolkit | 2.x |
| HTTP | axios（统一封装） | `src/utils/request.ts` |
| 路由 | react-router-dom | 7.x |
| 日期 | dayjs | 1.x |

---

## 2. 目录结构

```
src/
├── api/            # 按模块拆分API（product.ts / user.ts / order.ts）
├── components/     # 公共组件，按业务域分子目录（product/ / order/）
├── layouts/        # 布局组件（MainLayout）
├── pages/          # 页面组件，按功能模块分目录（product/ / cart/ / admin/）
├── stores/         # Redux Toolkit（slice + index.ts）
├── utils/          # 工具函数（request.ts 等）
├── types/          # 全局类型定义
└── assets/         # 静态资源
```

---

## 3. 命名规范

| 类型 | 规则 | 正例 | 反例 |
|---|---|---|---|
| 组件文件 | PascalCase | `ProductCard.tsx` | `productCard.tsx` |
| Hook 文件 | use+CamelCase | `useProductList.ts` | `productListHook.ts` |
| 工具/API文件 | camelCase | `request.ts` / `product.ts` | `Product.ts` |
| 类型/接口 | PascalCase | `ProductVO` / `LoginParams` | `product_vo` |
| 组件导出 | 默认导出 | `export default function ProductCard()` | `export const ProductCard` |
| 常量 | UPPER_SNAKE | `MAX_RETRY` | `maxRetry` |
| CSS类名 | kebab-case 或 BEM | `product-card__title` | `productCardTitle` |
| 事件处理 | handle+动作 | `handleClick` / `handleSubmit` | `click` / `onSubmit` |
| 布尔变量 | is/has/should 前缀 | `isLoading` / `hasError` | `loading` / `error` |

---

## 4. 组件规范

### 4.1 基本规则

- **函数组件 + Hooks**，禁止 class 组件
- 一个文件一个组件，文件名与组件名一致
- 组件 props 必须定义 `interface Props`，禁止 `any`
- 组件内逻辑超过 3 个 useEffect 或超过 80 行，拆分为自定义 Hook

### 4.2 组件结构顺序

```tsx
// 1. 导入
import { useState, useCallback } from 'react';
import { Card } from 'antd';
import type { ProductVO } from '@/api/product';

// 2. 类型定义
interface Props {
  product: ProductVO;
  onClick: (id: number) => void;
}

// 3. 常量（组件外）
const STATUS_CONFIG = { ... } as const;

// 4. 组件
export default function ProductCard({ product, onClick }: Props) {
  // 4a. Hooks
  // 4b. 事件处理
  // 4c. 渲染
  return <Card>...</Card>;
}
```

### 4.3 性能优化

| 场景 | 规范 |
|---|---|
| 引用稳定 | `useCallback` 包裹传给子组件的回调 |
| 计算缓存 | `useMemo` 包裹昂贵计算或传给子组件的对象/数组 |
| 列表渲染 | 必须用唯一且稳定的 `key`（用业务 ID，禁止 index） |
| 条件渲染 | 提前 return，减少嵌套 |
| 大列表 | 超过 200 条考虑虚拟滚动 |

---

## 5. TypeScript 规范

| 规则 | 说明 |
|---|---|
| 严格模式 | `tsconfig.json` 已开启 `strict: true` |
| 禁止 any | 用 `unknown` 替代，必须类型收窄后使用 |
| 类型导出 | API 响应类型与接口同文件定义并导出（`api/product.ts`） |
| 路径别名 | 使用 `@/` 前缀引用 src 下模块，禁止 `../../` 超过 2 层 |
| 枚举 | 禁止 `enum`，用 `as const` 对象或联合字面量类型 |
| 类型断言 | 优先类型守卫（`typeof`/`instanceof`），少用 `as`，禁止 `as any` |
| 泛型 | API 函数必须带泛型返回类型（`request.get<T>()`） |

```typescript
type StockStatus = 'sufficient' | 'low' | 'out_of_stock';
const STATUS_CONFIG = {
  sufficient: { color: '#52c41a', text: '现货充足' },
  low: { color: '#faad14', text: '库存紧张' },
  out_of_stock: { color: '#f5222d', text: '已售罄' },
} as const;
```

---

## 6. API 层规范

### 6.1 文件组织

每个模块一个文件，类型与 API 函数同文件：

```typescript
// api/product.ts
export interface ProductVO { id: number; name: string; /* ... */ }
export interface ProductSearchParams { keyword?: string; page: number; size: number; }

export const productApi = {
  getList: (params: ProductSearchParams) =>
    request.get<{ records: ProductVO[]; total: number }>('/product/list', { params }),
  getDetail: (id: number) => request.get<ProductDetailVO>(`/product/${id}`),
};
```

### 6.2 调用约束

- 禁止直接使用 `axios`，必须通过 `src/utils/request.ts`
- 禁止硬编码 API 地址，baseURL 在 request.ts 统一配置
- API 函数返回带泛型的 Promise，调用方无需再做类型断言
- 请求参数和响应类型必须定义，禁止 `any`

---

## 7. 状态管理规范（Redux Toolkit）

### 7.1 Slice 结构

```typescript
// stores/cartSlice.ts
interface CartState { items: CartItem[]; totalCount: number; }
const initialState: CartState = { items: [], totalCount: 0 };

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    setCartItems(state, action: PayloadAction<CartItem[]>) {
      state.items = action.payload;
      state.totalCount = action.payload.reduce((sum, i) => sum + i.quantity, 0);
    },
  },
});
```

### 7.2 使用约束

| 规则 | 说明 |
|---|---|
| Slice 命名 | 与模块名一致（`cart`/`user`/`order`） |
| 禁止嵌套 | State 扁平化，不超过 3 层 |
| 异步操作 | 用 `createAsyncThunk` 或在组件内调用 API 后 dispatch |
| 选择器 | 用 `useSelector((s: RootState) => s.xxx)`，频繁计算用 `reselect` |
| store 入口 | `stores/index.ts` 导出 `RootState` 和 `AppDispatch` |

---

## 8. 路由规范

| 规则 | 说明 |
|---|---|
| 路径命名 | kebab-case（`/product-list`），当前项目用无连字符短路径（`/product/:id`） |
| 鉴权路由 | 用 `PrivateRoute` 组件包裹，禁止在页面内判断登录态 |
| 懒加载 | 非首屏页面用 `React.lazy` + `Suspense` |
| 参数获取 | 用 `useSearchParams` / `useParams`，禁止手动解析 URL |

---

## 9. 样式规范

| 规则 | 说明 |
|---|---|
| 优先级 | Ant Design 组件属性 > inline style > CSS module |
| 禁止 | 全局污染性 CSS、`!important` |
| 响应式 | 使用 Ant Design 的 `Row`/`Col` 栅格，断点：`xs`/`sm`/`md`/`lg`/`xl` |
| 魔数 | 禁止硬编码颜色值（`#f5222d`），用 Ant Design token 或 CSS 变量 |
| z-index | 遵循 Ant Design 层级规范，自定义不超过 1000 |

---

## 10. 错误处理规范

| 场景 | 规范 |
|---|---|
| API 错误 | 由 `request.ts` 拦截器统一处理（401跳登录、其他弹 message.error） |
| 组件错误 | 用 ErrorBoundary 包裹关键模块，防止白屏 |
| 异步操作 | try-catch 包裹，finally 重置 loading；禁止吞掉错误（空 catch） |
| 表单校验 | 用 Ant Design Form 的 rules，禁止手动 if-else 校验 |
| 空状态 | 列表为空必须展示 `Empty` 组件，加载中展示 `Spin` |

---

## 11. 安全规范

| 规则 | 说明 |
|---|---|
| XSS | 禁止 `dangerouslySetInnerHTML`；用户输入渲染前转义 |
| Token | 存 `localStorage`，请求拦截器自动注入；登出清除 |
| 敏感数据 | 禁止 console.log 输出 Token/密码/手机号；生产构建自动去除 |
| URL 参数 | 不在 URL 中传递敏感信息（Token/密码） |
| 依赖安全 | 定期 `npm audit`，高危漏洞必须修复 |

---

## 12. 禁止事项清单

| # | 禁止项 |
|---|---|
| 1 | class 组件 |
| 2 | 直接调用原始 axios（必须通过 `request.ts`） |
| 3 | 硬编码 API 地址 |
| 4 | render 中创建新对象/函数引用（用 useMemo/useCallback） |
| 5 | 使用 `any` 类型（用 `unknown` + 类型守卫） |
| 6 | 使用 `enum`（用 `as const` 对象） |
| 7 | 列表渲染用 index 作 key |
| 8 | 空catch吞掉错误 |
| 9 | `dangerouslySetInnerHTML` |
| 10 | `../../` 超过 2 层（用 `@/` 别名） |
| 11 | 在组件内手动解析 URL 参数 |
| 12 | 硬编码颜色值（用 Ant Design token） |
