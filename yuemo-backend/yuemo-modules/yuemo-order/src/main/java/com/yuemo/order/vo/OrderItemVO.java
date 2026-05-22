package com.yuemo.order.vo;

import com.yuemo.order.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderItemVO(
    Long id,
    Long orderId,
    Long productId,
    Long skuId,
    String productName,
    String productImage,
    String specText,
    BigDecimal price,
    Integer quantity,
    BigDecimal totalAmount,
    LocalDateTime createTime
) {

    public static OrderItemVO from(OrderItem item) {
        return new OrderItemVO(
                item.getId(),
                item.getOrderId(),
                item.getProductId(),
                item.getSkuId(),
                item.getProductName(),
                item.getProductImage(),
                item.getSpecText(),
                item.getPrice(),
                item.getQuantity(),
                item.getTotalAmount(),
                item.getCreateTime()
        );
    }
}
