package com.commence.novel.service;

import com.commence.novel.DTO.ChapterDTO;
import com.commence.novel.DTO.ChapterSortDTO;
import com.commence.novel.utils.Result;

import java.util.List;

public interface ChapterService {
    //创建章节
    Result createChapter(ChapterDTO chapterDTO, Long authorId);

    //编辑章节内容
    Result editChapter(Long chapterId, ChapterDTO chapterDTO, Long authorId);

    //章节自动保存草稿
    Result autoSaveDraft(Long chapterId, String draftContent, String title, String brief, Long authorId,String msg);

    //发布章节
    Result publishChapter(Long chapterId,Long authorId);

    //章节排序调整逻辑(未发布)
    Result adjustChapterSort(Long novelId, List<ChapterSortDTO> chapterSortList, Long authorId);

    //章节排序调整(已发布)
    Result adjustPublishSort(Long novelId, List<ChapterSortDTO> chapterSortList, Long authorId);

    //查询章节列表
    Result getChapterList(Long novelId);

    //删除章节
    Result deleteChapter(Long chapterId, Long authorId);

    //下架已发布章节
    Result offlineChapter(Long chapterId, Long authorId);

    //恢复已发布章节
    Result restoreChapter(Long chapterId, Long authorId);

    //获取章节内容（读者端）
    Result getChapterContent(Long chapterId);

  }
