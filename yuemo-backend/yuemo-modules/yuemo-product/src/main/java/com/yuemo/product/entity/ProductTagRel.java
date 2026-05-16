package com.yuemo.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("yu_product_tag_rel")
public class ProductTagRel {
    private Long id;
    private Long productId;
    private Long tagId;
    private LocalDateTime createTime;
}
