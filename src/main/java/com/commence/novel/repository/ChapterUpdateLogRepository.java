package com.commence.novel.repository;

import com.commence.novel.entity.ChapterUpdateLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterUpdateLogRepository extends JpaRepository<ChapterUpdateLog, Long> {

}
