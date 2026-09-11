package com.commence.novel.repository;
import com.commence.novel.entity.Novel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long>, JpaSpecificationExecutor<Novel> {
    @Modifying
    @Query("UPDATE Novel n SET " +
            "n.title = COALESCE(n.pendingTitle, n.title), " +
            "n.intro = COALESCE(n.pendingIntro, n.intro), " +
            "n.coverUrl = COALESCE(n.pendingCoverUrl, n.coverUrl), " +
            "n.typeId = COALESCE(n.pendingTypeId, n.typeId), " +
            "n.pendingStatus = :status, " +
            "n.updateTime = :time " +
            "WHERE n.id = :novelId")
    void updateDetail(@Param("novelId") Long novelId,
                      @Param("status") Novel.PendingStatus status,
                      @Param("time") LocalDateTime time);

    @Modifying
    @Query("UPDATE Novel n SET n.pendingStatus = :status, n.reviewRejectReason = :reason," +
            "n.updateTime = :time WHERE n.id = :novelId")
    void updatePendingStatus(Long novelId, Novel.PendingStatus status, String reason, LocalDateTime time);

    @Query("SELECT COUNT(n) FROM Novel n")
    Long countTotalNovels();

}
