package com.yuemo.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuemo.product.entity.ProductTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductTagMapper extends BaseMapper<ProductTag> {

    @Select("SELECT t.id, t.name, t.color FROM yu_product_tag t " +
            "INNER JOIN yu_product_tag_rel r ON t.id = r.tag_id " +
            "WHERE r.product_id = #{productId} AND t.deleted = 0 ORDER BY t.sort_order")
    List<Map<String, Object>> selectTagsByProductId(@Param("productId") Long productId);
}
