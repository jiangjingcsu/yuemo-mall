# 后端编码规范（Java 17 / Spring Boot 3.2 / MyBatis-Plus）

> 基于《阿里巴巴Java开发手册》与项目实际代码模式提炼，新增代码必须遵守。

---

## 1. 命名规范（阿里强制）

| 类型 | 规则 | 正例 | 反例 |
|---|---|---|---|
| 类 | UpperCamelCase | `ProductService` | `productservice` |
| 方法/变量 | lowerCamelCase | `getById` | `GetById` |
| 常量 | 全大写+下划线 | `MAX_STOCK` | `maxStock` |
| 包名 | 全小写，单数 | `com.yuemo.product` | `com.yuemo.products` |
| 抽象类 | Base/Xxx前缀 | `BaseEntity` | `AbstractEntity` |
| 异常类 | XxxException | `BusinessException` | `BizError` |
| 测试类 | 被测类+Test | `ProductServiceTest` | `ProductTest` |
| Service | I+XxxService | `IProductService` | `ProductServiceImpl` |
| ServiceImpl | XxxServiceImpl | `ProductServiceImpl` | `ProductService` |
| Mapper | XxxMapper | `ProductMapper` | `ProductDao` |
| boolean字段 | is+Xxx | `isDeleted` | `deleted` |
| DTO | XxxRequest/XxxCriteria | `CreateProductRequest` | `ProductDTO` |
| VO | XxxVO | `ProductDetailVO` | `ProductView` |

- 禁止拼音与英文混用（`daZhePromotion` ❌ → `discountPromotion` ✅）
- 禁止缩写（`a2b` ❌），通用缩写除外（`id`/`dto`/`vo`/`dao`/`xml`/`html`/`http`/`ip`）
- 方法名：动词开头 — `get`/`list`/`count`/`save`/`update`/`delete`/`is`/`has`

---

## 2. 分层架构

```
Controller → Service(接口) → ServiceImpl → Mapper → DB
     ↕            ↕               ↕
  DTO/VO       Result<T>      Entity(BaseEntity)
```

| 层 | 职责 | 规范 |
|---|---|---|
| Controller | 参数接收、调用Service、返回Result | 不写业务逻辑 |
| Service接口 | `IXXXService extends IService<Entity>` | MyBatis-Plus模式 |
| ServiceImpl | 业务逻辑 | `@RequiredArgsConstructor`注入 |
| Mapper | `extends BaseMapper<Entity>` | 复杂查询写XML |
| Entity | 继承`BaseEntity` | `@Data`+`@TableName` |

---

## 3. Lombok & 依赖注入

| 场景 | 注解 |
|---|---|
| Entity | `@Data` + `@EqualsAndHashCode(callSuper = true)` |
| DTO/VO | **不使用Lombok**，用Java record |
| Service/Controller | `@RequiredArgsConstructor` |
| 日志 | `@Slf4j` |
| 配置类 | `@Getter` + `@Setter` + `@ConfigurationProperties` |
| Result响应类 | `@Getter` |
| 枚举 | `@Getter` + `@AllArgsConstructor` |

**构造器注入（强制）**：所有依赖通过`@RequiredArgsConstructor`注入，禁止`@Autowired`字段注入和setter注入。

---

## 4. Java Record（强制）

DTO和VO**必须**使用`record`，禁止普通类：

```java
public record SearchCriteria(String keyword, Integer page, Integer size) {
    public SearchCriteria {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;
    }
}
public record CreateProductRequest(
    @NotBlank String name, @NotNull Long categoryId,
    @NotNull @Positive BigDecimal price, @NotNull List<SkuRequest> skus
) {
    public record SkuRequest(@NotNull @Positive BigDecimal price, @NotNull Integer stock) {}
}
```

---

## 5. Entity 规范

| 项目 | 规范 |
|---|---|
| 继承 | 必须继承`BaseEntity`（提供`id`/`createTime`/`updateTime`/`deleted`） |
| 注解 | `@Data` + `@EqualsAndHashCode(callSuper = true)` + `@TableName` |
| 删除 | 逻辑删除（`deleted=true`），禁止物理删除 |
| 时间字段 | 由`AutoFillMetaObjectHandler`自动填充，不手动设置 |
| 表名 | `yu_`前缀+下划线（`yu_product`/`yu_order_item`） |
| 字段名 | DB下划线（`category_id`），Entity驼峰（`categoryId`） |

```java
@Data @EqualsAndHashCode(callSuper = true) @TableName("yu_product")
public class Product extends BaseEntity { private String name; private Long categoryId; }
```

---

## 6. MyBatis-Plus 规范

- 查询优先`LambdaQueryWrapper`，禁止字符串拼接SQL
- 分页用`IPage`，默认`page=1, size=20`

```java
productMapper.selectList(new LambdaQueryWrapper<Product>()
    .eq(Product::getDeleted, false).eq(Product::getStatus, 1).orderByDesc(Product::getSales));
```

---

## 7. 统一响应 & 错误码

**响应**：Controller方法**必须**返回`Result<T>`，成功`Result.success(data)`，失败抛`BusinessException`。

```java
@GetMapping("/list")
public Result<IPage<Coupon>> list(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size) {
    return Result.success(couponService.pageCoupons(page, size));
}
```

**错误码分段**（在`ResultCode`枚举中添加，禁止硬编码）：

| 范围 | 模块 | 范围 | 模块 |
|---|---|---|---|
| 1xxx | 用户 | 4xxx | 支付 |
| 2xxx | 商品 | 5xxx | 购物车 |
| 3xxx | 订单 | 6xxx | 促销 |

---

## 8. 异常处理

| 场景 | 规范 |
|---|---|
| 业务异常 | 抛`BusinessException(ResultCode)` |
| 全局处理 | `GlobalExceptionHandler`统一处理，禁止Controller中try-catch |
| 参数校验 | DTO用Jakarta Validation，非法请求由GlobalExceptionHandler返回400 |

---

## 9. 缓存规范

```java
@Cacheable(value = "productDetail", key = "#id", unless = "#result == null")
public ProductDetailVO getProductDetail(Long id) { ... }
@CacheEvict(value = {"product", "productDetail"}, key = "#id")
public void updateProduct(Long id, UpdateProductRequest request) { ... }
```

- value名驼峰，与业务对应；key用SpEL（`#id`）；写操作必须清缓存；TTL在yml配置（默认10分钟）

---

## 10. 事务规范

```java
@Transactional(rollbackFor = Exception.class)
public void createProduct(CreateProductRequest request) { ... }
```

- **必须**指定`rollbackFor = Exception.class`；事务加在Service层；只读查询不加；跨服务调用（MQ发送）不放在事务内

---

## 11. RocketMQ 规范

```java
@Slf4j @Component
@RocketMQMessageListener(topic = "payment-callback", consumerGroup = "yuemo-order-consumer")
@RequiredArgsConstructor
public class PaymentCallbackConsumer implements RocketMQListener<String> {
    private final OrderService orderService;
    @Override
    public void onMessage(String message) {
        try { /* 处理 */ } catch (Exception e) { log.error("消费失败", e); throw e; }
    }
}
```

- 生产者：`rocketMQTemplate.convertAndSend("order-stock-release", stockMap)`
- Topic：短横线分隔（`payment-callback`）；Consumer Group：`yuemo-{模块}-consumer`

---

## 12. 定时任务规范

```java
@Slf4j @Component @EnableScheduling @RequiredArgsConstructor
public class OrderTimeoutTask {
    private static final int BATCH_SIZE = 100;
    @Scheduled(fixedDelay = 30_000)
    public void cancelTimeoutOrders() { /* 批量处理，单条失败不影响批次 */ }
}
```

- `@EnableScheduling`加在任务类自身；数字用下划线分隔（`30_000`）；限制BATCH_SIZE防全表扫描

---

## 13. API 路径规范

| 类型 | 格式 | 示例 |
|---|---|---|
| 前台 | `/api/{module}/...` | `/api/product/list` |
| 后台 | `/api/admin/...` | `/api/admin/product` |
| 公开 | `/api/{module}/...` | `/api/user/login` |

HTTP方法：`GET`查询 / `POST`创建 / `PUT`更新 / `DELETE`删除

---

## 14. 日志规范

- `@Slf4j`，不用`LoggerFactory.getLogger()`
- 关键业务`info`，异常`error`；用`{}`占位符，不拼接字符串
- 禁止输出敏感数据（密码/Token/手机号）
- 异常日志必须包含堆栈：`log.error("创建订单失败, orderId={}", orderId, e)`（e放最后）

---

## 15. 配置属性规范

```java
@Getter @Setter @Component @ConfigurationProperties(prefix = "jwt")
public class JwtTokenProvider { private String secret; private long accessTokenExpiration = 1800; }
```

- 用`@ConfigurationProperties`绑定；设合理默认值；敏感配置不留默认值，启动时检查

---

## 16. 数据库规范

| 项目 | 规范 |
|---|---|
| 连接池 | HikariCP，最小空闲5，最大20 |
| 迁移 | Flyway，脚本放`sql/`目录 |
| 索引 | 高选择性列建索引，联合索引遵循最左前缀 |
| 小数 | 禁止float/double，用`DECIMAL`（对应Java `BigDecimal`） |
| 字符 | 统一`utf8mb4`，varchar长度按实际需要设 |
| 禁止 | 拼接动态SQL；存储过程；视图；触发器 |

---

## 17. 测试规范

- JUnit 5 + Mockito；目录镜像`src/main/java/`；Service层覆盖率≥80%
- 命名：`{被测类}Test.java`，方法用`@DisplayName`

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock ProductMapper productMapper; @InjectMocks ProductServiceImpl productService;
    @Test @DisplayName("商品不存在时抛出BusinessException")
    void getProductDetail_notFound() {
        when(productMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> productService.getProductDetail(99L)).isInstanceOf(BusinessException.class);
    }
}
```

---

## 18. OOP 规约（阿里）

| 规则 | 说明 |
|---|---|
| equals | 使用常量或确定非null对象调用：`"yes".equals(str)` ✅，`str.equals("yes")` ❌ |
| 包装类 | POJO/Entity/DTO必须用包装类（`Integer`/`Long`），局部变量可用基本类型 |
| NPE防护 | 集合判空用`CollectionUtils.isEmpty()`；字符串用`StringUtils.isBlank()`；所有返回集合的方法返回空集合而非null |
| toString | Entity禁止覆写toString（可能触发懒加载/OOM），record自带 |
| 序列化 | 禁止实现`Serializable`（本项目无RPC序列化需求） |
| 继承 | 优先组合而非继承；接口隔离，单个接口方法数≤5 |
| 可变参数 | 参数类型与个数确定时不用可变参数 |

---

## 19. 集合规约（阿里）

| 规则 | 说明 |
|---|---|
| 初始化容量 | `new ArrayList<>(expectedSize)`，避免扩容 |
| subList | 返回的是原视图，修改子列表影响原列表；禁止强转`ArrayList` |
| toArray | 用`list.toArray(new T[0])`，不用`list.toArray()` |
| 集合转数组 | `Arrays.asList(arr)`返回固定大小列表，不支持add/remove |
| 遍历删除 | 用`removeIf()`或Iterator，禁止foreach中remove |
| Map | 遍历用`entrySet()`，不用`keySet()`二次取值 |
| null键 | `ConcurrentHashMap`禁止null键/值；`HashMap`可以但应避免 |
| 排序 | `Collections.sort()`前先判空 |

---

## 20. 并发规约（阿里）

| 规则 | 说明 |
|---|---|
| 线程池 | 禁止`Executors.newXxx()`创建线程池，必须用`ThreadPoolExecutor`构造，明确核心线程数/队列/拒绝策略 |
| ThreadLocal | 用完必须`remove()`，防止内存泄漏和线程池复用污染 |
| SimpleDateFormat | 线程不安全，用`DateTimeFormatter`（线程安全）或`ThreadLocal` |
| 单例 | Double-Check Locking必须用`volatile`；推荐用枚举实现单例 |
| 锁 | 优先`Lock`接口（`ReentrantLock`），少用`synchronized`；锁范围尽量小 |
| 并发容器 | 多线程场景优先`ConcurrentHashMap`/`CopyOnWriteArrayList` |
| 线程安全方法 | `Collections.synchronizedXxx()`/`Collections.unmodifiableXxx()` |
| CountDownLatch | 子线程异常须`countDown()`，否则主线程永久阻塞 |

---

## 21. 控制语句规约（阿里）

| 规则 | 说明 |
|---|---|
| switch | 必须有`default`；`String`类型switch先判null防NPE |
| if-else | 超过3层用卫语句/策略模式/状态模式重构 |
| else | 非必要不写else，优先提前return |
| 参数校验 | 方法入口校验参数，非法直接抛异常或返回 |
| 循环体 | 不做方法调用级别的性能浪费（如`list.size()`在循环条件中每次调用不影响，但避免在循环内创建大量对象） |
| 三目运算 | 条件表达式类型必须一致，防止自动拆箱NPE |

---

## 22. 日期时间规约（阿里）

- 禁止`Date`/`Calendar`/`SimpleDateFormat`，使用`java.time`包
- `LocalDateTime`替代`Date`，`DateTimeFormatter`替代`SimpleDateFormat`
- 时间戳传递用`Instant`，数据库字段用`DATETIME`
- 获取时间戳：`System.currentTimeMillis()`，不用`new Date().getTime()`

---

## 23. 安全规约（阿里）

| 规则 | 说明 |
|---|---|
| SQL注入 | 禁止拼接SQL，必须参数化查询（MyBatis用`#{}`，禁止`${}`拼接用户输入） |
| XSS | 用户输入存储前转义，输出时编码；前端富文本过滤标签 |
| 敏感数据 | 密码用BCrypt哈希存储；手机号/身份证脱敏展示（`138****1234`） |
| 权限 | 后台接口必须鉴权；用户只能操作自己的数据（越权校验） |
| Token | JWT密钥强度≥256位；Token过期时间合理；登出使Token失效 |
| 限流 | 接口层限流（令牌桶/漏桶），防刷防重放 |
| CSRF | 状态修改操作必须验证Referer或CSRF Token |
| 日志 | 禁止输出密码/密钥/Token/完整手机号/身份证号 |
| 序列化 | 禁止反序列化不可信数据（防止RCE） |

---

## 24. 禁止事项清单

| # | 禁止项 |
|---|---|
| 1 | Controller中编写业务逻辑 |
| 2 | `@Autowired`字段注入 |
| 3 | DTO/VO使用普通类（必须用record） |
| 4 | 字符串拼接SQL |
| 5 | 物理删除数据 |
| 6 | 吞掉异常（至少记日志） |
| 7 | `@Transactional`不指定`rollbackFor` |
| 8 | 硬编码错误码和错误消息 |
| 9 | 日志中输出敏感信息 |
| 10 | 日志中使用字符串拼接（用`{}`占位符） |
| 11 | 返回Entity直接给前端 |
| 12 | `Executors.newXxx()`创建线程池 |
| 13 | `Date`/`SimpleDateFormat`（用`java.time`） |
| 14 | MyBatis中用`${}`拼接用户输入 |
| 15 | POJO/Entity使用基本类型（必须用包装类） |
| 16 | 集合返回null（返回空集合） |
| 17 | 存储过程/视图/触发器 |
| 18 | float/double存金额（用BigDecimal） |
