package com.yuemo.cart.mq.handler;

import com.yuemo.cart.dto.CartSyncMessage;
import com.yuemo.cart.mapper.CartItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddCartHandler implements CartActionHandler<CartSyncMessage.Add> {

    private final CartItemMapper cartItemMapper;

    @Override
    public Class<CartSyncMessage.Add> messageType() {
        return CartSyncMessage.Add.class;
    }

    @Override
    public void handle(CartSyncMessage.Add msg) {
        cartItemMapper.upsertOnDuplicateKey(
                msg.userId(), msg.productId(), msg.skuId(), msg.quantity());
        log.debug("购物车ADD落库: userId={}, skuId={}, quantity={}", msg.userId(), msg.skuId(), msg.quantity());
    }
}
