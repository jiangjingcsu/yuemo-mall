package com.yuemo.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuemo.cart.service.CartService;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.common.core.utils.IdGenerator;
import com.yuemo.order.dto.CreateOrderDTO;
import com.yuemo.order.entity.Order;
import com.yuemo.order.entity.OrderItem;
import com.yuemo.order.entity.OrderLog;
import com.yuemo.order.enums.OrderStatus;
import com.yuemo.order.mapper.OrderItemMapper;
import com.yuemo.order.mapper.OrderLogMapper;
import com.yuemo.order.mapper.OrderMapper;
import com.yuemo.order.service.OrderService;
import com.yuemo.product.entity.Product;
import com.yuemo.product.entity.ProductSku;
import com.yuemo.product.service.ProductService;
import com.yuemo.product.service.SkuService;
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
    private final SkuService skuService;
    private final CartService cartService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, CreateOrderDTO dto) {
        String orderNo = generateOrderNo(userId);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Long, Integer> skuStockMap = new java.util.LinkedHashMap<>();

        for (CreateOrderDTO.OrderItemDTO itemDTO : dto.items()) {
            Product product = productService.getProductById(itemDTO.productId());
            if (product == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
            }
            if (product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST);
            }

            BigDecimal price = product.getPrice();
            Long skuId = itemDTO.skuId();
            String specText = null;

            if (skuId != null) {
                ProductSku sku = skuService.getSkuById(skuId);
                if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
                    throw new BusinessException(ResultCode.SKU_NOT_FOUND);
                }
                if (sku.getStock() == null || sku.getStock() < itemDTO.quantity()) {
                    throw new BusinessException(ResultCode.SKU_STOCK_INSUFFICIENT);
                }
                if (sku.getPrice() != null) {
                    price = sku.getPrice();
                }
                specText = sku.getSpecText();
                skuStockMap.put(skuId, itemDTO.quantity());
            } else {
                if (product.getStock() != null && product.getStock() < itemDTO.quantity()) {
                    throw new BusinessException(ResultCode.SKU_STOCK_INSUFFICIENT);
                }
            }

            OrderItem item = new OrderItem();
            item.setProductId(itemDTO.productId());
            item.setSkuId(skuId);
            item.setQuantity(itemDTO.quantity());
            item.setPrice(price);
            item.setTotalAmount(price.multiply(BigDecimal.valueOf(itemDTO.quantity())));
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
            item.setSpecText(specText);
            items.add(item);
            totalAmount = totalAmount.add(item.getTotalAmount());
        }

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setStatus(OrderStatus.UNPAID.getCode());
        order.setAddressId(dto.addressId());
        order.setRemark(dto.remark());
        orderMapper.insert(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        if (!skuStockMap.isEmpty()) {
            Message<Map<Long, Integer>> message = MessageBuilder
                    .withPayload(skuStockMap)
                    .setHeader("orderId", order.getId())
                    .build();
            rocketMQTemplate.sendMessageInTransaction("order-stock-preoccupy", message, order);
        }

        try {
            cartService.clearSelected(userId);
        } catch (Exception e) {
            log.warn("清空购物车已选商品失败，不影响订单创建: userId={}", userId, e);
        }

        return order;
    }

    @Override
    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        order.setItems(orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, id)));
        return order;
    }

    @Override
    public Order getOrderByIdAndUserId(Long id, Long userId) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        order.verifyOwnership(userId);
        order.setItems(orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, id)));
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
        order.verifyOwnership(userId);
        int fromStatus = order.getStatus();
        order.cancel();
        orderMapper.updateById(order);
        saveOrderLog(orderId, fromStatus, OrderStatus.CANCELLED.getCode(), "user:" + userId, "取消订单");

        releaseStock(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        int fromStatus = order.getStatus();
        order.pay();
        orderMapper.updateById(order);
        saveOrderLog(order.getId(), fromStatus, OrderStatus.PAID.getCode(), "system", "支付成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, String logisticsCompany, String logisticsNo) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        int fromStatus = order.getStatus();
        order.ship(logisticsCompany, logisticsNo);
        orderMapper.updateById(order);
        saveOrderLog(orderId, fromStatus, OrderStatus.SHIPPED.getCode(), "system", "发货");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        order.verifyOwnership(userId);
        int fromStatus = order.getStatus();
        order.confirmReceive();
        orderMapper.updateById(order);
        saveOrderLog(orderId, fromStatus, OrderStatus.COMPLETED.getCode(), "user:" + userId, "确认收货");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        order.verifyOwnership(userId);
        if (!order.canDelete()) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
        order.setDeleted(true);
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrderWithCas(Long orderId) {
        int updated = orderMapper.update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatus.UNPAID.getCode())
                .set(Order::getStatus, OrderStatus.CANCELLED.getCode()));
        if (updated > 0) {
            saveOrderLog(orderId, OrderStatus.UNPAID.getCode(), OrderStatus.CANCELLED.getCode(), "system", "超时自动取消");
            releaseStock(orderId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        int fromStatus = order.getStatus();
        order.setStatus(OrderStatus.REFUNDED.getCode());
        orderMapper.updateById(order);
        saveOrderLog(orderId, fromStatus, OrderStatus.REFUNDED.getCode(), "system", "退款成功");
        releaseStock(orderId);
    }

    private void releaseStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));

        Map<Long, Integer> skuStockMap = items.stream()
                .filter(i -> i.getSkuId() != null)
                .collect(Collectors.toMap(OrderItem::getSkuId, OrderItem::getQuantity));

        if (!skuStockMap.isEmpty()) {
            rocketMQTemplate.convertAndSend("order-stock-release", skuStockMap);
        }
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
