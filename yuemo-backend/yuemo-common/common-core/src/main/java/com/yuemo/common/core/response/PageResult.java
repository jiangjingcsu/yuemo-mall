package com.yuemo.common.core.response;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public record PageResult<T>(long total, int page, int size, List<T> list) {

    public static <T> PageResult<T> from(IPage<T> iPage) {
        return new PageResult<>(
                iPage.getTotal(),
                (int) iPage.getCurrent(),
                (int) iPage.getSize(),
                iPage.getRecords()
        );
    }
}
