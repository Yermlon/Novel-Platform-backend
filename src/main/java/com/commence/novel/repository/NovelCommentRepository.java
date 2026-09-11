package com.commence.novel.repository;

import com.commence.novel.DTO.NovelCommentDTO;
import com.commence.novel.entity.NovelComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NovelCommentRepository extends JpaRepository<NovelComment, Long>, JpaSpecificationExecutor<NovelComment> {

    /**
     * 根据小说ID查询所有评论（未过滤删除状态）
     */
    List<NovelComment> findByNovelIdOrderByCreateTimeDesc(Long novelId);

    /**
     * 统计小说未删除的评论总数
     */
    long countByNovelIdAndIsDeletedFalse(Long novelId);

    // ========== 查询作者所有层级评论（返回实体） ==========

    /**
     * 查询小说一级评论（parentId=null，返回DTO，含用户名+笔名）
     */
    @Query("SELECT new com.commence.novel.DTO.NovelCommentDTO(" +
            "    c.id, c.novelId, c.userId, c.parentId, " +
            "    c.content, c.createTime, c.isTop, " +
            "    c.likeCount, c.isAuthor, " +
            "    u.uname, " +
            "    u.penName, " + //查询笔名
            "    u.avatarUrl, "+
            "    c.status, c.reviewRemark " +
            ") FROM NovelComment c " +
            "JOIN User u ON c.userId = u.uid " +
            "WHERE c.novelId = :novelId AND c.isDeleted = false AND c.parentId IS NULL " +
            "ORDER BY c.createTime DESC")
    List<NovelCommentDTO> findTopLevelCommentsWithUser(@Param("novelId") Long novelId);

    /**
     * 查询子评论（根据parentId，返回DTO，含用户名+笔名）
     */
    @Query("SELECT new com.commence.novel.DTO.NovelCommentDTO(" +
            "    c.id, c.novelId, c.userId, c.parentId, " +
            "    c.content, c.createTime, c.isTop, " +
            "    c.likeCount, c.isAuthor, " +
            "    u.uname, " +
            "    u.penName, " +
            "    u.avatarUrl, "+
            "    c.status, c.reviewRemark " +
            ") FROM NovelComment c " +
            "JOIN User u ON c.userId = u.uid " +
            "WHERE c.parentId = :parentId AND c.isDeleted = false " +
            "ORDER BY c.createTime DESC") //最新回复在前
    List<NovelCommentDTO> findChildCommentsWithUser(@Param("parentId") Long parentId);

    /**
     * 【复用】一级评论按创建时间降序（和findTopLevelCommentsWithUser逻辑一致，可保留）
     */
    @Query("SELECT new com.commence.novel.DTO.NovelCommentDTO(" +
            "    c.id, c.novelId, c.userId, c.parentId, " +
            "    c.content, c.createTime, c.isTop, " +
            "    c.likeCount, c.isAuthor, " +
            "    u.uname, " +
            "    u.penName, " +
            "    u.avatarUrl, "+
            "    c.status, c.reviewRemark " +
            ") FROM NovelComment c " +
            "JOIN User u ON c.userId = u.uid " +
            "WHERE c.novelId = :novelId " +
            "  AND c.isDeleted = false " +
            "  AND c.parentId IS NULL " +
            "ORDER BY c.createTime DESC")
    List<NovelCommentDTO> findTopLevelCommentsWithUserOrderByCreateTimeDesc(@Param("novelId") Long novelId);

    /**
     * 根据评论ID查询单个评论（返回DTO，含用户名+笔名）
     */
    @Query("SELECT new com.commence.novel.DTO.NovelCommentDTO(" +
            "    c.id, c.novelId, c.userId, c.parentId, " +
            "    c.content, c.createTime, c.isTop, " +
            "    c.likeCount, c.isAuthor, " +
            "    u.uname, " +
            "    u.penName, " +
            "    u.avatarUrl, "+
            "    c.status, c.reviewRemark " +
            ") FROM NovelComment c " +
            "JOIN User u ON c.userId = u.uid " +
            "WHERE c.id = :commentId")
    NovelCommentDTO findCommentDTOById(@Param("commentId") Long commentId);

    /**
     * 查询用户所有未删除的评论（含父评论、小说标题，含用户名+笔名）
     */
    @Query("SELECT new com.commence.novel.DTO.NovelCommentDTO(" +
            "    c.id, c.novelId, c.userId, c.parentId, " +
            "    c.content, c.createTime, c.isTop, " +
            "    c.likeCount, c.isAuthor, " +
            "    u.uname, " +
            "    u.penName, " +
            "    u.avatarUrl, "+
            "    c.status, c.reviewRemark, " +
            "    COALESCE(p.content, ''), COALESCE(pUser.uname, ''), " +
            "    n.title " +
            ") FROM NovelComment c " +
            "JOIN User u ON c.userId = u.uid " +
            "LEFT JOIN NovelComment p ON c.parentId = p.id " +
            "LEFT JOIN User pUser ON p.userId = pUser.uid " +
            "LEFT JOIN Novel n ON c.novelId = n.id " +
            "WHERE c.userId = :userId AND c.isDeleted = false " +
            "ORDER BY c.createTime DESC")
    List<NovelCommentDTO> findByUserIdAndIsDeletedFalse(@Param("userId") Long userId);

    /**
     * 根据ID查询未删除的评论（返回实体，用于置顶权限校验）
     */
    Optional<NovelComment> findByIdAndIsDeletedFalse(Long id);

    /**
     * 根据父评论ID查询未删除的子评论
     */
    List<NovelComment> findByParentIdAndIsDeletedFalse(Long parentId);

}