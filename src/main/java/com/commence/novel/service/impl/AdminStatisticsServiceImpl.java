package com.commence.novel.service.impl;

import com.commence.novel.DTO.StatisticsDTO; // 需先创建这个DTO
import com.commence.novel.repository.*;
import com.commence.novel.service.AdminStatisticsService;
import com.commence.novel.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminStatisticsServiceImpl implements AdminStatisticsService {
    private static final Logger log = LoggerFactory.getLogger(AdminStatisticsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NovelRepository novelRepository;


    @Autowired
    private StatisticsDao statisticsDao;

    /**
     * 获取平台核心统计数据（总用户、总小说、日活/月活、整体访问量）
     */
    @Override
    public Result getPlatformStatistics() {
        try {
            StatisticsDTO dto = new StatisticsDTO();

            // 1. 总用户数（复用UserDao）
            Long totalUserCount = userRepository.countTotalUsers();
            dto.setTotalUserCount(totalUserCount);

            // 2. 总小说数（复用NovelDao）
            Long totalNovelCount = novelRepository.countTotalNovels();
            dto.setTotalNovelCount(totalNovelCount);

            // 3. 登录用户日活（阅读+评论+收藏+点赞，去重）
            List<Map<String, Object>> dauList = statisticsDao.countLoginUserDAU();
            Map<String, Long> dauMap = new HashMap<>();
            for (Map<String, Object> item : dauList) {
                String date = (String) item.get("date");
                Long count = ((Number) item.get("activeUserCount")).longValue();
                dauMap.put(date, count);
            }
            dto.setDauData(dauMap);

            // 4. 登录用户月活（阅读+评论+收藏+点赞，去重）
            List<Map<String, Object>> mauList = statisticsDao.countLoginUserMAU();
            Map<String, Long> mauMap = new HashMap<>();
            for (Map<String, Object> item : mauList) {
                String month = (String) item.get("month");
                Long count = ((Number) item.get("activeUserCount")).longValue();
                mauMap.put(month, count);
            }
            dto.setMauData(mauMap);

            // 5. 整体访问量（含游客，基于登录日志）
            List<Map<String, Object>> visitList = statisticsDao.countTotalVisit();
            Map<String, Long> visitMap = new HashMap<>();
            for (Map<String, Object> item : visitList) {
                String date = (String) item.get("date");
                Long count = ((Number) item.get("visitCount")).longValue();
                visitMap.put(date, count);
            }
            dto.setVisitData(visitMap);

            return Result.success(dto, "获取平台统计数据成功");
        } catch (Exception e) {
            log.error("获取平台统计数据失败", e);
            return Result.error("500", "统计数据查询失败：" + e.getMessage());
        }
    }

    /**
     * 按角色统计用户数（管理员后台常用）
     */
    @Override
    public Result countUserByRole() {
        try {
            Map<String, Long> roleCountMap = new HashMap<>();
            roleCountMap.put("ADMIN", userRepository.countByRole("ADMIN"));
            roleCountMap.put("AUTHOR", userRepository.countByRole("AUTHOR"));
            roleCountMap.put("READER", userRepository.countByRole("READER"));
            return Result.success(roleCountMap, "按角色统计用户数成功");
        } catch (Exception e) {
            log.error("按角色统计用户数失败", e);
            return Result.error("500", "按角色统计失败：" + e.getMessage());
        }
    }
}
