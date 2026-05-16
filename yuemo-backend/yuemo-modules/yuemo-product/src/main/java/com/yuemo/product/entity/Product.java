package com.yuemo.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_product")
public class Product extends BaseEntity {

    private String name;
    private Long categoryId;
    private Long brandId;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String mainImage;
    private String images;
    private Integer status; // 0-下架 1-上架
    private Integer sales;
}
