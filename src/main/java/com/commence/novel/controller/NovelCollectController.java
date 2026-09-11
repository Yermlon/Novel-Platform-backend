package com.commence.novel.controller;

import com.commence.novel.service.NovelCollectService;
import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/collect")
public class NovelCollectController {
    @Autowired
    private NovelCollectService novelCollectService;

    // 从Request获取JWT解析的当前登录用户ID
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("uid");
    }

    // ========== 查询收藏状态 ==========
    @GetMapping("/status/{novelId}")
    public Result getCollectStatus(@PathVariable Long novelId, HttpServletRequest request) {
        if (novelId == null) {
            return Result.error("1030", "小说ID不能为空");
        }
        // 获取JWT解析的用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "未登录，无法查询收藏状态");
        }
        return novelCollectService.isCollected(userId, novelId);
    }

    // ========== 新增收藏 ==========
    @PostMapping("/{novelId}")
    public Result addcollect(
            @PathVariable Long novelId,
            @RequestParam Long categoryId,
            HttpServletRequest request) {
        if (novelId == null) {
            return Result.error("1030", "小说ID不能为空");
        }
        if (categoryId == null) {
            return Result.error("1060", "收藏分类ID不能为空");
        }
        // 获取JWT解析的用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "未登录，无法收藏小说");
        }
        return novelCollectService.collectNovel(userId, novelId, categoryId);
    }

    // ========== 取消收藏 ==========
    @DeleteMapping("/{novelId}")
    public Result delcollect(@PathVariable Long novelId, HttpServletRequest request) {
        if (novelId == null) {
            return Result.error("1030", "小说ID不能为空");
        }
        // 获取JWT解析的用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "未登录，无法取消收藏");
        }
        return novelCollectService.cancelCollect(userId, novelId);
    }

    // ========== 查询用户所有收藏 ==========
    @GetMapping("/user")
    public Result getUserCollect(HttpServletRequest request) {
        // 获取JWT解析的用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "未登录，无法查询收藏列表");
        }
        return novelCollectService.getCollectsByUserId(userId);
    }

    // ========== 公开接口（统计小说收藏数，无需登录） ==========
    @GetMapping("/count/{novelId}")
    public Result getCollectsByNovelId(@PathVariable Long novelId) {
        if (novelId == null) {
            return Result.error("1030", "小说ID不能为空");
        }
        return novelCollectService.getCollectCountByNovelId(novelId);
    }

    @GetMapping("/list")
    public Result getNovelList(
            @RequestParam Long userId,
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "collect") String sortType,
            HttpServletRequest request) {
        // 校验用户ID
        Long currentUserId = (Long) request.getAttribute("uid");
        if (currentUserId == null || !currentUserId.equals(userId)) {
            return Result.error("1006", "未登录或无权限查询");
        }

        return novelCollectService.getCollectsByUserIdAndCategoryId(userId, categoryId, sortType);
    }
}