package com.yuemo.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateOrderDTO {

    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    @NotEmpty(message = "订单商品不能为空")
    private List<OrderItemDTO> items;

    private String remark;
    private Long couponId;

    @Data
    public static class OrderItemDTO {
        @NotNull
        private Long productId;
        private Long skuId;
        @NotNull
        private Integer quantity;
    }
}
