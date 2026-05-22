package com.yuemo.order.mq;

import com.yuemo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "payment-callback",
    consumerGroup = "yuemo-order-consumer"
)
@RequiredArgsConstructor
public class PaymentCallbackConsumer implements RocketMQListener<String> {

    private static final String IDEMPOTENT_PREFIX = "mq:consumed:payment-callback:";
    private static final long IDEMPOTENT_TTL_MINUTES = 10;

    private final OrderService orderService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(String orderNo) {
        log.info("收到支付回调消息: orderNo={}", orderNo);
        String idempotentKey = IDEMPOTENT_PREFIX + orderNo;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL_MINUTES, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("支付回调重复消费，跳过: orderNo={}", orderNo);
            return;
        }

        try {
            orderService.paySuccess(orderNo);
            log.info("订单支付状态更新成功: orderNo={}", orderNo);
        } catch (Exception e) {
            redisTemplate.delete(idempotentKey);
            log.error("订单支付状态更新失败: orderNo={}", orderNo, e);
            throw e;
        }
    }
}
