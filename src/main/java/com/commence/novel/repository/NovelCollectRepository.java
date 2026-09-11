package com.commence.novel.repository;

import com.commence.novel.entity.NovelCollect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NovelCollectRepository extends JpaRepository<NovelCollect, Long> {
    Optional<NovelCollect> findByUserIdAndNovelId(Long userId, Long novelId);

    List<NovelCollect> findByUserId(Long userId);

    long countByNovelId(Long novelId);

    List<NovelCollect> findByUserIdAndCategoryId(Long userId, Long categoryId);
}
