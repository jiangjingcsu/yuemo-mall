package com.yuemo.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.payment.enums.PaymentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_payment")
public class Payment extends BaseEntity {

    private String paymentNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private Integer payType;
    private Integer status;
    private String thirdTradeNo;
    private String refundNo;
    private String refundReason;
    private LocalDateTime payTime;
    private LocalDateTime refundTime;

    public void verifyOwnership(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    public boolean isPending() {
        return this.status != null && this.status == PaymentStatus.PENDING.getCode();
    }

    public boolean isSuccess() {
        return this.status != null && this.status == PaymentStatus.SUCCESS.getCode();
    }

    public void markSuccess(String thirdTradeNo) {
        if (!isPending()) {
            throw new BusinessException(ResultCode.PAYMENT_CALLBACK_ERROR);
        }
        this.status = PaymentStatus.SUCCESS.getCode();
        this.thirdTradeNo = thirdTradeNo;
        this.payTime = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED.getCode();
    }

    public void markRefunded(String refundNo, String reason) {
        if (!isSuccess()) {
            throw new BusinessException(ResultCode.REFUND_FAILED);
        }
        this.status = PaymentStatus.REFUNDED.getCode();
        this.refundNo = refundNo;
        this.refundReason = reason;
        this.refundTime = LocalDateTime.now();
    }
}
