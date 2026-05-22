package com.yuemo.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.order.enums.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_order")
public class Order extends BaseEntity {

    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer status;
    private Long addressId;
    private String remark;
    private String logisticsCompany;
    private String logisticsNo;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;

    @TableField(exist = false)
    private List<OrderItem> items;

    public OrderStatus getStatusEnum() {
        return status != null ? OrderStatus.fromCode(status) : null;
    }

    public void verifyOwnership(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    public void ensureStatus(OrderStatus expected) {
        if (this.status == null || this.status != expected.getCode()) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR);
        }
    }

    public void pay() {
        ensureStatus(OrderStatus.UNPAID);
        this.status = OrderStatus.PAID.getCode();
        this.payTime = LocalDateTime.now();
    }

    public void ship(String logisticsCompany, String logisticsNo) {
        ensureStatus(OrderStatus.PAID);
        this.status = OrderStatus.SHIPPED.getCode();
        this.logisticsCompany = logisticsCompany;
        this.logisticsNo = logisticsNo;
        this.deliveryTime = LocalDateTime.now();
    }

    public void confirmReceive() {
        ensureStatus(OrderStatus.SHIPPED);
        this.status = OrderStatus.COMPLETED.getCode();
        this.receiveTime = LocalDateTime.now();
    }

    public void cancel() {
        ensureStatus(OrderStatus.UNPAID);
        this.status = OrderStatus.CANCELLED.getCode();
    }

    public boolean canDelete() {
        return this.status != null
                && (this.status == OrderStatus.COMPLETED.getCode()
                || this.status == OrderStatus.CANCELLED.getCode());
    }
}
