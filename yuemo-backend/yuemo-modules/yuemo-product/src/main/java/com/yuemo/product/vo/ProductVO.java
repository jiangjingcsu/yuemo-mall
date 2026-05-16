package com.yuemo.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductVO(
    Long id,
    String name,
    Long categoryId,
    Long brandId,
    String brandName,
    String title,
    BigDecimal minPrice,
    Integer totalStock,
    String stockStatus,
    String mainImage,
    Integer sales,
    Integer status,
    List<TagVO> tags,
    LocalDateTime createTime
) {}
