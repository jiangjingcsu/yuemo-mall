package com.yuemo.promotion.vo;

import com.yuemo.promotion.entity.UserCoupon;

import java.time.LocalDateTime;

public record UserCouponVO(
    Long id,
    Long userId,
    Long couponId,
    Integer status,
    Long orderId,
    LocalDateTime usedTime,
    LocalDateTime createTime
) {

    public static UserCouponVO from(UserCoupon userCoupon) {
        return new UserCouponVO(
                userCoupon.getId(),
                userCoupon.getUserId(),
                userCoupon.getCouponId(),
                userCoupon.getStatus(),
                userCoupon.getOrderId(),
                userCoupon.getUsedTime(),
                userCoupon.getCreateTime()
        );
    }
}
