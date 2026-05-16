package com.yuemo.product.service;

import com.yuemo.product.dto.CreateProductRequest;
import com.yuemo.product.entity.ProductSku;
import com.yuemo.product.vo.SkuVO;

import java.util.List;

public interface SkuService {
    List<SkuVO> getSkusByProductId(Long productId);
    ProductSku getSkuById(Long skuId);
    List<ProductSku> createSkus(Long productId, List<CreateProductRequest.SkuRequest> requests);
    void updateSku(Long skuId, ProductSku sku);
    void deleteSku(Long skuId);
    void updateSkuStock(Long skuId, Integer quantity);
    void syncProductAggregate(Long productId);
}
