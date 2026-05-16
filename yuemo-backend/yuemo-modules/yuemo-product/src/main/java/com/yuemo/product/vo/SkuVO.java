package com.yuemo.product.vo;

import java.math.BigDecimal;
import java.util.List;

public record SkuVO(
    Long id,
    String skuCode,
    List<Long> specIds,
    String specText,
    BigDecimal price,
    Integer stock,
    String image
) {}
