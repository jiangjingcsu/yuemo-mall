package com.yuemo.cart.mq;

import com.yuemo.cart.constant.CartConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "%DLQ%" + CartConstants.CART_CONSUMER_GROUP,
        consumerGroup = CartConstants.CART_CONSUMER_GROUP + "-dlq"
)
public class CartSyncDeadLetterConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.error("购物车同步消息进入死信队列，需人工介入: {}", message);
    }
}
