package com.yuemo.cart.dto;

import java.math.BigDecimal;

public record CartItemVO(
        Long skuId,
        Long productId,
        Integer quantity,
        Boolean selected,
        String productName,
        String productImage,
        String specText,
        BigDecimal price,
        BigDecimal subtotal
) {}
