package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name="novels_type")
@Data
public class NovelsType {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Integer typeId;

    @Column(unique = true, nullable = false)
    private String typeName;

    @Column(nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer status;//1-启用 0-禁用 默认1

    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private Date createTime;

    @Column(nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Date updateTime;
}
