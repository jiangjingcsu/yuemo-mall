package com.yuemo.product.controller;

import com.yuemo.common.core.response.PageResult;
import com.yuemo.common.core.response.Result;
import com.yuemo.product.dto.CreateReviewRequest;
import com.yuemo.product.dto.SearchCriteria;
import com.yuemo.product.service.BrandService;
import com.yuemo.product.service.ProductService;
import com.yuemo.product.service.ReviewService;
import com.yuemo.product.service.SearchService;
import com.yuemo.product.vo.*;
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
    private final SearchService searchService;

    @GetMapping("/list")
    public Result<PageResult<ProductVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "20") Integer size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(required = false) Long brandId,
                                               @RequestParam(required = false) BigDecimal priceMin,
                                               @RequestParam(required = false) BigDecimal priceMax,
                                               @RequestParam(defaultValue = "sales_desc") String sortBy) {
        SearchCriteria criteria = new SearchCriteria(
                keyword, categoryId, brandId, priceMin, priceMax, page, size, sortBy, null);
        return Result.success(PageResult.from(productService.searchProducts(criteria).convert(vo -> vo)));
    }

    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.success(productService.getProductDetail(id));
    }

    @GetMapping("/category/list")
    public Result<List<CategoryVO>> categories() {
        return Result.success(productService.getCategoryTree().stream().map(CategoryVO::from).toList());
    }

    @GetMapping("/brand/list")
    public Result<List<BrandVO>> brands() {
        return Result.success(brandService.listAll().stream().map(BrandVO::from).toList());
    }

    @GetMapping("/search/hot")
    public Result<List<SearchKeywordVO>> hotKeywords() {
        return Result.success(searchService.getHotKeywords(10));
    }

    @GetMapping("/search/suggest")
    public Result<List<SearchKeywordVO>> suggest(@RequestParam String keyword) {
        return Result.success(searchService.getSuggestions(keyword, 8));
    }

    @GetMapping("/{id}/reviews")
    public Result<PageResult<ProductReviewVO>> reviews(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(PageResult.from(reviewService.getReviewsByProduct(id, page, size).convert(ProductReviewVO::from)));
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
