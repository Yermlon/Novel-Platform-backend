package com.commence.novel.repository;

import com.commence.novel.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByNovelIdOrderBySortAsc(Long novelId);

    List<Chapter> findByNovelIdAndStatusInOrderBySortAsc(@Param("novelId") Long novelId,@Param("statusList") List<Chapter.ChapterStatus> statusList);

    List<Chapter> findByNovelIdAndStatusOrderByPublishSortAsc(Long novelId,Chapter.ChapterStatus status);

    Long countByNovelId(Long novelId);

    boolean existsByNovelIdAndSort(Long novelId, Integer sort);

    void deleteByNovelId(Long authorId);

    @Query("SELECT MAX(c.publishSort) FROM Chapter c WHERE c.novelId = :novelId AND c.status = :status")
    Integer findMaxPublishSortByNovelIdAndStatus(@Param("novelId") Long novelId, @Param("status") Chapter.ChapterStatus status);

    @Query("SELECT MAX(c.sort) FROM Chapter c WHERE c.novelId = :novelId AND c.status NOT IN ('PUBLISHED')")
    Integer findMaxSortByNovelIdAndStatusNotPublished(@Param("novelId") Long novelId);
}
