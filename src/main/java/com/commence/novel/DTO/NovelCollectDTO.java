package com.commence.novel.DTO;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收藏小说的DTO（包含小说详情）
 */
@Data
public class NovelCollectDTO {
    // 收藏记录基础字段
    private Long id;
    private Long novelId;
    private Long userId;
    private Long categoryId;
    private LocalDateTime collectTime;

    // 关联的小说详情字段（从Novel表查询）
    private String title;         // 小说标题
    private String intro;         // 简介
    private Integer wordCount;    // 字数
    private String coverUrl;      // 封面URL
    private LocalDateTime updateTime; // 小说更新时间
    private Integer updateStatus;

    private Integer readCount;

    // 关联 user 表（作者笔名）
    private String authorPenName;

    // 关联 novels_type 表（小说类型名）
    private String typeName;
}