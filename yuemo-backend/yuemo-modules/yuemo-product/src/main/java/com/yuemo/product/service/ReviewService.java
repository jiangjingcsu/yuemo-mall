package com.yuemo.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuemo.product.entity.ProductReview;
import com.yuemo.product.vo.ReviewSummaryVO;

public interface ReviewService {
    IPage<ProductReview> getReviewsByProduct(Long productId, Integer page, Integer size);
    ReviewSummaryVO getReviewSummary(Long productId);
    void createReview(Long productId, Long userId, Long orderId, Long skuId, Integer rating, String content, String images);
}
