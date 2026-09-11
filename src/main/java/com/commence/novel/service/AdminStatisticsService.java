package com.commence.novel.service;

import com.commence.novel.utils.Result;

public interface AdminStatisticsService {
    /**
     * 获取平台核心统计数据
     */
    Result getPlatformStatistics();

    /**
     * 按角色统计用户数
     */
    Result countUserByRole();
}
