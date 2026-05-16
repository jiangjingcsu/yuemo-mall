package com.yuemo.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_product_spec_template")
public class ProductSpecTemplate extends BaseEntity {
    private Long categoryId;
    private String name;
    private Integer sortOrder;
}
