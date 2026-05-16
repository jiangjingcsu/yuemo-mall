package com.yuemo.product.vo;

import java.util.Map;

public record ReviewSummaryVO(
    Double averageRating,
    Integer totalCount,
    Map<Integer, Integer> ratingDistribution
) {}
