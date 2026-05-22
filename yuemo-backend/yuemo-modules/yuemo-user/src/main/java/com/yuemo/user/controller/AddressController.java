package com.yuemo.user.controller;

import com.yuemo.common.core.response.Result;
import com.yuemo.user.entity.Address;
import com.yuemo.user.service.AddressService;
import com.yuemo.user.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/list")
    public Result<List<AddressVO>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(addressService.listAddresses(userId).stream().map(AddressVO::from).toList());
    }

    @GetMapping("/{id}")
    public Result<AddressVO> detail(@PathVariable Long id) {
        return Result.success(AddressVO.from(addressService.getAddressById(id)));
    }

    @PostMapping
    public Result<Void> create(@RequestAttribute("userId") Long userId,
                               @RequestBody Address address) {
        addressService.createAddress(userId, address);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id,
                               @RequestBody Address address) {
        addressService.updateAddress(userId, id, address);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("userId") Long userId,
                               @PathVariable Long id) {
        addressService.deleteAddress(userId, id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@RequestAttribute("userId") Long userId,
                                   @PathVariable Long id) {
        addressService.setDefault(userId, id);
        return Result.success();
    }
}
