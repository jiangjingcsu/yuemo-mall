package com.yuemo.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.product.dto.CreateProductRequest;
import com.yuemo.product.entity.Product;
import com.yuemo.product.entity.ProductSku;
import com.yuemo.product.mapper.ProductMapper;
import com.yuemo.product.mapper.ProductSkuMapper;
import com.yuemo.product.service.SkuService;
import com.yuemo.product.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkuServiceImpl implements SkuService {

    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;

    @Override
    public List<SkuVO> getSkusByProductId(Long productId) {
        List<ProductSku> skus = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId)
                .eq(ProductSku::getStatus, 1)
                .eq(ProductSku::getDeleted, false));
        return skus.stream().map(s -> new SkuVO(
                s.getId(), s.getSkuCode(), parseSpecIds(s.getSpecIds()), s.getSpecText(),
                s.getPrice(), s.getStock(), s.getImage()
        )).toList();
    }

    @Override
    public ProductSku getSkuById(Long skuId) {
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ResultCode.SKU_NOT_FOUND);
        }
        return sku;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProductSku> createSkus(Long productId, List<CreateProductRequest.SkuRequest> requests) {
        List<ProductSku> skus = new ArrayList<>();
        for (CreateProductRequest.SkuRequest req : requests) {
            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setSkuCode(req.skuCode());
            sku.setSpecIds(toJsonArray(req.specIds()));
            sku.setSpecText(req.specText());
            sku.setPrice(req.price());
            sku.setStock(req.stock());
            sku.setImage(req.image());
            sku.setStatus(1);
            skuMapper.insert(sku);
            skus.add(sku);
        }
        syncProductAggregate(productId);
        return skus;
    }

    @Override
    @CacheEvict(value = "product", key = "#sku.productId")
    public void updateSku(Long skuId, ProductSku sku) {
        ProductSku existing = getSkuById(skuId);
        sku.setId(skuId);
        skuMapper.updateById(sku);
        syncProductAggregate(existing.getProductId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSku(Long skuId) {
        ProductSku sku = getSkuById(skuId);
        sku.setDeleted(true);
        skuMapper.updateById(sku);
        syncProductAggregate(sku.getProductId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSkuStock(Long skuId, Integer quantity) {
        ProductSku sku = getSkuById(skuId);
        int newStock = sku.getStock() - quantity;
        if (newStock < 0) {
            throw new BusinessException(ResultCode.SKU_STOCK_INSUFFICIENT);
        }
        sku.setStock(newStock);
        skuMapper.updateById(sku);
        syncProductAggregate(sku.getProductId());
    }

    @Override
    public void syncProductAggregate(Long productId) {
        List<ProductSku> skus = skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId)
                .eq(ProductSku::getStatus, 1)
                .eq(ProductSku::getDeleted, false));

        Product product = productMapper.selectById(productId);
        if (product == null) return;

        if (skus.isEmpty()) {
            product.setPrice(BigDecimal.ZERO);
            product.setStock(0);
        } else {
            BigDecimal minPrice = skus.stream()
                    .map(ProductSku::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            int totalStock = skus.stream()
                    .mapToInt(ProductSku::getStock)
                    .sum();
            product.setPrice(minPrice);
            product.setStock(totalStock);
        }
        productMapper.updateById(product);
    }

    private String toJsonArray(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ids.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private List<Long> parseSpecIds(String specIdsJson) {
        if (specIdsJson == null || specIdsJson.isBlank() || "[]".equals(specIdsJson)) {
            return Collections.emptyList();
        }
        String trimmed = specIdsJson.replaceAll("[\\[\\]\"]", "");
        if (trimmed.isBlank()) return Collections.emptyList();
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();
    }
}
