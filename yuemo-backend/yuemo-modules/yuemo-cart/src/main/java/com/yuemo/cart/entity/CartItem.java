package com.yuemo.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_cart_item")
public class CartItem extends BaseEntity {

    private Long userId;
    private Long productId;
    private Long skuId;
    private Integer quantity;
    private Boolean selected;
}
