package com.yuemo.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.cart.entity.CartItem;
import com.yuemo.cart.mapper.CartItemMapper;
import com.yuemo.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_CART_ITEMS = 100;

    @Override
    public void addItem(Long userId, Long productId, Integer quantity) {
        long count = cartItemMapper.selectCount(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
        if (count >= MAX_CART_ITEMS) {
            throw new BusinessException(ResultCode.CART_ITEM_LIMIT_EXCEEDED);
        }

        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId));
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartItemMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setQuantity(quantity);
            item.setSelected(true);
            // TODO: 查询商品名称/图片/价格
            item.setProductName("商品" + productId);
            item.setProductImage("");
            item.setPrice(java.math.BigDecimal.ZERO);
            cartItemMapper.insert(item);
        }

        // 刷新 Redis 缓存
        redisTemplate.delete("cart:user:" + userId);
    }

    @Override
    public List<CartItem> listItems(Long userId) {
        // 优先查缓存
        String cacheKey = "cart:user:" + userId;
        @SuppressWarnings("unchecked")
        List<CartItem> cached = (List<CartItem>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getCreateTime));
        redisTemplate.opsForValue().set(cacheKey, items, 30, TimeUnit.MINUTES);
        return items;
    }

    @Override
    public void updateQuantity(Long userId, Long itemId, Integer quantity) {
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
        redisTemplate.delete("cart:user:" + userId);
    }

    @Override
    public void removeItem(Long userId, Long itemId) {
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        cartItemMapper.deleteById(itemId);
        redisTemplate.delete("cart:user:" + userId);
    }

    @Override
    public void toggleSelect(Long userId, Long itemId, Boolean selected) {
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        item.setSelected(selected);
        cartItemMapper.updateById(item);
        redisTemplate.delete("cart:user:" + userId);
    }

    @Override
    public void clearSelected(Long userId) {
        List<CartItem> selected = getSelectedItems(userId);
        for (CartItem item : selected) {
            cartItemMapper.deleteById(item.getId());
        }
        redisTemplate.delete("cart:user:" + userId);
    }

    @Override
    public List<CartItem> getSelectedItems(Long userId) {
        return cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSelected, true));
    }
}
