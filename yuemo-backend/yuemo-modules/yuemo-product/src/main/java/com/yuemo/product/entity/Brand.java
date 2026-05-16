package com.yuemo.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_brand")
public class Brand extends BaseEntity {
    private String name;
    private String logo;
    private String description;
    private Integer sortOrder;
    private Integer status;
}
