package com.commence.novel.service.impl;

import com.commence.novel.service.SensitiveWordService;
import com.commence.novel.utils.AutoAuditUtils;
import com.commence.novel.utils.Result;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Service
@Transactional
public class SensitiveWordServiceImpl implements SensitiveWordService {
    // ========== 实现：添加敏感词 ==========
    @Override
    public Result addSensitiveWord(String word) {
        // 1. 参数校验
        if (StringUtils.isBlank(word)) {
            return Result.error("400", "敏感词不能为空");
        }
        // 2. 去重校验
        List<String> sensitiveWords = new ArrayList<>(AutoAuditUtils.getSensitiveWords());
        if (sensitiveWords.contains(word.trim())) {
            return Result.error("400", "该敏感词已存在");
        }
        // 3. 调用工具类添加
        AutoAuditUtils.addSensitiveWord(word.trim());
        return Result.success(null, "敏感词添加成功");
    }

    // ========== 实现：查询所有敏感词 ==========
    @Override
    public Result getSensitiveWordList() {
        List<String> sensitiveWords = new ArrayList<>(AutoAuditUtils.getSensitiveWords());
        return Result.success(sensitiveWords, "查询敏感词列表成功");
    }

    // ========== 可选：删除敏感词 ==========
    @Override
    public Result deleteSensitiveWord(String word) {
        if (StringUtils.isBlank(word)) {
            return Result.error("400", "敏感词不能为空");
        }
        AutoAuditUtils.removeSensitiveWord(word.trim());
        return Result.success(null, "敏感词删除成功");
    }
}