package com.yuemo.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.common.core.utils.IdGenerator;
import com.yuemo.order.dto.CreateOrderDTO;
import com.yuemo.order.entity.Order;
import com.yuemo.order.entity.OrderItem;
import com.yuemo.order.mapper.OrderItemMapper;
import com.yuemo.order.mapper.OrderMapper;
import com.yuemo.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, CreateOrderDTO dto) {
        // 生成订单号
        String orderNo = generateOrderNo(userId);

        // 构建订单商品列表
        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderDTO.OrderItemDTO itemDTO : dto.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemDTO.getProductId());
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(BigDecimal.ZERO); // 实际场景应查询商品价格
            item.setTotalAmount(item.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            items.add(item);
            totalAmount = totalAmount.add(item.getTotalAmount());
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setStatus(0); // 待支付
        order.setAddressId(dto.getAddressId());
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            // 实际场景应查询商品名称和图片
            item.setProductName("商品" + item.getProductId());
            item.setProductImage("");
            orderItemMapper.insert(item);
        }

        // 发送预占库存事务消息
        Map<Long, Integer> stockMap = dto.getItems().stream()
                .collect(Collectors.toMap(CreateOrderDTO.OrderItemDTO::getProductId, CreateOrderDTO.OrderItemDTO::getQuantity));
        Message<Map<Long, Integer>> message = MessageBuilder
                .withPayload(stockMap)
                .setHeader("orderId", order.getId())
                .build();
        rocketMQTemplate.sendMessageInTransaction("order-stock-preoccupy", message, order);

        return order;
    }

    @Override
    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    @Override
    public IPage<Order> pageOrders(Long userId, Integer page, Integer size, Integer status) {
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(4);
        orderMapper.updateById(order);

        // 发送释放库存消息
        rocketMQTemplate.convertAndSend("order-stock-release", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private String generateOrderNo(Long userId) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + userId % 10000
                + IdGenerator.nextId() % 1000;
    }
}
