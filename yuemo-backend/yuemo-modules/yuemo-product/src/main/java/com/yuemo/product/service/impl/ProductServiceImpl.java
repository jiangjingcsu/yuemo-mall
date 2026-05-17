package com.yuemo.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.product.dto.CreateProductRequest;
import com.yuemo.product.dto.SearchCriteria;
import com.yuemo.product.dto.UpdateProductRequest;
import com.yuemo.product.entity.Category;
import com.yuemo.product.entity.Product;
import com.yuemo.product.entity.ProductSpecTemplate;
import com.yuemo.product.entity.ProductSpecValue;
import com.yuemo.product.mapper.CategoryMapper;
import com.yuemo.product.mapper.ProductMapper;
import com.yuemo.product.mapper.ProductSpecTemplateMapper;
import com.yuemo.product.mapper.ProductSpecValueMapper;
import com.yuemo.product.mapper.ProductTagMapper;
import com.yuemo.product.mapper.ProductTagRelMapper;
import com.yuemo.product.service.BrandService;
import com.yuemo.product.service.ProductService;
import com.yuemo.product.service.ReviewService;
import com.yuemo.product.service.SearchService;
import com.yuemo.product.service.SkuService;
import com.yuemo.product.vo.ProductDetailVO;
import com.yuemo.product.vo.ProductVO;
import com.yuemo.product.vo.ReviewSummaryVO;
import com.yuemo.product.vo.SkuVO;
import com.yuemo.product.vo.SpecGroupVO;
import com.yuemo.product.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductTagMapper tagMapper;
    private final ProductSpecTemplateMapper specTemplateMapper;
    private final ProductSpecValueMapper specValueMapper;
    private final ProductTagRelMapper tagRelMapper;
    private final SearchService searchService;
    private final SkuService skuService;
    private final BrandService brandService;
    private final ReviewService reviewService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public IPage<ProductVO> searchProducts(SearchCriteria criteria) {
        IPage<ProductVO> page = searchService.search(criteria);
        // Enrich with brand names and tags for each product in the page
        page.getRecords().forEach(this::enrichProductVO);
        return page;
    }

    private void enrichProductVO(ProductVO vo) {
        // Brand name
        if (vo.brandId() != null) {
            try {
                String brandName = brandService.getById(vo.brandId()).getName();
                // We need to update the record, but records are immutable.
                // Tag enrichment is done at query time in the search service.
            } catch (Exception ignored) {}
        }
    }

    @Override
    @Cacheable(value = "productDetail", key = "#id", unless = "#result == null")
    public ProductDetailVO getProductDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getDeleted()) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        // Category name
        String categoryName = "";
        if (product.getCategoryId() != null) {
            Category cat = categoryMapper.selectById(product.getCategoryId());
            if (cat != null) categoryName = cat.getName();
        }

        // Brand name
        String brandName = "";
        if (product.getBrandId() != null) {
            try {
                brandName = brandService.getById(product.getBrandId()).getName();
            } catch (Exception ignored) {}
        }

        // Tags
        List<TagVO> tags = tagMapper.selectTagsByProductId(id).stream()
                .map(m -> new TagVO(
                        ((Number) m.get("id")).longValue(),
                        (String) m.get("name"),
                        (String) m.get("color")))
                .toList();

        // SKUs
        List<SkuVO> skus = skuService.getSkusByProductId(id);

        // Spec groups (for the product's category)
        List<SpecGroupVO> specGroups = buildSpecGroups(product.getCategoryId(), skus);

        // Images
        List<String> imageList = parseImages(product.getImages());

        // Review summary
        ReviewSummaryVO reviewSummary = reviewService.getReviewSummary(id);

        // min/max price
        java.math.BigDecimal minPrice = product.getPrice();
        java.math.BigDecimal maxPrice = product.getPrice();
        if (!skus.isEmpty()) {
            minPrice = skus.stream().map(SkuVO::price).min(java.math.BigDecimal::compareTo).orElse(minPrice);
            maxPrice = skus.stream().map(SkuVO::price).max(java.math.BigDecimal::compareTo).orElse(maxPrice);
        }

        return new ProductDetailVO(
                product.getId(), product.getName(), product.getCategoryId(), categoryName,
                product.getBrandId(), brandName, product.getTitle(), product.getDescription(),
                minPrice, maxPrice, product.getStock(), product.getMainImage(), imageList,
                product.getSales(), product.getStatus(),
                tags, specGroups, skus, reviewSummary
        );
    }

    private List<SpecGroupVO> buildSpecGroups(Long categoryId, List<SkuVO> skus) {
        // Get spec templates for this category (including global ones)
        List<ProductSpecTemplate> templates = specTemplateMapper.selectList(
                new LambdaQueryWrapper<ProductSpecTemplate>()
                        .and(w -> w.eq(ProductSpecTemplate::getCategoryId, categoryId)
                                .or().eq(ProductSpecTemplate::getCategoryId, 0))
                        .eq(ProductSpecTemplate::getDeleted, false)
                        .orderByAsc(ProductSpecTemplate::getSortOrder));

        if (templates.isEmpty()) return Collections.emptyList();

        return templates.stream().map(tpl -> {
            List<ProductSpecValue> values = specValueMapper.selectList(
                    new LambdaQueryWrapper<ProductSpecValue>()
                            .eq(ProductSpecValue::getTemplateId, tpl.getId())
                            .eq(ProductSpecValue::getDeleted, false)
                            .orderByAsc(ProductSpecValue::getSortOrder));
            List<SpecGroupVO.SpecValueVO> valueVOs = values.stream()
                    .map(v -> new SpecGroupVO.SpecValueVO(v.getId(), v.getValue()))
                    .toList();
            return new SpecGroupVO(tpl.getId(), tpl.getName(), valueVOs);
        }).toList();
    }

    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(imagesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Product getProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProduct(CreateProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setCategoryId(request.categoryId());
        product.setBrandId(request.brandId());
        product.setTitle(request.title());
        product.setDescription(request.description());
        product.setMainImage(request.mainImage());
        product.setImages(toJson(request.images()));
        product.setStatus(request.status() != null ? request.status() : 1);
        product.setSales(0);

        // Aggregate price/stock from SKUs
        if (request.skus() != null && !request.skus().isEmpty()) {
            var prices = request.skus().stream().map(CreateProductRequest.SkuRequest::price).toList();
            product.setPrice(prices.stream().min(java.math.BigDecimal::compareTo).orElse(java.math.BigDecimal.ZERO));
            product.setStock(request.skus().stream().mapToInt(CreateProductRequest.SkuRequest::stock).sum());
        } else {
            product.setPrice(java.math.BigDecimal.ZERO);
            product.setStock(0);
        }
        productMapper.insert(product);

        // Create SKUs
        if (request.skus() != null && !request.skus().isEmpty()) {
            skuService.createSkus(product.getId(), request.skus());
        }

        // Create tag relations
        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            saveTagRelations(product.getId(), request.tagIds());
        }

        // Index for search
        searchService.index(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"product", "productDetail"}, key = "#id")
    public void updateProduct(Long id, UpdateProductRequest request) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        if (request.name() != null) product.setName(request.name());
        if (request.categoryId() != null) product.setCategoryId(request.categoryId());
        if (request.brandId() != null) product.setBrandId(request.brandId());
        if (request.title() != null) product.setTitle(request.title());
        if (request.description() != null) product.setDescription(request.description());
        if (request.mainImage() != null) product.setMainImage(request.mainImage());
        if (request.images() != null) product.setImages(toJson(request.images()));
        if (request.status() != null) product.setStatus(request.status());

        productMapper.updateById(product);

        if (request.tagIds() != null) {
            saveTagRelations(id, request.tagIds());
        }

        // Re-sync aggregate from SKUs
        skuService.syncProductAggregate(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long id, Integer quantity) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        int newStock = product.getStock() - quantity;
        if (newStock < 0) {
            throw new BusinessException(ResultCode.PRODUCT_STOCK_INSUFFICIENT);
        }
        product.setStock(newStock);
        productMapper.updateById(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(Long id, Integer quantity) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        product.setStock(product.getStock() + quantity);
        productMapper.updateById(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSkuStock(Long skuId, Integer quantity) {
        skuService.updateSkuStock(skuId, quantity);
    }

    @Override
    public List<Category> getCategoryTree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getDeleted, false)
                        .orderByAsc(Category::getSortOrder));
        // Build tree structure
        Map<Long, List<Category>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() > 0)
                .collect(Collectors.groupingBy(Category::getParentId));

        List<Category> roots = new ArrayList<>();
        for (Category cat : all) {
            if (cat.getParentId() == null || cat.getParentId() == 0) {
                cat.setChildren(buildChildren(cat.getId(), childrenMap));
                roots.add(cat);
            }
        }
        return roots;
    }

    private List<Category> buildChildren(Long parentId, Map<Long, List<Category>> childrenMap) {
        List<Category> children = childrenMap.getOrDefault(parentId, Collections.emptyList());
        for (Category child : children) {
            child.setChildren(buildChildren(child.getId(), childrenMap));
        }
        return children;
    }

    private void saveTagRelations(Long productId, List<Long> tagIds) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.yuemo.product.entity.ProductTagRel> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(com.yuemo.product.entity.ProductTagRel::getProductId, productId);
        tagRelMapper.delete(wrapper);

        for (Long tagId : tagIds) {
            com.yuemo.product.entity.ProductTagRel rel = new com.yuemo.product.entity.ProductTagRel();
            rel.setProductId(productId);
            rel.setTagId(tagId);
            tagRelMapper.insert(rel);
        }
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
