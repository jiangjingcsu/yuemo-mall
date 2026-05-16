package com.yuemo.payment.controller;

import com.yuemo.common.core.response.Result;
import com.yuemo.payment.entity.Payment;
import com.yuemo.payment.service.PaymentService;
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
                                @RequestParam Long orderId) {
        return Result.success(paymentService.createPayment(userId, orderId));
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

    @PostMapping("/refund")
    public Result<Void> refund(@RequestAttribute("userId") Long userId,
                               @RequestParam Long orderId) {
        paymentService.refund(userId, orderId);
        return Result.success();
    }
}
