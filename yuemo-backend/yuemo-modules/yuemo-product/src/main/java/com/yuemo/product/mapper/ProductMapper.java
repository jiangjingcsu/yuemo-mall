package com.yuemo.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuemo.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT id, name, category_id, brand_id, title, description, " +
            "price, stock, main_image, images, sales, status " +
            "FROM yu_product WHERE id = #{id} AND deleted = 0")
    Product selectProductById(@Param("id") Long id);

    @Update("UPDATE yu_product SET stock = stock - #{quantity} " +
            "WHERE id = #{id} AND deleted = 0 AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Update("UPDATE yu_product SET stock = stock + #{quantity} " +
            "WHERE id = #{id} AND deleted = 0")
    int restoreStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
