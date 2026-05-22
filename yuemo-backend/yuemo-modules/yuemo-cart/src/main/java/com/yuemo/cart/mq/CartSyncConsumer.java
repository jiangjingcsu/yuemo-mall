package com.yuemo.cart.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuemo.cart.constant.CartConstants;
import com.yuemo.cart.dto.CartSyncMessage;
import com.yuemo.cart.mq.handler.CartActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = CartConstants.CART_SYNC_TOPIC,
        consumerGroup = CartConstants.CART_CONSUMER_GROUP,
        maxReconsumeTimes = 5
)
public class CartSyncConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final Map<Class<? extends CartSyncMessage>, CartActionHandler<?>> handlerMap;

    public CartSyncConsumer(ObjectMapper objectMapper, List<CartActionHandler<?>> handlers) {
        this.objectMapper = objectMapper;
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(CartActionHandler::messageType, Function.identity()));
    }

    @Override
    public void onMessage(String message) {
        CartSyncMessage msg;
        try {
            msg = objectMapper.readValue(message, CartSyncMessage.class);
        } catch (Exception e) {
            log.error("解析购物车同步消息失败，消息将被丢弃: {}", message, e);
            return;
        }

        CartActionHandler<CartSyncMessage> handler =
                (CartActionHandler<CartSyncMessage>) handlerMap.get(msg.getClass());

        if (handler == null) {
            log.error("未找到消息处理器: type={}, userId={}", msg.getClass().getSimpleName(), msg.userId());
            return;
        }

        try {
            handler.handle(msg);
        } catch (Exception e) {
            log.error("购物车同步落库失败: type={}, userId={}", msg.getClass().getSimpleName(), msg.userId(), e);
            throw new RuntimeException("购物车同步落库失败，等待重试", e);
        }
    }
}
