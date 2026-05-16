package com.yuemo.order.mq;

import com.yuemo.order.entity.Order;
import com.yuemo.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;

/**
 * 下单预占库存事务监听器
 *
 * 半消息发送后执行本地事务（创建订单），
 * 若本地事务成功则提交消息通知商品模块扣库存，
 * 若失败则回滚消息不扣库存。
 */
@Slf4j
@RocketMQTransactionListener
@RequiredArgsConstructor
public class OrderStockTransactionListener implements RocketMQLocalTransactionListener {

    private final OrderMapper orderMapper;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        Long orderId = (Long) msg.getHeaders().get("orderId");
        log.info("执行本地事务: orderId={}", orderId);

        try {
            Order order = orderMapper.selectById(orderId);
            if (order != null && order.getStatus() != null) {
                // 订单已创建成功，提交消息 → 商品模块扣库存
                log.info("订单创建成功，提交预占库存消息: orderId={}", orderId);
                return RocketMQLocalTransactionState.COMMIT;
            }
            log.warn("订单不存在，回滚消息: orderId={}", orderId);
            return RocketMQLocalTransactionState.ROLLBACK;
        } catch (Exception e) {
            log.error("本地事务异常，回滚: orderId={}", orderId, e);
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        Long orderId = (Long) msg.getHeaders().get("orderId");
        log.info("回查本地事务: orderId={}", orderId);

        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            return RocketMQLocalTransactionState.COMMIT;
        }
        return RocketMQLocalTransactionState.ROLLBACK;
    }
}
