package com.yuemo.cart.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CartSyncMessage.Add.class, name = "ADD"),
    @JsonSubTypes.Type(value = CartSyncMessage.Update.class, name = "UPDATE"),
    @JsonSubTypes.Type(value = CartSyncMessage.Remove.class, name = "REMOVE"),
    @JsonSubTypes.Type(value = CartSyncMessage.SelectAll.class, name = "SELECT_ALL"),
    @JsonSubTypes.Type(value = CartSyncMessage.ClearSelected.class, name = "CLEAR_SELECTED"),
    @JsonSubTypes.Type(value = CartSyncMessage.ClearAll.class, name = "CLEAR_ALL")
})
public sealed interface CartSyncMessage
        permits CartSyncMessage.Add, CartSyncMessage.Update, CartSyncMessage.Remove,
        CartSyncMessage.SelectAll, CartSyncMessage.ClearSelected, CartSyncMessage.ClearAll {

    Long userId();

    record Add(Long userId, Long skuId, Long productId, Integer quantity, Boolean selected)
            implements CartSyncMessage {
    }

    record Update(Long userId, Long skuId, Integer quantity, Boolean selected)
            implements CartSyncMessage {
    }

    record Remove(Long userId, Long skuId) implements CartSyncMessage {
    }

    record SelectAll(Long userId, Boolean selected) implements CartSyncMessage {
    }

    record ClearSelected(Long userId) implements CartSyncMessage {
    }

    record ClearAll(Long userId) implements CartSyncMessage {
    }
}
