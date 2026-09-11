package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Table(name = "novel")
@Data
@NoArgsConstructor
@DynamicUpdate //仅更新修改过的字段
public class Novel {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 15)
    private String title;

    @Column
    private String coverUrl; //封面图片

    @Column(length = 300,columnDefinition = "TEXT")
    private String intro; //简介

    @Column(nullable = false)
    private Integer typeId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NovelStatus status; //小说状态：DRAFT(草稿)、PENDING_REVIEW（待审核）、PUBLISHED（已发布）、OFFLINE（下架）


    private  Integer updateStatus;

    @Column(columnDefinition = "TEXT")
    private String rejectReason;

    @Column(nullable = false)
    private Integer wordCount = 0; //总字数

    @Column(nullable = false)
    private Integer readCount = 0;

    @Column(nullable = false)
    private Integer collectCount = 0;

    @Column(nullable = false)
    private Integer commentCount = 0;

    @Column
    private LocalDateTime lastUpdateTime;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column(nullable = false)
    private LocalDateTime updateTime;

    public enum NovelStatus {
        DRAFT, PENDING_REVIEW,  REJECTED, PUBLISHED, OFFLINE
    }

    public enum NovelUpdateStatus {
        SERIALIZING(0),
        PAUSED(1),
        FINISHED(2),;

        private final int intValue;

        NovelUpdateStatus(int intValue) {
            this.intValue = intValue;
        }

        //枚举转整数
        public int toIntValue() {
            return this.intValue;
        }

        public static NovelUpdateStatus fromString(String name) {
            if (name == null || name.isBlank()) return null;
            try {
                return NovelUpdateStatus.valueOf(name.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

    }

    public void setUpdateStatusEnum(NovelUpdateStatus status) {
        this.updateStatus = (status == null) ? null : status.toIntValue();
    }

    @Column(length = 15)
    private String pendingTitle;

    @Column(columnDefinition = "TEXT")
    private String pendingIntro;

    private Integer pendingTypeId;

    private String pendingCoverUrl;

    @Enumerated(EnumType.STRING)
    private PendingStatus pendingStatus;

    @Column(columnDefinition = "TEXT")
    private String reviewRejectReason;

    public enum PendingStatus {
        PENDING, APPROVED, REJECTED
    }
}
