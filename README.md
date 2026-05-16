# 御魔商城 (Yuemo Mall)

基于 Spring Cloud Alibaba 微服务架构的全栈电商平台，包含后端服务和 React 前端应用。

## 🏗️ 项目架构

### 后端 (yuemo-backend)

Spring Cloud Alibaba 微服务架构，包含以下核心模块：

- **yuemo-gateway** - API网关
  - 限流(Rate Limiting)
  - 熔断(Circuit Breaker)
  - 认证鉴权(Authentication)
  - 请求日志(Request Logging)

- **yuemo-modules** - 业务模块
  - `yuemo-user` - 用户服务
  - `yuemo-product` - 商品服务
  - `yuemo-cart` - 购物车服务
  - `yuemo-order` - 订单服务
  - `yuemo-payment` - 支付服务
  - `yuemo-promotion` - 促销服务(优惠券)

- **yuemo-common** - 公共组件
  - `common-core` - 核心工具类
  - `common-mybatis` - MyBatis Plus 配置
  - `common-security` - 安全工具(JWT)

- **yuemo-admin** - 管理后台 API
- **yuemo-server** - 服务启动器

### 前端 (yuemo-frontend)

现代 React + TypeScript 技术栈：

- **框架**: React 18 + TypeScript
- **构建工具**: Vite
- **状态管理**: Redux Toolkit
- **路由**: React Router v6
- **HTTP 客户端**: Axios

#### 页面模块

- 用户模块：登录、注册、个人中心、地址管理
- 商品模块：商品列表、商品详情、搜索
- 购物车模块：购物车管理
- 订单模块：订单列表、订单详情
- 支付模块：支付页面
- 优惠模块：优惠券列表
- 管理模块：管理员仪表盘

## 📁 项目结构

```
yuemo_mall/
├── yuemo-backend/          # 后端项目
│   ├── docker/             # Docker 配置文件
│   │   ├── docker-compose.yml
│   │   └── nginx/         # Nginx 配置
│   ├── sql/               # 数据库脚本
│   │   ├── init-all.sql   # 初始化脚本
│   │   ├── test-data.sql  # 测试数据
│   │   └── database_init.py # Python 初始化脚本
│   ├── yuemo-gateway/     # API 网关
│   ├── yuemo-modules/     # 业务模块
│   │   ├── yuemo-user/
│   │   ├── yuemo-product/
│   │   ├── yuemo-cart/
│   │   ├── yuemo-order/
│   │   ├── yuemo-payment/
│   │   └── yuemo-promotion/
│   ├── yuemo-common/      # 公共组件
│   ├── yuemo-admin/       # 管理后台
│   └── yuemo-server/      # 服务启动器
│
├── yuemo-frontend/        # 前端项目
│   ├── src/
│   │   ├── api/          # API 调用
│   │   ├── components/   # 通用组件
│   │   ├── pages/       # 页面组件
│   │   ├── stores/      # Redux Store
│   │   ├── utils/       # 工具函数
│   │   └── layouts/     # 布局组件
│   └── package.json
│
└── design.md             # 设计文档
```

## 🚀 快速开始

### 后端启动

1. **环境要求**
   - JDK 17+
   - Maven 3.8+
   - MySQL 8.0+
   - Redis
   - Nacos
   - Sentinel

2. **数据库初始化**
   ```bash
   cd yuemo-backend/sql
   python database_init.py
   ```

3. **启动服务**
   ```bash
   cd yuemo-backend
   mvn clean install
   cd yuemo-server
   mvn spring-boot:run
   ```

### 前端启动

1. **环境要求**
   - Node.js 18+
   - npm 或 yarn

2. **安装依赖**
   ```bash
   cd yuemo-frontend
   npm install
   ```

3. **启动开发服务器**
   ```bash
   npm run dev
   ```

4. **构建生产版本**
   ```bash
   npm run build
   ```

## 🔧 技术栈

### 后端技术

- Spring Boot 3.0+
- Spring Cloud Alibaba 2022
- Nacos (服务发现与配置)
- Sentinel (流量控制)
- MyBatis Plus (ORM)
- Redis (缓存)
- JWT (认证)

### 前端技术

- React 18
- TypeScript
- Vite
- Redux Toolkit
- React Router v6
- Axios
- CSS3

## 📝 主要功能

### 用户模块
- ✅ 用户注册与登录
- ✅ JWT 身份认证
- ✅ 收货地址管理
- ✅ 个人中心

### 商品模块
- ✅ 商品分类浏览
- ✅ 商品搜索
- ✅ 商品详情展示
- ✅ SKU 选择
- ✅ 商品评价

### 购物车模块
- ✅ 添加商品到购物车
- ✅ 修改商品数量
- ✅ 删除购物车商品
- ✅ 购物车商品清理

### 订单模块
- ✅ 创建订单
- ✅ 订单列表
- ✅ 订单详情
- ✅ 订单超时处理
- ✅ 库存事务处理

### 支付模块
- ✅ 支付流程
- ✅ 支付回调处理
- ✅ 支付状态管理

### 促销模块
- ✅ 优惠券领取
- ✅ 优惠券使用
- ✅ 优惠券管理

## 🐳 Docker 部署

项目包含完整的 Docker 配置：

```bash
cd yuemo-backend/docker
docker-compose up -d
```

## 📊 数据库设计

完整的数据库表结构请参考 `yuemo-backend/sql/init-all.sql`，包含：

- 用户表 (user)
- 商品表 (product)
- SKU 表 (product_sku)
- 分类表 (category)
- 品牌表 (brand)
- 购物车表 (cart_item)
- 订单表 (order)
- 订单项表 (order_item)
- 支付表 (payment)
- 优惠券表 (coupon)
- 商品标签表 (product_tag)

## 🔐 API 网关

网关服务提供以下功能：

1. **限流**: 基于 Redis + Lua 实现分布式限流
2. **熔断**: Sentinel 熔断保护
3. **认证**: JWT Token 验证
4. **日志**: 请求日志记录

## 📦 依赖说明

### 后端主要依赖

- spring-boot-starter-web
- spring-cloud-starter-alibaba-nacos-discovery
- spring-cloud-starter-alibaba-sentinel
- mybatis-plus-boot-starter
- spring-boot-starter-data-redis
- jjwt (JWT)

### 前端主要依赖

- react
- react-dom
- react-router-dom
- @reduxjs/toolkit
- react-redux
- axios

## 🎯 开发指南

### 代码规范
- 后端遵循 Spring Boot 代码规范
- 前端使用 ESLint + Prettier
- 使用 TypeScript 严格模式

### Git 提交规范
```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式
refactor: 重构
test: 测试相关
chore: 构建/工具相关
```

## 📄 License

MIT License

## 👨‍💻 作者

jiangjingcsu

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！
