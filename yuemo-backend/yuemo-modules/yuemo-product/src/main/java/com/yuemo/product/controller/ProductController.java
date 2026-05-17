package com.yuemo.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuemo.common.core.response.Result;
import com.yuemo.product.dto.CreateReviewRequest;
import com.yuemo.product.dto.SearchCriteria;
import com.yuemo.product.entity.Brand;
import com.yuemo.product.entity.Category;
import com.yuemo.product.entity.ProductReview;
import com.yuemo.product.entity.SearchKeyword;
import com.yuemo.product.mapper.SearchKeywordMapper;
import com.yuemo.product.service.BrandService;
import com.yuemo.product.service.ProductService;
import com.yuemo.product.service.ReviewService;
import com.yuemo.product.vo.ProductDetailVO;
import com.yuemo.product.vo.ProductVO;
import com.yuemo.product.vo.ReviewSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final BrandService brandService;
    private final ReviewService reviewService;
    private final SearchKeywordMapper searchKeywordMapper;

    @GetMapping("/list")
    public Result<IPage<ProductVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) Long brandId,
                                          @RequestParam(required = false) BigDecimal priceMin,
                                          @RequestParam(required = false) BigDecimal priceMax,
                                          @RequestParam(defaultValue = "sales_desc") String sortBy) {
        SearchCriteria criteria = new SearchCriteria(
                keyword, categoryId, brandId, priceMin, priceMax, page, size, sortBy, null);
        return Result.success(productService.searchProducts(criteria));
    }

    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.success(productService.getProductDetail(id));
    }

    @GetMapping("/category/list")
    public Result<List<Category>> categories() {
        return Result.success(productService.getCategoryTree());
    }

    @GetMapping("/brand/list")
    public Result<List<Brand>> brands() {
        return Result.success(brandService.listAll());
    }

    @GetMapping("/search/hot")
    public Result<List<SearchKeyword>> hotKeywords() {
        return Result.success(searchKeywordMapper.selectHotKeywords(10));
    }

    @GetMapping("/search/suggest")
    public Result<List<SearchKeyword>> suggest(@RequestParam String keyword) {
        return Result.success(searchKeywordMapper.selectSuggestions(keyword, 8));
    }

    @GetMapping("/{id}/reviews")
    public Result<IPage<ProductReview>> reviews(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(reviewService.getReviewsByProduct(id, page, size));
    }

    @GetMapping("/{id}/review-summary")
    public Result<ReviewSummaryVO> reviewSummary(@PathVariable Long id) {
        return Result.success(reviewService.getReviewSummary(id));
    }

    @PostMapping("/{id}/review")
    public Result<Void> createReview(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long id,
                                      @Valid @RequestBody CreateReviewRequest request) {
        reviewService.createReview(id, userId, request.orderId(), request.skuId(),
                request.rating(), request.content(), request.images());
        return Result.success();
    }
}
