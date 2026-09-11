package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class NovelRead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long novelId;

    @Column(nullable = false)
    private Long chapterId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime readTime = LocalDateTime.now();

}
