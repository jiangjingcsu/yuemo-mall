package com.yuemo.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuemo.cart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {

    void upsertOnDuplicateKey(@Param("userId") Long userId,
                              @Param("productId") Long productId,
                              @Param("skuId") Long skuId,
                              @Param("quantity") Integer quantity);
}
