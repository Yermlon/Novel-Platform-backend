package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter")
@Data
@NoArgsConstructor
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long novelId;

    @Column(nullable = false,length = 20)
    private String title; //章节标题

    private String brief;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private Integer wordCount = 0;

    @Column(nullable = false)
    private Integer sort;

    private Integer publishSort;

    @Column(nullable = false,length = 20)
    @Enumerated(EnumType.STRING)
    private ChapterStatus status;

    @Column(columnDefinition = "LONGTEXT")
    private String draftContent; //草稿内容

    @Column
    private LocalDateTime draftSaveTime;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column(nullable = false)
    private LocalDateTime updateTime;

    public enum ChapterStatus {
        DRAFT("草稿"),
        PENDING("审核中"),
        REJECTED("审核驳回"),
        OFFLINE("下架"),
        PUBLISHED("已发布");

        private final String desc;
        ChapterStatus(String desc) {
            this.desc = desc;
        }
        public String getDesc() {return desc;}
    }

    private String auditReason;
    private LocalDateTime auditTime;



}
