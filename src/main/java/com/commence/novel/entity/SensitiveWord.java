package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sensitive_word") // 对应数据库表名
public class SensitiveWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自增主键
    private Long id;         // 主键

    @Column(unique = true, nullable = false, length = 50)
    private String word;     // 敏感词（唯一约束）

    @Column(length = 20)
    private String type = "DEFAULT"; // 敏感词类型，默认DEFAULT

    @Column(updatable = false)
    private LocalDateTime createTime = LocalDateTime.now(); // 创建时间

    private LocalDateTime updateTime = LocalDateTime.now(); // 更新时间

    // 更新时自动更新updateTime
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}