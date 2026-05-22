package com.yuemo.cart.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuemo.cart.dto.CartItemRedis;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CartSerializer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CartSerializer() {
    }

    public static String serialize(CartItemRedis item) {
        try {
            return OBJECT_MAPPER.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            log.error("序列化购物车项失败, skuId={}", item.skuId(), e);
            throw new CartSerializationException("序列化购物车项失败", e);
        }
    }

    public static CartItemRedis deserialize(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, CartItemRedis.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化购物车项失败: {}", json, e);
            throw new CartSerializationException("反序列化购物车项失败", e);
        }
    }

    public static class CartSerializationException extends RuntimeException {
        public CartSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
