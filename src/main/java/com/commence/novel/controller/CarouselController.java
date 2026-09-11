package com.commence.novel.controller;

import com.commence.novel.entity.Carousel;
import com.commence.novel.service.CarouselService;
import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carousel")
public class CarouselController {
    @Autowired
    private CarouselService carouselService;

    // 从JWT解析的Request属性中获取当前登录用户ID
    private Long getCurrentUserId(HttpServletRequest request) {
        Object uidObj = request.getAttribute("uid");
        return uidObj instanceof Long ? (Long) uidObj : null;
    }

    // 从JWT解析的Request属性中获取用户角色
    private String getCurrentUserRole(HttpServletRequest request) {
        Object roleObj = request.getAttribute("role");
        return roleObj instanceof String ? (String) roleObj : null;
    }

    // ==========  管理端查询所有轮播图 ==========
    @GetMapping("/list")
    public Result getAllCarouselList() {
        return carouselService.getAllCarouselList();
    }

    // ==========  新增轮播图  ==========
    @PostMapping("/add")
    public Result addCarousel(@RequestBody Carousel carousel, HttpServletRequest request) {
        //   校验登录状态
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }

        //   校验管理员权限（仅ADMIN角色可操作，修复原反向逻辑）
        String userRole = getCurrentUserRole(request);
        if (!"ADMIN".equals(userRole)) {
            return Result.error("1007", "无权限新增轮播图（仅管理员可操作）");
        }

        return carouselService.addCarousel(carousel);
    }

    // ==========  编辑轮播图  ==========
    @PostMapping("/edit/{id}")
    public Result editCarousel(@PathVariable Long id, @RequestBody Carousel carousel, HttpServletRequest request) {
        //   校验登录状态
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }

        //  校验管理员权限
        String userRole = getCurrentUserRole(request);
        if (!"ADMIN".equals(userRole)) {
            return Result.error("1007", "无权限编辑轮播图（仅管理员可操作）");
        }

        // 补充参数校验
        if (id == null) {
            return Result.error("1051", "轮播图ID不能为空");
        }
        return carouselService.editCarousel(id, carousel);
    }

    // ==========  删除轮播图  ==========
    @DeleteMapping("/delete/{id}")
    public Result deleteCarousel(@PathVariable Long id, HttpServletRequest request) {
        //   校验登录状态
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }

        //   校验管理员权限
        String userRole = getCurrentUserRole(request);
        if (!"ADMIN".equals(userRole)) {
            return Result.error("1007", "无权限删除轮播图（仅管理员可操作）");
        }

        // 补充参数校验
        if (id == null) {
            return Result.error("1051", "轮播图ID不能为空");
        }
        return carouselService.deleteCarousel(id);
    }
}