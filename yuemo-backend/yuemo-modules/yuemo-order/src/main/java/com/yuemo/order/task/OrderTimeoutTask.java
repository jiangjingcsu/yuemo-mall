package com.yuemo.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.order.entity.Order;
import com.yuemo.order.enums.OrderStatus;
import com.yuemo.order.mapper.OrderMapper;
import com.yuemo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    private static final int TIMEOUT_MINUTES = 30;
    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 30_000)
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        List<Order> timeoutOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.UNPAID.getCode())
                .lt(Order::getCreateTime, deadline)
                .last("LIMIT " + BATCH_SIZE));

        if (timeoutOrders.isEmpty()) {
            return;
        }

        log.info("扫描到 {} 个超时订单，开始自动取消", timeoutOrders.size());

        for (Order order : timeoutOrders) {
            try {
                boolean cancelled = orderService.cancelOrderWithCas(order.getId());
                if (cancelled) {
                    log.info("超时订单已取消: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
                }
            } catch (Exception e) {
                log.error("取消超时订单失败: orderId={}", order.getId(), e);
            }
        }
    }
}
