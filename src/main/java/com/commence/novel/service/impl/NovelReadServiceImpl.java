package com.commence.novel.service.impl;

import com.commence.novel.DTO.ReadCountDTO;
import com.commence.novel.entity.Novel; // 新增：需导入Novel实体
import com.commence.novel.entity.NovelRead;
import com.commence.novel.repository.NovelRepository; // 新增：需注入NovelDao
import com.commence.novel.repository.NovelReadRepository;
import com.commence.novel.service.NovelReadService;
import com.commence.novel.utils.Result;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class NovelReadServiceImpl implements NovelReadService {
    @Autowired
    private NovelReadRepository novelReadRepository;

    @Autowired
    private NovelRepository novelRepository; // 用于校验小说作者

    @Override
    public Result addReadRecord(Long userId, Long novelId, Long chapterId) {
        if (novelId == null || chapterId == null) {
            return Result.error("1060","小说ID、章节ID不能为空");
        }

        // 1. 先校验小说是否存在（提前返回，避免后续无效操作）
        Novel novel = novelRepository.findById(novelId).orElse(null);
        if (novel == null) {
            return Result.error("1031", "小说不存在");
        }

        // 2. 登录用户：保存阅读记录（用于“继续阅读”功能）
        if (userId != null) {
            NovelRead readRecord = new NovelRead();
            readRecord.setUserId(userId);
            readRecord.setNovelId(novelId);
            readRecord.setChapterId(chapterId);
            readRecord.setReadTime(LocalDateTime.now());
            novelReadRepository.save(readRecord);
        }

        // 3. 更新阅读量 + 强制刷库
        // 避免事务延迟提交导致数据库不更新
        Integer currentReadCount = novel.getReadCount() == null ? 0 : novel.getReadCount();
        novel.setReadCount(currentReadCount + 1);
        novelRepository.saveAndFlush(novel); // aveAndFlush，强制刷新到数据库

        return Result.success(null, "阅读量统计成功");
    }

    @Override
    public Result getLatestRead(Long userId, Long novelId) {
        if (userId == null || novelId == null) {
            return Result.error("1020","用户ID和小说ID不能为空");
        }
        Optional<NovelRead> latestRead = novelReadRepository.findFirstByUserIdAndNovelIdOrderByReadTimeDesc(userId,novelId);
        return Result.success(latestRead.orElse(null),null);
    }

    @Override
    public Result getReadRecordByUserId(Long userId) {
        if (userId == null) {
            return Result.error("1020","用户ID不能为空");
        }
        List<NovelRead> readList = novelReadRepository.findByUserIdOrderByReadTimeDesc(userId);
        return Result.success(readList,null);
    }

    @Override
    public Result countReadByNovelId(Long novelId){
        if (novelId == null) {
            return Result.error("1030","小说ID不能为空");
        }
        List<NovelRead> allRead = novelReadRepository.findByNovelId(novelId);
        long totalReadCount = allRead.size();
        long uniqueUserCount = allRead.stream().map(NovelRead::getUserId).distinct().count();
        ReadCountDTO countdto = new ReadCountDTO(totalReadCount, uniqueUserCount);
        return Result.success(countdto,null);
    }

    // ========== 无权限校验 ==========
    @Override
    public Result getReadTrend(Long novelId, String timeType) {
        if (novelId == null) {
            return Result.error("1030","小说ID不能为空");
        }
        if (timeType == null || !("day".equals(timeType) || "week".equals(timeType) || "month".equals(timeType))) {
            return Result.error("1068","时间维度只能是day/week/month");
        }

        Map<String, List<Object>> resultMap = new HashMap<>();
        List<Object> xAxisData = new ArrayList<>();
        List<Object> seriesData = new ArrayList<>();

        if ("day".equals(timeType)) {
            List<Map<String, Object>> dayData = novelReadRepository.countByDay(novelId);
            for(Map<String, Object> data : dayData) {
                xAxisData.add(data.get("date"));
                seriesData.add(data.get("readCount"));
            }
        } else if ("week".equals(timeType)) {
            List<Map<String, Object>> weekData = novelReadRepository.countByWeek(novelId);
            for(Map<String, Object> data : weekData) {
                xAxisData.add(data.get("week"));
                seriesData.add(data.get("readCount"));
            }
        } else if ("month".equals(timeType)) {
            List<Map<String, Object>> monthData = novelReadRepository.countByMonth(novelId);
            for(Map<String, Object> data : monthData) {
                xAxisData.add(data.get("month"));
                seriesData.add(data.get("readCount"));
            }
        }

        resultMap.put("xAxisData", xAxisData);
        resultMap.put("seriesData", seriesData);

        return Result.success(resultMap,null);
    }

    @Override
    public Result getLastReadChapterId(Long userId, Long novelId) {
        if (userId == null || novelId == null) {
            return Result.error("1020", "用户ID和小说ID不能为空");
        }
        // 复用原有查询逻辑：获取最新的阅读记录 → 提取章节ID
        Optional<NovelRead> latestRead = novelReadRepository.findFirstByUserIdAndNovelIdOrderByReadTimeDesc(userId, novelId);
        Long chapterId = latestRead.map(NovelRead::getChapterId).orElse(null);
        return Result.success(chapterId, null);
    }

    @Override
    public Result saveReadRecord(Long userId, Long novelId, Long chapterId) {
        if (userId == null || novelId == null || chapterId == null) {
            return Result.error("1060", "用户ID、小说ID、章节ID不能为空");
        }
        // 逻辑：有则更新，无则新增
        Optional<NovelRead> existRecord = novelReadRepository.findFirstByUserIdAndNovelIdOrderByReadTimeDesc(userId, novelId);
        NovelRead readRecord = existRecord.orElse(new NovelRead());

        readRecord.setUserId(userId);
        readRecord.setNovelId(novelId);
        readRecord.setChapterId(chapterId);
        readRecord.setReadTime(LocalDateTime.now()); // 更新最新阅读时间

        NovelRead savedRecord = novelReadRepository.save(readRecord);
        return Result.success(savedRecord, null);
    }
}