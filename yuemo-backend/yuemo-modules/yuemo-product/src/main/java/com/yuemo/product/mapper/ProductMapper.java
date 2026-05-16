package com.yuemo.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuemo.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT id, name, category_id, brand_id, title, description, " +
            "price, stock, main_image, images, sales, status " +
            "FROM yu_product WHERE id = #{id} AND deleted = 0")
    Product selectProductById(@Param("id") Long id);
}
