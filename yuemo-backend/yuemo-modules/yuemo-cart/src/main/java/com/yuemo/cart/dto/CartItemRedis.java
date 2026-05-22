package com.yuemo.cart.dto;

public record CartItemRedis(
        Long skuId,
        Long productId,
        Integer quantity,
        Boolean selected
) {
    public static CartItemRedis of(Long skuId, Long productId, Integer quantity, Boolean selected) {
        return new CartItemRedis(skuId, productId, quantity, selected);
    }

    public CartItemRedis withQuantity(Integer newQuantity) {
        return new CartItemRedis(skuId, productId, newQuantity, selected);
    }

    public CartItemRedis withSelected(Boolean newSelected) {
        return new CartItemRedis(skuId, productId, quantity, newSelected);
    }
}
