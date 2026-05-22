package com.yuemo.cart.constant;

public final class CartConstants {

    private CartConstants() {
    }

    public static final String CART_KEY_PREFIX = "cart:";
    
    public static final String CART_SYNC_TOPIC = "cart-sync";

    public static final String CART_CONSUMER_GROUP = "yuemo-cart-consumer";

    public static final int MAX_CART_ITEMS = 100;

    public static final int CART_EXPIRE_DAYS = 7;

    public static final long CART_CACHE_TTL_SECONDS = 7 * 24 * 60 * 60L;
}
