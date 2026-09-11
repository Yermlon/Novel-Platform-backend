package com.commence.novel.DTO;

import lombok.Data;

@Data
public class CategorySortDTO {
    private Long categoryId; // 分组ID
    private Integer sort;    // 排序值
}
