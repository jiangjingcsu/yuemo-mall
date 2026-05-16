package com.yuemo.product.vo;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailVO(
    Long id,
    String name,
    Long categoryId,
    String categoryName,
    Long brandId,
    String brandName,
    String title,
    String description,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Integer totalStock,
    String mainImage,
    List<String> images,
    Integer sales,
    Integer status,
    List<TagVO> tags,
    List<SpecGroupVO> specGroups,
    List<SkuVO> skus,
    ReviewSummaryVO reviewSummary
) {}
