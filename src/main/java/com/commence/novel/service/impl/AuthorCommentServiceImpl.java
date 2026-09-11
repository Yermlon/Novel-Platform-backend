package com.commence.novel.service.impl;

import com.commence.novel.entity.Novel;
import com.commence.novel.entity.NovelComment;
import com.commence.novel.repository.NovelCommentRepository;
import com.commence.novel.service.AuthorCommentService;
import com.commence.novel.service.NovelService;
import com.commence.novel.utils.AutoAuditUtils;
import com.commence.novel.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthorCommentServiceImpl implements AuthorCommentService {
    @Autowired
    private NovelCommentRepository commentDao;

    @Autowired
    private NovelService novelService;

    // ========== 置顶/取消置顶评论 ==========
    @Override
    public Result toggleCommentTop(Long commentId, Long userId) { // 🔥 uid → userId
        if (userId == null) {
            return Result.error("1006", "请先登录");
        }

        NovelComment comment;
        try {
            comment = commentDao.findByIdAndIsDeletedFalse(commentId)
                    .orElseThrow(() -> new RuntimeException("1061:评论不存在或已删除"));
        } catch (RuntimeException e) {
            return Result.error(e.getMessage().split(":")[0], e.getMessage().split(":")[1]);
        }

        Novel novel = novelService.getNovelById(comment.getNovelId());
        if (novel == null) {
            return Result.error("1062", "评论所属小说不存在");
        }

        if (!novel.getAuthorId().equals(userId)) {
            return Result.error("1060", "无权限操作该评论（非本人小说）");
        }

        comment.setIsTop(!comment.getIsTop());
        commentDao.save(comment);

        String msg = comment.getIsTop() ? "置顶成功" : "取消置顶成功";
        return Result.success(msg);
    }

    // ========== 作者回复读者评论 ==========
    @Override
    public Result replyToReader(Long novelId, Long parentId, Long userId, String content) { // 🔥 uid → userId
        if (userId == null) return Result.error("1006", "请先登录");
        if (novelId == null) return Result.error("1063", "小说ID不能为空");
        if (parentId == null) return Result.error("1064", "父评论ID不能为空");
        if (content == null || content.trim().isEmpty()) {
            return Result.error("1065", "回复内容不能为空");
        }

        // 调用自动审核工具
        AutoAuditUtils.AuditResult auditResult = AutoAuditUtils.autoAuditComment(content);

        Novel novel = novelService.getNovelById(novelId);
        if (novel == null || !novel.getAuthorId().equals(userId)) {
            return Result.error("1060", "无权限操作该小说");
        }

        NovelComment parentComment = commentDao.findByIdAndIsDeletedFalse(parentId).orElse(null);
        if (parentComment == null) {
            return Result.error("1066", "父评论不存在或已删除");
        }

        NovelComment authorReply = new NovelComment();
        authorReply.setNovelId(novelId);
        authorReply.setUserId(userId);
        authorReply.setContent(content.trim());
        authorReply.setParentId(parentId);
        authorReply.setIsAuthor(1);
        authorReply.setIsTop(false);
        authorReply.setIsDeleted(false);

        // ========== 设置审核状态和备注 ==========
        authorReply.setStatus(auditResult.isPass() ? "PUBLISHED" : "BLOCKED");
        authorReply.setReviewRemark(auditResult.getReason());
        authorReply.setReviewTime(LocalDateTime.now());

        authorReply.setCreateTime(LocalDateTime.now());

        //  用saveAndFlush确保立即入库
        NovelComment savedReply = commentDao.saveAndFlush(authorReply);

        // ========== 根据审核结果返回不同提示 ==========
        if (auditResult.isPass()) {
            return Result.success(savedReply, "回复成功");
        } else {
            return Result.success(savedReply, auditResult.getReason());
        }
    }

    // ========== 作者发表独立评论 ==========
    @Override
    public Result publishAuthorComment(Long novelId, Long userId, String content) { // 🔥 uid → userId
        if (userId == null) return Result.error("1006", "请先登录");
        if (novelId == null) return Result.error("1063", "小说ID不能为空");
        if (content == null || content.trim().isEmpty()) {
            return Result.error("1067", "评论内容不能为空");
        }

        // 调用自动审核工具
        AutoAuditUtils.AuditResult auditResult = AutoAuditUtils.autoAuditComment(content);

        Novel novel = novelService.getNovelById(novelId);
        if (novel == null || !novel.getAuthorId().equals(userId)) {
            return Result.error("1060", "无权限操作该小说");
        }

        NovelComment authorComment = new NovelComment();
        authorComment.setNovelId(novelId);
        authorComment.setUserId(userId);
        authorComment.setContent(content.trim());
        authorComment.setParentId(null);
        authorComment.setIsAuthor(1);
        authorComment.setIsTop(false);
        authorComment.setIsDeleted(false);

        // ========== 设置审核状态和备注 ==========
        authorComment.setStatus(auditResult.isPass() ? "PUBLISHED" : "BLOCKED");
        authorComment.setReviewRemark(auditResult.getReason());
        authorComment.setReviewTime(LocalDateTime.now());

        authorComment.setCreateTime(LocalDateTime.now());

        // 用saveAndFlush确保立即入库
        NovelComment savedComment = commentDao.saveAndFlush(authorComment);

        // ========== 根据审核结果返回不同提示 ==========
        if (auditResult.isPass()) {
            return Result.success(savedComment, "评论发布成功");
        } else {
            return Result.success(savedComment, auditResult.getReason());
        }
    }


}