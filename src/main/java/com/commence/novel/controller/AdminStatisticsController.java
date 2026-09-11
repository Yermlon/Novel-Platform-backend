package com.commence.novel.controller;

import com.commence.novel.DTO.UserDTO;
import com.commence.novel.DTO.StatisticsDTO;
import com.commence.novel.service.AdminStatisticsService;
import com.commence.novel.service.UserService;
import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员数据统计接口（仅ADMIN角色可访问）
 * 核心：复用getCurrentUser逻辑，保证登录态校验统一
 */
@RestController
@RequestMapping("/admin/statistics")
public class AdminStatisticsController {
    private static final Logger log = LoggerFactory.getLogger(AdminStatisticsController.class);

    @Autowired
    private AdminStatisticsService adminStatisticsService;

    // 简化权限校验：直接用拦截器解析的role，无需调用userService.getCurrentUser
    private void checkAdminPermission(HttpServletRequest request) {
        try {
            // 1. 校验登录态（拦截器已解析uid）
            Long uid = (Long) request.getAttribute("uid");
            if (uid == null) {
                throw new RuntimeException("未登录(游客状态)");
            }

            // 2. 直接获取拦截器解析的role（无需查库）
            String role = (String) request.getAttribute("role");
            log.info("当前用户 uid={}, role={}", uid, role);

            // 3. 校验管理员角色
            if (!"ADMIN".equals(role)) {
                throw new RuntimeException("无管理员权限，禁止访问统计接口");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("权限校验失败：" + e.getMessage());
        }
    }

    @GetMapping("/platform")
    public Result getPlatformStatistics(HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            return adminStatisticsService.getPlatformStatistics();
        } catch (RuntimeException e) {
            log.error("获取平台统计数据失败：{}", e.getMessage());
            return Result.error("10086", e.getMessage());
        } catch (Exception e) {
            log.error("平台统计接口异常", e);
            return Result.error("500", "服务器内部错误，获取统计数据失败");
        }
    }

    @GetMapping("/user/role")
    public Result countUserByRole(HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            return adminStatisticsService.countUserByRole();
        } catch (RuntimeException e) {
            log.error("按角色统计用户数失败：{}", e.getMessage());
            return Result.error("10086", e.getMessage());
        } catch (Exception e) {
            log.error("角色统计接口异常", e);
            return Result.error("500", "服务器内部错误，按角色统计失败");
        }
    }

    @GetMapping("/visit")
    public Result getTotalVisit(HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            Result visitResult = adminStatisticsService.getPlatformStatistics();
            StatisticsDTO statsDTO = (StatisticsDTO) visitResult.getData();
            return Result.success(statsDTO.getVisitData(), "获取访问量数据成功");
        } catch (RuntimeException e) {
            log.error("获取访问量数据失败：{}", e.getMessage());
            return Result.error("10086", e.getMessage());
        } catch (Exception e) {
            log.error("访问量统计接口异常", e);
            return Result.error("500", "服务器内部错误，获取访问量失败");
        }
    }
}