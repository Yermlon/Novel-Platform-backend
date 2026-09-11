package com.commence.novel.service;


import com.commence.novel.utils.Result;

public interface NovelCollectService {
    //查询收藏状态
    Result isCollected(Long userId, Long novelId);

    //新增收藏
    Result collectNovel(Long userId, Long novelId, Long categoryId);

    //取消收藏
    Result cancelCollect(Long userId, Long novelId);

    //查询用户所有收藏
    Result getCollectsByUserId(Long userId);

    //查询小说收藏数
    Result getCollectCountByNovelId(Long novelId);

    //书架的小说列表查询
    Result getCollectsByUserIdAndCategoryId(Long userId, Long categoryId, String sortType);
}
