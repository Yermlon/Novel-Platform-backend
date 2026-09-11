package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_record")
@Data
public class ReviewRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long novelId;

    @Column
    private Long chapterId;

    @Column(nullable = false)
    private Long authorId;

    @Column
    private Long reviewerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReviewStatus reviewStatus;

    // ========== 新增：审核类型 ==========
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReviewType reviewType;

    @Column(columnDefinition = "TEXT")
    private String reviewContent; //审核意见

    @Column(nullable = false)
    private LocalDateTime submitTime; //提交审核时间

    @Column
    private LocalDateTime reviewTime;

    // 审核结果状态（保持原有）
    public enum ReviewStatus {
        PENDING, // 待审核
        APPROVED, // 通过（兼容原有）
        REJECTED, // 驳回
        PASSED // 已通过
    }

    // ========== 新增：审核类型枚举 ==========
    public enum ReviewType {
        NOVEL_PUBLISH, // 小说整体发布审核（对应status）
        NOVEL_DETAIL,  // 小说详情修改审核（对应pendingStatus）
        CHAPTER_OFFLINE // 章节下架/上架审核（预留扩展）
    }
}