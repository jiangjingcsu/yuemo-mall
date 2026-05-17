package com.yuemo.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuemo.common.core.response.Result;
import com.yuemo.order.dto.CreateOrderDTO;
import com.yuemo.order.entity.Order;
import com.yuemo.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public Result<Order> create(@RequestAttribute("userId") Long userId,
                                 @Valid @RequestBody CreateOrderDTO dto) {
        return Result.success(orderService.createOrder(userId, dto));
    }

    @GetMapping("/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.success(orderService.getOrderById(id));
    }

    @GetMapping("/list")
    public Result<IPage<Order>> list(@RequestAttribute("userId") Long userId,
                                      @RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size,
                                      @RequestParam(required = false) Integer status) {
        return Result.success(orderService.pageOrders(userId, page, size, status));
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id) {
        orderService.cancelOrder(userId, id);
        return Result.success();
    }

    @PostMapping("/ship/{id}")
    public Result<Void> ship(@PathVariable Long id,
                              @RequestParam String logisticsCompany,
                              @RequestParam String logisticsNo) {
        orderService.shipOrder(id, logisticsCompany, logisticsNo);
        return Result.success();
    }

    @PostMapping("/confirm/{id}")
    public Result<Void> confirm(@RequestAttribute("userId") Long userId,
                                 @PathVariable Long id) {
        orderService.confirmReceive(userId, id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                                @PathVariable Long id) {
        orderService.deleteOrder(userId, id);
        return Result.success();
    }
}
