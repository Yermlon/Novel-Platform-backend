package com.commence.novel.service;

import com.commence.novel.utils.Result;

public interface SensitiveWordService {
    // 添加敏感词
    Result addSensitiveWord(String word);
    // 查询所有敏感词
    Result getSensitiveWordList();
    // 删除敏感词
    Result deleteSensitiveWord(String word);
}
