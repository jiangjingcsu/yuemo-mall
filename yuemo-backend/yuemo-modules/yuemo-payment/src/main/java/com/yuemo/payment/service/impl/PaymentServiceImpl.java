package com.yuemo.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.common.core.utils.IdGenerator;
import com.yuemo.order.entity.Order;
import com.yuemo.order.enums.OrderStatus;
import com.yuemo.order.service.OrderService;
import com.yuemo.payment.constant.PaymentRedisKeyConstants;
import com.yuemo.payment.dto.RefundMessage;
import com.yuemo.payment.entity.Payment;
import com.yuemo.payment.enums.PayType;
import com.yuemo.payment.enums.PaymentStatus;
import com.yuemo.payment.handler.PayTypeHandler;
import com.yuemo.payment.handler.PayTypeHandlerFactory;
import com.yuemo.payment.mapper.PaymentMapper;
import com.yuemo.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderService orderService;
    private final PayTypeHandlerFactory payTypeHandlerFactory;

    public PaymentServiceImpl(PaymentMapper paymentMapper,
                              RocketMQTemplate rocketMQTemplate,
                              RedisTemplate<String, Object> redisTemplate,
                              OrderService orderService,
                              PayTypeHandlerFactory payTypeHandlerFactory) {
        this.paymentMapper = paymentMapper;
        this.rocketMQTemplate = rocketMQTemplate;
        this.redisTemplate = redisTemplate;
        this.orderService = orderService;
        this.payTypeHandlerFactory = payTypeHandlerFactory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment createPayment(Long userId, Long orderId, Integer payType) {
        PayType pt = PayType.fromCode(payType);

        Order order = orderService.getOrderByIdAndUserId(orderId, userId);
        if (order.getStatus() != OrderStatus.UNPAID.getCode()) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        Payment existing = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getStatus, PaymentStatus.PENDING.getCode()));
        if (existing != null) {
            return existing;
        }

        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo(userId));
        payment.setOrderId(orderId);
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(userId);
        payment.setAmount(order.getPayAmount());
        payment.setPayType(pt.getCode());

        PayTypeHandler handler = payTypeHandlerFactory.get(payType);
        handler.doPay(userId, payment);

        paymentMapper.insert(payment);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(Integer payType, Map<String, String> params) {
        String sign = params.get("sign");
        if (sign == null || sign.isBlank()) {
            log.error("支付回调缺少签名: payType={}", payType);
            return "fail";
        }

        if (!verifySign(payType, params)) {
            log.error("支付回调验签失败: payType={}", payType);
            return "fail";
        }

        String paymentNo = params.get("out_trade_no");
        String thirdTradeNo = params.get("trade_no");

        String idempotentKey = PaymentRedisKeyConstants.PAYMENT_CALLBACK_PREFIX + paymentNo;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", PaymentRedisKeyConstants.CALLBACK_TTL_MINUTES, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            log.warn("重复回调（Redis幂等）: paymentNo={}", paymentNo);
            return "success";
        }

        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentNo, paymentNo));
        if (payment == null) {
            throw new BusinessException(ResultCode.PAYMENT_CALLBACK_ERROR);
        }

        if (!payment.isPending()) {
            return "success";
        }

        payment.markSuccess(thirdTradeNo);
        paymentMapper.updateById(payment);

        rocketMQTemplate.convertAndSend("payment-callback", payment.getOrderNo());
        return "success";
    }

    private boolean verifySign(Integer payType, Map<String, String> params) {
        if (payType == PayType.BALANCE.getCode()) {
            return true;
        }
        log.error("支付回调签名验证未实现: payType={}, 真实SDK未接入，拒绝回调", payType);
        return false;
    }

    @Override
    public Payment getPaymentByIdAndUserId(Long id, Long userId) {
        Payment payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw new BusinessException(ResultCode.PAYMENT_NOT_FOUND);
        }
        payment.verifyOwnership(userId);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long userId, Long orderId, String reason) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
        if (payment == null) {
            throw new BusinessException(ResultCode.PAYMENT_NOT_FOUND);
        }
        payment.verifyOwnership(userId);
        payment.markRefunded(generateRefundNo(userId), reason);
        paymentMapper.updateById(payment);

        RefundMessage refundMessage = new RefundMessage(orderId, userId, payment.getAmount());
        rocketMQTemplate.convertAndSend("payment-refund", refundMessage);
    }

    @Override
    public IPage<Payment> listPayments(Long userId, Integer page, Integer size) {
        Page<Payment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<Payment>()
                .eq(Payment::getUserId, userId)
                .orderByDesc(Payment::getCreateTime);
        return paymentMapper.selectPage(pageParam, wrapper);
    }

    private String generatePaymentNo(Long userId) {
        return "PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + userId % 10000
                + IdGenerator.nextId() % 1000;
    }

    private String generateRefundNo(Long userId) {
        return "REF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + userId % 10000
                + IdGenerator.nextId() % 1000;
    }
}
