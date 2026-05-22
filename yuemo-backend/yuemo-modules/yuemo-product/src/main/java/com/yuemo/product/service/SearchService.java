package com.yuemo.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuemo.product.dto.SearchCriteria;
import com.yuemo.product.vo.ProductVO;
import com.yuemo.product.vo.SearchKeywordVO;

import java.util.List;

public interface SearchService {
    IPage<ProductVO> search(SearchCriteria criteria);
    void index(ProductVO product);
    void removeIndex(Long productId);
    List<SearchKeywordVO> getHotKeywords(int limit);
    List<SearchKeywordVO> getSuggestions(String prefix, int limit);
}
