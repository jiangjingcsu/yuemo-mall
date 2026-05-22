package com.yuemo.product.vo;

import com.yuemo.product.entity.Category;

import java.time.LocalDateTime;
import java.util.List;

public record CategoryVO(
    Long id,
    String name,
    Long parentId,
    Integer level,
    Integer sortOrder,
    List<CategoryVO> children,
    LocalDateTime createTime
) {

    public static CategoryVO from(Category category) {
        return new CategoryVO(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.getLevel(),
                category.getSortOrder(),
                category.getChildren() != null ? category.getChildren().stream().map(CategoryVO::from).toList() : null,
                category.getCreateTime()
        );
    }
}
