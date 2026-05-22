package com.yuemo.product.mq;

import com.yuemo.product.service.ProductService;
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
    topic = "order-stock-release",
    consumerGroup = "yuemo-product-stock-release-consumer"
)
@RequiredArgsConstructor
public class StockReleaseConsumer implements RocketMQListener<Map<Long, Integer>> {

    private static final String IDEMPOTENT_PREFIX = "mq:consumed:stock-release:";
    private static final long IDEMPOTENT_TTL_MINUTES = 10;

    private final ProductService productService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(Map<Long, Integer> stockMap) {
        log.info("收到释放库存消息: {}", stockMap);
        String fingerprint = buildFingerprint(stockMap);
        String idempotentKey = IDEMPOTENT_PREFIX + fingerprint;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", IDEMPOTENT_TTL_MINUTES, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("释放库存重复消费，跳过: fingerprint={}", fingerprint);
            return;
        }

        try {
            stockMap.forEach((productId, quantity) -> {
                productService.restoreStock(productId, quantity);
                log.info("释放库存成功: productId={}, quantity={}", productId, quantity);
            });
        } catch (Exception e) {
            redisTemplate.delete(idempotentKey);
            log.error("释放库存失败: fingerprint={}", fingerprint, e);
            throw e;
        }
    }

    private String buildFingerprint(Map<Long, Integer> stockMap) {
        StringBuilder sb = new StringBuilder();
        stockMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append(':').append(e.getValue()).append(';'));
        return Integer.toHexString(sb.toString().hashCode());
    }
}
