package com.commence.novel.interceptor;

import com.commence.novel.entity.VisitLog;
import com.commence.novel.repository.VisitLogRepository;
import com.commence.novel.utils.JwtUtil;
import com.commence.novel.utils.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.time.LocalDateTime;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    // ====================== 注入访问日志仓库 ======================
    @Autowired
    private VisitLogRepository visitLogRepository;

    // 白名单：精准控制放行范围，移除/upload的宽泛放行
    private static final String[] WHITE_LIST = {
            "/user/login",
            "/user/register",
            "/user/send-register-code",
            "/user/send-reset-code",
            "/user/reset-password",
            "/user/check-penname",

            // ========== 小说模块（公开接口） ==========
            "/novel/bookStore/list",    // 公开书城列表
            "/novel/bookStore/detail",  // 公开小说详情
            "/novel/author/works",      // 公开作者作品列表
            "/novel/novelsType",        // 公开小说类型列表
            "/novel/search",            // 公开搜索列表

            // ========== 评论模块（公开接口） ==========
            "/comment/list",            // 评论列表（带小说ID）
            "/comment/count",           // 评论数统计

            // ========== 章节模块（公开接口） ==========
            "/chapter/list",            // 章节列表（传novelId）
            "/chapter/content",         // 章节内容（带chapterId）

            // ========== 轮播图模块（公开接口） ==========
            "/carousel/list",           // 启用的轮播图列表

            // ========== 公告模块（公开接口） ==========
            "/announcement/latest",     // 最新公告

            // ========== 阅读统计模块（公开接口） ==========
            "/read/count",              // 小说阅读量统计
            "/read/trend",


            // ========== 收藏模块（公开接口） ==========
            "/collect/count",           // 小说收藏数统计

            // 上传相关：仅放行封面上传接口 + 上传文件的访问路径（静态资源）
            "/upload/novelCover",       // 小说封面上传（无需登录）
            "/upload/user_avatar/**",   // 头像文件访问（静态资源）
            "/upload/novel_cover/**",   // 封面文件访问（静态资源）
            "/upload/carousel",
            "/upload/carousel/**"       // 轮播图文件访问（静态资源）
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        log.info("===== 开始处理请求：{} =====", requestURI);

        // ========== 重构白名单匹配逻辑：精准匹配，避免误判 ==========
        boolean isWhiteList = false;
        for (String white : WHITE_LIST) {
            // 1. 精确匹配（如 /user/login、/upload/novelCover）
            if (requestURI.equals(white)) {
                isWhiteList = true;
                log.info("请求URI {} 精确匹配白名单：{}，直接放行", requestURI, white);
                break;
            }
            // 2. 通配符匹配（仅处理带/**的白名单，如 /upload/user_avatar/123.png）
            else if (white.endsWith("/**")) {
                String prefix = white.substring(0, white.length() - 3); // 去掉/**
                if (requestURI.startsWith(prefix)) {
                    isWhiteList = true;
                    log.info("请求URI {} 通配符匹配白名单：{}，直接放行", requestURI, white);
                    break;
                }
            }
            // 3. 子路径匹配（如 /novel/bookStore/list/1）
            else if (requestURI.startsWith(white + "/")) {
                isWhiteList = true;
                log.info("请求URI {} 子路径匹配白名单：{}，直接放行", requestURI, white);
                break;
            }
        }

        // ====================== 核心：记录访问日志（不管是否登录都记录） ======================
        try {
            VisitLog visitLog = new VisitLog();
            visitLog.setIp(getClientIp(request));       // IP
            visitLog.setRequestUrl(requestURI);          // 访问地址
            visitLog.setVisitTime(LocalDateTime.now());  // 访问时间

            // 尝试获取 uid（游客为 null）
            Long uid = null;
            if (request.getAttribute("uid") != null) {
                uid = (Long) request.getAttribute("uid");
            }
            visitLog.setUid(uid);

            visitLogRepository.save(visitLog);
            log.info("✅ 访问日志已记录：ip={}, uid={}, url={}", visitLog.getIp(), uid, requestURI);
        } catch (Exception e) {
            log.error("❌ 记录访问日志失败：", e);
        }

        if (isWhiteList) {
            return true;
        }

        // ========== Token 鉴权逻辑（原有逻辑保留，已处理Bearer前缀） ==========
        String token = request.getHeader("Authorization");
        log.info("从Authorization头获取Token：{}", token == null ? "null" : "非空（已脱敏）");

        if (token == null || token.isEmpty()) {
            token = request.getHeader("token");
            log.info("从token头获取Token：{}", token == null ? "null" : "非空（已脱敏）");
        }

        // 处理Bearer前缀（关键：确保Token格式正确）
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
            log.info("移除Bearer前缀后的Token：非空（已脱敏）");
        }

        if (token == null || token.isEmpty()) {
            log.warn("请求URI {} 未携带Token，返回未登录", requestURI);
            returnJson(response, Result.error("401", "未携带 Token，请先登录"));
            return false;
        }

        // Token 有效性验证
        log.info("开始验证Token有效性...");
        if (!jwtUtil.validateToken(token)) {
            log.warn("请求URI {} 的Token无效或已过期", requestURI);
            returnJson(response, Result.error("401", "Token 无效或已过期，请重新登录"));
            return false;
        }
        log.info("Token有效性验证通过");

        // Token 解析逻辑
        Object userIdObj = null;
        String role = null;
        try {
            userIdObj = jwtUtil.getUidFromToken(token);
            role = jwtUtil.getRoleFromToken(token);
            log.info("Token解析结果 - userIdObj类型：{}，值：{}",
                    userIdObj == null ? "null" : userIdObj.getClass().getName(),
                    userIdObj);
            log.info("Token解析结果 - role：{}", role);
        } catch (Exception e) {
            log.error("Token解析异常：", e);
            returnJson(response, Result.error("401", "Token 解析失败，请重新登录"));
            return false;
        }

        // userId 类型转换
        Long userId = null;
        if (userIdObj != null) {
            if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else if (userIdObj instanceof String) {
                try {
                    userId = Long.parseLong((String) userIdObj);
                } catch (NumberFormatException e) {
                    log.error("userId字符串转Long失败：", e);
                }
            }
        }

        // 空值校验
        if (userId == null || role == null) {
            log.warn("Token解析出的userId/role为空 - userId={}, role={}", userId, role);
            returnJson(response, Result.error("401", "Token 解析失败，请重新登录"));
            return false;
        }

        // 存入request属性
        request.setAttribute("uid", userId);
        request.setAttribute("role", role);
        log.info("===== Token解析成功，userId={}，role={}，放行请求 =====", userId, role);

        return true;
    }

    // ====================== 获取真实客户端IP ======================
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // 响应 JSON 数据给前端
    private void returnJson(HttpServletResponse response, Result result) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.write(new ObjectMapper().writeValueAsString(result));
        writer.flush();
        writer.close();
    }
}