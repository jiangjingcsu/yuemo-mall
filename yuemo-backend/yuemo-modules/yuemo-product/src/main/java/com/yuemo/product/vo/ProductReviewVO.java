package com.yuemo.product.vo;

import com.yuemo.product.entity.ProductReview;

import java.time.LocalDateTime;

public record ProductReviewVO(
    Long id,
    Long productId,
    Long skuId,
    Long userId,
    Long orderId,
    Integer rating,
    String content,
    String images,
    String reply,
    Integer status,
    LocalDateTime createTime
) {

    public static ProductReviewVO from(ProductReview review) {
        return new ProductReviewVO(
                review.getId(),
                review.getProductId(),
                review.getSkuId(),
                review.getUserId(),
                review.getOrderId(),
                review.getRating(),
                review.getContent(),
                review.getImages(),
                review.getReply(),
                review.getStatus(),
                review.getCreateTime()
        );
    }
}
