package com.commence.novel.DTO;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class NovelCommentDTO {
    private Long commentId;
    private Long novelId;
    private Long userId;
    private Long parentId;
    private String content;
    private LocalDateTime createTime;
    private Boolean isTop;
    private Boolean isLiked;
    private Integer likeCount;
    private Integer isAuthor;

    private List<NovelCommentDTO> childComments;

    private String uname;
    private String penName;
    private String avatarUrl;

    private String status;
    private String reviewRemark;
    private String parentContent;
    private String parentUsername;
    private String novelTitle;
    private String novelStatus;

    // 基础版
    public NovelCommentDTO(
            Long commentId, Long novelId, Long userId, Long parentId,
            String content, LocalDateTime createTime, Boolean isTop,
            Integer likeCount, Integer isAuthor,
            String uname,
            String penName,
            String avatarUrl,
            String status,
            String reviewRemark
    ) {
        this.commentId = commentId;
        this.novelId = novelId;
        this.userId = userId;
        this.parentId = parentId;
        this.content = content;
        this.createTime = createTime;
        this.isTop = isTop;
        this.likeCount = likeCount;
        this.isAuthor = isAuthor;
        this.uname = uname;
        this.penName = penName;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.reviewRemark = reviewRemark;
    }

    // 包含父评论信息版
    public NovelCommentDTO(
            Long commentId, Long novelId, Long userId, Long parentId,
            String content, LocalDateTime createTime, Boolean isTop,
            Integer likeCount, Integer isAuthor,
            String uname,
            String penName,
            String avatarUrl,
            String status, String reviewRemark,
            String parentContent, String parentUsername,
            String novelTitle
    ) {
        this.commentId = commentId;
        this.novelId = novelId;
        this.userId = userId;
        this.parentId = parentId;
        this.content = content;
        this.createTime = createTime;
        this.isTop = isTop;
        this.likeCount = likeCount;
        this.isAuthor = isAuthor;
        this.uname = uname;
        this.penName = penName;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.reviewRemark = reviewRemark;
        this.parentContent = parentContent;
        this.parentUsername = parentUsername;
        this.novelTitle = novelTitle;
    }

    // 无参构造方法
    public NovelCommentDTO() {
        // 空实现
    }
}