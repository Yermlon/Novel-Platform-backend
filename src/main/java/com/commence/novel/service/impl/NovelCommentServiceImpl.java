package com.commence.novel.service.impl;

import com.commence.novel.DTO.NovelCommentDTO;
import com.commence.novel.entity.Novel;
import com.commence.novel.entity.NovelComment;
import com.commence.novel.entity.NovelCommentLike;
import com.commence.novel.entity.User;
import com.commence.novel.repository.NovelCommentRepository;
import com.commence.novel.repository.NovelCommentLikeDao;
import com.commence.novel.repository.NovelRepository;
import com.commence.novel.repository.UserRepository;
import com.commence.novel.service.NovelCommentService;
import com.commence.novel.utils.AutoAuditUtils;
import com.commence.novel.utils.Result;
import jakarta.transaction.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class NovelCommentServiceImpl implements NovelCommentService {
    @Autowired
    private NovelCommentRepository novelCommentRepository;

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private NovelCommentLikeDao novelCommentLikeDao;

    @Autowired
    private UserRepository userRepository;

    // 集成自动审核
    @Override
    public Result addComment(Long userId, Long novelId, String content, Long parentId) {
        if (novelId == null || userId == null){
            return Result.error("1030","小说ID和用户ID不能为空");
        }
        if (content == null || content.trim().isEmpty()){
            return Result.error("1060","评论内容不能为空");
        }

        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isEmpty()){
            return Result.error("1031","小说不存在");
        }
        Novel novel = novelOpt.get();
        Integer isAuthor = novel.getAuthorId().equals(userId) ? 1 : 0;

        // ========== 核心新增：调用自动审核工具 ==========
        AutoAuditUtils.AuditResult auditResult = AutoAuditUtils.autoAuditComment(content);

        NovelComment comment = new NovelComment();
        comment.setNovelId(novelId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setLikeCount(0);
        comment.setParentId(parentId);
        comment.setIsAuthor(isAuthor);
        comment.setCreateTime(LocalDateTime.now());
        comment.setIsDeleted(false);
        comment.setIsTop(false);

        // ========== 新增：设置审核状态和备注 ==========
        comment.setStatus(auditResult.isPass() ? "PUBLISHED" : "BLOCKED");
        comment.setReviewRemark(auditResult.getReason());
        comment.setReviewTime(LocalDateTime.now());

        NovelComment savedComment = novelCommentRepository.saveAndFlush(comment);
        NovelCommentDTO commentDTO = novelCommentRepository.findCommentDTOById(savedComment.getId());

        // ========== 🔥 核心修改：审核通过才+1评论数 ==========
        if (auditResult.isPass()) {
            // 处理空值，避免NullPointerException
            Integer currentCommentCount = novel.getCommentCount() == null ? 0 : novel.getCommentCount();
            novel.setCommentCount(currentCommentCount + 1);
            novelRepository.saveAndFlush(novel); // 强制刷库生效
        }

        // ========== 新增：根据审核结果返回不同提示 ==========
        if (auditResult.isPass()) {
            return Result.success(comment,"评论发布成功");
        } else {
            return Result.success(comment, auditResult.getReason());
        }
    }


    public Result likeComment(Long commentId, Long userId) {
        if (commentId == null || userId == null){
            return Result.error("1060","评论ID和用户ID不能为空");
        }

        Optional<NovelComment> commentOpt = novelCommentRepository.findByIdAndIsDeletedFalse(commentId);
        if (commentOpt.isEmpty()){
            System.out.println("返回错误：评论不存在 (1061)");
            return Result.error("1061","评论不存在");
        }
        NovelComment comment = commentOpt.get();

        Optional<NovelCommentLike> likeOpt = novelCommentLikeDao.findByCommentIdAndUserId(commentId, userId);
        if (likeOpt.isPresent()){
            return Result.error("1063","已点赞该评论，无法重复点赞");
        }

        NovelCommentLike newLike = new NovelCommentLike();
        newLike.setCommentId(commentId);
        newLike.setUserId(userId);
        novelCommentLikeDao.save(newLike);

        Integer currentLikeCount = comment.getLikeCount();
        comment.setLikeCount(currentLikeCount == null ? 1 : currentLikeCount + 1);
        novelCommentRepository.save(comment);

        return Result.success(comment,"点赞成功");
    }

    @Override
    public List<Long> getLikedCommentIds(Long novelId, Long userId) {
        List<NovelComment> novelComments = novelCommentRepository.findByNovelIdOrderByCreateTimeDesc(novelId);
        List<Long> commentIds = novelComments.stream().map(NovelComment::getId).toList();

        if (commentIds.isEmpty()){
            return Collections.emptyList();
        }
        return  novelCommentLikeDao.findCommentIdsByUserIdAndCommentIdsIn(userId,commentIds);
    }

    @Override
    public Result cancelLikeComment(Long commentId, Long userId) {
        if (commentId == null || userId == null){
            return Result.error("1060","评论ID和用户ID不能为空");
        }
        Optional<NovelCommentLike>  likeOpt = novelCommentLikeDao.findByCommentIdAndUserId(commentId, userId);
        if (likeOpt.isEmpty()){
            return Result.error("1064","未点赞该评论，无法取消");
        }

        novelCommentLikeDao.delete(likeOpt.get());
        Optional<NovelComment> commentOpt = novelCommentRepository.findById(commentId);
        if (commentOpt.isPresent()){
            NovelComment comment = commentOpt.get();
            Integer currentLikeCount = comment.getLikeCount();
            if (currentLikeCount != null && currentLikeCount > 0) {
                comment.setLikeCount(currentLikeCount - 1);
                novelCommentRepository.save(comment);
            }
        }

        return Result.success(null,"取消点赞成功");
    }

    private List<NovelCommentDTO> getChildCommentsRecursively(Long parentCommentId) {
        List<NovelCommentDTO> childComments = novelCommentRepository.findChildCommentsWithUser(parentCommentId);
        for(NovelCommentDTO child : childComments) {
            List<NovelCommentDTO> grandChildComments = getChildCommentsRecursively(child.getCommentId());
            child.setChildComments(grandChildComments);
        }
        return childComments;
    }

    @Override
    public Result getCommentList(Long novelId, String filter) {
        if (novelId == null) {
            return Result.error("1030", "小说ID不能为空");
        }
        final String finalFilter = (filter == null || filter.trim().isEmpty()) ? "all" : filter.trim();

        try {
            List<NovelCommentDTO> topLevelComments = new ArrayList<>();
            Novel novel = novelRepository.findById(novelId).orElse(null);
            if (novel == null) {
                return Result.error("1033", "小说不存在");
            }
            Long authorId = novel.getAuthorId();

            switch (finalFilter) {
                case "author":
                    // ========== 完整查询作者所有层级评论 ==========
                    // 1. 查询该小说下所有一级评论（不管作者是谁）
                    List<NovelCommentDTO> allTopComments = novelCommentRepository.findTopLevelCommentsWithUser(novelId);

                    // 2. 遍历一级评论，筛选出包含作者评论的条目
                    for (NovelCommentDTO topComment : allTopComments) {
                        boolean isAuthorTopComment = authorId.equals(topComment.getUserId());
                        List<NovelCommentDTO> authorChildComments = new ArrayList<>();

                        // 3. 递归筛选该一级评论下所有作者的子评论
                        List<NovelCommentDTO> allChildComments = getChildCommentsRecursively(topComment.getCommentId());
                        filterAuthorChildComments(allChildComments, authorId, authorChildComments);

                        // 4. 只要满足以下任一条件就保留该一级评论：
                        //    - 一级评论是作者发的
                        //    - 有作者的子评论（作者回复了该评论）
                        if (isAuthorTopComment || !authorChildComments.isEmpty()) {
                            NovelCommentDTO newTopComment = new NovelCommentDTO();
                            BeanUtils.copyProperties(topComment, newTopComment);
                            // 只保留作者的子评论
                            newTopComment.setChildComments(authorChildComments);
                            topLevelComments.add(newTopComment);
                        }
                    }
                    break;
                case "latest":
                    topLevelComments = novelCommentRepository.findTopLevelCommentsWithUserOrderByCreateTimeDesc(novelId);
                    break;
                case "all":
                default:
                    topLevelComments = novelCommentRepository.findTopLevelCommentsWithUser(novelId);
                    break;
            }

            // 递归构建子评论（保留原有逻辑）
            for (NovelCommentDTO topComment : topLevelComments) {
                List<NovelCommentDTO> allchildComments = getChildCommentsRecursively(topComment.getCommentId());
                topComment.setChildComments(allchildComments);
            }

            return Result.success(topLevelComments, "查询评论列表成功");
        } catch (RuntimeException e) {
            return Result.error("1031", e.getMessage());
        } catch (Exception e) {
            return Result.error("1032", "查询评论列表失败");
        }
    }

    // ========== 递归筛选子评论中作者的评论 ==========
    private void filterAuthorChildComments(List<NovelCommentDTO> childComments, Long authorId, List<NovelCommentDTO> result) {
        if (childComments == null || childComments.isEmpty()) {
            return;
        }
        for (NovelCommentDTO child : childComments) {
            // 如果是作者的评论，加入结果集
            if (authorId.equals(child.getUserId())) {
                NovelCommentDTO authorChild = new NovelCommentDTO();
                BeanUtils.copyProperties(child, authorChild);
                // 递归处理该作者评论的子评论（如果有）
                List<NovelCommentDTO> grandChildResult = new ArrayList<>();
                filterAuthorChildComments(child.getChildComments(), authorId, grandChildResult);
                authorChild.setChildComments(grandChildResult);
                result.add(authorChild);
            } else {
                // 如果不是作者的评论，继续递归查询其子评论（作者可能回复了该评论）
                filterAuthorChildComments(child.getChildComments(), authorId, result);
            }
        }
    }

    @Override
    public Result deleteComment(Long novelId, Long userId, Long commentId) {
        if (novelId == null || userId == null || commentId == null){
            return Result.error("1060","评论ID、用户ID、用户ID不能为空");
        }
        Optional<NovelComment> commentOpt = novelCommentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return Result.error("1061","评论不存在");
        }
        NovelComment targetComment = commentOpt.get();
        if (!targetComment.getUserId().equals(userId)){
            return Result.error("1062","无权限删除该评论");
        }

        if (targetComment.getParentId() == null) {
            List<NovelComment> childComments = novelCommentRepository.findByParentIdAndIsDeletedFalse(commentId);
            for (NovelComment child : childComments) {
                child.setIsDeleted(true);
                novelCommentRepository.save(child);
            }
        }

        targetComment.setIsDeleted(true);
        novelCommentRepository.save(targetComment);

        // ========== 评论数减1逻辑 ==========
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            // 处理空值 + 防负数（评论数不能小于0）
            Integer currentCommentCount = novel.getCommentCount() == null ? 0 : novel.getCommentCount();
            novel.setCommentCount(Math.max(0, currentCommentCount - 1));
            novelRepository.saveAndFlush(novel); // 强制刷库生效
        }

        return Result.success(null,"删除评论成功");
    }

    @Override
    public Result countCommentsByNovelId(Long novelId) {
        if (novelId == null){
            return Result.error("1030","小说ID不能为空");
        }
        long commentCount = novelCommentRepository.countByNovelIdAndIsDeletedFalse(novelId);
        return Result.success(commentCount,"统计评论数成功");
    }

    @Override
    public List<NovelCommentDTO> getMyCommentList(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }

        // 1. 查询当前用户所有评论
        List<NovelCommentDTO> myComments = novelCommentRepository.findByUserIdAndIsDeletedFalse(userId);

        // 2. 补全父评论 + 小说信息（核心改造：适配枚举状态）
        for (NovelCommentDTO dto : myComments) {
            // 2.1 补全父评论信息（原有逻辑不变）
            if (dto.getParentId() != null) {
                Optional<NovelComment> parentCommentOpt = novelCommentRepository.findById(dto.getParentId());
                if (parentCommentOpt.isPresent()) {
                    NovelComment parentComment = parentCommentOpt.get();
                    dto.setParentContent(parentComment.getContent());
                    Optional<User> parentUserOpt = userRepository.findById(parentComment.getUserId());
                    dto.setParentUsername(parentUserOpt.map(User::getUname).orElse("匿名用户"));
                }
            }

            // 2.2 补全小说标题 + 状态（适配枚举）
            if (dto.getNovelId() != null) {
                Optional<Novel> novelOpt = novelRepository.findById(dto.getNovelId());
                if (novelOpt.isPresent()) {
                    Novel novel = novelOpt.get();
                    dto.setNovelTitle(novel.getTitle());
                    // 枚举转字符串（OFFLINE/PUBLISHED 等）
                    dto.setNovelStatus(novel.getStatus().name());
                } else {
                    dto.setNovelTitle("未知小说");
                    dto.setNovelStatus("OFFLINE"); // 小说不存在视为下架
                }
            }
        }

        return myComments;
    }
}