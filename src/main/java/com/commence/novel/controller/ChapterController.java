package com.commence.novel.controller;

import com.commence.novel.DTO.ChapterDTO;
import com.commence.novel.DTO.ChapterDraftDTO;
import com.commence.novel.DTO.ChapterSortDTO;
import com.commence.novel.service.ChapterService;
import com.commence.novel.utils.Result;
import jakarta.servlet.http.HttpServletRequest; // 替换 HttpSession 为 HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chapter")
public class ChapterController {
    @Autowired
    private ChapterService chapterService;

    //  从JWT解析的Request属性中获取当前登录用户ID
    private Long getCurrentUserId(HttpServletRequest request) {
        Object uidObj = request.getAttribute("uid");
        if (uidObj == null) {
            return null;
        }
        // 确保类型转换安全
        return uidObj instanceof Long ? (Long) uidObj : null;
    }

    // ==========  创建章节  ==========
    @PostMapping("/create")
    public Result createChapter(@RequestBody ChapterDTO chapterDTO, HttpServletRequest request) {
        Long authorId = getCurrentUserId(request);
        if (authorId == null) {
            return Result.error("1006","请登录");
        }
        return chapterService.createChapter(chapterDTO, authorId);
    }

    // ==========  编辑章节 ==========
    @PostMapping("/update/{chapterId}")
    public Result editChapter(@PathVariable Long chapterId, @RequestBody ChapterDTO chapterDTO, HttpServletRequest request) {
        Long authorId = getCurrentUserId(request);
        if (authorId == null) {
            return Result.error("1006","请登录");
        }
        return chapterService.editChapter(chapterId, chapterDTO, authorId);
    }

    // ========== 自动保存草稿  ==========
    @PostMapping("/auto-save/{chapterId}")
    public Result autoSaveDraft(@PathVariable Long chapterId, @RequestBody ChapterDraftDTO chapterDraftDTO, HttpServletRequest request) {
        Long authorId = getCurrentUserId(request);
        if (authorId == null) {
            return Result.error("1006","请登录");
        }
        String draftContent = chapterDraftDTO.getDraftContent();
        boolean isAuto = chapterDraftDTO.isAuto();
        String msg = isAuto ? "草稿自动保存成功" : "草稿保存成功";

        return chapterService.autoSaveDraft(chapterId, draftContent, chapterDraftDTO.getTitle(), chapterDraftDTO.getBrief(), authorId, msg);
    }

    // ========== 发布章节  ==========
    @PostMapping("/publish/{chapterId}")
    public Result publishChapter(@PathVariable Long chapterId, HttpServletRequest request) {
        Long authorId = getCurrentUserId(request);
        if (authorId == null) {
            return Result.error("1006","请登录");
        }
        return chapterService.publishChapter(chapterId, authorId);
    }

    // ========== 调整未发布章节排序  ==========
    @PostMapping("/adjust-sort/{novelId}")
    public Result adjustSort(@PathVariable Long novelId, @RequestBody List<ChapterSortDTO> chapterSortList, HttpServletRequest request) {
        Long authorId = getCurrentUserId(request);
        if (authorId == null) {
            return Result.error("1006","请登录");
        }
        return chapterService.adjustChapterSort(novelId, chapterSortList, authorId);
    }

    // ========== 调整已发布章节排序  ==========
    @PostMapping("/adjust-publish-sort/{novelId}")
    public Result adjustPublishSort(@PathVariable Long novelId, @RequestBody List<ChapterSortDTO> chapterSortList, HttpServletRequest request) {
        Long authorId = getCurrentUserId(request);
        if (authorId == null) {
            return Result.error("1006","请登录");
        }
        return chapterService.adjustPublishSort(novelId, chapterSortList, authorId);
    }

    // ========== 公开接口（查询章节列表，无需登录） ==========
    @GetMapping("/list")
    public Result getChapterList(@RequestParam Long novelId) {
        if (novelId == null) {
            return Result.error("1030", "小说ID不能为空"); // 补充参数校验
        }
        return chapterService.getChapterList(novelId);
    }

    // ========== 删除章节  ==========
    @DeleteMapping("/delete/{chapterId}")
    public Result deleteChapter(@PathVariable Long chapterId, HttpServletRequest request) {
        Long authorId = getCurrentUserId(request);
        if (authorId == null) {
            return Result.error("1006","请登录");
        }
        return chapterService.deleteChapter(chapterId, authorId);
    }

    // ========== 下架章节  ==========
    @PostMapping("/offline/{chapterId}")
    public Result offlineChapter(@PathVariable Long chapterId, HttpServletRequest request) {
        Long authorId = getCurrentUserId(request);
        if (authorId == null) {
            return Result.error("1006","请登录");
        }
        return chapterService.offlineChapter(chapterId, authorId);
    }

    // ========== 恢复章节  ==========
    @PostMapping("/restore/{chapterId}")
    public Result restoreChapter(@PathVariable Long chapterId, HttpServletRequest request) {
        Long authorId = getCurrentUserId(request);
        if (authorId == null) {
            return Result.error("1006","请登录");
        }
        return chapterService.restoreChapter(chapterId, authorId);
    }

    // ========== 公开接口（获取章节内容，无需登录） ==========
    @GetMapping("/content/{chapterId}")
    public Result getChapterContent(@PathVariable Long chapterId) {
        if (chapterId == null) {
            return Result.error("1040", "章节ID不能为空"); // 补充参数校验
        }
        return chapterService.getChapterContent(chapterId);
    }
}