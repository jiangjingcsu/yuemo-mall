package com.yuemo.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateReviewRequest(
    Long skuId,
    @Min(1) @Max(5) Integer rating,
    @NotBlank String content,
    String images
) {}
