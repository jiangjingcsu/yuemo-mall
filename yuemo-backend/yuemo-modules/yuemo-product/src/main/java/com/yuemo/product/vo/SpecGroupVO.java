package com.yuemo.product.vo;

import java.util.List;

public record SpecGroupVO(
    Long templateId,
    String name,
    List<SpecValueVO> values
) {
    public record SpecValueVO(Long id, String value) {}
}
