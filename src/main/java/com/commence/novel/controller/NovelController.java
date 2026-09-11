package com.commence.novel.controller;

import com.commence.novel.DTO.NovelDTO;
import com.commence.novel.DTO.NovelsTypeDTO;
import com.commence.novel.entity.Novel;
import com.commence.novel.service.NovelService;
import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/novel")
@Slf4j
public class NovelController {
    @Autowired
    private NovelService novelService;

    // ========== 从Request获取JWT解析的用户信息 ==========
    private Long getCurrentUserId(HttpServletRequest request) {
        Object uidObj = request.getAttribute("uid");
        if (uidObj == null) {
            log.warn("request中未获取到uid，Token解析失败或拦截器未生效");
            return null;
        }
        // 兼容 Integer/Long 类型
        if (uidObj instanceof Integer) {
            return ((Integer) uidObj).longValue();
        } else if (uidObj instanceof Long) {
            return (Long) uidObj;
        } else {
            log.error("uid类型错误，期望Integer/Long，实际：{}", uidObj.getClass().getName());
            return null;
        }
    }

    private String getCurrentUserRole(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (role == null) {
            log.warn("request中未获取到role，Token解析失败或拦截器未生效");
        }
        return role;
    }

    // ========== 创建小说（作者权限） ==========
    @PostMapping("/create")
    public Result createNovel(@Valid @RequestBody NovelDTO novelDTO, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        String role = getCurrentUserRole(request);

        // 未登录 或 非作者角色
        if (userId == null || !"AUTHOR".equals(role)) {
            return Result.error("1006","请以作者身份登录");
        }
        return novelService.createNovel(novelDTO, userId);
    }

    // ========== 编辑小说（登录校验） ==========
    @PostMapping("/edit/{novelId}")
    public Result updateNovel(
            @PathVariable Long novelId,
            @Valid @RequestBody NovelDTO novelDTO,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006","请登录");
        }
        return novelService.updateNovel(novelId, novelDTO, userId);
    }

    // ========== 小说下架（登录校验） ==========
    @PostMapping("/offline/{novelId}")
    public Result offlineNovel(@PathVariable Long novelId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006","请登录");
        }
        return novelService.offlineNovel(novelId, userId);
    }

    // ========== 提交审核（登录校验） ==========
    @PostMapping("/submit-review/{novelId}")
    public Result submitReview(@PathVariable Long novelId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006","请登录");
        }
        return novelService.submitReview(novelId, userId);
    }

    // ========== 作者查询小说列表（登录校验） ==========
    @GetMapping("/list")
    public Result getAuthorNovels(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006","请登录");
        }

        Novel.NovelStatus novelStatus = null;
        if (status != null) {
            String trimedStatus = status.trim();
            if (!trimedStatus.isEmpty()) {
                try {
                    novelStatus = Novel.NovelStatus.valueOf(trimedStatus.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return Result.error("400","无效的小说状态：" + status);
                }
            }
        }
        return novelService.getAuthorNovels(userId, pageNum, pageSize, novelStatus, name);
    }

    // ========== 删除小说（登录校验） ==========
    @DeleteMapping("/delete/{novelId}")
    public Result deleteNovel(@PathVariable Long novelId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006","请登录");
        }
        return novelService.deleteNovel(novelId, userId);
    }

    // ========== 查询小说详情（登录校验） ==========
    @GetMapping("/detail/{novelId}")
    public Result getNovelDetail(@PathVariable Long novelId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006","请登录");
        }
        return novelService.getNovelDetail(novelId, userId);
    }

    // ========== 查询小说类型（公开接口） ==========
    @GetMapping("/novelsType")
    public Result<List<NovelsTypeDTO>> getNovelsTypeList() {
        try {
            List<NovelsTypeDTO> novelsTypeDTOList = novelService.listAllValidNovelsType();
            return Result.success(novelsTypeDTOList, "获取小说类型成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("500","获取小说类型失败，请稍后重试");
        }
    }

    // ========== 小说整体审核通过（管理员，极简版） ==========
    @PostMapping("/review/approve/{novelId}")
    public Result approveNovel(
            @PathVariable Long novelId,
            HttpServletRequest request) {
        // 1. 管理员权限校验（和驳回接口完全一致）
        Long userId = getCurrentUserId(request);
        String role = getCurrentUserRole(request);
        if (userId == null || !"ADMIN".equals(role)) {
            return Result.error("1007","无审核权限，无法执行审核通过操作");
        }
        // 2. 直接调用Service并返回结果（和驳回接口完全一致）
        return novelService.passReview(novelId, userId);
    }


    // ========== 审核驳回（管理员权限） ==========
    @PostMapping("/review/reject/{novelId}")
    public Result rejectNovel(
            @PathVariable Long novelId,
            @RequestParam String rejectReason,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        String role = getCurrentUserRole(request);

        // 未登录 或 非管理员角色
        if (userId == null || !"ADMIN".equals(role)) {
            return Result.error("1007","无审核权限，无法执行驳回操作");
        }
        return novelService.rejectNovel(novelId, rejectReason, userId);
    }

    @PostMapping("/review/detail/approve/{novelId}")
    public Result approveNovelDetail(@PathVariable Long novelId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        String role = getCurrentUserRole(request);
        if (userId == null || !"ADMIN".equals(role)) {
            return Result.error("1007","无审核权限");
        }
        return novelService.SpassReview(novelId, userId);
    }

    // ========== 详情审核驳回（管理员权限） ==========
    @PostMapping("/review/detail/reject/{novelId}")
    public Result rejectNovelDetail(
            @PathVariable Long novelId,
            @RequestParam String reason,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        String role = getCurrentUserRole(request);

        if (userId == null || !"ADMIN".equals(role)) {
            return Result.error("1007","无审核权限");
        }
        return novelService.SrejectNovel(novelId, reason, userId);
    }

    // ========== 查询复审状态（登录校验） ==========
    @GetMapping("/review/detail/{novelId}")
    public Result getSNovelDetail(@PathVariable Long novelId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("1006","请登录");
        }
        return novelService.getSNovelDetail(novelId, userId);
    }

    // ========== 书城列表（公开接口） ==========
    @GetMapping("/bookStore/list")
    public Result getBookStoreList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long typeId,
            @RequestParam String sortType,
            @RequestParam(required = false) Long wordCount,
            @RequestParam(required = false) Long updateStatus) {
        return novelService.getBookStoreNovels(pageNum, pageSize, typeId, sortType,wordCount, updateStatus);
    }

    // ========== 公开小说详情（公开接口） ==========
    @GetMapping("/bookStore/detail/{novelId}")
    public Result getPublicNovelDetail(@PathVariable Long novelId) {
        return novelService.getPublicNovelDetail(novelId);
    }

    // ========== 查询作者公开作品（公开接口，前端传authorId） ==========
    @GetMapping("/author/works")
    public Result getAuthorPublicWorks(@RequestParam Long authorId) {
        return novelService.getAuthorPublicWorks(authorId);
    }

    // ========== 小说搜索接口（公开接口，无需登录） ==========
    @GetMapping("/search")
    public Result searchNovels(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 关键词为空时，返回空结果（或返回热门小说，根据需求调整）
        if (StringUtils.isBlank(keyword)) {
            return Result.error("400", "搜索关键词不能为空");
        }
        return novelService.searchNovels(keyword.trim(), pageNum, pageSize);
    }

    @PostMapping("/bookStore/detail/batch")
    public Result getNovelDetailsBatch(@RequestBody List<Long> novelIds) {
        if (novelIds == null || novelIds.isEmpty()) {
            return Result.error("400", "小说ID列表不能为空");
        }
        return novelService.getNovelDetailsBatch(novelIds);
    }

    @GetMapping("/admin/list")
    public Result getAllNovels(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "100") Integer pageSize,
            HttpServletRequest request) {
        // 1. 校验是否为 ADMIN 角色
        String role = getCurrentUserRole(request);
        if (!"ADMIN".equals(role)) {
            return Result.error("1007", "无权限");
        }

        // 2. 复用 getBookStoreNovels 方法，不传筛选条件 = 查询所有已发布小说
        return novelService.getBookStoreNovels(pageNum, pageSize, null, null, null, null);
    }


    // ========== 管理员查询待审核小说列表 ==========
    @GetMapping("/admin/review/list")
    public Result getReviewNovelList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        // 1. 管理员权限校验
        String role = getCurrentUserRole(request);
        if (!"ADMIN".equals(role)) {
            return Result.error("1007", "无权限访问审核列表");
        }

        // 2. 调用service查询
        return novelService.getReviewNovelList(pageNum, pageSize, status);
    }


}