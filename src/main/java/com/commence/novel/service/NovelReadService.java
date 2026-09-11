package com.commence.novel.service;

import com.commence.novel.utils.Result;

public interface NovelReadService {
    // 新增阅读记录
    Result addReadRecord(Long userId, Long novelId, Long chapterId);

    // 查询用户小说最新阅读记录
    Result getLatestRead(Long userId, Long novelId);

    // 查询用户所有阅读记录
    Result getReadRecordByUserId(Long userId);

    // 统计小说阅读量
    Result countReadByNovelId(Long novelId);

    // 查询小说阅读趋势（基础版：无权限校验）
    Result getReadTrend(Long novelId, String timeType);

    //获取用户最后阅读的章节
    Result getLastReadChapterId(Long userId, Long novelId);

    //保存/更新阅读记录
    Result saveReadRecord(Long userId, Long novelId, Long chapterId);
}