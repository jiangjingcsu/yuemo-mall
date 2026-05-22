package com.yuemo.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuemo.product.dto.SearchCriteria;
import com.yuemo.product.entity.Brand;
import com.yuemo.product.entity.Product;
import com.yuemo.product.mapper.BrandMapper;
import com.yuemo.product.mapper.ProductMapper;
import com.yuemo.product.mapper.ProductTagMapper;
import com.yuemo.product.mapper.SearchKeywordMapper;
import com.yuemo.product.service.SearchService;
import com.yuemo.product.vo.ProductVO;
import com.yuemo.product.vo.SearchKeywordVO;
import com.yuemo.product.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MySqlSearchServiceImpl implements SearchService {

    private final ProductMapper productMapper;
    private final BrandMapper brandMapper;
    private final ProductTagMapper productTagMapper;
    private final SearchKeywordMapper searchKeywordMapper;

    @Override
    public IPage<ProductVO> search(SearchCriteria criteria) {
        boolean hasKeyword = StringUtils.hasText(criteria.keyword());

        if (hasKeyword) {
            try {
                searchKeywordMapper.incrementSearchCount(criteria.keyword().trim());
            } catch (Exception e) {
                log.debug("搜索词记录失败: {}", criteria.keyword());
            }
        }

        Page<Product> page = new Page<>(criteria.page(), criteria.size());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        wrapper.eq(Product::getDeleted, false);

        if (hasKeyword) {
            String kw = criteria.keyword().trim();
            wrapper.and(w -> w
                    .apply("MATCH(name, title, description) AGAINST({0} IN BOOLEAN MODE)", kw)
                    .or()
                    .like(Product::getName, kw));
        }

        if (criteria.categoryId() != null) {
            wrapper.eq(Product::getCategoryId, criteria.categoryId());
        }
        if (criteria.brandId() != null) {
            wrapper.eq(Product::getBrandId, criteria.brandId());
        }
        if (criteria.priceMin() != null) {
            wrapper.ge(Product::getPrice, criteria.priceMin());
        }
        if (criteria.priceMax() != null) {
            wrapper.le(Product::getPrice, criteria.priceMax());
        }

        // Sort — use `last()` to append custom ORDER BY clause
        String sortBy = criteria.sortBy();
        wrapper.last(buildOrderBy(sortBy, hasKeyword ? criteria.keyword().trim() : null));

        IPage<Product> productPage = productMapper.selectPage(page, wrapper);

        // Batch-enrich: brand names and tags
        List<Long> productIds = productPage.getRecords().stream()
                .map(Product::getId).toList();

        Map<Long, String> brandNameMap = buildBrandNameMap(productPage.getRecords());
        Map<Long, List<TagVO>> tagMap = buildTagMap(productIds);

        return productPage.convert(p -> {
            String stockStatus;
            if (p.getStock() == null || p.getStock() == 0) {
                stockStatus = "out_of_stock";
            } else if (p.getStock() < 10) {
                stockStatus = "low";
            } else {
                stockStatus = "sufficient";
            }
            return new ProductVO(
                    p.getId(), p.getName(), p.getCategoryId(), p.getBrandId(),
                    brandNameMap.getOrDefault(p.getId(), ""),
                    p.getTitle(), p.getPrice(), p.getStock(), stockStatus,
                    p.getMainImage(), p.getSales(), p.getStatus(),
                    tagMap.getOrDefault(p.getId(), Collections.emptyList()),
                    p.getCreateTime()
            );
        });
    }

    private String buildOrderBy(String sortBy, String keyword) {
        return switch (sortBy) {
            case "price_asc" -> "ORDER BY price ASC";
            case "price_desc" -> "ORDER BY price DESC";
            case "time_desc" -> "ORDER BY create_time DESC";
            default -> "ORDER BY sales DESC"; // sales_desc or relevance
        };
    }

    private Map<Long, String> buildBrandNameMap(List<Product> products) {
        List<Long> brandIds = products.stream()
                .map(Product::getBrandId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (brandIds.isEmpty()) return Collections.emptyMap();
        List<Brand> brands = brandMapper.selectBatchIds(brandIds);
        return brands.stream()
                .collect(Collectors.toMap(Brand::getId, Brand::getName));
    }

    private Map<Long, List<TagVO>> buildTagMap(List<Long> productIds) {
        if (productIds.isEmpty()) return Collections.emptyMap();
        return productIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> productTagMapper.selectTagsByProductId(id).stream()
                                .map(m -> new TagVO(
                                        ((Number) m.get("id")).longValue(),
                                        (String) m.get("name"),
                                        (String) m.get("color")))
                                .toList()
                ));
    }

    @Override
    public void index(ProductVO product) {
        // MySQL: no-op, FULLTEXT index updates automatically
    }

    @Override
    public void removeIndex(Long productId) {
        // MySQL: no-op, FULLTEXT index updates automatically
    }

    @Override
    public List<SearchKeywordVO> getHotKeywords(int limit) {
        return searchKeywordMapper.selectHotKeywords(limit).stream()
                .map(SearchKeywordVO::from).toList();
    }

    @Override
    public List<SearchKeywordVO> getSuggestions(String prefix, int limit) {
        return searchKeywordMapper.selectSuggestions(prefix, limit).stream()
                .map(SearchKeywordVO::from).toList();
    }
}
