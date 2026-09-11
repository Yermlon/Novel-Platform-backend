package com.commence.novel.controller;

import com.commence.novel.DTO.AuthorCommentDTO;
import com.commence.novel.service.AuthorCommentService;
import com.commence.novel.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/author/interact")
public class AuthorCommentController {

    @Autowired
    private AuthorCommentService commentService;

    // ========== 置顶/取消置顶接口 ==========
    @PostMapping("/top/{commentId}")
    public Result toggleCommentTop(
            @PathVariable Long commentId,
            @RequestAttribute Long uid //匹配拦截器存入的属性名
    ) {
        return commentService.toggleCommentTop(commentId, uid);
    }

    @PostMapping("/reply")
    public Result replyToReader(
            @RequestParam Long novelId,
            @RequestParam Long parentId,
            @RequestParam String content,
            @RequestAttribute Long uid
    ) {
        return commentService.replyToReader(novelId, parentId, uid, content);
    }

    @PostMapping("/comment")
    public Result publishAuthorComment(
            @RequestBody AuthorCommentDTO dto,
            @RequestAttribute Long uid
    ) {
        return commentService.publishAuthorComment(dto.getNovelId(), uid, dto.getContent()); // 传 userId
    }
}