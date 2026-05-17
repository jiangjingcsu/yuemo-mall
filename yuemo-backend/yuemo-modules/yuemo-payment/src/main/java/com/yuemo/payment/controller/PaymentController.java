package com.yuemo.payment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuemo.common.core.response.Result;
import com.yuemo.payment.dto.PayRequest;
import com.yuemo.payment.entity.Payment;
import com.yuemo.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public Result<Payment> pay(@RequestAttribute("userId") Long userId,
                                @Valid @RequestBody PayRequest request) {
        return Result.success(paymentService.createPayment(userId, request.getOrderId(), request.getPayType()));
    }

    @PostMapping("/callback/wechat")
    public String wechatCallback(@RequestBody Map<String, String> params) {
        return paymentService.handleCallback(1, params);
    }

    @PostMapping("/callback/alipay")
    public String alipayCallback(@RequestBody Map<String, String> params) {
        return paymentService.handleCallback(2, params);
    }

    @GetMapping("/{id}")
    public Result<Payment> detail(@PathVariable Long id) {
        return Result.success(paymentService.getPaymentById(id));
    }

    @GetMapping("/list")
    public Result<IPage<Payment>> list(@RequestAttribute("userId") Long userId,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(paymentService.listPayments(userId, page, size));
    }

    @PostMapping("/refund")
    public Result<Void> refund(@RequestAttribute("userId") Long userId,
                               @RequestParam Long orderId,
                               @RequestParam(required = false) String reason) {
        paymentService.refund(userId, orderId, reason);
        return Result.success();
    }
}
