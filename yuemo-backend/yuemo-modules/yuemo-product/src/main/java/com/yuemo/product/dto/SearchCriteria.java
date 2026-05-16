package com.yuemo.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record SearchCriteria(
    String keyword,
    Long categoryId,
    Long brandId,
    BigDecimal priceMin,
    BigDecimal priceMax,
    Integer page,
    Integer size,
    String sortBy,
    List<Long> tagIds
) {
    public SearchCriteria {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;
        if (sortBy == null || sortBy.isBlank()) sortBy = "sales_desc";
    }
}
