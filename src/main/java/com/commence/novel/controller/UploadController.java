package com.commence.novel.controller;

import com.commence.novel.enums.UploadScene;
import com.commence.novel.utils.JwtUtil;
import com.commence.novel.utils.LocalFileUploadUtil;
import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @Autowired
    private LocalFileUploadUtil localFileUploadUtil;

    @Autowired
    private JwtUtil jwtUtil;

    // 读者端：头像上传
    @PostMapping("/avatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token == null || token.isBlank()) {
                return Result.error("1006", "请先登录");
            }
            if (token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }

            Long userId = null;
            try {
                // 用注入的 jwtUtil 实例调用方法
                userId = jwtUtil.getUidFromToken(token);
            } catch (Exception e) {
                return Result.error("1006", "登录已过期，请重新登录");
            }

            if (userId == null) {
                return Result.error("1006", "请先登录");
            }

            String url = localFileUploadUtil.upload(file, UploadScene.USER_AVATAR);
            return Result.success(new UploadResponse(url), "头像上传成功");
        } catch (IllegalArgumentException e) {
            return Result.error("2001", e.getMessage());
        } catch (Exception e) {
            return Result.error("2001", "头像上传失败：" + e.getMessage());
        }
    }

    // 作者端：封面上传
    @PostMapping("/novelCover")
    public Result uploadNovelCover(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            String url = localFileUploadUtil.upload(file, UploadScene.NOVEL_COVER);
            return Result.success(new UploadResponse(url), "封面上传成功");
        } catch (IllegalArgumentException e) {
            return Result.error("2002", e.getMessage());
        } catch (Exception e) {
            return Result.error("2002", "封面上传失败：" + e.getMessage());
        }
    }

    // 管理员端：轮播图上传
    @PostMapping("/carousel")
    public Result uploadCarousel(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            String url = localFileUploadUtil.upload(file, UploadScene.CAROUSEL_IMAGE);
            return Result.success(new UploadResponse(url), "轮播图上传成功");
        } catch (IllegalArgumentException e) {
            return Result.error("2003", e.getMessage());
        } catch (Exception e) {
            return Result.error("2003", "轮播图上传失败：" + e.getMessage());
        }
    }

    // 上传返回结果
    public static class UploadResponse {
        private String url;
        public UploadResponse(String url) { this.url = url; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}