package com.yuemo.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
    @NotBlank String name,
    @NotNull Long categoryId,
    Long brandId,
    String title,
    String description,
    String mainImage,
    List<String> images,
    Integer status,
    List<Long> tagIds,
    @NotNull List<SkuRequest> skus
) {
    public record SkuRequest(
        String skuCode,
        List<Long> specIds,
        String specText,
        @NotNull @Positive BigDecimal price,
        @NotNull Integer stock,
        String image
    ) {}
}
