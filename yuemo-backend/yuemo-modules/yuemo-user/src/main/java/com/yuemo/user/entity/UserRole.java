package com.yuemo.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuemo.common.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("yu_user_role")
public class UserRole extends BaseEntity {

    private Long userId;
    private String role;
}
