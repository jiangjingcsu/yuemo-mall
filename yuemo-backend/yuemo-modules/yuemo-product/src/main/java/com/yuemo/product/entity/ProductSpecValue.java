package com.yuemo.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_product_spec_value")
public class ProductSpecValue extends BaseEntity {
    private Long templateId;
    private String value;
    private Integer sortOrder;
}
