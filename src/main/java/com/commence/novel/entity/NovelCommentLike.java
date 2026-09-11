package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class NovelCommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long commentId;
    private Long userId;

    private java.time.LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = java.time.LocalDateTime.now();
    }

}
