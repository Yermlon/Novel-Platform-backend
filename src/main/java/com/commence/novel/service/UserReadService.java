package com.commence.novel.service;

import com.commence.novel.DTO.UserReadProgressDTO;
import com.commence.novel.DTO.UserReadSettingDTO;
import com.commence.novel.utils.Result;

public interface UserReadService {
    // 获取用户阅读设置
    Result<UserReadSettingDTO> getReadSetting(Long userId);

    // 保存/更新用户阅读设置
    Result<Void> saveReadSetting(Long userId, UserReadSettingDTO dto);

    // 获取用户小说阅读进度
    Result<UserReadProgressDTO> getProgress(Long userId, String novelId);

    // 保存阅读进度
    Result<Void> saveProgress(Long userId, UserReadProgressDTO dto);

    //获取最后的阅读记录
    Result<UserReadProgressDTO> getLastReadProgress(Long userId, String novelId);
}