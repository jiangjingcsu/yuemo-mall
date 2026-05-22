# 前端编码规范（TypeScript / React 19 / Vite 6 / Ant Design 5）

> 基于 React 官方最佳实践与项目实际代码模式提炼，新增代码必须遵守。
> 实操技能：`.harness/skills/code-verify/SKILL.md`

```yaml
元信息:
  版本: 1.1
  最后更新: 2026-05-22
  更新触发条件:
    - 前端技术栈版本升级时
    - 新增/修改前端架构模式时
    - 发现新的反模式时
```

---

## 1. 命名规范

| 类型 | 规则 | 正例 | 反例 |
|---|---|---|---|
| 组件文件 | PascalCase.tsx | `ProductCard.tsx` | `productCard.tsx` |
| 非组件文件 | camelCase.ts | `request.ts` | `Request.ts` |
| 组件导出 | PascalCase | `ProductCard` | `productCard` |
| 自定义 Hook | use + PascalCase | `useCart` | `cartHook` |
| 常量 | 全大写+下划线 | `API_BASE_URL` | `apiBaseUrl` |
| 类型/接口 | PascalCase | `ProductVO` | `productVo` |
| API 函数对象 | xxxApi | `productApi` | `productService` |
| Store Slice | xxxSlice | `cartSlice` | `cart` |
| CSS 类名 | kebab-case 或 CSS Modules | `product-card` | `productCard` |

---

## 2. 组件规范

```yaml
必须:
  - 函数组件 + Hooks（禁止 class 组件）
  - Props 使用 interface 定义
  - 组件文件名与导出名一致
  - 列表渲染使用业务 ID 作 key（禁止 index）
  - 列表为空展示 Empty，加载中展示 Spin

建议:
  - 非首屏页面使用 React.lazy + Suspense 懒加载（当前项目尚未实施，新增页面时优先使用）
  - 关键模块使用 ErrorBoundary 包裹（当前项目尚未实施，新增模块时优先添加）

禁止:
  - 直接使用 dangerouslySetInnerHTML（XSS 风险）

允许（仅限以下场景）:
  - 配合 DOMPurify.sanitize() 渲染服务端富文本内容
  - 例: <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(html) }} />
  - 必须安装 dompurify 依赖
  - 禁止对用户直接输入的内容使用（即使经过 sanitize）
```

---

## 3. 类型规范

```yaml
必须:
  - API 响应类型与 API 函数同文件定义并导出
  - 严格模式（tsconfig.json strict: true）
  - 泛型调用 API: request.get<T>() / request.post<T>()

禁止:
  - 使用 any 类型（用 unknown + 类型守卫）
  - 使用 enum（用 as const 对象或联合字面量类型）

API 文件质量检查:
  - 每个 API 文件必须定义并导出响应类型 interface
  - 所有 API 方法必须使用泛型参数（request.get<T>() / request.post<T>()）
  - 缺少类型定义的 API 文件视为技术债务，新增/修改时必须补全

当前已知缺失:
  - coupon.ts: 无 CouponVO / UserCouponVO 接口定义，所有方法缺少泛型
```

---

## 4. API 调用规范

```yaml
必须:
  - 使用 src/utils/request.ts 封装的 request 对象
  - API 函数放在 src/api/ 目录，按模块拆分文件
  - 路径使用 @/ 别名（禁止 ../../ 超过 2 层）

禁止:
  - 直接 import axios
  - 硬编码 API 地址
  - 在 URL 中传递敏感信息（Token/密码）
  - console.log 输出 Token/密码/手机号
```

### API 文件模式

```typescript
import request from '@/utils/request';

export interface XxxVO {
  id: number;
  name: string;
}

export const xxxApi = {
  getList: () => request.get<XxxVO[]>('/xxx/list'),
  getDetail: (id: number) => request.get<XxxVO>(`/xxx/${id}`),
};
```

---

## 5. 状态管理规范

```yaml
必须:
  - 使用 Redux Toolkit（@reduxjs/toolkit）
  - Store 按模块拆分 Slice
  - 导出 RootState 和 AppDispatch 类型
  - 定义类型化 Hooks（useAppDispatch / useAppSelector）

Slice 文件模式:
  - 文件名: xxxSlice.ts
  - 导出: reducer（default）+ actions（named）
  - State 类型: interface XxxState

类型化 Hooks 模板（src/stores/index.ts 中添加）:
  export const useAppDispatch = useDispatch.withTypes<AppDispatch>()
  export const useAppSelector = useSelector.withTypes<RootState>()

异步状态管理:
  - 推荐: createAsyncThunk 处理 API 调用 + 状态追踪（loading/success/error）
  - 允许: 组件内 useEffect + dispatch（简单场景）
  - 禁止: 在 reducer 中直接调用 API

新增 Slice 步骤:
  1. 创建 src/stores/xxxSlice.ts
  2. 在 src/stores/index.ts 的 reducer 中注册
  3. 组件中通过 useAppSelector 读取、useAppDispatch 派发
```

---

## 6. 样式规范

```yaml
必须:
  - 颜色使用 theme.useToken()，禁止硬编码色值
  - 使用 Ant Design 组件库提供的布局和样式系统
  - 组件级样式使用 CSS Modules（*.module.css / *.module.less）

禁止:
  - 硬编码颜色值（用 Ant Design token 或 CSS 变量）
  - 全局 CSS 修改 Ant Design 组件默认样式（用 ConfigProvider token 覆盖）

建议:
  - 响应式布局使用 Ant Design Grid（Row/Col）
```

---

## 7. 路由规范

```yaml
必须:
  - 鉴权路由使用 PrivateRoute 组件包裹
  - 管理路由使用 AdminRoute 组件包裹（校验 JWT role === 'ADMIN'）
  - URL 参数使用 useSearchParams / useParams
  - 嵌套路由使用 <Outlet /> 渲染子路由

建议:
  - 非首屏页面使用 React.lazy + Suspense 懒加载（当前项目尚未实施，新增页面时优先使用）

禁止:
  - 在组件内手动解析 URL 参数
  - 在路由守卫中硬编码角色判断逻辑（应通过 API 验证）

路由守卫模式:
  - PrivateRoute: 检查 localStorage token，无 token 跳转 /login
  - AdminRoute: 继承 PrivateRoute + 校验 JWT payload role
  - 新增守卫: 参照 PrivateRoute 模式，检查对应权限条件

React Router 版本说明:
  当前: react-router-dom v7.5.0
  当前用法: v6 兼容模式（BrowserRouter + Routes + Route）
  演进方向: 迁移到 v7 数据路由模式（createBrowserRouter + loader/action）
  当前约束: 新增路由继续使用 v6 兼容模式，待整体迁移后再切换
```

---

## 8. 性能规范

```yaml
必须:
  - 大数据列表使用 useMemo/useCallback
  - 避免在 render 中创建新对象/函数引用
  - 图片使用 loading="lazy" 属性

建议:
  - 长列表（>100 条）使用虚拟滚动（如 react-window）
  - 大组件使用 React.lazy + Suspense 代码分割
  - 使用 React.memo 包裹纯展示组件（props 不频繁变化时）

性能检查:
  - React DevTools Profiler 检测不必要的重渲染
  - Lighthouse 评分 Performance >= 80
```

---

## 9. 测试规范

> 前端测试遵循能力成熟度模型（CMM），详见 `.harness/agents/tester-agent.md` §测试策略。

### 当前等级：L0（无测试基础设施）

### L1 升级准备

当项目升级到 L1 时，需安装以下依赖：

```bash
npm install -D vitest @testing-library/react @testing-library/jest-dom jsdom @testing-library/user-event
```

### L1 测试规范

```yaml
框架: Vitest + React Testing Library
命名:
  - 测试文件: {被测模块名}.test.ts / .test.tsx
  - 目录: 与源码同目录或 __tests__/ 子目录
  - describe/it: 英文描述行为

工具函数测试:
  - 纯函数直接测试输入输出
  - 边界值 + 异常输入

Hook 测试:
  - 使用 renderHook（@testing-library/react）
  - 测试状态变更和副作用

Redux Slice 测试:
  - 测试 reducer 纯函数
  - 覆盖每个 action 的状态变更

覆盖率:
  - 目标: 核心 Hook 和工具函数 >= 80%
  - 可忽略: 纯样式组件、第三方组件封装
```

### L1 测试示例

```typescript
import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useCounter } from './useCounter';

describe('useCounter', () => {
  it('should initialize with default value', () => {
    const { result } = renderHook(() => useCounter());
    expect(result.current.count).toBe(0);
  });

  it('should increment count', () => {
    const { result } = renderHook(() => useCounter());
    act(() => { result.current.increment(); });
    expect(result.current.count).toBe(1);
  });
});
```

### L2 组件测试规范

```yaml
范围:
  - 渲染测试: 正确渲染、props 传递
  - 交互测试: 点击、输入、表单提交
  - 状态测试: loading/error/success

覆盖率:
  - 目标: 核心业务组件 >= 60%
```

### L3 集成测试规范

```yaml
范围:
  - API 调用层（使用 msw Mock）
  - 路由跳转
  - 跨组件交互

可选:
  - E2E（Playwright/Cypress）
```

---

## 10. 禁止事项清单

| 编号 | 禁止项 | 原因 |
|---|---|---|
| F01 | 直接 import axios | 统一使用 request 封装 |
| F02 | 硬编码 API 地址 | 环境变量管理 |
| F03 | 使用 any 类型 | 类型安全 |
| F04 | 直接使用 dangerouslySetInnerHTML（未经 DOMPurify 消毒） | XSS 风险 |
| F05 | 空 catch 块吞掉错误 | 错误不可见 |
| F06 | class 组件 | 统一函数组件 + Hooks |
| F07 | 使用 enum | 用 as const 或联合类型 |
| F08 | 列表渲染用 index 作 key | 渲染性能和状态问题 |
| F09 | ../../ 超过 2 层 | 用 @/ 别名 |
| F10 | 在组件内手动解析 URL 参数 | 用 React Router Hooks |
| F11 | 硬编码颜色值 | 用 Ant Design token |
| F12 | render 中创建新引用 | 用 useMemo/useCallback |
| F13 | URL 中传递敏感信息 | 安全风险 |

---

## 11. 错误处理规范

```yaml
API 调用错误:
  - request.ts 拦截器已统一处理 HTTP 错误和业务错误（弹 message.error）
  - 组件内 try/catch 捕获后，仅需处理业务逻辑层面的恢复（如重置表单）
  - 禁止空 catch 块（至少 console.error 记录）

组件错误:
  - 使用 ErrorBoundary 捕获渲染错误（当前项目尚未实施）
  - ErrorBoundary fallback 展示友好的错误提示和重试按钮

全局错误:
  - 401 统一跳转登录页（request.ts 拦截器已处理）
  - 网络异常展示全局提示（request.ts 拦截器已处理）
```

---

## 12. 组件目录规范

```yaml
pages/: 页面级组件（与路由一一对应）
  - 每个页面一个子目录: pages/{module}/XxxPage.tsx
  - 页面组件 default export

components/: 可复用组件
  - 按业务域分组: components/{module}/XxxComponent.tsx
  - 跨模块公共组件: components/common/XxxComponent.tsx
  - 组件 named export

layouts/: 布局组件
  - MainLayout.tsx 等全局布局
```

---

## 13. any 类型技术债务

```yaml
当前已知 any 类型使用（新增/修改时必须修复）:
  | 文件 | 位置 | 修复方向 |
  |---|---|---|
  | ProductList.tsx | useState<any[]>([]) (categories) | 定义 Category 接口 |
  | ProductList.tsx | sortBy as any | 使用联合类型断言 |
  | SearchSuggestions.tsx | useState<any[]>([]) x2 | 定义接口 |
  | CategorySidebar.tsx | buildItems 返回 any[] | 定义 Menu items 类型 |
  | AdminDashboard.tsx | useState<any[]>([]) x2 + as any x2 + render any x2 | 使用已有 ProductVO/CouponVO |
  | CouponList.tsx | .then((data: any) => | 定义 CouponPage 接口 |
  | AddressPage.tsx | data as any | 使用已有 Address 接口泛型 |
```

---

## 14. 与现有规则的关联

```yaml
本规则与以下规则联动:
  - docs/api-conventions.md: 后端 API 约定（响应格式/分页/认证/VO 规范）
  - rules/security.md: 安全规范（XSS/Token/敏感数据）
  - rules/code-smell-governance.md: 代码坏味道（超长方法/魔法值等，前端同样适用）
  - rules/module-boundary.md: 模块边界（API 层按模块拆分文件）

前端特有约束:
  - request.ts 拦截器已解包后端统一响应体 { code, message, data }
  - API 函数泛型 T 对应的是 data 字段的类型，而非整个响应体
  - 分页响应统一为 { records: T[], total: number, current: number, pages: number }
```
