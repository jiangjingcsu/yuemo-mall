package com.yuemo.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.user.entity.Address;
import com.yuemo.user.mapper.AddressMapper;
import com.yuemo.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    public List<Address> listAddresses(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreateTime));
    }

    @Override
    public Address getAddressById(Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "地址不存在");
        }
        return address;
    }

    @Override
    public void createAddress(Long userId, Address address) {
        long count = addressMapper.selectCount(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId));
        address.setUserId(userId);
        address.setIsDefault(count == 0); // 第一个地址设为默认
        addressMapper.insert(address);
    }

    @Override
    public void updateAddress(Long userId, Long addressId, Address address) {
        Address existing = getAddressById(addressId);
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        address.setId(addressId);
        addressMapper.updateById(address);
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        Address existing = getAddressById(addressId);
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        addressMapper.deleteById(addressId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long addressId) {
        // 取消所有默认
        List<Address> addresses = addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, true));
        for (Address addr : addresses) {
            addr.setIsDefault(false);
            addressMapper.updateById(addr);
        }

        // 设置新默认
        Address address = getAddressById(addressId);
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        address.setIsDefault(true);
        addressMapper.updateById(address);
    }
}
