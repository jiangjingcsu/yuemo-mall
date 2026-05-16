package com.yuemo.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuemo.order.dto.CreateOrderDTO;
import com.yuemo.order.entity.Order;

public interface OrderService {

    Order createOrder(Long userId, CreateOrderDTO dto);

    Order getOrderById(Long id);

    IPage<Order> pageOrders(Long userId, Integer page, Integer size, Integer status);

    void cancelOrder(Long userId, Long orderId);

    void paySuccess(String orderNo);
}
