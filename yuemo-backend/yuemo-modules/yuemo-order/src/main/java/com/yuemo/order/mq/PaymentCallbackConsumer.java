package com.yuemo.order.mq;

import com.yuemo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 支付回调消费者 — 支付成功后更新订单状态
 */
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
    public void onMessage(String orderNo) {
        log.info("收到支付回调消息: orderNo={}", orderNo);
        try {
            orderService.paySuccess(orderNo);
            log.info("订单支付状态更新成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("订单支付状态更新失败: orderNo={}", orderNo, e);
            throw e; // 重试
        }
    }
}
