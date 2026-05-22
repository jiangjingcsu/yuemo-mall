package com.yuemo.order.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuemo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "payment-refund",
    consumerGroup = "yuemo-order-refund-consumer"
)
@RequiredArgsConstructor
public class OrderRefundConsumer implements RocketMQListener<String> {

    private static final String IDEMPOTENT_PREFIX = "mq:consumed:order-refund:";
    private static final long IDEMPOTENT_TTL_MINUTES = 10;

    private final OrderService orderService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(String message) {
        log.info("收到退款消息（订单侧）: {}", message);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> refundMap = objectMapper.readValue(message, Map.class);
            Long orderId = ((Number) refundMap.get("orderId")).longValue();

            String idempotentKey = IDEMPOTENT_PREFIX + orderId;
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL_MINUTES, TimeUnit.MINUTES);
            if (Boolean.FALSE.equals(acquired)) {
                log.warn("订单退款重复消费，跳过: orderId={}", orderId);
                return;
            }

            try {
                orderService.refundOrder(orderId);
                log.info("订单退款处理成功: orderId={}", orderId);
            } catch (Exception e) {
                redisTemplate.delete(idempotentKey);
                log.error("订单退款处理失败: orderId={}", orderId, e);
                throw e;
            }
        } catch (Exception e) {
            log.error("解析退款消息失败: {}", message, e);
            throw new RuntimeException("解析退款消息失败", e);
        }
    }
}
