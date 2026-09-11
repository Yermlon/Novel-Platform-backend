package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class ShelfCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String name;
    private LocalDateTime createTime;
    private Integer sort;


    // 更新时间（新增）
    private LocalDateTime updateTime;

    // 标记是否为默认分组（新增，也可通过名称判断）
    @Transient // 不映射到数据库，仅内存使用
    private boolean isDefault;

    // 自动填充时间（可选，也可在Service中手动设置）
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
