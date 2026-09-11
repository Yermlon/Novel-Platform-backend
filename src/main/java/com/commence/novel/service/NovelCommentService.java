package com.commence.novel.service;

import com.commence.novel.DTO.NovelCommentDTO;
import com.commence.novel.utils.Result;

import java.util.List;

public interface NovelCommentService {
    //发布评论
    Result addComment(Long userId, Long novelId, String content, Long parentId);

    //点赞评论（防重复点赞
    Result likeComment(Long commentId, Long userId);

    //删除评论
    Result deleteComment(Long userId, Long novelId, Long commentId);

    //统计总评论数
    Result countCommentsByNovelId(Long novelId);

    //筛选评论列表
    Result getCommentList(Long novelId, String filter);

    //取消点赞
    Result cancelLikeComment(Long commentId, Long userId);

    //获取点赞状态
    List<Long> getLikedCommentIds(Long novelId, Long userId);

    //查询用户评论
    List<NovelCommentDTO> getMyCommentList(Long userId);

}
