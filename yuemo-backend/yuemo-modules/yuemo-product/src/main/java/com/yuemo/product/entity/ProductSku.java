package com.yuemo.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_product_sku")
public class ProductSku extends BaseEntity {
    private Long productId;
    private String skuCode;
    private String specIds;
    private String specText;
    private BigDecimal price;
    private Integer stock;
    private String image;
    private Integer status;
}
