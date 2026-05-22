package com.yuemo.cart.mq.handler;

import com.yuemo.cart.dto.CartSyncMessage;

public interface CartActionHandler<T extends CartSyncMessage> {

    Class<T> messageType();

    void handle(T message);
}
