package com.commence.novel.DTO;

import lombok.Data;

import java.util.Map;

@Data
public class StatisticsDTO {
    /** 平台总用户数 */
    private Long totalUserCount;
    /** 小说总数 */
    private Long totalNovelCount;
    /** 登录用户日活：key=日期(yyyy-MM-dd)，value=活跃用户数 */
    private Map<String, Long> dauData;
    /** 登录用户月活：key=月份(yyyy-MM)，value=活跃用户数 */
    private Map<String, Long> mauData;
    /** 整体访问量（含游客）：key=日期(yyyy-MM-dd)，value=访问次数 */
    private Map<String, Long> visitData;
}
