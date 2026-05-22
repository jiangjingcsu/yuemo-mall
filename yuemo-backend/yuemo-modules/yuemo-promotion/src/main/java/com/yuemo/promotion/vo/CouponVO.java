package com.yuemo.promotion.vo;

import com.yuemo.promotion.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponVO(
    Long id,
    String name,
    Integer type,
    BigDecimal threshold,
    BigDecimal value,
    Integer totalCount,
    Integer receivedCount,
    Integer usedCount,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Integer status,
    LocalDateTime createTime
) {

    public static CouponVO from(Coupon coupon) {
        return new CouponVO(
                coupon.getId(),
                coupon.getName(),
                coupon.getType(),
                coupon.getThreshold(),
                coupon.getValue(),
                coupon.getTotalCount(),
                coupon.getReceivedCount(),
                coupon.getUsedCount(),
                coupon.getStartTime(),
                coupon.getEndTime(),
                coupon.getStatus(),
                coupon.getCreateTime()
        );
    }
}
