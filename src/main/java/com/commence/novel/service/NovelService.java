package com.commence.novel.service;

import com.commence.novel.DTO.NovelDTO;
import com.commence.novel.DTO.NovelsTypeDTO;
import com.commence.novel.entity.Novel;
import com.commence.novel.utils.Result;

import java.util.List;

public interface NovelService {
    //创建小说（草稿
    Result createNovel(NovelDTO novelDTO, Long authorId);

    //编辑小说基本信息
    Result updateNovel(Long novelId, NovelDTO novelDTO, Long authorId);

    //小说状态管理
    Result offlineNovel(Long novelId, Long authorId);

    //提交小说审核
    Result submitReview(Long novelId, Long authorId);

    //作者查询名下小说列表
    Result getAuthorNovels(Long authorId, Integer pageNum, Integer pageSize, Novel.NovelStatus status, String name);

    //删除小说草稿
    Result deleteNovel(Long novelId, Long authorId);

    //查询小说详情
    Result getNovelDetail(Long novelId, Long authorId);

    //查询小说类型
    List<NovelsTypeDTO> listAllValidNovelsType();

    /**
     *
     * 以下的authorid要改成管理员的，是管理员的功能
     *
     * **/

    //审核通过
    Result passReview(Long novelId, Long reviewerId);

    //审核驳回
    Result rejectNovel(Long novelId, String rejectReason,Long reviewerId);

    //修改已发布小说详情通过
    Result SpassReview(Long novelId, Long reviewerId);

    //修改已发布小说详情驳回
    Result SrejectNovel(Long novelId,String reason, Long reviewerId);

    //作者查询复审状态
    Result getSNovelDetail(Long novelId, Long authorId);

    //管理员查询待审核小说列表
    Result getReviewNovelList(Integer pageNum, Integer pageSize, String status);

    //分页列表、分类筛选、热门排序
    Result getBookStoreNovels(Integer pageNum, Integer pageSize, Long typeId, String sortType, Long wordCount, Long updateStatus);

    //小说详情
    Result getPublicNovelDetail(Long novelId);

    //访问作者名下小说列表（all
    Result getAuthorPublicWorks(Long authorId);

    //小说搜索（按标题、作者名
    Result searchNovels(String keyword, Integer pageNum, Integer pageSize);

    //批量查询
    Result getNovelDetailsBatch(List<Long> novelIds);

    // 根据ID获取小说（用于权限校验）
    Novel getNovelById(Long novelId);


}
