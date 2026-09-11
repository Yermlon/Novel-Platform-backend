package com.commence.novel.controller;

import com.commence.novel.DTO.CategorySortDTO;
import com.commence.novel.DTO.ShelfCategoryDTO;
import com.commence.novel.service.ShelfCategoryService;
import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/shelf/category")
public class ShelfCategoryController {
    @Autowired
    private ShelfCategoryService categoryService;

    @GetMapping("/list")
    public Result getShelfCategoryList(HttpServletRequest request) {
        // 从JWT拦截器解析的请求属性中获取当前登录用户ID
        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法查询书架分类");
        }
        return categoryService.getCategoryList(userId);
    }

    @PostMapping("/add")
    public Result addShelfCategory(
            HttpServletRequest request,
            @Valid @RequestBody ShelfCategoryDTO requestDTO) {
        // 获取JWT解析的uid
        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法新增书架分类");
        }
        // 仅传递uid和分类名称（防止伪造）
        return categoryService.addCategory(userId, requestDTO.getName());
    }

    // ========== 修改分组名称 ==========
    @PostMapping("/update/name")
    public Result updateCategoryName(
            HttpServletRequest request,
            @RequestBody Map<String, Object> params) {
        // 获取用户ID
        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法修改分组名称");
        }

        // 获取参数
        Long categoryId = params.get("categoryId") != null ? Long.valueOf(params.get("categoryId").toString()) : null;
        String newName = params.get("newName") != null ? params.get("newName").toString() : null;

        return categoryService.updateCategoryName(userId, categoryId, newName);
    }

    // ========== 调整分组排序 ==========
    @PostMapping("/adjust-sort/{userId}")
    public Result adjustCategorySort(
            @PathVariable Long userId,
            @RequestBody List<CategorySortDTO> categorySortList,
            HttpServletRequest request) {
        try {
            // 校验登录态（兜底）
            Long loginUserId = (Long) request.getAttribute("uid");
            if (loginUserId == null || !loginUserId.equals(userId)) {
                return Result.error("10086", "未登录或无权限操作");
            }
            return categoryService.adjustCategorySort(userId, categorySortList);
        } catch (Exception e) {
            log.error("批量调整分组排序异常", e);
            return Result.error("1081", "批量调整分组排序失败");
        }
    }
    // ========== 删除分组 ==========
    @PostMapping("/delete")
    public Result deleteCategory(
            HttpServletRequest request,
            @RequestBody Map<String, Object> params) {
        // 获取用户ID
        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法删除分组");
        }

        // 获取参数
        Long categoryId = params.get("categoryId") != null ? Long.valueOf(params.get("categoryId").toString()) : null;

        return categoryService.deleteCategory(userId, categoryId);
    }
}