package com.yuemo.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_category")
public class Category extends BaseEntity {

    private String name;
    private Long parentId;
    private Integer level;
    private Integer sortOrder;

    @TableField(exist = false)
    private List<Category> children;
}
