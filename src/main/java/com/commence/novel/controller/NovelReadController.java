package com.commence.novel.controller;

import com.commence.novel.service.NovelReadService;
import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/read")
public class NovelReadController {
    @Autowired
    private NovelReadService novelReadService;

    @PostMapping
    public Result addReadRecord(
            HttpServletRequest request,
            @RequestParam Long novelId,
            @RequestParam Long chapterId) {

        // 从JWT拦截器解析的请求属性中获取当前登录用户ID
        Long userId = (Long) request.getAttribute("uid");

        return novelReadService.addReadRecord(userId, novelId, chapterId);
    }

    @GetMapping("/latest")
    public Result getLatestRead(
            HttpServletRequest request,
            @RequestParam Long novelId) {

        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法查询最新阅读记录");
        }

        return novelReadService.getLatestRead(userId, novelId);
    }

    @GetMapping("/user")
    public Result getReadRecordByUserId(HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法查询阅读记录");
        }

        return novelReadService.getReadRecordByUserId(userId);
    }

    // ========== 纯数据统计，无用户关联 ==========
    @GetMapping("/count/{novelId}")
    public Result countReadByNovelId(@PathVariable Long novelId){
        return novelReadService.countReadByNovelId(novelId);
    }

    @GetMapping("/trend")
    public Result getReadTrend(
            @RequestParam Long novelId,
            @RequestParam String timeType) {
        return novelReadService.getReadTrend(novelId, timeType);
    }

    @GetMapping("/last-chapter/{novelId}")
    public Result getLastReadChapter(@PathVariable Long novelId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法查询阅读记录");
        }
        return novelReadService.getLastReadChapterId(userId, novelId);
    }

    // 保存阅读记录
    @PostMapping("/record")
    public Result saveReadRecord(
            @RequestParam Long novelId,
            @RequestParam Long chapterId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法保存阅读记录");
        }
        return novelReadService.saveReadRecord(userId, novelId, chapterId);
    }
}