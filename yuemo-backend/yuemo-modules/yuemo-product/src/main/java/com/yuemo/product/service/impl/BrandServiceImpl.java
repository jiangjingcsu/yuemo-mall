package com.yuemo.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuemo.common.core.exception.BusinessException;
import com.yuemo.common.core.response.ResultCode;
import com.yuemo.product.entity.Brand;
import com.yuemo.product.mapper.BrandMapper;
import com.yuemo.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandMapper brandMapper;

    @Override
    public List<Brand> listAll() {
        return brandMapper.selectList(new LambdaQueryWrapper<Brand>()
                .eq(Brand::getStatus, 1)
                .orderByAsc(Brand::getSortOrder));
    }

    @Override
    public Brand getById(Long id) {
        Brand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new BusinessException(ResultCode.BRAND_NOT_FOUND);
        }
        return brand;
    }

    @Override
    public void create(Brand brand) {
        brandMapper.insert(brand);
    }

    @Override
    public void update(Long id, Brand brand) {
        brand.setId(id);
        brandMapper.updateById(brand);
    }
}
