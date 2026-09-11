package com.commence.novel.repository;

import com.commence.novel.entity.ShelfCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShelfCategoryRepository extends JpaRepository<ShelfCategory,Long> {
    List<ShelfCategory> findByUserIdOrderBySortAscCreateTimeAsc(Long userId);

    boolean existsByUserIdAndName(Long userId,String categoryName);

    // 按ID和用户ID查询（防止越权）
    Optional<ShelfCategory> findByIdAndUserId(Long id, Long userId);

    // 删除分组（按ID和用户ID）
    void deleteByIdAndUserId(Long id, Long userId);

    // 查询用户的非默认分组（按sort升序）
    List<ShelfCategory> findByUserIdAndNameNotOrderBySortAsc(Long userId, String defaultGroupName);

    // 查询用户的默认分组
    Optional<ShelfCategory> findByUserIdAndName(Long userId, String defaultName);

    // 获取用户最大sort值
    @Query("SELECT MAX(c.sort) FROM ShelfCategory c WHERE c.userId = :userId")
    Integer findMaxSortByUserId(@Param("userId") Long userId);
}
