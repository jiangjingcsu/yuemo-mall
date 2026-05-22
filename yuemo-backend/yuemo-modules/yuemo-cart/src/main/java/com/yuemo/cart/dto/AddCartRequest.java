package com.yuemo.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartRequest(
        @NotNull @Positive Long skuId,
        @NotNull @Positive Integer quantity
) {
    public AddCartRequest {
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }
    }
}
