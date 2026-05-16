package com.yuemo.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuemo.product.entity.SearchKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SearchKeywordMapper extends BaseMapper<SearchKeyword> {

    @Select("SELECT id, keyword, search_count FROM yu_search_keyword " +
            "WHERE is_hot = 1 AND deleted = 0 ORDER BY sort_order, search_count DESC LIMIT #{limit}")
    List<SearchKeyword> selectHotKeywords(@Param("limit") int limit);

    @Update("INSERT INTO yu_search_keyword (keyword, search_count) VALUES (#{keyword}, 1) " +
            "ON DUPLICATE KEY UPDATE search_count = search_count + 1")
    void incrementSearchCount(@Param("keyword") String keyword);

    @Select("SELECT id, keyword, search_count FROM yu_search_keyword " +
            "WHERE keyword LIKE CONCAT(#{prefix}, '%') AND deleted = 0 " +
            "ORDER BY search_count DESC LIMIT #{limit}")
    List<SearchKeyword> selectSuggestions(@Param("prefix") String prefix, @Param("limit") int limit);
}
