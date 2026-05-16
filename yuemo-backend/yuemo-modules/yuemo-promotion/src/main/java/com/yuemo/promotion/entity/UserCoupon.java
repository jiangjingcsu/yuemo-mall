package com.yuemo.promotion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_user_coupon")
public class UserCoupon extends BaseEntity {

    private Long userId;
    private Long couponId;
    private Integer status; // 0-未使用 1-已使用 2-已过期
    private Long orderId;
    private LocalDateTime usedTime;
}
