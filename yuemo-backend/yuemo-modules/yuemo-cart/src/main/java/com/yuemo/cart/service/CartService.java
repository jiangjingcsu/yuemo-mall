package com.yuemo.cart.service;

import com.yuemo.cart.entity.CartItem;

import java.util.List;

public interface CartService {

    void addItem(Long userId, Long skuId, Integer quantity);

    List<CartItem> listItems(Long userId);

    void updateQuantity(Long userId, Long itemId, Integer quantity);

    void removeItem(Long userId, Long itemId);

    void toggleSelect(Long userId, Long itemId, Boolean selected);

    void clearSelected(Long userId);

    List<CartItem> getSelectedItems(Long userId);
}
