package com.yuemo.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.payment.entity.Payment;
import com.yuemo.payment.mapper.PaymentMapper;
import com.yuemo.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Payment createPayment(Long userId, Long orderId) {
        // TODO: 调用订单服务查询订单信息
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setStatus(0);
        paymentMapper.insert(payment);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(Integer payType, Map<String, String> params) {
        String paymentNo = params.get("out_trade_no");
        String thirdTradeNo = params.get("trade_no");

        // 幂等校验
        String idempotentKey = "payment:callback:" + paymentNo;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", 30, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            log.warn("重复回调: paymentNo={}", paymentNo);
            return "success";
        }

        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) {
            throw new BusinessException(ResultCode.PAYMENT_CALLBACK_ERROR);
        }
        if (payment.getStatus() != 0) {
            return "success"; // 已处理
        }

        payment.setStatus(1);
        payment.setThirdTradeNo(thirdTradeNo);
        payment.setPayTime(LocalDateTime.now());
        paymentMapper.updateById(payment);

        // 通知订单模块
        rocketMQTemplate.convertAndSend("payment-callback", payment.getOrderNo());

        return "success";
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long userId, Long orderId) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
        if (payment == null || payment.getStatus() != 1) {
            throw new BusinessException(ResultCode.REFUND_FAILED);
        }

        // TODO: 调用第三方退款接口
        payment.setStatus(3);
        paymentMapper.updateById(payment);

        rocketMQTemplate.convertAndSend("payment-refund", orderId);
    }
}
