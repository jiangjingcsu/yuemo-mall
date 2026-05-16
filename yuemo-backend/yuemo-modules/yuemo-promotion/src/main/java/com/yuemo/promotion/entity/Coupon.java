package com.yuemo.promotion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_coupon")
public class Coupon extends BaseEntity {

    private String name;
    private Integer type; // 1-满减 2-折扣 3-立减
    private BigDecimal threshold; // 使用门槛
    private BigDecimal value; // 优惠金额 / 折扣率
    private Integer totalCount;
    private Integer receivedCount;
    private Integer usedCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status; // 0-未开始 1-进行中 2-已结束
}
