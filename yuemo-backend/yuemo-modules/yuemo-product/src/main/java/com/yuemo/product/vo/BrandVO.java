package com.yuemo.product.vo;

import com.yuemo.product.entity.Brand;

import java.time.LocalDateTime;

public record BrandVO(
    Long id,
    String name,
    String logo,
    String description,
    Integer sortOrder,
    Integer status,
    LocalDateTime createTime
) {

    public static BrandVO from(Brand brand) {
        return new BrandVO(
                brand.getId(),
                brand.getName(),
                brand.getLogo(),
                brand.getDescription(),
                brand.getSortOrder(),
                brand.getStatus(),
                brand.getCreateTime()
        );
    }
}
