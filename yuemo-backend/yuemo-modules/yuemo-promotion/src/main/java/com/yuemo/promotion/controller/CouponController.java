package com.yuemo.promotion.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuemo.common.core.response.Result;
import com.yuemo.promotion.entity.Coupon;
import com.yuemo.promotion.entity.UserCoupon;
import com.yuemo.promotion.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/list")
    public Result<IPage<Coupon>> list(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(couponService.pageCoupons(page, size));
    }

    @PostMapping("/receive/{couponId}")
    public Result<Void> receive(@RequestAttribute("userId") Long userId,
                                @PathVariable Long couponId) {
        couponService.receiveCoupon(userId, couponId);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<UserCoupon>> myCoupons(@RequestAttribute("userId") Long userId,
                                               @RequestParam(required = false) Integer status) {
        return Result.success(couponService.getUserCoupons(userId, status));
    }
}
