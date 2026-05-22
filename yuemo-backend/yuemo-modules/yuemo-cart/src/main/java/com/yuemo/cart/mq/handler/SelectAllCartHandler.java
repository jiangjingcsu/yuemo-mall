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
public class SelectAllCartHandler implements CartActionHandler<CartSyncMessage.SelectAll> {

    private final CartItemMapper cartItemMapper;

    @Override
    public Class<CartSyncMessage.SelectAll> messageType() {
        return CartSyncMessage.SelectAll.class;
    }

    @Override
    public void handle(CartSyncMessage.SelectAll msg) {
        cartItemMapper.update(new LambdaUpdateWrapper<CartItem>()
                .eq(CartItem::getUserId, msg.userId())
                .set(CartItem::getSelected, msg.selected()));
        log.debug("购物车SELECT_ALL落库: userId={}, selected={}", msg.userId(), msg.selected());
    }
}
