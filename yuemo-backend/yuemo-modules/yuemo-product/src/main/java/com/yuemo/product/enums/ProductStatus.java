package com.yuemo.product.enums;

import lombok.Getter;

@Getter
public enum ProductStatus {

    OFF_SHELF(0, "下架"),
    ON_SHELF(1, "上架");

    private final int code;
    private final String desc;

    ProductStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProductStatus fromCode(int code) {
        for (ProductStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知商品状态: " + code);
    }
}
