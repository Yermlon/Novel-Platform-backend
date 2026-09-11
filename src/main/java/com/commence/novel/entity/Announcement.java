package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length = 1000)
    private String content;

    private String status = "PUBLISHED";
    private LocalDateTime publishedTime = LocalDateTime.now();
    private LocalDateTime createdTime = LocalDateTime.now();
}
