package com.yuemo.cart.controller;

import com.yuemo.cart.entity.CartItem;
import com.yuemo.cart.service.CartService;
import com.yuemo.common.core.response.Result;
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
                            @RequestParam Long skuId,
                            @RequestParam(defaultValue = "1") Integer quantity) {
        cartService.addItem(userId, skuId, quantity);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<CartItem>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(cartService.listItems(userId));
    }

    @PutMapping("/{itemId}")
    public Result<Void> updateQuantity(@RequestAttribute("userId") Long userId,
                                        @PathVariable Long itemId,
                                        @RequestParam Integer quantity) {
        cartService.updateQuantity(userId, itemId, quantity);
        return Result.success();
    }

    @DeleteMapping("/{itemId}")
    public Result<Void> remove(@RequestAttribute("userId") Long userId,
                                @PathVariable Long itemId) {
        cartService.removeItem(userId, itemId);
        return Result.success();
    }

    @PutMapping("/{itemId}/select")
    public Result<Void> toggleSelect(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long itemId,
                                      @RequestParam Boolean selected) {
        cartService.toggleSelect(userId, itemId, selected);
        return Result.success();
    }
}
