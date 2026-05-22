package com.yuemo.order.vo;

import com.yuemo.order.entity.Order;
import com.yuemo.order.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderVO(
    Long id,
    String orderNo,
    Long userId,
    BigDecimal totalAmount,
    BigDecimal payAmount,
    Integer status,
    Long addressId,
    String remark,
    String logisticsCompany,
    String logisticsNo,
    LocalDateTime payTime,
    LocalDateTime deliveryTime,
    LocalDateTime receiveTime,
    List<OrderItemVO> items,
    LocalDateTime createTime
) {

    public static OrderVO from(Order order) {
        return new OrderVO(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getPayAmount(),
                order.getStatus(),
                order.getAddressId(),
                order.getRemark(),
                order.getLogisticsCompany(),
                order.getLogisticsNo(),
                order.getPayTime(),
                order.getDeliveryTime(),
                order.getReceiveTime(),
                order.getItems() != null ? order.getItems().stream().map(OrderItemVO::from).toList() : null,
                order.getCreateTime()
        );
    }
}
