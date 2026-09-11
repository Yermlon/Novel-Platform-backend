package com.commence.novel.service;

import com.commence.novel.DTO.CategorySortDTO;
import com.commence.novel.utils.Result;

import java.util.List;

public interface ShelfCategoryService {
    //获取书架列表
    Result getCategoryList(Long userId);

    //新增上架分类
    Result addCategory(Long userId, String categoryName);

    // 修改分组名称（新增）
    Result updateCategoryName(Long userId, Long categoryId, String newName);

    // 调整分组排序（新增）
    Result adjustCategorySort(Long userId, List<CategorySortDTO> categorySortList);

    // 删除分组（新增，删除后小说移入默认分组）
    Result deleteCategory(Long userId, Long categoryId);
}
