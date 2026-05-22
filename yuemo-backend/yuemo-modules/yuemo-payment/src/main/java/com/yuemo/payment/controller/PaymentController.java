package com.yuemo.payment.controller;

import com.yuemo.common.core.response.PageResult;
import com.yuemo.common.core.response.Result;
import com.yuemo.payment.dto.PayRequest;
import com.yuemo.payment.dto.RefundRequest;
import com.yuemo.payment.service.PaymentService;
import com.yuemo.payment.vo.PaymentVO;
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
    public Result<PaymentVO> pay(@RequestAttribute("userId") Long userId,
                                  @Valid @RequestBody PayRequest request) {
        return Result.success(PaymentVO.from(paymentService.createPayment(userId, request.getOrderId(), request.getPayType())));
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
    public Result<PaymentVO> detail(@RequestAttribute("userId") Long userId,
                                     @PathVariable Long id) {
        return Result.success(PaymentVO.from(paymentService.getPaymentByIdAndUserId(id, userId)));
    }

    @GetMapping("/list")
    public Result<PageResult<PaymentVO>> list(@RequestAttribute("userId") Long userId,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(PageResult.from(paymentService.listPayments(userId, page, size).convert(PaymentVO::from)));
    }

    @PostMapping("/refund")
    public Result<Void> refund(@RequestAttribute("userId") Long userId,
                               @Valid @RequestBody RefundRequest request) {
        paymentService.refund(userId, request.getOrderId(), request.getReason());
        return Result.success();
    }
}
