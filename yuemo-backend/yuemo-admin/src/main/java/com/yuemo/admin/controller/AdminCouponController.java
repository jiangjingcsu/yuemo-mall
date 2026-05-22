package com.yuemo.admin.controller;

import com.yuemo.common.core.response.Result;
import com.yuemo.promotion.service.CouponService;
import com.yuemo.promotion.vo.CouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coupon")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping
    public Result<CouponVO> create(@RequestBody com.yuemo.promotion.entity.Coupon coupon) {
        couponService.createCoupon(coupon);
        return Result.success(CouponVO.from(coupon));
    }
}
