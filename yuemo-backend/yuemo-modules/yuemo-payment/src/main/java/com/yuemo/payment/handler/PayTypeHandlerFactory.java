package com.yuemo.payment.handler;

import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PayTypeHandlerFactory {

    private final Map<Integer, PayTypeHandler> handlerMap;

    public PayTypeHandlerFactory(List<PayTypeHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(PayTypeHandler::payType, Function.identity()));
    }

    public PayTypeHandler get(Integer payType) {
        PayTypeHandler handler = handlerMap.get(payType);
        if (handler == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的支付方式: " + payType);
        }
        return handler;
    }
}
