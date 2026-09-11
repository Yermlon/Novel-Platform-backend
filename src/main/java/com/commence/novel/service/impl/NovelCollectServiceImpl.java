package com.commence.novel.service.impl;

import com.commence.novel.DTO.NovelCollectDTO;
import com.commence.novel.entity.Novel;
import com.commence.novel.entity.NovelCollect;
import com.commence.novel.entity.NovelsType;
import com.commence.novel.entity.User;
import com.commence.novel.repository.NovelCollectRepository;
import com.commence.novel.repository.NovelRepository;
import com.commence.novel.repository.NovelsTypeRepository;
import com.commence.novel.repository.UserRepository;
import com.commence.novel.service.NovelCollectService;
import com.commence.novel.utils.Result;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class NovelCollectServiceImpl implements NovelCollectService {
    @Autowired
    private NovelCollectRepository novelCollectRepository;

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NovelsTypeRepository novelsTypeRepository;

    @Override
    public Result isCollected(Long userId, Long novelId) {
        if (userId == null || novelId == null) {
            return Result.error("1020","用户ID和小说ID不能为空");
        }
        Optional<NovelCollect> existCollect = novelCollectRepository.findByUserIdAndNovelId(userId,novelId);
        Boolean isCollected = existCollect.isPresent();
        return Result.success(isCollected,null);
    }

    @Override
    public Result collectNovel(Long userId, Long novelId, Long categoryId) {
        if (userId == null || novelId == null) {
            return Result.error("1020","用户ID和小说ID不能为空");
        }

        Optional<NovelCollect> existCollect = novelCollectRepository.findByUserIdAndNovelId(userId, novelId);
        if (existCollect.isPresent()) {
            return Result.error("1024","已收藏该小说，无需重复收藏");
        }
        NovelCollect collect = new NovelCollect();
        collect.setUserId(userId);
        collect.setNovelId(novelId);
        collect.setCategoryId(categoryId);
        collect.setCollectTime(LocalDateTime.now());
        novelCollectRepository.save(collect);

        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            novel.setCollectCount((novel.getCollectCount() == null ? 0 : novel.getCollectCount()) + 1);
            novelRepository.save(novel);
        }

        return Result.success(null,"收藏小说成功");
    }

    @Override
    public Result cancelCollect(Long userId, Long novelId) {
        if (userId == null || novelId == null) {
            return Result.error("1020","用户ID和小说ID不能为空");
        }
        Optional<NovelCollect> collectOpt = novelCollectRepository.findByUserIdAndNovelId(userId, novelId);
        if (collectOpt.isEmpty()) {
            return Result.error("1025","未收藏该小说，无法取消");
        }

        NovelCollect collect = collectOpt.get();
        novelCollectRepository.delete(collect);

        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            int newCount = (novel.getCollectCount() == null ? 0 : novel.getCollectCount()) - 1;
            novel.setCollectCount(Math.max(newCount, 0));
            novelRepository.save(novel);
        }

        return Result.success(null,"取消收藏成功");
    }

    @Override
    public Result getCollectsByUserId(Long userId) {
        if (userId == null) {
            return Result.error("1020","用户ID不能为空");
        }
        List<NovelCollect> collectList = novelCollectRepository.findByUserId(userId);
        // 转换为DTO列表（包含小说详情）
        List<NovelCollectDTO> dtoList = convertToDTOList(collectList);
        return Result.success(dtoList,"查询所有收藏成功");
    }

    @Override
    public Result getCollectCountByNovelId(Long novelId) {
        if (novelId == null) {
            return Result.error("1030","小说ID不能为空");
        }
        long count = novelCollectRepository.countByNovelId(novelId);
        return Result.success(count,null);
    }

    public Result getCollectsByUserIdAndCategoryId(Long userId, Long categoryId, String sortType) {
        if (userId == null) {
            return Result.error("1020","用户ID不能为空");
        }
        if (categoryId == null) {
            return Result.error("1060","分类ID不能为空");
        }

        // 1. 查询收藏记录
        List<NovelCollect> collectList = novelCollectRepository.findByUserIdAndCategoryId(userId, categoryId);

        // 2. 先转成 DTO
        List<NovelCollectDTO> dtoList = convertToDTOList(collectList);

        // 3. 在 DTO 列表上排序
        if (dtoList != null && !dtoList.isEmpty()) {
            // 默认：按收藏时间降序（最新收藏在最上面）
            if ("collect".equals(sortType) || sortType == null) {
                dtoList.sort((d1, d2) -> {
                    if (d1.getCollectTime() == null && d2.getCollectTime() == null) return 0;
                    if (d1.getCollectTime() == null) return 1;
                    if (d2.getCollectTime() == null) return -1;
                    return d2.getCollectTime().compareTo(d1.getCollectTime());
                });
            }
            // 按字数降序
            else if ("wordCount".equals(sortType)) {
                dtoList.sort((d1, d2) -> {
                    Integer wc1 = d1.getWordCount() == null ? 0 : d1.getWordCount();
                    Integer wc2 = d2.getWordCount() == null ? 0 : d2.getWordCount();
                    return wc2.compareTo(wc1);
                });
            }
            // 按阅读量降序
            else if ("readCount".equals(sortType)) {
                dtoList.sort((d1, d2) -> {
                    Integer rc1 = d1.getReadCount() == null ? 0 : d1.getReadCount();
                    Integer rc2 = d2.getReadCount() == null ? 0 : d2.getReadCount();
                    return rc2.compareTo(rc1);
                });
            }
        }

        return Result.success(dtoList,"查询分类下收藏小说成功");
    }

    private List<NovelCollectDTO> convertToDTOList(List<NovelCollect> collectList) {
        return collectList.stream().map(collect -> {
            NovelCollectDTO dto = new NovelCollectDTO();
            // 1. 复制收藏记录基础字段
            dto.setId(collect.getId());
            dto.setNovelId(collect.getNovelId());
            dto.setUserId(collect.getUserId());
            dto.setCategoryId(collect.getCategoryId());
            dto.setCollectTime(collect.getCollectTime());

            // 2. 查询小说详情并填充
            Optional<Novel> novelOpt = novelRepository.findById(collect.getNovelId());
            if (novelOpt.isPresent()) {
                Novel novel = novelOpt.get();
                // 填充小说基础字段
                dto.setTitle(novel.getTitle());
                dto.setIntro(novel.getIntro());
                dto.setWordCount(novel.getWordCount());
                dto.setCoverUrl(novel.getCoverUrl());

                LocalDateTime lastUpdateTime = novel.getLastUpdateTime();
                if (lastUpdateTime == null) {
                    lastUpdateTime = novel.getCreateTime(); // 用创建时间兜底
                }
                dto.setUpdateTime(lastUpdateTime);

                // 3. 关联查询 user 表：获取作者笔名
                Optional<User> userOpt = userRepository.findById(novel.getAuthorId());
                if (userOpt.isPresent()) {
                    User author = userOpt.get();
                    dto.setAuthorPenName(author.getPenName()); // 从 user 表取 pen_name
                }

                // 🌟 4. 关联查询 novels_type 表：获取小说类型名
                Optional<NovelsType> typeOpt = novelsTypeRepository.findById(novel.getTypeId());
                if (typeOpt.isPresent()) {
                    NovelsType type = typeOpt.get();
                    dto.setTypeName(type.getTypeName()); // 从 novels_type 表取 type_name
                }

                // ========== 补充小说更新状态 ==========
                // 直接取 novel 的 updateStatus 数字字段（0=连载中，1=暂停，2=已完结）
                Integer updateStatus = novel.getUpdateStatus();
                // 兜底处理：如果字段为null，默认0（连载中）
                dto.setUpdateStatus(updateStatus == null ? 0 : updateStatus);
            }
            return dto;
        }).collect(Collectors.toList());
    }

}
