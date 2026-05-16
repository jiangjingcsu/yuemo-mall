package com.yuemo.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_product_tag")
public class ProductTag extends BaseEntity {
    private String name;
    private String color;
    private Integer sortOrder;
}
