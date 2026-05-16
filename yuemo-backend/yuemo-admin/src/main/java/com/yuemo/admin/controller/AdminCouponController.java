package com.yuemo.admin.controller;

import com.yuemo.common.core.response.Result;
import com.yuemo.promotion.entity.Coupon;
import com.yuemo.promotion.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupon")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping
    public Result<Void> create(@RequestBody Coupon coupon) {
        couponService.createCoupon(coupon);
        return Result.success();
    }
}
