package com.yuemo.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PayRequest {
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "支付方式不能为空")
    private Integer payType;
}
