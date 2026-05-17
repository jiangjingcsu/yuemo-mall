package com.yuemo.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.order.dto.CreateOrderDTO;
import com.yuemo.order.entity.Order;
import com.yuemo.order.entity.OrderItem;
import com.yuemo.order.mapper.OrderItemMapper;
import com.yuemo.order.mapper.OrderLogMapper;
import com.yuemo.order.mapper.OrderMapper;
import com.yuemo.product.entity.Product;
import com.yuemo.product.service.ProductService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl 单元测试")
class OrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private OrderLogMapper orderLogMapper;
    @Mock private RocketMQTemplate rocketMQTemplate;
    @Mock private ProductService productService;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderMapper, orderItemMapper, orderLogMapper, rocketMQTemplate, productService);
    }

    @Nested
    @DisplayName("创建订单")
    class CreateOrder {

        @Test
        @DisplayName("成功创建订单并查询真实商品价格")
        void shouldCreateOrderWithRealPrice() {
            Product product = new Product();
            product.setId(1L);
            product.setName("测试商品");
            product.setPrice(new BigDecimal("99.00"));
            product.setMainImage("/img/test.jpg");
            when(productService.getProductById(1L)).thenReturn(product);

            CreateOrderDTO dto = new CreateOrderDTO();
            dto.setAddressId(1L);
            dto.setRemark("测试备注");
            CreateOrderDTO.OrderItemDTO itemDTO = new CreateOrderDTO.OrderItemDTO();
            itemDTO.setProductId(1L);
            itemDTO.setQuantity(2);
            dto.setItems(List.of(itemDTO));

            Order result = orderService.createOrder(1001L, dto);

            assertThat(result.getUserId()).isEqualTo(1001L);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("198.00"));
            assertThat(result.getStatus()).isEqualTo(0);
            assertThat(result.getOrderNo()).isNotNull();
            verify(orderMapper).insert(any(Order.class));
            ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
            verify(orderItemMapper, times(1)).insert(itemCaptor.capture());
            OrderItem captured = itemCaptor.getValue();
            assertThat(captured.getProductName()).isEqualTo("测试商品");
            assertThat(captured.getPrice()).isEqualByComparingTo(new BigDecimal("99.00"));
            assertThat(captured.getQuantity()).isEqualTo(2);
            verify(rocketMQTemplate).sendMessageInTransaction(eq("order-stock-preoccupy"), any(), any());
        }
    }

    @Nested
    @DisplayName("取消订单")
    class CancelOrder {

        @Test
        @DisplayName("成功取消待支付订单")
        void shouldCancelPendingOrder() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(0);
            when(orderMapper.selectById(1L)).thenReturn(order);

            OrderItem item = new OrderItem();
            item.setProductId(1L);
            item.setQuantity(3);
            when(orderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

            orderService.cancelOrder(1001L, 1L);

            assertThat(order.getStatus()).isEqualTo(4);
            verify(orderMapper).updateById(order);
            verify(orderLogMapper).insert(any(com.yuemo.order.entity.OrderLog.class));
            verify(rocketMQTemplate).convertAndSend(eq("order-stock-release"), any(Map.class));
        }

        @Test
        @DisplayName("取消已支付订单抛出异常")
        void shouldThrowWhenCancelPaidOrder() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(1);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.cancelOrder(1001L, 1L))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("支付成功回调")
    class PaySuccess {

        @Test
        @DisplayName("支付成功更新订单状态")
        void shouldUpdateToPaid() {
            Order order = new Order();
            order.setStatus(0);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

            orderService.paySuccess("ORDER2024010100001");

            assertThat(order.getStatus()).isEqualTo(1);
            assertThat(order.getPayTime()).isNotNull();
            verify(orderMapper).updateById(order);
        }

        @Test
        @DisplayName("订单状态不是待支付时抛出异常")
        void shouldThrowWhenStatusNotPending() {
            Order order = new Order();
            order.setStatus(1);
            when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

            assertThatThrownBy(() -> orderService.paySuccess("ORDER2024010100001"))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("发货")
    class ShipOrder {

        @Test
        @DisplayName("成功发货")
        void shouldShipOrder() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(1);
            when(orderMapper.selectById(1L)).thenReturn(order);

            orderService.shipOrder(1L, "顺丰速运", "SF1234567890");

            assertThat(order.getStatus()).isEqualTo(2);
            assertThat(order.getLogisticsCompany()).isEqualTo("顺丰速运");
            assertThat(order.getLogisticsNo()).isEqualTo("SF1234567890");
            assertThat(order.getDeliveryTime()).isNotNull();
            verify(orderLogMapper).insert(any(com.yuemo.order.entity.OrderLog.class));
        }

        @Test
        @DisplayName("发货非已支付订单抛出异常")
        void shouldThrowWhenNotPaid() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(0);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.shipOrder(1L, "顺丰", "SF001"))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("确认收货")
    class ConfirmReceive {

        @Test
        @DisplayName("成功确认收货")
        void shouldConfirmReceive() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(2);
            when(orderMapper.selectById(1L)).thenReturn(order);

            orderService.confirmReceive(1001L, 1L);

            assertThat(order.getStatus()).isEqualTo(3);
            assertThat(order.getReceiveTime()).isNotNull();
            verify(orderLogMapper).insert(any(com.yuemo.order.entity.OrderLog.class));
        }
    }

    @Nested
    @DisplayName("删除订单")
    class DeleteOrder {

        @Test
        @DisplayName("成功删除已完成订单")
        void shouldDeleteCompletedOrder() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(3);
            when(orderMapper.selectById(1L)).thenReturn(order);

            orderService.deleteOrder(1001L, 1L);

            assertThat(order.getDeleted()).isTrue();
        }

        @Test
        @DisplayName("删除待支付订单抛出异常")
        void shouldThrowWhenDeletingPendingOrder() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(0);
            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.deleteOrder(1001L, 1L))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("查询")
    class Query {

        @Test
        @DisplayName("按ID查询订单成功")
        void shouldFindById() {
            Order order = new Order();
            order.setId(1L);
            order.setOrderNo("ORDER001");
            when(orderMapper.selectById(1L)).thenReturn(order);

            Order result = orderService.getOrderById(1L);

            assertThat(result.getOrderNo()).isEqualTo("ORDER001");
        }

        @Test
        @DisplayName("查不存在的订单抛出异常")
        void shouldThrowWhenNotFound() {
            when(orderMapper.selectById(99L)).thenReturn(null);

            assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("分页查询成功")
        void shouldPageOrders() {
            Page<Order> page = new Page<>(1, 10);
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            IPage<Order> result = orderService.pageOrders(1001L, 1, 10, null);

            assertThat(result).isNotNull();
        }
    }
}
