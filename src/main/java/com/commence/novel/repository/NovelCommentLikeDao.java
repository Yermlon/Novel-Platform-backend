package com.commence.novel.repository;

import com.commence.novel.entity.NovelCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NovelCommentLikeDao extends JpaRepository<NovelCommentLike, Long> {
    Optional<NovelCommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    @Query("SELECT ncl.commentId FROM NovelCommentLike ncl WHERE ncl.userId = :userId AND ncl.commentId IN :commentIds")
    List<Long> findCommentIdsByUserIdAndCommentIdsIn(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);
}
