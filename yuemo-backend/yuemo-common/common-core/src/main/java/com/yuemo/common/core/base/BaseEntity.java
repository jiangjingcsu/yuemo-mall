package com.yuemo.common.core.base;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity 基类
 */
@Getter
@Setter
public abstract class BaseEntity implements Serializable {

    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean deleted = false;
}
