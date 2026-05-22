package com.yuemo.user.vo;

import com.yuemo.user.entity.Address;

import java.time.LocalDateTime;

public record AddressVO(
    Long id,
    Long userId,
    String receiverName,
    String receiverPhone,
    String province,
    String city,
    String district,
    String detail,
    String zipCode,
    Boolean isDefault,
    LocalDateTime createTime
) {

    public static AddressVO from(Address address) {
        return new AddressVO(
                address.getId(),
                address.getUserId(),
                address.getReceiverName(),
                address.getReceiverPhone(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetail(),
                address.getZipCode(),
                address.getIsDefault(),
                address.getCreateTime()
        );
    }
}
