package com.yuemo.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuemo.product.dto.CreateProductRequest;
import com.yuemo.product.dto.SearchCriteria;
import com.yuemo.product.dto.UpdateProductRequest;
import com.yuemo.product.entity.Category;
import com.yuemo.product.entity.Product;
import com.yuemo.product.vo.ProductDetailVO;
import com.yuemo.product.vo.ProductVO;

import java.util.List;

public interface ProductService {

    IPage<ProductVO> searchProducts(SearchCriteria criteria);

    ProductDetailVO getProductDetail(Long id);

    Product getProductById(Long id);

    void createProduct(CreateProductRequest request);

    void updateProduct(Long id, UpdateProductRequest request);

    void updateStock(Long id, Integer quantity);

    void restoreStock(Long id, Integer quantity);

    void updateSkuStock(Long skuId, Integer quantity);

    List<Category> getCategoryTree();

    List<Product> batchGetProductsByIds(List<Long> ids);
}
