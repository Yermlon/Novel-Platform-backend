package com.commence.novel.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_novel", columnNames = {"user_id", "novel_id"})
})
@Data
@NoArgsConstructor
public class UserReadProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String userId;
    @Column(nullable = false, length = 64)
    private String novelId;
    @Column(nullable = false, length = 64)
    private String chapterId;

    private Integer progress = 0;
    private LocalDateTime readTime = LocalDateTime.now();
    private LocalDateTime updateTime = LocalDateTime.now();
}
