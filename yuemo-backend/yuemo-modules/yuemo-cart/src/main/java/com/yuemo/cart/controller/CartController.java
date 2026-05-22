package com.yuemo.cart.controller;

import com.yuemo.cart.dto.AddCartRequest;
import com.yuemo.cart.dto.CartItemVO;
import com.yuemo.cart.dto.ToggleSelectRequest;
import com.yuemo.cart.dto.UpdateQuantityRequest;
import com.yuemo.cart.service.CartService;
import com.yuemo.common.core.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public Result<Void> add(@RequestAttribute("userId") Long userId,
                            @Valid @RequestBody AddCartRequest request) {
        cartService.addItem(userId, request.skuId(), request.quantity());
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<CartItemVO>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(cartService.listItems(userId));
    }

    @PutMapping("/sku/{skuId}")
    public Result<Void> updateQuantity(@RequestAttribute("userId") Long userId,
                                       @PathVariable Long skuId,
                                       @Valid @RequestBody UpdateQuantityRequest request) {
        cartService.updateQuantity(userId, skuId, request.quantity());
        return Result.success();
    }

    @DeleteMapping("/sku/{skuId}")
    public Result<Void> remove(@RequestAttribute("userId") Long userId,
                               @PathVariable Long skuId) {
        cartService.removeItem(userId, skuId);
        return Result.success();
    }

    @PutMapping("/sku/{skuId}/select")
    public Result<Void> toggleSelect(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long skuId,
                                     @Valid @RequestBody ToggleSelectRequest request) {
        cartService.toggleSelect(userId, skuId, request.selected());
        return Result.success();
    }

    @PutMapping("/select-all")
    public Result<Void> selectAll(@RequestAttribute("userId") Long userId,
                                  @RequestParam Boolean selected) {
        cartService.selectAll(userId, selected);
        return Result.success();
    }

    @DeleteMapping("/selected")
    public Result<Void> clearSelected(@RequestAttribute("userId") Long userId) {
        cartService.clearSelected(userId);
        return Result.success();
    }
}
