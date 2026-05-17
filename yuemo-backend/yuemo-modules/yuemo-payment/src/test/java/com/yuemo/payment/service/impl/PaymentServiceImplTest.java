package com.yuemo.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.order.entity.Order;
import com.yuemo.order.service.OrderService;
import com.yuemo.payment.entity.Payment;
import com.yuemo.payment.mapper.PaymentMapper;
import com.yuemo.user.service.UserService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl 单元测试")
class PaymentServiceImplTest {

    @Mock private PaymentMapper paymentMapper;
    @Mock private RocketMQTemplate rocketMQTemplate;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private OrderService orderService;
    @Mock private UserService userService;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentMapper, rocketMQTemplate, redisTemplate, orderService, userService);
    }

    @Nested
    @DisplayName("创建支付")
    class CreatePayment {

        @Test
        @DisplayName("成功创建支付记录")
        void shouldCreatePayment() {
            Order order = new Order();
            order.setId(1L);
            order.setOrderNo("ORDER001");
            order.setPayAmount(new BigDecimal("198.00"));
            order.setStatus(0);
            when(orderService.getOrderById(1L)).thenReturn(order);

            Payment result = paymentService.createPayment(1001L, 1L, 1);

            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getUserId()).isEqualTo(1001L);
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("198.00"));
            assertThat(result.getPaymentNo()).isNotNull();
            assertThat(result.getStatus()).isEqualTo(0);
            verify(paymentMapper).insert(any(Payment.class));
        }

        @Test
        @DisplayName("订单状态不正确时抛出异常")
        void shouldThrowWhenOrderStatusInvalid() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(1); // 已支付
            when(orderService.getOrderById(1L)).thenReturn(order);

            assertThatThrownBy(() -> paymentService.createPayment(1001L, 1L, 1))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("余额支付成功：扣减余额并创建已完成支付记录")
        void shouldPayByBalance() {
            Order order = new Order();
            order.setId(1L);
            order.setOrderNo("ORDER001");
            order.setPayAmount(new BigDecimal("198.00"));
            order.setStatus(0);
            when(orderService.getOrderById(1L)).thenReturn(order);
            when(userService.getBalance(1001L)).thenReturn(new BigDecimal("500.00"));

            Payment result = paymentService.createPayment(1001L, 1L, 3);

            assertThat(result.getStatus()).isEqualTo(1);
            assertThat(result.getPayType()).isEqualTo(3);
            assertThat(result.getPaymentNo()).isNotNull();
            assertThat(result.getPayTime()).isNotNull();
            verify(userService).deductBalance(1001L, new BigDecimal("198.00"));
            verify(paymentMapper).insert(any(Payment.class));
            verify(rocketMQTemplate).convertAndSend("payment-callback", "ORDER001");
        }

        @Test
        @DisplayName("余额不足时抛出异常")
        void shouldThrowWhenBalanceInsufficient() {
            Order order = new Order();
            order.setId(1L);
            order.setOrderNo("ORDER001");
            order.setPayAmount(new BigDecimal("198.00"));
            order.setStatus(0);
            when(orderService.getOrderById(1L)).thenReturn(order);
            when(userService.getBalance(1001L)).thenReturn(new BigDecimal("50.00"));

            assertThatThrownBy(() -> paymentService.createPayment(1001L, 1L, 3))
                .isInstanceOf(BusinessException.class);

            verify(userService, never()).deductBalance(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("支付回调")
    class HandleCallback {

        @Test
        @DisplayName("成功处理支付回调")
        void shouldHandleCallback() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(true);

            Payment payment = new Payment();
            payment.setPaymentNo("PAY001");
            payment.setStatus(0);
            payment.setOrderNo("ORDER001");
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);

            Map<String, String> params = new HashMap<>();
            params.put("out_trade_no", "PAY001");
            params.put("trade_no", "WX123456");
            params.put("sign", "test-sign");

            String result = paymentService.handleCallback(1, params);

            assertThat(result).isEqualTo("success");
            assertThat(payment.getStatus()).isEqualTo(1);
            assertThat(payment.getThirdTradeNo()).isEqualTo("WX123456");
            verify(rocketMQTemplate).convertAndSend("payment-callback", "ORDER001");
        }

        @Test
        @DisplayName("回调缺少签名返回失败")
        void shouldFailWithoutSign() {
            Map<String, String> params = new HashMap<>();
            params.put("out_trade_no", "PAY001");

            String result = paymentService.handleCallback(1, params);

            assertThat(result).isEqualTo("fail");
        }

        @Test
        @DisplayName("重复回调返回 success")
        void shouldReturnSuccessForDuplicate() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any())).thenReturn(false);

            Map<String, String> params = new HashMap<>();
            params.put("out_trade_no", "PAY001");
            params.put("trade_no", "WX123");
            params.put("sign", "test-sign");

            String result = paymentService.handleCallback(1, params);

            assertThat(result).isEqualTo("success");
            verify(paymentMapper, never()).updateById(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("退款")
    class Refund {

        @Test
        @DisplayName("成功退款")
        void shouldRefund() {
            Payment payment = new Payment();
            payment.setStatus(1);
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);

            paymentService.refund(1001L, 1L, "商品不满意");

            assertThat(payment.getStatus()).isEqualTo(3);
            assertThat(payment.getRefundReason()).isEqualTo("商品不满意");
            assertThat(payment.getRefundNo()).isNotNull();
            assertThat(payment.getRefundTime()).isNotNull();
            verify(rocketMQTemplate).convertAndSend("payment-refund", 1L);
        }

        @Test
        @DisplayName("退款不存在的支付记录抛出异常")
        void shouldThrowWhenPaymentNotFound() {
            when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> paymentService.refund(1001L, 1L, ""))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("支付列表")
    class ListPayments {

        @Test
        @DisplayName("分页查询支付记录")
        void shouldListPayments() {
            Page<Payment> page = new Page<>(1, 10);
            when(paymentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            IPage<Payment> result = paymentService.listPayments(1001L, 1, 10);

            assertThat(result).isNotNull();
        }
    }
}
