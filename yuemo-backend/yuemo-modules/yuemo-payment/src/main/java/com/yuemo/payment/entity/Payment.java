package com.yuemo.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
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
    private Integer payType; // 1-微信 2-支付宝 3-平台余额
    private Integer status; // 0-待支付 1-支付成功 2-支付失败 3-已退款
    private String thirdTradeNo;
    private String refundNo;
    private String refundReason;
    private LocalDateTime payTime;
    private LocalDateTime refundTime;
}
