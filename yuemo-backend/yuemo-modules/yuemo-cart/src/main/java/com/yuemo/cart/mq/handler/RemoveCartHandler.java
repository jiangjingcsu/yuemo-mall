package com.yuemo.cart.mq.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.cart.dto.CartSyncMessage;
import com.yuemo.cart.entity.CartItem;
import com.yuemo.cart.mapper.CartItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveCartHandler implements CartActionHandler<CartSyncMessage.Remove> {

    private final CartItemMapper cartItemMapper;

    @Override
    public Class<CartSyncMessage.Remove> messageType() {
        return CartSyncMessage.Remove.class;
    }

    @Override
    public void handle(CartSyncMessage.Remove msg) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, msg.userId())
                .eq(CartItem::getSkuId, msg.skuId()));
        log.debug("购物车REMOVE落库: userId={}, skuId={}", msg.userId(), msg.skuId());
    }
}
