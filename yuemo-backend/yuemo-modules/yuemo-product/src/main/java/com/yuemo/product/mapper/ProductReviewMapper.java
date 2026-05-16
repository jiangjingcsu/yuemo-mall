package com.yuemo.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuemo.product.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    @Select("SELECT IFNULL(AVG(rating), 0) AS avg_rating, COUNT(*) AS total_count FROM yu_product_review " +
            "WHERE product_id = #{productId} AND status = 1 AND deleted = 0")
    Map<String, Object> selectRatingSummary(@Param("productId") Long productId);

    @Select("SELECT rating, COUNT(*) AS cnt FROM yu_product_review " +
            "WHERE product_id = #{productId} AND status = 1 AND deleted = 0 GROUP BY rating")
    List<Map<String, Object>> selectRatingDistribution(@Param("productId") Long productId);
}
