package com.yuemo.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuemo.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE yu_user SET balance = balance - #{amount} " +
            "WHERE id = #{userId} AND deleted = 0 AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    @Update("UPDATE yu_user SET balance = balance + #{amount} " +
            "WHERE id = #{userId} AND deleted = 0")
    int addBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
