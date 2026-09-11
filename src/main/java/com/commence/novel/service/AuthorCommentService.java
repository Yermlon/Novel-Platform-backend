package com.commence.novel.service;

import com.commence.novel.utils.Result;

public interface AuthorCommentService {
    // 置顶/取消置顶评论
    Result toggleCommentTop(Long commentId, Long userId);

    // 作者回复读者评论
    Result replyToReader(Long novelId, Long parentId, Long userId, String content);

    // 作者发表独立评论
    Result publishAuthorComment(Long novelId, Long userId, String content);

}
