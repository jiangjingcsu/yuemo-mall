package com.yuemo.payment.handler;

import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.payment.entity.Payment;
import com.yuemo.payment.enums.PaymentStatus;
import com.yuemo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class BalancePayHandler implements PayTypeHandler {

    private final UserService userService;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public int payType() {
        return 3;
    }

    @Override
    public Payment doPay(Long userId, Payment payment) {
        BigDecimal amount = payment.getAmount();
        BigDecimal balance = userService.getBalance(userId);

        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(ResultCode.PAYMENT_FAILED.getCode(),
                    "账户余额不足，当前余额 ¥" + balance);
        }

        userService.deductBalance(userId, amount);

        payment.setStatus(PaymentStatus.SUCCESS.getCode());
        payment.setPayTime(java.time.LocalDateTime.now());
        log.info("余额支付成功: userId={}, orderNo={}, amount={}", userId, payment.getOrderNo(), amount);

        rocketMQTemplate.convertAndSend("payment-callback", payment.getOrderNo());
        return payment;
    }
}
