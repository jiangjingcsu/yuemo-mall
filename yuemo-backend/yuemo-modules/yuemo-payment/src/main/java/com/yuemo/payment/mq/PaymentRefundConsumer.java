package com.yuemo.payment.mq;

import com.yuemo.payment.dto.RefundMessage;
import com.yuemo.user.service.UserService;
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
    topic = "payment-refund",
    consumerGroup = "yuemo-payment-consumer"
)
@RequiredArgsConstructor
public class PaymentRefundConsumer implements RocketMQListener<RefundMessage> {

    private static final String IDEMPOTENT_PREFIX = "mq:consumed:payment-refund:";
    private static final long IDEMPOTENT_TTL_MINUTES = 10;

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(RefundMessage msg) {
        log.info("收到退款消息: orderId={}, userId={}, amount={}", msg.orderId(), msg.userId(), msg.amount());
        String idempotentKey = IDEMPOTENT_PREFIX + msg.orderId();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL_MINUTES, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("退款消息重复消费，跳过: orderId={}", msg.orderId());
            return;
        }

        try {
            userService.addBalance(msg.userId(), msg.amount());
            log.info("退款余额恢复成功: userId={}, amount={}", msg.userId(), msg.amount());
        } catch (Exception e) {
            redisTemplate.delete(idempotentKey);
            log.error("退款余额恢复失败: orderId={}, userId={}", msg.orderId(), msg.userId(), e);
            throw e;
        }
    }
}
