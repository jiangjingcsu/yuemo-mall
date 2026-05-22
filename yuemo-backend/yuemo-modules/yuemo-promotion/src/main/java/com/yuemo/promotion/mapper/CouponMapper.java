package com.yuemo.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuemo.promotion.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    @Update("UPDATE yu_coupon SET received_count = received_count + 1 " +
            "WHERE id = #{id} AND deleted = 0 AND received_count < total_count")
    int incrementReceivedCount(@Param("id") Long id);
}
