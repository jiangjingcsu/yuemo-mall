package com.yuemo.admin.controller;

import com.yuemo.common.core.response.Result;
import com.yuemo.product.dto.CreateProductRequest;
import com.yuemo.product.dto.UpdateProductRequest;
import com.yuemo.product.entity.Brand;
import com.yuemo.product.entity.ProductSku;
import com.yuemo.product.service.BrandService;
import com.yuemo.product.service.ProductService;
import com.yuemo.product.service.SkuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final BrandService brandService;
    private final SkuService skuService;

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateProductRequest request) {
        productService.createProduct(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        productService.updateProduct(id, request);
        return Result.success();
    }

    @PutMapping("/{id}/stock")
    public Result<Void> updateStock(@PathVariable Long id, @RequestParam Integer quantity) {
        productService.updateStock(id, quantity);
        return Result.success();
    }

    @PutMapping("/sku/{skuId}/stock")
    public Result<Void> updateSkuStock(@PathVariable Long skuId, @RequestParam Integer quantity) {
        productService.updateSkuStock(skuId, quantity);
        return Result.success();
    }

    @PostMapping("/{id}/sku")
    public Result<Void> createSku(@PathVariable Long id, @RequestBody ProductSku sku) {
        sku.setProductId(id);
        skuMapper.insert(sku);
        return Result.success();
    }

    @PutMapping("/sku/{skuId}")
    public Result<Void> updateSku(@PathVariable Long skuId, @RequestBody ProductSku sku) {
        skuService.updateSku(skuId, sku);
        return Result.success();
    }

    @DeleteMapping("/sku/{skuId}")
    public Result<Void> deleteSku(@PathVariable Long skuId) {
        skuService.deleteSku(skuId);
        return Result.success();
    }

    @PostMapping("/brand")
    public Result<Void> createBrand(@RequestBody Brand brand) {
        brandService.create(brand);
        return Result.success();
    }

    @PutMapping("/brand/{id}")
    public Result<Void> updateBrand(@PathVariable Long id, @RequestBody Brand brand) {
        brandService.update(id, brand);
        return Result.success();
    }

    private final com.yuemo.product.mapper.ProductSkuMapper skuMapper;
}
