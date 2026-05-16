package com.yuemo.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_search_keyword")
public class SearchKeyword extends BaseEntity {
    private String keyword;
    private Integer searchCount;
    private Integer isHot;
    private Integer sortOrder;
}
