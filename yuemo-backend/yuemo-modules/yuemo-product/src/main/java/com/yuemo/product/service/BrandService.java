package com.yuemo.product.service;

import com.yuemo.product.entity.Brand;

import java.util.List;

public interface BrandService {
    List<Brand> listAll();
    Brand getById(Long id);
    void create(Brand brand);
    void update(Long id, Brand brand);
}
