package com.yuemo.promotion.controller;

import com.yuemo.common.core.response.PageResult;
import com.yuemo.common.core.response.Result;
import com.yuemo.promotion.service.CouponService;
import com.yuemo.promotion.vo.CouponVO;
import com.yuemo.promotion.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/list")
    public Result<PageResult<CouponVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(PageResult.from(couponService.pageCoupons(page, size)));
    }

    @PostMapping("/receive/{couponId}")
    public Result<Void> receive(@RequestAttribute("userId") Long userId,
                                @PathVariable Long couponId) {
        couponService.receiveCoupon(userId, couponId);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<UserCouponVO>> myCoupons(@RequestAttribute("userId") Long userId,
                                                 @RequestParam(required = false) Integer status) {
        return Result.success(couponService.getUserCoupons(userId, status).stream().map(UserCouponVO::from).toList());
    }
}
