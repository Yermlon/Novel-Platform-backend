package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class VisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 访问IP
    @Column(length = 64)
    private String ip;

    // 用户ID（游客为null）
    private Long uid;

    // 访问的接口/页面URL
    @Column(length = 255)
    private String requestUrl;

    // 访问时间
    private LocalDateTime visitTime = LocalDateTime.now();
}