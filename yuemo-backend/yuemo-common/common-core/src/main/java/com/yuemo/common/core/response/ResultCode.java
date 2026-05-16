package com.yuemo.common.core.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "数据冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误码 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_ALREADY_EXISTS(1003, "用户已存在"),
    USER_TOKEN_EXPIRED(1004, "Token 已过期"),

    // 业务错误码 2xxx
    PRODUCT_NOT_FOUND(2001, "商品不存在"),
    PRODUCT_STOCK_INSUFFICIENT(2002, "库存不足"),
    CATEGORY_NOT_FOUND(2003, "分类不存在"),

    // 业务错误码 3xxx
    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_STATUS_ERROR(3002, "订单状态异常"),
    ORDER_CREATE_FAILED(3003, "订单创建失败"),

    // 业务错误码 4xxx
    PAYMENT_FAILED(4001, "支付失败"),
    PAYMENT_CALLBACK_ERROR(4002, "支付回调处理异常"),
    REFUND_FAILED(4003, "退款失败"),

    // 业务错误码 5xxx
    CART_ITEM_NOT_FOUND(5001, "购物车商品不存在"),
    CART_ITEM_LIMIT_EXCEEDED(5002, "购物车商品数量超限"),

    // 业务错误码 6xxx
    COUPON_NOT_FOUND(6001, "优惠券不存在"),
    COUPON_EXPIRED(6002, "优惠券已过期"),
    COUPON_USED(6003, "优惠券已使用"),

    // 业务错误码 2xxx 扩展
    SKU_NOT_FOUND(2004, "SKU不存在"),
    SKU_STOCK_INSUFFICIENT(2005, "SKU库存不足"),
    BRAND_NOT_FOUND(2006, "品牌不存在"),
    SPEC_TEMPLATE_NOT_FOUND(2007, "规格模板不存在");

    private final int code;
    private final String message;
}
