package com.yuemo.product.mq;

import com.yuemo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 释放库存消费者 — 订单取消/超时后恢复库存
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "order-stock-release",
    consumerGroup = "yuemo-product-consumer"
)
@RequiredArgsConstructor
public class StockReleaseConsumer implements RocketMQListener<Map<Long, Integer>> {

    private final ProductService productService;

    @Override
    public void onMessage(Map<Long, Integer> stockMap) {
        log.info("收到释放库存消息: {}", stockMap);
        stockMap.forEach((productId, quantity) -> {
            try {
                productService.restoreStock(productId, quantity);
                log.info("释放库存成功: productId={}, quantity={}", productId, quantity);
            } catch (Exception e) {
                log.error("释放库存失败: productId={}, quantity={}", productId, quantity, e);
                throw e;
            }
        });
    }
}
