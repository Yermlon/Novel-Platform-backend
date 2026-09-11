package com.commence.novel.service.impl;

import com.commence.novel.DTO.NovelDTO;
import com.commence.novel.DTO.NovelsTypeDTO;
import com.commence.novel.entity.*;
import com.commence.novel.repository.*;
import com.commence.novel.service.NovelService;
import com.commence.novel.utils.Result;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional
@Slf4j
public class NovelServiceImpl implements NovelService {
    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private NovelsTypeRepository novelsTypeRepository;

    @Autowired
    private ReviewRecordRepository reviewRecordRepository;

    @Autowired
    private ChapterUpdateLogRepository chapterUpdateLogRepository;

    @Override
    public Result createNovel(NovelDTO novelDTO, Long authorId) {
        if (StringUtils.isBlank(novelDTO.getTitle())) {
            return Result.error("1030", "小说名不能为空");
        }

        if (novelDTO.getTypeId() == null) {
            return Result.error("1030", "小说分类不能为空");
        }

        Novel novel = new Novel();
        novel.setAuthorId(authorId);
        novel.setTitle(novelDTO.getTitle());
        novel.setCoverUrl(novelDTO.getCoverUrl());
        novel.setIntro(novelDTO.getIntro());
        novel.setTypeId(novelDTO.getTypeId());
        novel.setTypeId(novelDTO.getTypeId());
        novel.setStatus(Novel.NovelStatus.DRAFT); //默认草稿
        novel.setCreateTime(LocalDateTime.now());
        novel.setUpdateTime(LocalDateTime.now());

        Novel savedNovel = novelRepository.save(novel);
        return Result.success(savedNovel.getId(), "小说创建成功（草稿）");

    }

    @Override
    public Result updateNovel(Long novelId, NovelDTO novelDTO, Long authorId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("1031:小说不存在"));
        if (!novel.getAuthorId().equals(authorId)) {
            return Result.error("1032", "无权编辑该小说");
        }

        Novel.NovelStatus status = novel.getStatus();
        if (Novel.NovelStatus.PUBLISHED.equals(status)) {
            boolean hasPendingChanges = false;
            if (StringUtils.isNotBlank(novelDTO.getTitle()) && !novelDTO.getTitle().equals(novel.getTitle())) {
                novel.setPendingTitle(novelDTO.getTitle());
                hasPendingChanges = true;
            }
            if (StringUtils.isNotBlank(novelDTO.getIntro()) && !novelDTO.getIntro().equals(novel.getIntro())) {
                novel.setPendingIntro(novelDTO.getIntro());
                hasPendingChanges = true;
            }
            if (StringUtils.isNotBlank(novelDTO.getCoverUrl()) && !novelDTO.getCoverUrl().equals(novel.getCoverUrl())) {
                novel.setPendingCoverUrl(novelDTO.getCoverUrl());
                hasPendingChanges = true;
            }
            if (novelDTO.getTypeId() != null && !novelDTO.getTypeId().equals(novel.getTypeId())) {
                novel.setPendingTypeId(novelDTO.getTypeId());
                hasPendingChanges = true;
            }

            if (hasPendingChanges) {
                novel.setPendingStatus(Novel.PendingStatus.PENDING);
                novel.setReviewRejectReason(null);
            }

            String updateStatusStr = novelDTO.getUpdateStatus();
            if (updateStatusStr != null && !updateStatusStr.isBlank()) {
                Novel.NovelUpdateStatus updateStatusEnum = Novel.NovelUpdateStatus.fromString(updateStatusStr);
                if (updateStatusEnum == null) {
                    return Result.error("1047","无效的更新状态");
                }
                novel.setUpdateStatusEnum(updateStatusEnum);
            }

            novel.setUpdateTime(LocalDateTime.now());
            novelRepository.save(novel);

            String msg = hasPendingChanges ? "修改小说详情审核提交成功，请耐心等待审核" : "小说更新状态修改成功";
            return Result.success(null,msg);
        }

        if (novel.getStatus() == Novel.NovelStatus.DRAFT
            || novel.getStatus() == Novel.NovelStatus.REJECTED) {
            if (StringUtils.isNotBlank(novelDTO.getTitle())){
                novel.setTitle(novelDTO.getTitle());
            }
            if (StringUtils.isNotBlank(novelDTO.getIntro())){
                novel.setIntro(novelDTO.getIntro());
            }
            if (StringUtils.isNotBlank(novelDTO.getCoverUrl())){
                novel.setCoverUrl(novelDTO.getCoverUrl());
            }
            if (novelDTO.getTypeId() != null) {
                novel.setTypeId(novelDTO.getTypeId());
            }

            novel.setRejectReason(null);

            novel.setUpdateTime(LocalDateTime.now());
            novelRepository.save(novel);
            return Result.success(null,"小说信息更新成功");
        }

        return Result.error("1036","仅草稿、审核驳回状态下小说可编辑");

    }

    @Override
    public Result SpassReview(Long novelId, Long reviewerId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));

        // 校验详情修改状态
        if (novel.getPendingStatus() != Novel.PendingStatus.PENDING) {
            return Result.error("1051","该小说无待审核的详情修改");
        }

        // 更新详情审核状态
        novelRepository.updateDetail(novelId,Novel.PendingStatus.APPROVED,LocalDateTime.now());

        // 更新/创建审核记录（标记为「详情修改审核」）
        try {
            ReviewRecord reviewRecord = reviewRecordRepository.findFirstByNovelIdOrderBySubmitTimeDesc(novelId);
            if (reviewRecord == null) {
                reviewRecord = new ReviewRecord();
                reviewRecord.setNovelId(novelId);
                reviewRecord.setAuthorId(novel.getAuthorId());
                reviewRecord.setSubmitTime(LocalDateTime.now());
                reviewRecord.setReviewType(ReviewRecord.ReviewType.NOVEL_DETAIL); // 标记为详情修改审核
                reviewRecord.setReviewStatus(ReviewRecord.ReviewStatus.PENDING);
            }
            // 更新审核结果
            reviewRecord.setReviewStatus(ReviewRecord.ReviewStatus.APPROVED);
            reviewRecord.setReviewerId(reviewerId); // 审核人（管理员ID）
            reviewRecord.setReviewTime(LocalDateTime.now());
            reviewRecord.setReviewContent("小说详情修改审核通过：标题/简介等详情已更新"); // 审核意见
            reviewRecordRepository.save(reviewRecord);
        } catch (Exception e) {
            log.warn("更新审核记录失败，novelId={}", novelId, e);
        }

        return Result.success(null,"详情审核通过");
    }

    @Override
    public Result SrejectNovel(Long novelId, String reason, Long reviewerId) { // 注意：参数改为reviewerId（审核人，管理员ID）
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));

        // 校验详情修改状态
        if (novel.getPendingStatus() != Novel.PendingStatus.PENDING) {
            return Result.error("1051","该小说无待审核的详情修改");
        }

        // 更新详情审核状态
        novelRepository.updatePendingStatus(novelId,Novel.PendingStatus.REJECTED,reason,LocalDateTime.now());

        // 更新/创建审核记录（标记为「详情修改审核」）
        try {
            ReviewRecord reviewRecord = reviewRecordRepository.findFirstByNovelIdOrderBySubmitTimeDesc(novelId);
            if (reviewRecord == null) {
                reviewRecord = new ReviewRecord();
                reviewRecord.setNovelId(novelId);
                reviewRecord.setAuthorId(novel.getAuthorId());
                reviewRecord.setSubmitTime(LocalDateTime.now());
                reviewRecord.setReviewType(ReviewRecord.ReviewType.NOVEL_DETAIL); // 标记为详情修改审核
                reviewRecord.setReviewStatus(ReviewRecord.ReviewStatus.PENDING);
            }
            // 更新审核结果
            reviewRecord.setReviewStatus(ReviewRecord.ReviewStatus.REJECTED);
            reviewRecord.setReviewerId(reviewerId); // 审核人（管理员ID）
            reviewRecord.setReviewTime(LocalDateTime.now());
            reviewRecord.setReviewContent("小说详情修改审核驳回：" + reason); // 审核意见（含驳回原因）
            reviewRecordRepository.save(reviewRecord);
        } catch (Exception e) {
            log.warn("更新审核记录失败，novelId={}", novelId, e);
        }

        return Result.success(null,"详情审核驳回");
    }

    @Override
    public Result getSNovelDetail(Long novelId, Long authorId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031：小说不存在"));

        if (!novel.getAuthorId().equals(authorId)) {
            return Result.error("1032","无权查看");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("PendingStatus", novel.getPendingStatus());
        data.put("ReviewRejectReason", novel.getReviewRejectReason());
        data.put("pendingTitle", novel.getPendingTitle());
        data.put("pendingIntro", novel.getPendingIntro());
        data.put("pendingTypeId", novel.getPendingTypeId());
        data.put("pendingCoverUrl", novel.getPendingCoverUrl());
        return Result.success(data,"查询成功");
    }

    //下架 <-可发布
    @Override
    public Result offlineNovel(Long novelId, Long authorId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));
        if (!novel.getAuthorId().equals(authorId)) {
            return Result.error("1032", "无权限操作");
        }

        if (!Novel.NovelStatus.PUBLISHED.equals(novel.getStatus())) {
            return Result.error("1036", "仅发布状态的小说可下架");
        }

        novel.setStatus(Novel.NovelStatus.OFFLINE);
        novel.setUpdateTime(LocalDateTime.now());
        novelRepository.save(novel);
        return Result.success(null, "小说下架成功");
    }

    //->待审核
    @Override
    public Result submitReview(Long novelId, Long authorId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));
        if (!novel.getAuthorId().equals(authorId)) {
            return Result.error("1032", "无权限操作");
        }

        novel.setStatus(Novel.NovelStatus.PENDING_REVIEW);
        novel.setUpdateTime(LocalDateTime.now());
        novelRepository.save(novel);

        ReviewRecord reviewRecord = new ReviewRecord();
        reviewRecord.setNovelId(novelId);
        reviewRecord.setAuthorId(authorId);
        reviewRecord.setReviewStatus(ReviewRecord.ReviewStatus.PENDING);
        reviewRecord.setReviewType(ReviewRecord.ReviewType.NOVEL_PUBLISH);
        reviewRecord.setSubmitTime(LocalDateTime.now());
        reviewRecordRepository.save(reviewRecord);

        return Result.success(null, "小说已提交审核，等待审核结果");
    }

    @Override
    public Result getAuthorNovels(Long authorId, Integer pageNum, Integer pageSize, Novel.NovelStatus status, String name) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by("createTime").descending());
        Page<Novel> novelPage;

        Specification<Novel> spec = ((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("authorId"), authorId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.isNotBlank(name)) {
                predicates.add(cb.like(root.get("title"), "%" + name + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        novelPage = novelRepository.findAll(spec, pageable);

        Map<String, Object> result = new HashMap<>();
        result.put("records", novelPage.getContent());
        result.put("total", novelPage.getTotalElements());
        result.put("pages", novelPage.getTotalPages());

        String msg;
        if (novelPage.getTotalElements() > 0) {
            msg = "查询成功";
        } else {
            boolean hasFilter = (status != null) || StringUtils.isNotBlank(name);
            msg = hasFilter ? "未找到符合条件的小说" : "暂无小说数据";
        }
        return Result.success(result, msg);

    }


    @Override
    public Result deleteNovel(Long novelId, Long authorId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));

        if (!novel.getAuthorId().equals(authorId)) {
            return Result.error("1032", "无权限操作");
        }

        if (!Novel.NovelStatus.DRAFT.equals(novel.getStatus())
        && !Novel.NovelStatus.OFFLINE.equals(novel.getStatus())) {
            return Result.error("1046", "仅草稿或下架状态的小说可删除，已发布的小说请选择下架");
        }

        chapterRepository.deleteByNovelId(novelId);
        novelRepository.delete(novel);

        return Result.success(null, "小说删除成功");
    }

    @Override
    public Result getNovelDetail(Long novelId, Long authorId) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isEmpty()) {
            return Result.error("1031", "小说不存在");
        }
        Novel novel = novelOpt.get();

        if (!novel.getAuthorId().equals(authorId)) {
            return Result.error("1032", "无权限查看该小说详情");
        }

        NovelsType novelsType = novelsTypeRepository.findById(novel.getTypeId())
                .orElse(null);
        String typeName = novelsType != null ? novelsType.getTypeName() : "未知分类";

        Map<String, Object> detail = new HashMap<>();
        detail.put("novelId", novelId);
        detail.put("title", novel.getTitle());
        detail.put("coverUrl", novel.getCoverUrl());
        detail.put("intro", novel.getIntro());
        detail.put("TypeId", novel.getTypeId());
        detail.put("typeName", typeName);
        detail.put("createTime", novel.getCreateTime());
        detail.put("updateTime", novel.getUpdateTime());
        //小说状态
        detail.put("status", novel.getStatus());
        detail.put("updateStatus", novel.getUpdateStatus());
        detail.put("rejectReason", novel.getRejectReason());
        //创作数据
        detail.put("wordCount", novel.getWordCount());
        detail.put("readCount", novel.getReadCount());
        detail.put("collectCount", novel.getCollectCount());
        detail.put("commentCount", novel.getCommentCount());
        //章节数
        Long chapterCount = chapterRepository.countByNovelId(novelId);
        detail.put("chapterCount", chapterCount);

        //审核进度
        ReviewRecord reviewRecord = reviewRecordRepository.findFirstByNovelIdOrderBySubmitTimeDesc(novelId);
        if (reviewRecord != null) {
            Map<String, Object> reviewInfo = new HashMap<>();
            reviewInfo.put("submitTime", reviewRecord.getSubmitTime());
            reviewInfo.put("reviewStatus", reviewRecord.getReviewStatus().name());
            reviewInfo.put("rejectReason", reviewRecord.getReviewContent());
            detail.put("latestReviewInfo", reviewInfo);
        }

        return Result.success(detail, "小说详情查询成功");
    }

    @Override
    public List<NovelsTypeDTO> listAllValidNovelsType() {
        List<NovelsType> novelsTypeList = novelsTypeRepository.findByStatusOrderByTypeIdAsc(1);

        return novelsTypeList.stream()
                .map(novelsType -> {
                    NovelsTypeDTO dto = new NovelsTypeDTO();
                    dto.setLabel(novelsType.getTypeName());
                    dto.setValue(novelsType.getTypeId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Result passReview(Long novelId, Long reviewerId) {
        // 1. 校验小说是否存在
        Optional<Novel> optionalNovel = novelRepository.findById(novelId);
        if (optionalNovel.isEmpty()) {
            return Result.error("1031", "小说不存在");
        }

        Novel novel = optionalNovel.get();

        // 2. 校验小说状态是否为待审核
        if (!Novel.NovelStatus.PENDING_REVIEW.equals(novel.getStatus())) {
            return Result.error("1050", "仅待审核（PENDING_REVIEW）状态的小说可审核通过");
        }

        // 3. 核心逻辑：更新小说状态为已发布
        novel.setStatus(Novel.NovelStatus.PUBLISHED);
        novel.setUpdateStatus(Novel.NovelUpdateStatus.SERIALIZING.toIntValue());
        novel.setUpdateTime(LocalDateTime.now());
        novelRepository.save(novel);

        // 4. 补充：更新/创建审核记录（标记为「整体发布审核」）
        try {
            ReviewRecord latestReview = reviewRecordRepository.findFirstByNovelIdOrderBySubmitTimeDesc(novelId);
            if (latestReview == null) {
                // 无历史记录则新建
                latestReview = new ReviewRecord();
                latestReview.setNovelId(novelId);
                latestReview.setAuthorId(novel.getAuthorId()); // 小说作者ID
                latestReview.setSubmitTime(LocalDateTime.now()); // 提交时间（若有前端传参则用前端值）
                latestReview.setReviewType(ReviewRecord.ReviewType.NOVEL_PUBLISH); // 标记为整体发布审核
                latestReview.setReviewStatus(ReviewRecord.ReviewStatus.PENDING); // 初始状态
            }
            // 更新审核结果
            latestReview.setReviewStatus(ReviewRecord.ReviewStatus.APPROVED);
            latestReview.setReviewerId(reviewerId); // 审核人（管理员ID）
            latestReview.setReviewTime(LocalDateTime.now()); // 审核时间
            latestReview.setReviewContent("小说整体发布审核通过，状态更新为已发布"); // 审核意见
            reviewRecordRepository.save(latestReview);
        } catch (Exception e) {
            log.warn("更新审核记录失败，novelId={}", novelId, e);
            // 审核记录更新失败不影响核心流程，仅打日志
        }

        return Result.success(null, "小说审核通过，已发布为连载状态");
    }

    @Override
    public Result rejectNovel(Long novelId, String rejectReason, Long reviewerId) { // 注意：参数改为reviewerId（审核人，管理员ID）
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));

        // 校验小说状态
        if (!Novel.NovelStatus.PENDING_REVIEW.equals(novel.getStatus())) {
            return Result.error("1046", "仅待审核状态的小说可执行驳回操作");
        }

        // 更新小说状态
        novel.setStatus(Novel.NovelStatus.REJECTED);
        novel.setRejectReason(rejectReason);
        novel.setUpdateTime(LocalDateTime.now());
        novelRepository.save(novel);

        // 更新/创建审核记录（标记为「整体发布审核」）
        try {
            ReviewRecord reviewRecord = reviewRecordRepository.findFirstByNovelIdOrderBySubmitTimeDesc(novelId);
            if (reviewRecord == null) {
                reviewRecord = new ReviewRecord();
                reviewRecord.setNovelId(novelId);
                reviewRecord.setAuthorId(novel.getAuthorId());
                reviewRecord.setSubmitTime(LocalDateTime.now());
                reviewRecord.setReviewType(ReviewRecord.ReviewType.NOVEL_PUBLISH); // 标记为整体发布审核
                reviewRecord.setReviewStatus(ReviewRecord.ReviewStatus.PENDING);
            }
            // 更新审核结果
            reviewRecord.setReviewStatus(ReviewRecord.ReviewStatus.REJECTED);
            reviewRecord.setReviewerId(reviewerId); // 审核人（管理员ID）
            reviewRecord.setReviewTime(LocalDateTime.now());
            reviewRecord.setReviewContent("小说整体发布审核驳回：" + rejectReason); // 审核意见（含驳回原因）
            reviewRecordRepository.save(reviewRecord);
        } catch (Exception e) {
            log.warn("更新审核记录失败，novelId={}", novelId, e);
        }

        return Result.success(null, "小说审核驳回成功，已通知作者");
    }
    @Override
    public Result getBookStoreNovels(Integer pageNum, Integer pageSize, Long typeId, String sortType, Long wordCount, Long updateStatus) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        Specification<Novel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"),"PUBLISHED"));
            if (typeId != null && typeId != 0) {
                predicates.add(cb.equal(root.get("typeId"), typeId));
            }

            if (wordCount != null) {
                if (wordCount == 100000) {
                    // 10万以下
                    predicates.add(cb.lt(root.get("wordCount"), 100000));
                } else if (wordCount == 500000) {
                    // 10-50万
                    predicates.add(cb.between(root.get("wordCount"), 100000, 500000));
                } else if (wordCount == 1000000) {
                    // 50-100万
                    predicates.add(cb.between(root.get("wordCount"), 500000, 1000000));
                } else if (wordCount == 1000001) {
                    // 100万以上
                    predicates.add(cb.gt(root.get("wordCount"), 1000000));
                }
            }

            if (updateStatus != null) {
                // updateStatus 是 Integer 类型，直接等值匹配
                predicates.add(cb.equal(root.get("updateStatus"), updateStatus.intValue()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = null;
        if ("hot".equals(sortType)) {
            sort = Sort.by(Sort.Direction.DESC, "readCount","collectCount");
        } else if ("update".equals(sortType)) {
            sort = Sort.by(Sort.Direction.DESC, "updateTime");
        } else if ("wordCount".equals(sortType)) { // 补充：按字数排序
            sort = Sort.by(Sort.Direction.DESC, "wordCount");
        } else {
            sort = Sort.by(Sort.Direction.DESC, "readCount");
        }

        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        Page<Novel> novelPage = novelRepository.findAll(spec, pageable);

        Map<Long,String> typeIdToNameMap = new HashMap<>();
        List<NovelsType> allVaildTypes = novelsTypeRepository.findByStatusOrderByTypeIdAsc(1);
        for (NovelsType type : allVaildTypes) {
            Long typeIdLong = type.getTypeId().longValue();
            typeIdToNameMap.put(typeIdLong, type.getTypeName());
        }

        Map<Long,String> authorIdToNameMap = new HashMap<>();
        Set<Long> authorIds = novelPage.getContent().stream().map(Novel::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!authorIds.isEmpty()) {
            List<User> authors = userRepository.findAllById(authorIds);
            for (User user : authors) {
                authorIdToNameMap.put(user.getUid(), user.getPenName());
            }
        }

        List<Map<String, Object>> novelList = novelPage.getContent().stream().map(novel -> {
            Map<String, Object> data = new HashMap<>();
            data.put("novelId", novel.getId());
            data.put("title",novel.getTitle());
            data.put("authorName", authorIdToNameMap.get(novel.getAuthorId()));
            data.put("typeId", novel.getTypeId());
            data.put("typeName", typeIdToNameMap.getOrDefault(novel.getTypeId(),"未知分类"));
            data.put("coverUrl", novel.getCoverUrl() == null ? "" : novel.getCoverUrl());
            data.put("intro", novel.getIntro());
            data.put("readCount", novel.getReadCount());
            data.put("collectCount", novel.getCollectCount());
            data.put("updateTime", novel.getUpdateTime());
            data.put("wordCount", novel.getWordCount() == null ? 0 : novel.getWordCount());
            data.put("updateStatus", novel.getUpdateStatus() == null ? 0 : novel.getUpdateStatus());
            return data;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("novelList", novelList);
        result.put("total", novelPage.getTotalElements());
        result.put("pages", novelPage.getTotalPages());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        String msg = novelPage.getTotalElements() > 0 ? "查询书城列表成功" : "暂无已审核的小说";
        return  Result.success(result,msg);
    }

    @Override
    public Result getPublicNovelDetail(Long novelId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031: 小说不存在"));

        if (!Novel.NovelStatus.PUBLISHED.equals(novel.getStatus())) {
            return Result.error("1033","该小说暂未发布");
        }

        String typeName = "未知分类";
        if (novel.getTypeId() != null) {
            Optional<NovelsType> typeOpt = novelsTypeRepository.findByTypeIdAndStatus(novel.getTypeId(), 1);
            if (typeOpt.isPresent()) {
                typeName = typeOpt.get().getTypeName();
            }
        }

        String authorName = null;
        if (novel.getAuthorId() != null) {
            Optional<User> userOpt = userRepository.findById(novel.getAuthorId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                authorName = user.getPenName();
            }
        }

        Map<String, Object> novelData = new HashMap<>();
        novelData.put("novelId", novel.getId());
        novelData.put("title", novel.getTitle());
        novelData.put("authorId", novel.getAuthorId());
        novelData.put("authorName", authorName);
        novelData.put("typeId", novel.getTypeId());
        novelData.put("typeName", typeName);
        novelData.put("intro", novel.getIntro());
        novelData.put("coverUrl", novel.getCoverUrl() == null ? "" : novel.getCoverUrl());
        novelData.put("readCount", novel.getReadCount());
        novelData.put("collectCount", novel.getCollectCount());
        novelData.put("updateTime", novel.getUpdateTime());
        novelData.put("wordCount", novel.getWordCount() == null ? 0 : novel.getWordCount());

        List<Chapter> chapterList = chapterRepository.findByNovelIdAndStatusOrderByPublishSortAsc(novelId, Chapter.ChapterStatus.PUBLISHED);
        List<Map<String, Object>> chapterDataList = chapterList.stream().map(chapter -> {
            Map<String, Object> chapterData = new HashMap<>();
            chapterData.put("chapterId", chapter.getId());
            chapterData.put("chapterTitle", chapter.getTitle());
            chapterData.put("sortNum", chapter.getPublishSort());
            chapterData.put("brief", chapter.getBrief());
            chapterData.put("publishTime", chapter.getCreateTime());
            chapterData.put("wordCount", chapter.getWordCount() == null ? 0 : chapter.getWordCount());
            return chapterData;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("novelInfo", novelData);
        result.put("chapterList", chapterDataList);

        return Result.success(result,"查询小说详情成功");

    }

    @Override
    public Result getAuthorPublicWorks(Long authorId) {
        Optional<User> userOpt = userRepository.findById(authorId);
        final String authorPenName = userOpt.map(User::getPenName).orElse("未知作者");
        final String authorAvatarUrl = userOpt.map(User::getAvatarUrl).orElse("");

        //查询作者已发布的小说
        Specification<Novel> spec = ((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("authorId"), authorId));
            predicates.add(cb.equal(root.get("status"), Novel.NovelStatus.PUBLISHED));
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        List<Novel> novelList = novelRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createTime"));

        //组装小说列表
        List<Map<String, Object>> resultList = novelList.stream().map(novel -> {
            Map<String, Object> map = new HashMap<>();
            map.put("novelId", novel.getId());
            map.put("title", novel.getTitle());
            map.put("authorName", authorPenName);
            map.put("coverUrl", novel.getCoverUrl());
            map.put("intro", novel.getIntro());
            map.put("createTime", novel.getCreateTime());
            map.put("updateTime", novel.getUpdateTime());
            Integer updateStatus = novel.getUpdateStatus();
            map.put("updateStatus", updateStatus == null ? 0 : updateStatus);
            return map;
        }).collect(Collectors.toList());

        //组装最终返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("authorInfo", Map.of(
                "uid", authorId,
                "penName", authorPenName,
                "avatarUrl", authorAvatarUrl
        ));
        result.put("novelList", resultList);

        return Result.success(result, "查询成功");
    }

    @Override
    public Result searchNovels(String keyword, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 100) pageSize = 10;

        // 1. 先查询符合条件的作者ID（根据笔名模糊匹配）
        Set<Long> matchAuthorIds = new HashSet<>();
        List<User> matchAuthors = userRepository.findByPenNameLike("%" + keyword + "%");
        for (User author : matchAuthors) {
            matchAuthorIds.add(author.getUid());
        }

        // 2. 构建查询条件：已发布 + (标题包含关键词 OR 作者ID在匹配列表中)
        Specification<Novel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 强制过滤已发布状态
            predicates.add(cb.equal(root.get("status"), Novel.NovelStatus.PUBLISHED));

            // 标题模糊匹配
            Predicate titleLike = cb.like(root.get("title"), "%" + keyword + "%");
            // 作者ID匹配（解决join报错问题）
            Predicate authorMatch = matchAuthorIds.isEmpty()
                    ? cb.disjunction()
                    : root.get("authorId").in(matchAuthorIds);

            // 标题或作者匹配
            predicates.add(cb.or(titleLike, authorMatch));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Order.desc("readCount"), Sort.Order.desc("collectCount"), Sort.Order.desc("updateTime"));
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, sort);
        Page<Novel> novelPage = novelRepository.findAll(spec, pageable);

        // 2. 预加载作者名和分类名
        Map<Long, String> typeIdToNameMap = new HashMap<>();
        List<NovelsType> allValidTypes = novelsTypeRepository.findByStatusOrderByTypeIdAsc(1);
        for (NovelsType type : allValidTypes) {
            // 关键修复：确保typeId的类型匹配（如果数据库是Integer，转Long）
            Long typeIdLong = type.getTypeId() != null ? type.getTypeId().longValue() : null;
            if (typeIdLong != null) {
                typeIdToNameMap.put(typeIdLong, type.getTypeName());
            }
        }

        Map<Long, String> authorIdToNameMap = new HashMap<>();
        Set<Long> authorIds = novelPage.getContent().stream()
                .map(Novel::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!authorIds.isEmpty()) {
            List<User> authors = userRepository.findAllById(authorIds);
            for (User user : authors) {
                authorIdToNameMap.put(user.getUid(), user.getPenName());
            }
        }

        // 3. 组装返回数据（字段名和前端匹配）
        List<Map<String, Object>> novelList = novelPage.getContent().stream().map(novel -> {
            Map<String, Object> data = new HashMap<>();
            data.put("novelId", novel.getId());
            data.put("title", novel.getTitle()); // 改为 title，和前端对应
            data.put("authorName", authorIdToNameMap.getOrDefault(novel.getAuthorId(), "未知作者"));
            // 关键修复：确保novel.getTypeId()转Long后再匹配
            Long novelTypeId = novel.getTypeId() != null ? novel.getTypeId().longValue() : null;
            data.put("typeName", typeIdToNameMap.getOrDefault(novelTypeId, "未知分类"));
            data.put("coverUrl", novel.getCoverUrl() == null ? "" : novel.getCoverUrl());
            data.put("intro", novel.getIntro());
            data.put("wordCount", novel.getWordCount() == null ? 0 : novel.getWordCount());
            data.put("updateTime", novel.getUpdateTime());
            data.put("readCount", novel.getReadCount() == null ? 0 : novel.getReadCount());
            data.put("collectCount", novel.getCollectCount() == null ? 0 : novel.getCollectCount());

            Integer updateStatus = novel.getUpdateStatus();
            // 兜底处理：如果字段为null，默认0（连载中）
            data.put("updateStatus", updateStatus == null ? 0 : updateStatus);
            return data;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("novelList", novelList);
        result.put("total", novelPage.getTotalElements());
        result.put("pages", novelPage.getTotalPages());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        String msg = novelPage.getTotalElements() > 0 ? "搜索到" + novelPage.getTotalElements() + "本小说" : "未找到相关小说";
        return Result.success(result, msg);
    }

    @Override
    public Result getNovelDetailsBatch(List<Long> novelIds) {
        List<Novel> novels = novelRepository.findAllById(novelIds);
        List<Map<String, Object>> novelInfoList = novels.stream().map(novel -> {
            Map<String, Object> info = new HashMap<>();
            info.put("novelId", novel.getId());
            info.put("title", novel.getTitle());
            // 关联作者、类型等信息（和 getPublicNovelDetail 逻辑一致）
            Optional<User> authorOpt = userRepository.findById(novel.getAuthorId());
            info.put("authorName", authorOpt.map(User::getPenName).orElse("未知作者"));
            Optional<NovelsType> typeOpt = novelsTypeRepository.findById(novel.getTypeId());
            info.put("typeName", typeOpt.map(NovelsType::getTypeName).orElse("未知分类"));
            info.put("intro", novel.getIntro());
            info.put("wordCount", novel.getWordCount());
            info.put("coverUrl", novel.getCoverUrl());
            info.put("updateStatus", novel.getUpdateStatus() == null ? 0 : novel.getUpdateStatus());
            return info;
        }).collect(Collectors.toList());
        return Result.success(novelInfoList, null);
    }

    // 新增实现
    @Override
    public Novel getNovelById(Long novelId) {
        return novelRepository.findById(novelId)
                .orElse(null); // 不存在则返回null，由上层处理
    }

    @Override
    public Result getReviewNovelList(Integer pageNum, Integer pageSize, String status) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by("createTime").descending());
        Specification<Novel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 🌟 核心重构：根据 status 参数精准筛选，不再默认拼接 OR
            if (StringUtils.isNotBlank(status)) {
                switch (status.toUpperCase()) {
                    case "PENDING":
                        // 只查【整体待审核】：status = PENDING_REVIEW
                        predicates.add(cb.equal(root.get("status"), Novel.NovelStatus.PENDING_REVIEW));
                        break;
                    case "DETAIL_PENDING":
                        // 只查【详情待审核】：status = PUBLISHED 且 pendingStatus = PENDING
                        predicates.add(cb.equal(root.get("status"), Novel.NovelStatus.PUBLISHED));
                        predicates.add(cb.equal(root.get("pendingStatus"), Novel.PendingStatus.PENDING));
                        break;
                    case "APPROVED":
                        // 只查已发布（整体审核通过）
                        predicates.add(cb.equal(root.get("status"), Novel.NovelStatus.PUBLISHED));
                        break;
                    case "REJECTED":
                        // 只查整体驳回
                        predicates.add(cb.equal(root.get("status"), Novel.NovelStatus.REJECTED));
                        break;
                    default:
                        // 未知参数：默认查所有待审核（整体+详情）
                        Predicate overallPending = cb.equal(root.get("status"), Novel.NovelStatus.PENDING_REVIEW);
                        Predicate detailPending = cb.and(
                                cb.equal(root.get("status"), Novel.NovelStatus.PUBLISHED),
                                cb.equal(root.get("pendingStatus"), Novel.PendingStatus.PENDING)
                        );
                        predicates.add(cb.or(overallPending, detailPending));
                        break;
                }
            } else {
                // 无筛选参数：默认查所有待审核（整体+详情）
                Predicate overallPending = cb.equal(root.get("status"), Novel.NovelStatus.PENDING_REVIEW);
                Predicate detailPending = cb.and(
                        cb.equal(root.get("status"), Novel.NovelStatus.PUBLISHED),
                        cb.equal(root.get("pendingStatus"), Novel.PendingStatus.PENDING)
                );
                predicates.add(cb.or(overallPending, detailPending));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Novel> novelPage = novelRepository.findAll(spec, pageable);

        // 组装返回数据（保持不变）
        List<Map<String, Object>> records = novelPage.getContent().stream().map(novel -> {
            Map<String, Object> map = new HashMap<>();
            map.put("novelId", novel.getId());
            map.put("title", novel.getTitle());
            map.put("coverUrl", novel.getCoverUrl());
            map.put("intro",novel.getIntro());
            map.put("typeId",novel.getTypeId());
            map.put("authorId", novel.getAuthorId());
            map.put("status", novel.getStatus().name());
            // 后端返回枚举英文（PENDING/APPROVED），前端通过 map 转中文
            map.put("pendingTitle", novel.getPendingTitle());
            map.put("pendingCoverUrl", novel.getPendingCoverUrl());
            map.put("pendingIntro", novel.getPendingIntro());
            map.put("pendingStatus", novel.getPendingStatus() != null ? novel.getPendingStatus().name() : "无修改");
            map.put("pendingTypeId", novel.getPendingTypeId());
            map.put("createTime", novel.getCreateTime());

            ReviewRecord latestReview = reviewRecordRepository.findFirstByNovelIdOrderBySubmitTimeDesc(novel.getId());
            if (latestReview != null) {
                map.put("submitTime", latestReview.getSubmitTime());
            }
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", novelPage.getTotalElements());
        result.put("pages", novelPage.getTotalPages());

        return Result.success(result, "查询待审核小说列表成功");
    }
}