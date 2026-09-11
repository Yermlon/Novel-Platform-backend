package com.commence.novel.service.impl;

import com.commence.novel.DTO.UserReadSettingDTO;
import com.commence.novel.DTO.UserReadProgressDTO;
import com.commence.novel.entity.UserReadProgress;
import com.commence.novel.entity.UserReadSetting;
import com.commence.novel.repository.UserReadProgressRepository;
import com.commence.novel.repository.UserReadSettingRepository;
import com.commence.novel.service.UserReadService;
import com.commence.novel.utils.Result;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserReadServiceImpl implements UserReadService {
    @Resource
    private UserReadSettingRepository userReadSettingRepository;

    @Resource
    private UserReadProgressRepository userReadProgressRepository;

    @Override
    public Result<UserReadSettingDTO> getReadSetting(Long userId) {
        if (userId == null) { // 仅判空，无需判空字符串
            return Result.error("1040","用户ID不能为空");
        }

        // 若数据库中 userId 是 String，需转换：String userIdStr = String.valueOf(userId);
        Optional<UserReadSetting> settingOpt = userReadSettingRepository.findByUserId(userId.toString());
        UserReadSetting setting = settingOpt.orElse(null);

        UserReadSettingDTO dto = new UserReadSettingDTO();
        dto.setUserId(userId.toString()); // 若DTO中userId是String，转String
        dto.setFontSize(17);
        dto.setBgColor("#f8f6f2");
        dto.setTextColor("#333333");
        dto.setLineHeight(new BigDecimal("1.8"));

        if (setting != null) {
            BeanUtils.copyProperties(setting,dto);
        }
        return Result.success(dto,null);
    }

    @Override
    public Result<Void> saveReadSetting(Long userId, UserReadSettingDTO dto) {
        if (userId == null) {
            return Result.error("1041","用户ID不能为空");
        }
        if (dto == null) {
            return Result.error("1042","阅读设置数据不能为空");
        }

        // 若数据库中 userId 是 String，转换：
        String userIdStr = String.valueOf(userId);
        Optional<UserReadSetting> settingOpt = userReadSettingRepository.findByUserId(userIdStr);
        UserReadSetting setting = settingOpt.orElse(null);

        if (setting == null) {
            setting = new UserReadSetting();
            // 强制设置当前登录用户ID，覆盖DTO中的值（防止伪造）
            setting.setUserId(userIdStr);
            BeanUtils.copyProperties(dto, setting, "id", "userId");
            setting.setUpdateTime(LocalDateTime.now());

            if (setting.getFontSize() == null) setting.setFontSize(17);
            if (setting.getBgColor() == null) setting.setBgColor("#f8f6f2");
            if (setting.getTextColor() == null) setting.setTextColor("#333333");
            if (setting.getLineHeight() == null) setting.setLineHeight(new BigDecimal("1.8"));

            userReadSettingRepository.save(setting);
        } else {
            BeanUtils.copyProperties(dto, setting, "id", "userId"); // 排除userId
            setting.setUserId(userIdStr); // 确保用户ID正确
            setting.setUpdateTime(LocalDateTime.now());

            userReadSettingRepository.save(setting);
        }
        return Result.success(null,null);
    }

    @Override
    public Result<UserReadProgressDTO> getProgress(Long userId, String novelId) {
        if (userId == null) {
            return Result.error("1050", "用户ID不能为空");
        }
        if (novelId == null || novelId.trim().isEmpty()) {
            return Result.error("1051", "小说ID不能为空");
        }

        // 若数据库中 userId 是 String，转换：
        String userIdStr = String.valueOf(userId);
        Optional<UserReadProgress> progressOpt = userReadProgressRepository.findByUserIdAndNovelId(userIdStr, novelId);
        UserReadProgress progress = progressOpt.orElse(null);

        UserReadProgressDTO dto = new UserReadProgressDTO();
        if (progress != null) {
            BeanUtils.copyProperties(progress, dto);
        }
        return Result.success(dto, null);
    }

    @Override
    public Result<Void> saveProgress(Long userId, UserReadProgressDTO dto) {
        if (userId == null) {
            return Result.error("1050", "用户ID不能为空");
        }
        if (dto == null) {
            return Result.error("1052", "进度数据不能为空");
        }
        if (dto.getNovelId() == null || dto.getNovelId().trim().isEmpty()) {
            return Result.error("1051", "小说ID不能为空");
        }
        if (dto.getChapterId() == null || dto.getChapterId().trim().isEmpty()) {
            return Result.error("1053", "章节ID不能为空");
        }

        // 若数据库中 userId 是 String，转换：
        String userIdStr = String.valueOf(userId);
        Optional<UserReadProgress> progressOpt = userReadProgressRepository.findByUserIdAndNovelId(userIdStr, dto.getNovelId());
        UserReadProgress progress = progressOpt.orElse(null);

        if (progress == null) {
            progress = new UserReadProgress();
            // 强制设置当前登录用户ID
            progress.setUserId(userIdStr);
            BeanUtils.copyProperties(dto, progress, "id", "userId", "createTime");
            progress.setReadTime(LocalDateTime.now());
            progress.setUpdateTime(LocalDateTime.now());

            if (progress.getProgress() == null) progress.setProgress(0);

            userReadProgressRepository.save(progress);
        } else {
            BeanUtils.copyProperties(dto, progress, "id", "userId", "createTime");
            progress.setUserId(userIdStr); // 确保用户ID正确
            progress.setReadTime(LocalDateTime.now());
            progress.setUpdateTime(LocalDateTime.now());

            userReadProgressRepository.save(progress);
        }

        return Result.success(null);
    }

    @Override
    public Result<UserReadProgressDTO> getLastReadProgress(Long userId, String novelId) {
        if (userId == null) {
            return Result.error("1050", "用户ID不能为空");
        }
        if (novelId == null || novelId.trim().isEmpty()) {
            return Result.error("1051", "小说ID不能为空");
        }

        // 转换用户ID（数据库中是String）
        String userIdStr = String.valueOf(userId);
        // 查询该用户对这本小说的最后阅读记录（按更新时间倒序取第一条）
        Optional<UserReadProgress> progressOpt = userReadProgressRepository.findTopByUserIdAndNovelIdOrderByUpdateTimeDesc(userIdStr, novelId);

        UserReadProgressDTO dto = new UserReadProgressDTO();
        if (progressOpt.isPresent()) {
            BeanUtils.copyProperties(progressOpt.get(), dto);
        } else {
            // 无记录时返回空DTO（前端判断后跳转到第一章）
            dto.setNovelId(novelId);
            dto.setProgress(0);
        }
        return Result.success(dto, null);
    }
}