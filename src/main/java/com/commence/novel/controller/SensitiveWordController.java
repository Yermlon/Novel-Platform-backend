package com.commence.novel.controller;

import com.commence.novel.service.SensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/sensitive")
public class SensitiveWordController {
    @Autowired
    private SensitiveWordService sensitiveWordService;

    private String getCurrentUserRole(HttpServletRequest request) {
        return (String) request.getAttribute("role");
    }

    @PostMapping("/add")
    public Result addSensitiveWord(@RequestParam String word, HttpServletRequest request) {
        // 校验管理员权限
        String role = getCurrentUserRole(request);
        if (!"ADMIN".equals(role)) {
            return Result.error("1007", "无权限");
        }
        return sensitiveWordService.addSensitiveWord(word); // 调用Service
    }

    @GetMapping("/list")
    public Result getSensitiveWordList(HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        if (!"ADMIN".equals(role)) {
            return Result.error("1007", "无权限");
        }
        return sensitiveWordService.getSensitiveWordList(); // 调用Service
    }

    // 删除敏感词接口
    @DeleteMapping("/delete")
    public Result deleteSensitiveWord(@RequestParam String word, HttpServletRequest request) {
        String role = getCurrentUserRole(request);
        if (!"ADMIN".equals(role)) {
            return Result.error("1007", "无权限");
        }
        return sensitiveWordService.deleteSensitiveWord(word);
    }
}
