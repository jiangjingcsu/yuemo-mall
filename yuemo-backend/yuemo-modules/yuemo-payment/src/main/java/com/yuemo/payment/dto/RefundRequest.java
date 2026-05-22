package com.yuemo.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundRequest {
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
    private String reason;
}
