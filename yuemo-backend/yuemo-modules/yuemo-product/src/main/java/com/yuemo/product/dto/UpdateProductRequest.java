package com.yuemo.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductRequest(
    String name,
    Long categoryId,
    Long brandId,
    String title,
    String description,
    String mainImage,
    List<String> images,
    Integer status,
    List<Long> tagIds
) {}
