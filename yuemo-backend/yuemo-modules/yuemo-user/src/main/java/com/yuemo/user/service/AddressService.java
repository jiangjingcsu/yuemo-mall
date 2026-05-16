package com.yuemo.user.service;

import com.yuemo.user.entity.Address;

import java.util.List;

public interface AddressService {

    List<Address> listAddresses(Long userId);

    Address getAddressById(Long id);

    void createAddress(Long userId, Address address);

    void updateAddress(Long userId, Long addressId, Address address);

    void deleteAddress(Long userId, Long addressId);

    void setDefault(Long userId, Long addressId);
}
