package com.commence.novel.repository;

import com.commence.novel.entity.UserReadProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserReadProgressRepository extends JpaRepository<UserReadProgress, Long> {
    Optional<UserReadProgress> findByUserIdAndNovelId(String userId, String novelId);

    Optional<UserReadProgress> findTopByUserIdAndNovelIdOrderByUpdateTimeDesc(String userId, String novelId);
}
