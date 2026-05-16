package com.yuemo.product.mq;

import com.yuemo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "order-stock-preoccupy",
    consumerGroup = "yuemo-product-consumer"
)
@RequiredArgsConstructor
public class StockPreoccupyConsumer implements RocketMQListener<Map<Long, Integer>> {

    private final ProductService productService;

    @Override
    public void onMessage(Map<Long, Integer> stockMap) {
        log.info("收到预占库存消息: {}", stockMap);
        stockMap.forEach((skuId, quantity) -> {
            try {
                // Now operates at SKU level
                productService.updateSkuStock(skuId, quantity);
                log.info("预占库存成功: skuId={}, quantity={}", skuId, quantity);
            } catch (Exception e) {
                log.error("预占库存失败: skuId={}, quantity={}", skuId, quantity, e);
                throw e;
            }
        });
    }
}
