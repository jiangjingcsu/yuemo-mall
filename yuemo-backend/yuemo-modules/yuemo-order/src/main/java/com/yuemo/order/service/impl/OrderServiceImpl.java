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
import com.yuemo.order.entity.OrderLog;
import com.yuemo.order.mapper.OrderItemMapper;
import com.yuemo.order.mapper.OrderLogMapper;
import com.yuemo.order.mapper.OrderMapper;
import com.yuemo.order.service.OrderService;
import com.yuemo.product.entity.Product;
import com.yuemo.product.service.ProductService;
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
    private final OrderLogMapper orderLogMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final ProductService productService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, CreateOrderDTO dto) {
        // 生成订单号
        String orderNo = generateOrderNo(userId);

        // 构建订单商品列表
        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderDTO.OrderItemDTO itemDTO : dto.getItems()) {
            Product product = productService.getProductById(itemDTO.getProductId());

            OrderItem item = new OrderItem();
            item.setProductId(itemDTO.getProductId());
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(product.getPrice());
            item.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
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
        int fromStatus = order.getStatus();
        order.setStatus(4);
        orderMapper.updateById(order);
        saveOrderLog(orderId, fromStatus, 4, "user:" + userId, "取消订单");

        // 查询订单明细，构建库存释放信息
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        Map<Long, Integer> stockMap = items.stream()
                .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity));
        rocketMQTemplate.convertAndSend("order-stock-release", stockMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            log.warn("订单状态异常，无法更新为已支付: orderNo={}, currentStatus={}", orderNo, order.getStatus());
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String logisticsCompany, String logisticsNo) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        int fromStatus = order.getStatus();
        order.setStatus(2);
        order.setLogisticsCompany(logisticsCompany);
        order.setLogisticsNo(logisticsNo);
        order.setDeliveryTime(LocalDateTime.now());
        orderMapper.updateById(order);
        saveOrderLog(orderId, fromStatus, 2, "system", "发货");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 2) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        int fromStatus = order.getStatus();
        order.setStatus(3);
        order.setReceiveTime(LocalDateTime.now());
        orderMapper.updateById(order);
        saveOrderLog(orderId, fromStatus, 3, "user:" + userId, "确认收货");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 3 && order.getStatus() != 4) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        order.setDeleted(true);
        orderMapper.updateById(order);
    }

    private void saveOrderLog(Long orderId, Integer fromStatus, Integer toStatus, String operator, String remark) {
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(orderId);
        logEntry.setFromStatus(fromStatus);
        logEntry.setToStatus(toStatus);
        logEntry.setOperator(operator);
        logEntry.setRemark(remark);
        orderLogMapper.insert(logEntry);
    }

    private String generateOrderNo(Long userId) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + userId % 10000
                + IdGenerator.nextId() % 1000;
    }
}
