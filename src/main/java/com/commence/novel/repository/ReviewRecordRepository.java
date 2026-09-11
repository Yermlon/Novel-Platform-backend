package com.commence.novel.repository;

import com.commence.novel.entity.ReviewRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long>, JpaSpecificationExecutor<ReviewRecord> {

    ReviewRecord findFirstByNovelIdOrderBySubmitTimeDesc(Long novelId);

    // 分页查询（用于 getReportedChapterList）
    Page<ReviewRecord> findAll(Specification<ReviewRecord> spec, Pageable pageable);
}
