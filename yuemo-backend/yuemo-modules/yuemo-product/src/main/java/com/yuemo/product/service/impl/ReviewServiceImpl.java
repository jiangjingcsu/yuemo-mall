package com.yuemo.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuemo.product.entity.ProductReview;
import com.yuemo.product.mapper.ProductReviewMapper;
import com.yuemo.product.service.ReviewService;
import com.yuemo.product.vo.ReviewSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewMapper reviewMapper;

    @Override
    public IPage<ProductReview> getReviewsByProduct(Long productId, Integer page, Integer size) {
        Page<ProductReview> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getProductId, productId)
                .eq(ProductReview::getStatus, 1)
                .eq(ProductReview::getDeleted, false)
                .orderByDesc(ProductReview::getCreateTime);
        return reviewMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Cacheable(value = "reviewSummary", key = "#productId")
    public ReviewSummaryVO getReviewSummary(Long productId) {
        Map<String, Object> summaryMap = reviewMapper.selectRatingSummary(productId);
        if (summaryMap == null || summaryMap.isEmpty()) {
            return new ReviewSummaryVO(0.0, 0, Map.of());
        }

        Number totalCountNum = (Number) summaryMap.get("total_count");
        long totalCount = totalCountNum != null ? totalCountNum.longValue() : 0;
        if (totalCount == 0) {
            return new ReviewSummaryVO(0.0, 0, Map.of());
        }

        Number avgRatingNum = (Number) summaryMap.get("avg_rating");
        double avgRating = avgRatingNum != null ? avgRatingNum.doubleValue() : 0.0;

        List<Map<String, Object>> distribution = reviewMapper.selectRatingDistribution(productId);
        Map<Integer, Integer> distMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) distMap.put(i, 0);
        for (Map<String, Object> row : distribution) {
            Integer rating = ((Number) row.get("rating")).intValue();
            Integer cnt = ((Number) row.get("cnt")).intValue();
            distMap.put(rating, cnt);
        }

        return new ReviewSummaryVO(avgRating, (int) totalCount, distMap);
    }

    @Override
    @CacheEvict(value = "reviewSummary", key = "#productId")
    public void createReview(Long productId, Long userId, Long orderId, Long skuId,
                             Integer rating, String content, String images) {
        ProductReview review = new ProductReview();
        review.setProductId(productId);
        review.setUserId(userId);
        review.setOrderId(orderId);
        review.setSkuId(skuId);
        review.setRating(rating);
        review.setContent(content);
        review.setImages(images);
        review.setStatus(1);
        reviewMapper.insert(review);
    }
}
