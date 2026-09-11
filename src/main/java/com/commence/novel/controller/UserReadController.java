package com.commence.novel.controller;

import com.commence.novel.DTO.UserReadProgressDTO;
import com.commence.novel.DTO.UserReadSettingDTO;
import com.commence.novel.service.UserReadService;
import com.commence.novel.utils.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/read")
public class UserReadController {
    @Resource
    private UserReadService userReadService;

    @GetMapping("/setting")
    public Result<UserReadSettingDTO> getReadSetting(HttpServletRequest request) {
        // 从JWT拦截器解析的请求属性中获取当前登录用户ID（Long类型）
        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法获取阅读设置");
        }
        return userReadService.getReadSetting(userId);
    }

    @PostMapping("/setting")
    public Result<Void> saveReadSetting(
            HttpServletRequest request,
            @RequestBody UserReadSettingDTO dto) {
        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法保存阅读设置");
        }
        return userReadService.saveReadSetting(userId, dto);
    }

    @GetMapping("/progress")
    public Result<UserReadProgressDTO> getProgress(
            HttpServletRequest request,
            @RequestParam String novelId // 仅保留novelId参数
    ) {
        Long userId = (Long) request.getAttribute("uid");
        System.out.println("=== getProgress接口uid值：" + userId + "，novelId：" + novelId);
        if (userId == null) {
            return Result.error("10086", "未登录，无法获取阅读进度");
        }
        return userReadService.getProgress(userId, novelId);
    }

    @PostMapping("/progress")
    public Result<Void> saveProgress(
            HttpServletRequest request,
            @RequestBody UserReadProgressDTO dto) {
        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法保存阅读进度");
        }
        return userReadService.saveProgress(userId, dto);
    }

    @GetMapping("/last-progress")
    public Result<UserReadProgressDTO> getLastReadProgress(
            HttpServletRequest request,
            @RequestParam String novelId // 仅传小说ID，用户ID从JWT解析
    ) {
        // 从JWT拦截器获取当前登录用户ID
        Long userId = (Long) request.getAttribute("uid");
        if (userId == null) {
            return Result.error("10086", "未登录，无法获取最后阅读进度");
        }
        return userReadService.getLastReadProgress(userId, novelId);
    }
}