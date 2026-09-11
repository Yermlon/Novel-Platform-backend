package com.commence.novel.controller;

import com.commence.novel.entity.Announcement;
import com.commence.novel.service.AnnouncementService;
import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest; // 替换 HttpSession 为 HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/announcement")
public class AnnouncementController {
    @Autowired
    private AnnouncementService announcementService;

    // 从JWT解析的Request属性中获取当前登录用户ID
    private Long getCurrentUserId(HttpServletRequest request) {
        Object uidObj = request.getAttribute("uid");
        return uidObj instanceof Long ? (Long) uidObj : null;
    }

    // 从JWT解析的Request属性中获取用户角色（需JWT拦截器将role存入Request）
    private String getCurrentUserRole(HttpServletRequest request) {
        Object roleObj = request.getAttribute("role");
        return roleObj instanceof String ? (String) roleObj : null;
    }

    // ========== 公开接口（所有用户可查询最新公告） ==========
    @GetMapping("/latest")
    public Result latestAnnouncement() {
        return announcementService.getLatestPublishedAnnouncement();
    }

    @GetMapping("/list")
    public Result getAllAnnouncementList(HttpServletRequest request) {
        // 权限校验：仅管理员可查看
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }
        String userRole = getCurrentUserRole(request);
        if (!"ADMIN".equals(userRole)) {
            return Result.error("1007", "无权限查看公告列表");
        }
        return announcementService.getAllAnnouncementList();
    }

    // ==========  发布公告（JWT 替换 Session） ==========
    @PostMapping("/publish")
    public Result publishAnnouncement(@RequestBody Announcement announcement, HttpServletRequest request) {
        // 1. 校验登录状态
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }

        // 2. 校验管理员权限
        String userRole = getCurrentUserRole(request);
        if (!"ADMIN".equals(userRole)) {
            return Result.error("1007", "无权限发布公告（仅管理员可操作）");
        }

        return announcementService.publishAnnouncement(announcement);
    }

    // ========== 编辑公告（JWT 替换 Session + 补充参数校验） ==========
    @PostMapping("/edit/{id}")
    public Result editAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement, HttpServletRequest request) {
        // 1. 校验登录状态
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }

        // 2. 校验管理员权限
        String userRole = getCurrentUserRole(request);
        if (!"ADMIN".equals(userRole)) {
            return Result.error("1007", "无权限编辑公告（仅管理员可操作）");
        }

        // 3. 补充参数校验
        if (id == null) {
            return Result.error("1061", "公告ID不能为空");
        }
        return announcementService.editAnnouncement(id, announcement);
    }

    // ========== 删除公告（JWT 替换 Session + 补充参数校验） ==========
    @DeleteMapping("/delete/{id}")
    public Result deleteAnnouncement(@PathVariable Long id, HttpServletRequest request) {
        // 1. 校验登录状态
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }

        // 2. 校验管理员权限
        String userRole = getCurrentUserRole(request);
        if (!"ADMIN".equals(userRole)) {
            return Result.error("1007", "无权限删除公告（仅管理员可操作）");
        }

        // 3. 补充参数校验
        if (id == null) {
            return Result.error("1061", "公告ID不能为空");
        }
        return announcementService.deleteAnnouncement(id);
    }
}