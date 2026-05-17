package com.yuemo.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.cart.entity.CartItem;
import com.yuemo.cart.mapper.CartItemMapper;
import com.yuemo.cart.service.CartService;
import com.yuemo.product.entity.Product;
import com.yuemo.product.entity.ProductSku;
import com.yuemo.product.mapper.ProductMapper;
import com.yuemo.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SkuService skuService;
    private final ProductMapper productMapper;

    private static final int MAX_CART_ITEMS = 100;

    @Override
    public void addItem(Long userId, Long skuId, Integer quantity) {
        ProductSku sku = skuService.getSkuById(skuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BusinessException(ResultCode.SKU_NOT_FOUND);
        }
        if (sku.getPrice() == null || sku.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        if (sku.getStock() == null || sku.getStock() <= 0) {
            throw new BusinessException(ResultCode.SKU_STOCK_INSUFFICIENT);
        }

        Product product = productMapper.selectProductById(sku.getProductId());
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        long count = cartItemMapper.selectCount(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
        if (count >= MAX_CART_ITEMS) {
            throw new BusinessException(ResultCode.CART_ITEM_LIMIT_EXCEEDED);
        }

        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, skuId));
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            cartItemMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(sku.getProductId());
            item.setSkuId(skuId);
            item.setSpecText(sku.getSpecText());
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
            item.setPrice(sku.getPrice());
            item.setQuantity(quantity);
            item.setSelected(true);
            cartItemMapper.insert(item);
        }

        redisTemplate.delete("cart:user:" + userId);
    }

    @Override
    public List<CartItem> listItems(Long userId) {
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
