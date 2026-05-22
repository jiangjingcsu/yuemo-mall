package com.yuemo.product.vo;

import com.yuemo.product.entity.SearchKeyword;

import java.time.LocalDateTime;

public record SearchKeywordVO(
    Long id,
    String keyword,
    Integer searchCount,
    Integer isHot,
    Integer sortOrder,
    LocalDateTime createTime
) {

    public static SearchKeywordVO from(SearchKeyword keyword) {
        return new SearchKeywordVO(
                keyword.getId(),
                keyword.getKeyword(),
                keyword.getSearchCount(),
                keyword.getIsHot(),
                keyword.getSortOrder(),
                keyword.getCreateTime()
        );
    }
}
