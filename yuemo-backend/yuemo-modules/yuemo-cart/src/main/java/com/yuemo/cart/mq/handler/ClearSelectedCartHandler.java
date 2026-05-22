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
public class ClearSelectedCartHandler implements CartActionHandler<CartSyncMessage.ClearSelected> {

    private final CartItemMapper cartItemMapper;

    @Override
    public Class<CartSyncMessage.ClearSelected> messageType() {
        return CartSyncMessage.ClearSelected.class;
    }

    @Override
    public void handle(CartSyncMessage.ClearSelected msg) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, msg.userId())
                .eq(CartItem::getSelected, true));
        log.debug("购物车CLEAR_SELECTED落库: userId={}", msg.userId());
    }
}
