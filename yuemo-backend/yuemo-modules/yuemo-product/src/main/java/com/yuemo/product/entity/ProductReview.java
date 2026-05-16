package com.yuemo.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_product_review")
public class ProductReview extends BaseEntity {
    private Long productId;
    private Long skuId;
    private Long userId;
    private Long orderId;
    private Integer rating;
    private String content;
    private String images;
    private String reply;
    private Integer status;
}
