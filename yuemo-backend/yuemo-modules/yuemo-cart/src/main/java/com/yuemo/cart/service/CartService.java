package com.yuemo.cart.service;

import com.yuemo.cart.dto.CartItemVO;

import java.util.List;

public interface CartService {

    void addItem(Long userId, Long skuId, Integer quantity);

    List<CartItemVO> listItems(Long userId);

    void updateQuantity(Long userId, Long skuId, Integer quantity);

    void removeItem(Long userId, Long skuId);

    void toggleSelect(Long userId, Long skuId, Boolean selected);

    void clearSelected(Long userId);

    List<CartItemVO> getSelectedItems(Long userId);

    void selectAll(Long userId, Boolean selected);
}
