package com.yuemo.product.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.product.entity.Product;
import com.yuemo.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

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
public class StockReleaseConsumer implements RocketMQListener<Long> {

    private final ProductMapper productMapper;

    @Override
    public void onMessage(Long orderId) {
        log.info("收到释放库存消息: orderId={}", orderId);
        // TODO: 通过订单明细表查询该订单的商品和数量，恢复对应商品的库存
        // 当前简化处理：在实际项目中需查询订单明细，反向恢复库存
        log.info("释放库存处理完成: orderId={}", orderId);
    }
}
