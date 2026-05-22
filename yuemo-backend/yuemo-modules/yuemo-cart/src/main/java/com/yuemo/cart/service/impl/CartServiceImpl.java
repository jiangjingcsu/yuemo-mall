package com.yuemo.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.cart.constant.CartConstants;
import com.yuemo.cart.dto.CartItemRedis;
import com.yuemo.cart.dto.CartItemVO;
import com.yuemo.cart.dto.CartSyncMessage;
import com.yuemo.cart.entity.CartItem;
import com.yuemo.cart.mapper.CartItemMapper;
import com.yuemo.cart.service.CartService;
import com.yuemo.cart.util.CartSerializer;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.product.entity.Product;
import com.yuemo.product.entity.ProductSku;
import com.yuemo.product.service.ProductService;
import com.yuemo.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SkuService skuService;
    private final ProductService productService;
    private final RocketMQTemplate rocketMQTemplate;

    private String cartKey(Long userId) {
        return CartConstants.CART_KEY_PREFIX + userId;
    }

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

        Product product = productService.getProductById(sku.getProductId());
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        String key = cartKey(userId);
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();

        Object existing = hashOps.get(key, String.valueOf(skuId));
        int finalQuantity;
        if (existing != null) {
            CartItemRedis item = CartSerializer.deserialize(existing.toString());
            finalQuantity = item.quantity() + quantity;
            if (finalQuantity > sku.getStock()) {
                throw new BusinessException(ResultCode.SKU_STOCK_INSUFFICIENT);
            }
            CartItemRedis updated = item.withQuantity(finalQuantity);
            hashOps.put(key, String.valueOf(skuId), CartSerializer.serialize(updated));
        } else {
            if (quantity > sku.getStock()) {
                throw new BusinessException(ResultCode.SKU_STOCK_INSUFFICIENT);
            }
            long size = hashOps.size(key);
            if (size >= CartConstants.MAX_CART_ITEMS) {
                throw new BusinessException(ResultCode.CART_ITEM_LIMIT_EXCEEDED);
            }
            finalQuantity = quantity;
            CartItemRedis item = CartItemRedis.of(skuId, sku.getProductId(), quantity, true);
            hashOps.put(key, String.valueOf(skuId), CartSerializer.serialize(item));
        }

        redisTemplate.expire(key, CartConstants.CART_CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        rocketMQTemplate.convertAndSend(CartConstants.CART_SYNC_TOPIC,
                new CartSyncMessage.Add(userId, skuId, sku.getProductId(), finalQuantity, true));
        log.debug("购物车加购: userId={}, skuId={}, quantity={}", userId, skuId, finalQuantity);
    }

    @Override
    public List<CartItemVO> listItems(Long userId) {
        Map<String, CartItemRedis> redisItems = getRedisCart(userId);
        if (redisItems.isEmpty()) {
            return loadFromDbToRedis(userId);
        }
        return buildCartItemVOs(redisItems.values());
    }

    @Override
    public void updateQuantity(Long userId, Long skuId, Integer quantity) {
        ProductSku sku = skuService.getSkuById(skuId);
        if (sku != null && quantity > sku.getStock()) {
            throw new BusinessException(ResultCode.SKU_STOCK_INSUFFICIENT);
        }

        String key = cartKey(userId);
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        String field = String.valueOf(skuId);

        Object existing = hashOps.get(key, field);
        if (existing == null) {
            throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        CartItemRedis item = CartSerializer.deserialize(existing.toString());
        CartItemRedis updated = item.withQuantity(quantity);
        hashOps.put(key, field, CartSerializer.serialize(updated));

        redisTemplate.expire(key, CartConstants.CART_CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        rocketMQTemplate.convertAndSend(CartConstants.CART_SYNC_TOPIC,
                new CartSyncMessage.Update(userId, skuId, quantity, null));
    }

    @Override
    public void removeItem(Long userId, Long skuId) {
        String key = cartKey(userId);
        redisTemplate.opsForHash().delete(key, String.valueOf(skuId));

        rocketMQTemplate.convertAndSend(CartConstants.CART_SYNC_TOPIC,
                new CartSyncMessage.Remove(userId, skuId));
    }

    @Override
    public void toggleSelect(Long userId, Long skuId, Boolean selected) {
        String key = cartKey(userId);
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        String field = String.valueOf(skuId);

        Object existing = hashOps.get(key, field);
        if (existing == null) {
            throw new BusinessException(ResultCode.CART_ITEM_NOT_FOUND);
        }
        CartItemRedis item = CartSerializer.deserialize(existing.toString());
        CartItemRedis updated = item.withSelected(selected);
        hashOps.put(key, field, CartSerializer.serialize(updated));

        redisTemplate.expire(key, CartConstants.CART_CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        rocketMQTemplate.convertAndSend(CartConstants.CART_SYNC_TOPIC,
                new CartSyncMessage.Update(userId, skuId, null, selected));
    }

    @Override
    public void clearSelected(Long userId) {
        String key = cartKey(userId);
        Map<String, CartItemRedis> redisItems = getRedisCart(userId);

        List<String> selectedFields = redisItems.entrySet().stream()
                .filter(e -> e.getValue().selected() != null && e.getValue().selected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!selectedFields.isEmpty()) {
            redisTemplate.opsForHash().delete(key, selectedFields.toArray());
        }

        rocketMQTemplate.convertAndSend(CartConstants.CART_SYNC_TOPIC,
                new CartSyncMessage.ClearSelected(userId));
    }

    @Override
    public List<CartItemVO> getSelectedItems(Long userId) {
        Map<String, CartItemRedis> redisItems = getRedisCart(userId);
        List<CartItemRedis> selected = redisItems.values().stream()
                .filter(item -> item.selected() != null && item.selected())
                .collect(Collectors.toList());
        return buildCartItemVOs(selected);
    }

    @Override
    public void selectAll(Long userId, Boolean selected) {
        String key = cartKey(userId);
        Map<String, CartItemRedis> redisItems = getRedisCart(userId);

        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        for (Map.Entry<String, CartItemRedis> entry : redisItems.entrySet()) {
            CartItemRedis updated = entry.getValue().withSelected(selected);
            hashOps.put(key, entry.getKey(), CartSerializer.serialize(updated));
        }

        redisTemplate.expire(key, CartConstants.CART_CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        rocketMQTemplate.convertAndSend(CartConstants.CART_SYNC_TOPIC,
                new CartSyncMessage.SelectAll(userId, selected));
    }

    private Map<String, CartItemRedis> getRedisCart(Long userId) {
        String key = cartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, CartItemRedis> result = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String field = entry.getKey().toString();
            CartItemRedis item = CartSerializer.deserialize(entry.getValue().toString());
            result.put(field, item);
        }
        return result;
    }

    private List<CartItemVO> loadFromDbToRedis(Long userId) {
        List<CartItem> dbItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getCreateTime));

        if (dbItems.isEmpty()) {
            return Collections.emptyList();
        }

        String key = cartKey(userId);
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        Map<String, String> batch = new HashMap<>();
        for (CartItem dbItem : dbItems) {
            Long skuId = dbItem.getSkuId() != null ? dbItem.getSkuId() : dbItem.getProductId();
            CartItemRedis redisItem = CartItemRedis.of(
                    skuId, dbItem.getProductId(),
                    dbItem.getQuantity(), dbItem.getSelected());
            batch.put(String.valueOf(skuId), CartSerializer.serialize(redisItem));
        }
        hashOps.putAll(key, batch);

        redisTemplate.expire(key, CartConstants.CART_CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        return buildCartItemVOs(batch.values().stream()
                .map(CartSerializer::deserialize)
                .collect(Collectors.toList()));
    }

    private List<CartItemVO> buildCartItemVOs(Collection<CartItemRedis> items) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        List<CartItemRedis> itemList = new ArrayList<>(items);

        Set<Long> productIds = itemList.stream()
                .map(CartItemRedis::productId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Long> skuIds = itemList.stream()
                .map(CartItemRedis::skuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Product> products = productService.batchGetProductsByIds(new ArrayList<>(productIds));
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        List<ProductSku> skus = skuService.batchGetSkusByIds(skuIds);
        Map<Long, ProductSku> skuMap = skus.stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s, (a, b) -> a));

        List<CartItemVO> result = new ArrayList<>();
        for (CartItemRedis item : itemList) {
            Product product = productMap.get(item.productId());
            ProductSku sku = item.skuId() != null ? skuMap.get(item.skuId()) : null;

            String productName = product != null ? product.getName() : null;
            String productImage = product != null ? product.getMainImage() : null;
            BigDecimal price = null;
            String specText = null;

            if (sku != null) {
                price = sku.getPrice();
                specText = sku.getSpecText();
            } else if (product != null) {
                price = product.getPrice();
            }

            BigDecimal subtotal = null;
            if (price != null && item.quantity() != null) {
                subtotal = price.multiply(BigDecimal.valueOf(item.quantity()));
            }

            result.add(new CartItemVO(
                    item.skuId(), item.productId(), item.quantity(), item.selected(),
                    productName, productImage, specText, price, subtotal));
        }
        return result;
    }
}
