package com.commence.novel.controller;

import com.commence.novel.DTO.NovelCommentDTO;
import com.commence.novel.service.NovelCommentService;
import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest; // 新增：获取JWT解析的uid
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
public class NovelCommentController {
    @Autowired
    private NovelCommentService novelCommentService;

    // 从Request获取JWT解析的当前登录用户ID
    private Long getCurrentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("uid");
    }
    

    // ========== 公开接口（查询评论列表，无需登录） ==========
    @GetMapping("/list/{novelId}")
    public Result getCommentList(@PathVariable("novelId") Long novelId, @RequestParam(required = false, defaultValue = "all") String filter) {
        return novelCommentService.getCommentList(novelId,filter);
    }

    // ========== 发布评论 ==========
    @PostMapping("/novel/{novelId}")
    public Result addComment(
            @PathVariable Long novelId,
            @RequestParam String content,
            @RequestParam(required = false) Long parentId,
            HttpServletRequest request) {
        // 获取JWT解析的用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "未登录，无法发布评论");
        }
        // 调用Service
        return novelCommentService.addComment(userId, novelId, content, parentId);
    }

    // ========== 点赞评论 ==========
    @PostMapping("/like/{commentId}")
    public Result likeComment(@PathVariable Long commentId, HttpServletRequest request) {
        if (commentId == null) {
            return Result.error("1060","评论ID不能为空");
        }
        // 获取JWT解析的用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "未登录，无法点赞评论");
        }
        return novelCommentService.likeComment(commentId, userId);
    }

    // ========== 获取已点赞评论ID ==========
    @GetMapping("/liked/list/{novelId}")
    public Result getLikeCommentIds(@PathVariable Long novelId, HttpServletRequest request) {
        if (novelId == null) {
            return Result.error("1020","小说ID不能为空");
        }
        // 获取JWT解析的用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "未登录，无法查询点赞记录");
        }
        List<Long> likedIds = novelCommentService.getLikedCommentIds(novelId, userId);
        return Result.success((Object) likedIds);
    }

    // ========== 取消点赞 ==========
    @DeleteMapping("/like/{commentId}")
    public Result cancelComment(@PathVariable Long commentId, HttpServletRequest request) {
        // 获取JWT解析的用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "未登录，无法取消点赞");
        }
        return novelCommentService.cancelLikeComment(commentId, userId);
    }

    // ========== 删除评论 ==========
    @DeleteMapping("/{commentId}")
    public Result deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long novelId,
            HttpServletRequest request) {
        if (commentId == null) {
            return Result.error("1020", "评论ID不能为空");
        }
        if (novelId == null) {
            return Result.error("1020", "小说ID不能为空");
        }
        // 获取JWT解析的用户ID
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "未登录，无法删除评论");
        }
        // 注意：Service的deleteComment参数顺序是(novelId, userId, commentId)，需对齐
        return novelCommentService.deleteComment(novelId, userId, commentId);
    }

    // ========== 公开接口（统计评论数，无需登录） ==========
    @GetMapping("/count/{novelId}")
    public Result countCommentsByNovelId(@PathVariable Long novelId) {
        return novelCommentService.countCommentsByNovelId(novelId);
    }

    @GetMapping("/my/list")
    public Result getMyCommentList(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "未登录");
        }
        List<NovelCommentDTO> list = novelCommentService.getMyCommentList(userId);
        return Result.success(list, "查询成功");
    }

}