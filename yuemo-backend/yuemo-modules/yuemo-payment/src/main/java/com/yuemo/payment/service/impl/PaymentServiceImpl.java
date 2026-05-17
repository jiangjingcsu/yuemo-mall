package com.yuemo.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.common.core.utils.IdGenerator;
import com.yuemo.order.entity.Order;
import com.yuemo.order.service.OrderService;
import com.yuemo.payment.entity.Payment;
import com.yuemo.payment.mapper.PaymentMapper;
import com.yuemo.payment.service.PaymentService;
import com.yuemo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderService orderService;
    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment createPayment(Long userId, Long orderId, Integer payType) {
        Order order = orderService.getOrderById(orderId);

        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }

        // 余额支付：同步校验余额并扣款
        if (payType != null && payType == 3) {
            return payByBalance(userId, order);
        }

        // 微信/支付宝：创建待支付记录，等待回调
        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo(userId));
        payment.setOrderId(orderId);
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(userId);
        payment.setAmount(order.getPayAmount());
        payment.setPayType(payType);
        payment.setStatus(0);
        paymentMapper.insert(payment);
        return payment;
    }

    private Payment payByBalance(Long userId, Order order) {
        BigDecimal amount = order.getPayAmount();
        BigDecimal balance = userService.getBalance(userId);

        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(ResultCode.PAYMENT_FAILED.getCode(), "账户余额不足，当前余额 ¥" + balance);
        }

        userService.deductBalance(userId, amount);

        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo(userId));
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setPayType(3);
        payment.setStatus(1);
        payment.setPayTime(LocalDateTime.now());
        paymentMapper.insert(payment);

        rocketMQTemplate.convertAndSend("payment-callback", payment.getOrderNo());

        log.info("余额支付成功: userId={}, orderNo={}, amount={}", userId, order.getOrderNo(), amount);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(Integer payType, Map<String, String> params) {
        if (!verifySign(payType, params)) {
            log.error("支付回调验签失败: payType={}, params={}", payType, params);
            return "fail";
        }

        String paymentNo = params.get("out_trade_no");
        String thirdTradeNo = params.get("trade_no");

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
            return "success";
        }

        payment.setStatus(1);
        payment.setThirdTradeNo(thirdTradeNo);
        payment.setPayTime(LocalDateTime.now());
        paymentMapper.updateById(payment);

        rocketMQTemplate.convertAndSend("payment-callback", payment.getOrderNo());

        return "success";
    }

    /**
     * 验签 — 实际应调用微信/支付宝 SDK 验证回调参数签名
     */
    private boolean verifySign(Integer payType, Map<String, String> params) {
        String sign = params.remove("sign");
        if (sign == null || sign.isBlank()) {
            return false;
        }
        // TODO: 接入真实的微信/支付宝 SDK 签名验证
        // 微信: WXPayUtil.verifySign(params, mchKey, sign)
        // 支付宝: AlipaySignature.rsaCheckV1(params, alipayPublicKey, charset, signType)
        return true;
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long userId, Long orderId, String reason) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
        if (payment == null || payment.getStatus() != 1) {
            throw new BusinessException(ResultCode.REFUND_FAILED);
        }

        // TODO: 调用第三方退款接口
        payment.setStatus(3);
        payment.setRefundNo(generateRefundNo(userId));
        payment.setRefundReason(reason);
        payment.setRefundTime(LocalDateTime.now());
        paymentMapper.updateById(payment);

        rocketMQTemplate.convertAndSend("payment-refund", orderId);
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
