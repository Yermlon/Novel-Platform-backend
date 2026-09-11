package com.commence.novel.repository;

import com.commence.novel.entity.VisitLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {
}