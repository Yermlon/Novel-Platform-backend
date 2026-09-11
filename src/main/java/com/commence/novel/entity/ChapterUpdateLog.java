package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_update_log")
@Data
public class ChapterUpdateLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Long novelId;

    @Column(nullable = false)
    private Long chapterId;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UpdateType updateType;

    @Column(nullable = false)
    private LocalDateTime updateTime;

    public enum UpdateType {
        CREATE, EDIT, DELETE,PUBLISH,OFFLINE
    }
}
