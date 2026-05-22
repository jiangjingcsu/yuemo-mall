package com.yuemo.cart.mq.handler;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yuemo.cart.dto.CartSyncMessage;
import com.yuemo.cart.entity.CartItem;
import com.yuemo.cart.mapper.CartItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateCartHandler implements CartActionHandler<CartSyncMessage.Update> {

    private final CartItemMapper cartItemMapper;

    @Override
    public Class<CartSyncMessage.Update> messageType() {
        return CartSyncMessage.Update.class;
    }

    @Override
    public void handle(CartSyncMessage.Update msg) {
        LambdaUpdateWrapper<CartItem> wrapper = new LambdaUpdateWrapper<CartItem>()
                .eq(CartItem::getUserId, msg.userId())
                .eq(CartItem::getSkuId, msg.skuId());
        if (msg.quantity() != null) {
            wrapper.set(CartItem::getQuantity, msg.quantity());
        }
        if (msg.selected() != null) {
            wrapper.set(CartItem::getSelected, msg.selected());
        }
        cartItemMapper.update(null, wrapper);
        log.debug("购物车UPDATE落库: userId={}, skuId={}", msg.userId(), msg.skuId());
    }
}
