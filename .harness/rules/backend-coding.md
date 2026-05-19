# 后端编码规范（Java 17 / Spring Boot 3.2 / MyBatis-Plus）

> 本规范基于项目实际代码模式提炼，新增代码必须遵守。

---

## 1. 分层架构

```
Controller → Service(接口) → ServiceImpl → Mapper → DB
     ↕            ↕               ↕
  DTO/VO       Result<T>      Entity(BaseEntity)
```

- **Controller**：只做参数接收、调用 Service、返回 `Result<T>`，不写业务逻辑
- **Service 接口**：定义 `IXXXService extends IService<Entity>`（MyBatis-Plus 模式）
- **ServiceImpl**：业务逻辑实现，用 `@RequiredArgsConstructor` 注入依赖
- **Mapper**：`extends BaseMapper<Entity>`，复杂查询写在 XML 中
- **Entity**：继承 `BaseEntity`，用 `@Data` + `@TableName`

---

## 2. Lombok 使用规范

| 场景 | 注解 | 说明 |
|---|---|---|
| Entity | `@Data` + `@EqualsAndHashCode(callSuper = true)` | 需要 getter/setter，继承 BaseEntity |
| DTO / VO | **不使用 Lombok** | 优先使用 Java `record` |
| Service / Controller | `@RequiredArgsConstructor` | 构造器注入 |
| 日志 | `@Slf4j` | 统一日志门面 |
| 配置类 | `@Getter` + `@Setter` | 配合 `@ConfigurationProperties` |
| Result 响应类 | `@Getter` | 不可变对象，构造时赋值 |
| 枚举 | `@Getter` + `@AllArgsConstructor` | 错误码等枚举 |

---

## 3. Java Record（强制）

DTO 和 VO **必须**使用 Java `record`，禁止用普通类。

```java
// DTO — 用 compact constructor 做默认值
public record SearchCriteria(
    String keyword,
    Integer page,
    Integer size
) {
    public SearchCriteria {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;
    }
}

// DTO — 用 Jakarta Validation 校验
public record CreateProductRequest(
    @NotBlank String name,
    @NotNull Long categoryId,
    @NotNull @Positive BigDecimal price,
    @NotNull List<SkuRequest> skus
) {
    public record SkuRequest(
        @NotNull @Positive BigDecimal price,
        @NotNull Integer stock
    ) {}
}

// VO — 纯数据载体
public record ProductVO(
    Long id,
    String name,
    BigDecimal minPrice,
    List<TagVO> tags
) {}
```

---

## 4. 构造器注入（强制）

所有依赖通过构造器注入，使用 Lombok `@RequiredArgsConstructor`：

```java
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;
    private final SearchService searchService;
    private final SkuService skuService;
}
```

禁止 `@Autowired` 字段注入和 setter 注入。

---

## 5. Entity 设计规范

### 5.1 继承 BaseEntity

所有数据库实体必须继承 `BaseEntity`：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_product")
public class Product extends BaseEntity {
    private String name;
    private Long categoryId;
    // ...
}
```

`BaseEntity` 提供：`id`、`createTime`、`updateTime`、`deleted`（逻辑删除）

### 5.2 逻辑删除

所有删除操作使用逻辑删除（`deleted = true`），禁止物理删除。

### 5.3 自动填充

`createTime` / `updateTime` 由 `AutoFillMetaObjectHandler` 自动填充，Entity 中不手动设置。

### 5.4 表名规范

- 表名：`yu_` 前缀 + 下划线分隔（如 `yu_product`、`yu_order_item`）
- 字段名：下划线分隔（如 `category_id`、`create_time`）
- Entity 属性：驼峰命名（MyBatis-Plus 自动映射）

---

## 6. MyBatis-Plus 使用规范

### 6.1 查询

优先使用 `LambdaQueryWrapper`，禁止字符串拼接 SQL：

```java
List<Product> products = productMapper.selectList(
    new LambdaQueryWrapper<Product>()
        .eq(Product::getDeleted, false)
        .eq(Product::getStatus, 1)
        .orderByDesc(Product::getSales)
);
```

### 6.2 分页

使用 MyBatis-Plus 的 `IPage`：

```java
IPage<ProductVO> page = searchService.search(criteria);
// 返回给 Controller，Controller 包装为 Result.success(page)
```

分页默认值：`page=1, size=20`。

---

## 7. 统一响应（Result<T>）

所有 Controller 方法**必须**返回 `Result<T>`：

```java
@GetMapping("/list")
public Result<IPage<Coupon>> list(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size) {
    return Result.success(couponService.pageCoupons(page, size));
}

@PostMapping
public Result<Void> create(@Valid @RequestBody CreateProductRequest request) {
    productService.createProduct(request);
    return Result.success();
}
```

成功用 `Result.success(data)`，失败抛 `BusinessException`。

---

## 8. 错误码规范（ResultCode）

业务错误码按模块分段，新增错误码必须遵守：

| 范围 | 模块 |
|---|---|
| 1xxx | 用户模块 |
| 2xxx | 商品模块 |
| 3xxx | 订单模块 |
| 4xxx | 支付模块 |
| 5xxx | 购物车模块 |
| 6xxx | 促销模块 |

新增错误码在 `ResultCode` 枚举中添加，禁止硬编码 code/message。

---

## 9. 异常处理

### 9.1 业务异常

抛 `BusinessException`，传入 `ResultCode`：

```java
if (product == null) {
    throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
}
```

### 9.2 全局处理

`GlobalExceptionHandler` 统一处理，禁止在 Controller 中 try-catch。

### 9.3 参数校验

DTO 上使用 Jakarta Validation，非法请求由 `GlobalExceptionHandler` 统一返回 400。

---

## 10. 缓存规范

```java
// 查询缓存
@Cacheable(value = "productDetail", key = "#id", unless = "#result == null")
public ProductDetailVO getProductDetail(Long id) { ... }

// 更新清除缓存
@CacheEvict(value = {"product", "productDetail"}, key = "#id")
public void updateProduct(Long id, UpdateProductRequest request) { ... }
```

- 缓存 value 名使用驼峰，与业务领域对应
- key 使用 SpEL 表达式，明确指定 `#id` 等参数
- 写操作必须清除相关缓存
- 缓存 TTL 在 `application.yml` 中配置（默认 10 分钟）

---

## 11. 事务规范

```java
@Transactional(rollbackFor = Exception.class)  // 必须指定 rollbackFor
public void createProduct(CreateProductRequest request) { ... }
```

- **必须**指定 `rollbackFor = Exception.class`（Spring 默认只回滚 RuntimeException）
- 事务加在 Service 层方法上
- 只读查询不需要事务
- 跨服务调用（RocketMQ 发送）不放在事务内

---

## 12. RocketMQ 规范

### 12.1 消费者

```java
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "payment-callback",
    consumerGroup = "yuemo-order-consumer"
)
@RequiredArgsConstructor
public class PaymentCallbackConsumer implements RocketMQListener<String> {
    private final OrderService orderService;

    @Override
    public void onMessage(String message) {
        try {
            // 处理消息
        } catch (Exception e) {
            log.error("消费失败", e);
            throw e; // 抛出异常触发重试
        }
    }
}
```

### 12.2 生产者

```java
rocketMQTemplate.convertAndSend("order-stock-release", stockMap);
```

### 12.3 Topic 命名

- 使用短横线分隔（`payment-callback`、`order-stock-release`）
- Consumer Group：`yuemo-{模块}-consumer`

---

## 13. 定时任务规范

```java
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OrderTimeoutTask {
    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 30_000)
    public void cancelTimeoutOrders() {
        // 批量处理，限制每次处理数量
        // 单条失败不影响其他记录
    }
}
```

- `@EnableScheduling` 加在任务类自身（模块化）
- `fixedDelay` / `fixedRate` 用下划线分隔数字（`30_000`）
- 批量处理限制 `BATCH_SIZE`，防止全表扫描
- 单条记录失败用 try-catch 包裹，不影响批次

---

## 14. API 路径规范

| 类型 | 路径格式 | 示例 |
|---|---|---|
| 前台接口 | `/api/{module}/...` | `/api/product/list` |
| 后台接口 | `/api/admin/...` | `/api/admin/product` |
| 公开接口 | `/api/{module}/...` | `/api/user/login`、`/api/user/register` |

HTTP 方法：
- `GET` — 查询
- `POST` — 创建
- `PUT` — 完整更新
- `DELETE` — 删除

---

## 15. 日志规范

```java
@Slf4j  // Lombok 统一日志
public class ProductServiceImpl {
    public void createProduct(CreateProductRequest request) {
        log.info("创建商品: name={}", request.name());
        // ...
    }
}
```

- 使用 `@Slf4j`（Lombok），不用 `LoggerFactory.getLogger()`
- 关键业务操作记 `info`，异常记 `error`
- 日志消息中用 `{}` 占位符，不拼接字符串
- 日志中不输出敏感数据（密码、Token、手机号）

---

## 16. 配置属性规范

```java
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtTokenProvider {
    private String secret;
    private long accessTokenExpiration = 1800;
}
```

- 使用 `@ConfigurationProperties` 绑定配置
- 设置合理的默认值
- 敏感配置（密钥、密码）不留默认值，启动时检查

---

## 17. 数据库规范

- **连接池**：HikariCP（Spring Boot 默认），最小空闲 5，最大 20
- **迁移工具**：Flyway，迁移脚本放在 `sql/` 目录
- **索引**：高选择性列建索引，联合索引遵循最左前缀
- **禁止**：在代码中拼接动态 SQL（防注入）

---

## 18. 测试规范

- 测试框架：JUnit 5 + Mockito
- 测试目录：`src/test/java/`，包结构镜像 `src/main/java/`
- 覆盖率目标：Service 层 ≥ 80%
- 命名：`{被测类}Test.java`，方法用 `@DisplayName` 描述

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock private ProductMapper productMapper;
    @InjectMocks private ProductServiceImpl productService;

    @Test
    @DisplayName("商品不存在时抛出 BusinessException")
    void getProductDetail_notFound_throwsException() {
        when(productMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> productService.getProductDetail(99L))
            .isInstanceOf(BusinessException.class);
    }
}
```

---

## 19. 禁止事项清单

- 禁止在 Controller 中编写业务逻辑
- 禁止 `@Autowired` 字段注入
- 禁止 DTO/VO 使用普通类（必须用 record）
- 禁止字符串拼接 SQL
- 禁止物理删除数据
- 禁止吞掉异常（至少记日志）
- 禁止 `@Transactional` 不指定 `rollbackFor`
- 禁止硬编码错误码和错误消息
- 禁止在日志中输出敏感信息
- 禁止日志中使用字符串拼接（用 `{}` 占位符）
- 禁止返回 Entity 直接给前端
