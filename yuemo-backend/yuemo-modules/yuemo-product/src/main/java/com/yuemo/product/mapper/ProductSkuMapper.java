package com.yuemo.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuemo.product.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    @Update("UPDATE yu_product_sku SET stock = stock - #{quantity} " +
            "WHERE id = #{id} AND deleted = 0 AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
