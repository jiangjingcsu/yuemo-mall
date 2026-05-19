# 前端编码规范（TypeScript / React）

## 技术栈

- TypeScript + Vite 6 + React 19
- UI 库：Ant Design 5
- 状态管理：Redux Toolkit（`src/stores/`）
- HTTP 请求：axios 统一封装（`src/utils/request.ts`）

## 组件规范

- 函数组件 + Hooks，不使用 class 组件
- 页面组件放在 `src/pages/`，按功能模块分目录
- 公共组件放在 `src/components/`
- 自定义 Hooks 放在 `src/hooks/`

## 命名规范

- 组件文件：PascalCase（`ProductList.tsx`）
- Hook 文件：`use` 前缀（`useProductList.ts`）
- 工具函数：camelCase
- 类型/接口：PascalCase

## 目录结构约定

```
src/
├── components/      # 公共组件
├── pages/           # 页面组件（按功能模块分目录）
├── hooks/           # 自定义 Hooks
├── stores/          # Redux Toolkit stores
├── utils/           # 工具函数
│   └── request.ts   # axios 封装
├── types/           # 类型定义
└── assets/          # 静态资源
```

## 禁止事项

- 禁止使用 class 组件
- 禁止直接在组件中调用原始 axios（必须通过 `src/utils/request.ts`）
- 禁止硬编码 API 地址
- 禁止在 render 中创建新对象/函数（使用 useMemo/useCallback）
