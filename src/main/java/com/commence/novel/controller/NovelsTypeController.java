package com.commence.novel.controller;

import com.commence.novel.entity.NovelsType;
import com.commence.novel.service.NovelsTypeService;
import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/novelsType")
public class NovelsTypeController {

    @Autowired
    private NovelsTypeService novelsTypeService;

    // 复用你已有的权限校验工具方法
    private Long getCurrentUserId(HttpServletRequest request) {
        Object uidObj = request.getAttribute("uid");
        return uidObj instanceof Long ? (Long) uidObj : null;
    }

    private String getCurrentUserRole(HttpServletRequest request) {
        Object roleObj = request.getAttribute("role");
        return roleObj instanceof String ? (String) roleObj : null;
    }

    // 新增小说类型（仅管理员）
    @PostMapping("/add")
    public Result addNovelsType(@RequestBody NovelsType novelsType, HttpServletRequest request) {
        // 登录校验
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }

        // 管理员权限校验
        String userRole = getCurrentUserRole(request);
        if (!"ADMIN".equals(userRole)) {
            return Result.error("1007", "无权限新增小说类型（仅管理员可操作）");
        }

        return novelsTypeService.addNovelsType(novelsType);
    }

    // 编辑小说类型（仅管理员）
    @PostMapping("/edit/{typeId}")
    public Result editNovelsType(@PathVariable Integer typeId, @RequestBody NovelsType novelsType, HttpServletRequest request) {
        // 登录校验
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }

        // 管理员权限校验
        String userRole = getCurrentUserRole(request);
        if (!"ADMIN".equals(userRole)) {
            return Result.error("1007", "无权限编辑小说类型（仅管理员可操作）");
        }

        return novelsTypeService.editNovelsType(typeId, novelsType);
    }

    // 修改小说类型状态（仅管理员）
    @PostMapping("/changeStatus/{typeId}")
    public Result changeNovelsTypeStatus(@PathVariable Integer typeId, @RequestParam Integer status, HttpServletRequest request) {
        // 登录校验
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }

        // 管理员权限校验
        String userRole = getCurrentUserRole(request);
        if (!"ADMIN".equals(userRole)) {
            return Result.error("1007", "无权限修改小说类型状态（仅管理员可操作）");
        }

        return novelsTypeService.changeNovelsTypeStatus(typeId, status);
    }

    // 查询所有小说类型（管理员可见全部，普通用户仅可见启用的）
    @GetMapping("/listAll")
    public Result listAllNovelsType(HttpServletRequest request) {
        // 登录校验（仅管理员可看全部，普通用户走原有接口）
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }

        String userRole = getCurrentUserRole(request);
        if (!"ADMIN".equals(userRole)) {
            return Result.error("1007", "无权限查看所有小说类型（仅管理员可操作）");
        }

        return novelsTypeService.listAllNovelsType();
    }

}