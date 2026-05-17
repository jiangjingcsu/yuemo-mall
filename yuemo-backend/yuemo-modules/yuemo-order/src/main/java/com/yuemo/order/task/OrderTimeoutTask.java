package com.yuemo.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.order.entity.Order;
import com.yuemo.order.entity.OrderItem;
import com.yuemo.order.mapper.OrderItemMapper;
import com.yuemo.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单超时取消定时任务
 * 每 30 秒扫描一次，将超过 30 分钟未支付的订单自动取消
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RocketMQTemplate rocketMQTemplate;

    private static final int TIMEOUT_MINUTES = 30;
    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 30_000)
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        List<Order> timeoutOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 0)
                .lt(Order::getCreateTime, deadline)
                .last("LIMIT " + BATCH_SIZE));

        if (timeoutOrders.isEmpty()) {
            return;
        }

        log.info("扫描到 {} 个超时订单，开始自动取消", timeoutOrders.size());

        for (Order order : timeoutOrders) {
            try {
                order.setStatus(4);
                orderMapper.updateById(order);

                // 查询订单明细，发送库存释放消息
                List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId()));
                Map<Long, Integer> stockMap = items.stream()
                        .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity));
                rocketMQTemplate.convertAndSend("order-stock-release", stockMap);

                log.info("超时订单已取消: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
            } catch (Exception e) {
                log.error("取消超时订单失败: orderId={}", order.getId(), e);
            }
        }
    }
}
