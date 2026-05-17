package com.yuemo.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_order_log")
public class OrderLog extends BaseEntity {
    private Long orderId;
    private Integer fromStatus;
    private Integer toStatus;
    private String operator;
    private String remark;
}
