package com.yuemo.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderDTO(
    @NotNull(message = "收货地址不能为空") Long addressId,
    @NotEmpty(message = "订单商品不能为空") List<OrderItemDTO> items,
    String remark,
    Long couponId
) {
    public record OrderItemDTO(
        @NotNull Long productId,
        Long skuId,
        @NotNull Integer quantity
    ) {}
}
