package com.yuemo.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_order_item")
public class OrderItem extends BaseEntity {

    private Long orderId;
    private Long productId;
    private Long skuId;
    private String productName;
    private String productImage;
    private String specText;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalAmount;
}
