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
public class ClearAllCartHandler implements CartActionHandler<CartSyncMessage.ClearAll> {

    private final CartItemMapper cartItemMapper;

    @Override
    public Class<CartSyncMessage.ClearAll> messageType() {
        return CartSyncMessage.ClearAll.class;
    }

    @Override
    public void handle(CartSyncMessage.ClearAll msg) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, msg.userId()));
        log.debug("购物车CLEAR_ALL落库: userId={}", msg.userId());
    }
}
