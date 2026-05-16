package com.yuemo.promotion.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuemo.promotion.entity.Coupon;
import com.yuemo.promotion.entity.UserCoupon;

import java.util.List;

public interface CouponService {

    void createCoupon(Coupon coupon);

    IPage<Coupon> pageCoupons(Integer page, Integer size);

    void receiveCoupon(Long userId, Long couponId);

    List<UserCoupon> getUserCoupons(Long userId, Integer status);

    void useCoupon(Long userCouponId, Long orderId);
}
