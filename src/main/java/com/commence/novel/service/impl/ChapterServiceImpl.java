package com.commence.novel.service.impl;

import com.commence.novel.DTO.ChapterDTO;
import com.commence.novel.DTO.ChapterSortDTO;
import com.commence.novel.entity.Chapter;
import com.commence.novel.entity.ChapterUpdateLog;
import com.commence.novel.entity.Novel;
import com.commence.novel.repository.ChapterRepository;
import com.commence.novel.repository.ChapterUpdateLogRepository;
import com.commence.novel.repository.NovelRepository;
import com.commence.novel.repository.ReviewRecordRepository;
import com.commence.novel.service.ChapterService;
import com.commence.novel.utils.AutoAuditUtils;
import com.commence.novel.utils.Result;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class ChapterServiceImpl implements ChapterService {
    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private ChapterUpdateLogRepository updateLogDao;

    @Autowired
    private ReviewRecordRepository reviewRecordRepository;

    @Override
    public Result createChapter(ChapterDTO chapterDTO,Long authorId){
        if (chapterDTO == null || chapterDTO.getNovelId() == null){
            return Result.error("1030","小说ID不能为空");
        }

        Long novelId = chapterDTO.getNovelId();
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));
        if (!novel.getAuthorId().equals(authorId)){
            return Result.error("1032","无权限操作该小说");
        }

        log.info("创建章节请求参数：{}",chapterDTO);

        Integer maxSort = chapterRepository.findMaxSortByNovelIdAndStatusNotPublished(novelId);
        Integer newSort = (maxSort == null ) ? 1 : maxSort + 1;

        Chapter chapter = new Chapter();
        chapter.setNovelId(novelId);
        chapter.setTitle("新章节");
        chapter.setContent("");
        chapter.setDraftContent(chapterDTO.getContent());
        chapter.setDraftSaveTime(LocalDateTime.now());
        chapter.setSort(newSort);
        chapter.setStatus(Chapter.ChapterStatus.DRAFT);
        int wordCount = calculateWordCount(chapterDTO.getContent());
        chapter.setWordCount(wordCount);
        chapter.setCreateTime(LocalDateTime.now());
        chapter.setUpdateTime(LocalDateTime.now());

        try {
            Chapter savedChapter = chapterRepository.save(chapter);
            try {
                savedUpdateLog(novelId,savedChapter.getId(), authorId, ChapterUpdateLog.UpdateType.CREATE);
            } catch (Exception e) {
                log.error("保存章节更新日志失败",e);
            }
            return Result.success(savedChapter.getId(),"章节创建成功（草稿）");
        } catch (Exception e) {
            log.error("创建章节失败",e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error("1049","章节创建失败：" + e.getMessage());
        }
            }

    //保存章节更新日志
    private void savedUpdateLog(Long novelId, Long chapterId, Long authorId, ChapterUpdateLog.UpdateType updateType){
        ChapterUpdateLog log = new ChapterUpdateLog();
        log.setNovelId(novelId);
        log.setChapterId(chapterId);
        log.setAuthorId(authorId);
        log.setUpdateType(updateType);
        log.setUpdateTime(LocalDateTime.now());
        updateLogDao.save(log);
    }

    @Override
    public Result editChapter(Long chapterId, ChapterDTO chapterDTO,Long authorId){
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("1041:章节不存在"));

        Novel novel = novelRepository.findById(chapter.getNovelId())
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));
        if (!novel.getAuthorId().equals(authorId)){
            return Result.error("1032","无权限操作");
        }

        if (StringUtils.isNotBlank(chapterDTO.getTitle())){
            chapter.setTitle(chapterDTO.getTitle());
        }
        if (StringUtils.isNotBlank(chapterDTO.getContent())){
            chapter.setContent(chapterDTO.getContent());
            chapter.setDraftSaveTime(LocalDateTime.now());
            chapter.setWordCount(calculateWordCount(chapterDTO.getContent()));
        }
        if (StringUtils.isNotBlank(chapterDTO.getBrief())){
            chapter.setBrief(chapterDTO.getBrief());
        }
        if (chapterDTO.getSort() != null && !chapterDTO.getSort().equals(chapter.getSort())){
            if (chapterRepository.existsByNovelIdAndSort(chapter.getNovelId(),chapterDTO.getSort())){
                return Result.error("1044","该排序号已被占用");
            }
            chapter.setSort(chapterDTO.getSort());

            if (Chapter.ChapterStatus.PUBLISHED.equals(chapter.getStatus())){
                chapter.setPublishSort(chapterDTO.getSort());
            }
        }
        chapter.setUpdateTime(LocalDateTime.now());

        chapterRepository.save(chapter);
        savedUpdateLog(chapter.getNovelId(),chapterId,authorId,ChapterUpdateLog.UpdateType.EDIT);
        return Result.success(null,"章节编辑成功");
    }

    @Override
    public Result autoSaveDraft(Long chapterId, String draftContent, String title, String brief, Long authorId, String msg){
        if (chapterId == null || StringUtils.isBlank(draftContent)){
            return Result.error("1040","章节ID和草稿内容不能为空");
        }

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("1041:章节不存在"));

        Novel novel = novelRepository.findById(chapter.getNovelId()).orElseThrow();
        if (!novel.getAuthorId().equals(authorId)){
            return Result.error("1032","无权限操作");
        }

        int wordCount = calculateWordCount(draftContent);

        chapter.setDraftContent(draftContent);
        chapter.setDraftSaveTime(LocalDateTime.now());
        chapter.setWordCount(wordCount);
        chapter.setBrief(brief);
        chapter.setTitle(title != null && !title.trim().isEmpty() ? title : "新章节");
        chapter.setUpdateTime(LocalDateTime.now());

        chapterRepository.save(chapter);
        return Result.success(null,msg);
    }

    private int calculateWordCount(String content){
        if (content == null || content.isEmpty()) return 0;
        String plainText = Jsoup.parse(content).text();
        plainText = plainText.replace("\\s+","");
        return plainText.length();
    }

    @Override
    public Result publishChapter(Long chapterId, Long authorId){
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("1041:章节不存在"));
        Novel novel = novelRepository.findById(chapter.getNovelId()).orElseThrow(() -> new RuntimeException("1031:小说不存在"));
        if (!novel.getAuthorId().equals(authorId)){
            return Result.error("1032","无权限操作");
        }

        Integer maxPublishSort = chapterRepository.findMaxPublishSortByNovelIdAndStatus(
                chapter.getNovelId(), Chapter.ChapterStatus.PUBLISHED
        );
        Integer newPublishSort = (maxPublishSort == null) ? 1 : maxPublishSort + 1;
        chapter.setPublishSort(maxPublishSort == null ? 1 : maxPublishSort + 1);

        String publishContent = StringUtils.isNotBlank(chapter.getDraftContent())
                ? chapter.getDraftContent()
                : chapter.getContent();

        chapter.setStatus(Chapter.ChapterStatus.PENDING);
        chapterRepository.save(chapter);

        AutoAuditUtils.AuditResult auditResult = AutoAuditUtils.autoAudit(
                publishContent,
                chapter.getTitle(),
                chapter.getBrief()
        );

        if (!auditResult.isPass()) {
            chapter.setStatus(Chapter.ChapterStatus.REJECTED);
            chapter.setAuditReason(auditResult.getReason());
            chapter.setAuditTime(LocalDateTime.now());
            chapterRepository.save(chapter);
            return Result.error("1050",auditResult.getReason());
        }

        chapter.setContent(publishContent);
        chapter.setPublishSort(newPublishSort);
        chapter.setStatus(Chapter.ChapterStatus.PUBLISHED);
        chapter.setAuditReason("自动审核通过");
        chapter.setAuditTime(LocalDateTime.now());
        chapter.setUpdateTime(LocalDateTime.now());

        String htmlContent = chapter.getDraftContent();
        int pureWordCount = calculateWordCount(htmlContent);
        chapter.setWordCount(pureWordCount);

        chapter.setSort(0);
        chapterRepository.save(chapter);

        novel.setWordCount(novel.getWordCount() + chapter.getWordCount());
        novel.setLastUpdateTime(LocalDateTime.now());
        novel.setUpdateTime(LocalDateTime.now());
        novelRepository.save(novel);

        recordPublished(novel.getId());
        recoedUnpublished(novel.getId());

        savedUpdateLog(novel.getId(),chapterId,authorId,ChapterUpdateLog.UpdateType.PUBLISH);

        return Result.success(null,"章节发布成功");
    }

    @Override
    public Result adjustChapterSort(Long novelId, List<ChapterSortDTO> chapterSortList, Long authorId){
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));
        if (!novel.getAuthorId().equals(authorId)){
            return Result.error("1032","无权限操作");
        }

        for (ChapterSortDTO chapterSortDTO : chapterSortList){
            Chapter chapter = chapterRepository.findById(chapterSortDTO.getChapterId())
                    .orElseThrow(() -> new RuntimeException("1041:章节不存在"));
            chapter.setSort(chapterSortDTO.getSort());
            chapter.setUpdateTime(LocalDateTime.now());
            chapterRepository.save(chapter);
        }
        return Result.success(null,"章节排序更新成功");
    }

    @Override
    public Result adjustPublishSort(Long novelId, List<ChapterSortDTO> chapterSortList, Long authorId){
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));
        if (!novel.getAuthorId().equals(authorId)){
            return Result.error("1032","无权限操作");
        }

        for (ChapterSortDTO chapterSortDTO : chapterSortList) {
            Chapter chapter = chapterRepository.findById(chapterSortDTO.getChapterId())
                    .orElseThrow(() -> new RuntimeException("1041:章节不存在"));

            if (chapter.getStatus() != Chapter.ChapterStatus.PUBLISHED) {
                return Result.error("1043", "只能调整已发布章节的顺序");
            }
        }

        List<Chapter> allPublishedChapters = chapterRepository.findByNovelIdAndStatusOrderByPublishSortAsc(
                novelId, Chapter.ChapterStatus.PUBLISHED
        );
        if (allPublishedChapters.isEmpty()){
            return Result.error("1045","该小说暂无已发布章节");
        }

        for (ChapterSortDTO chapterSortDTO : chapterSortList) {
            Chapter chapter = allPublishedChapters.stream()
                    .filter(c -> c.getId().equals(chapterSortDTO.getChapterId()))
                    .findFirst().orElseThrow(() -> new RuntimeException("1041:章节不存在"));
            chapter.setPublishSort(chapterSortDTO.getSort());
            chapter.setUpdateTime(LocalDateTime.now());
        }

        List<Chapter> sortedChapters = allPublishedChapters.stream()
                        .sorted((c1,c2) -> c1.getPublishSort().compareTo(c2.getPublishSort()))
                        .collect(Collectors.toList());
        for (int i = 0; i < sortedChapters.size(); i++) {
            Chapter chapter = sortedChapters.get(i);
            chapter.setPublishSort(i + 1);
            chapter.setUpdateTime(LocalDateTime.now());
            chapterRepository.save(chapter);
        }

        return Result.success(null, "已发布章节顺序更新成功");
    }

    @Override
    public Result getChapterList(Long novelId){
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));

        List<Chapter> chapterList = chapterRepository.findByNovelIdOrderBySortAsc(novelId);

        List<Chapter> publishedChapters = new ArrayList<>();
        List<Chapter> draftChapters = new ArrayList<>();
        for (Chapter chapter : chapterList){
            if (Chapter.ChapterStatus.PUBLISHED.equals(chapter.getStatus())){
                publishedChapters.add(chapter);
            } else {
                draftChapters.add(chapter);
            }
        }

        publishedChapters.sort(Comparator.comparingInt(chapter ->
                chapter.getPublishSort() == null ? 0 : chapter.getPublishSort()

        ));
        for (int i = 0; i < publishedChapters.size(); i++) {
            Chapter ch = publishedChapters.get(i);
            if (ch.getPublishSort() == null || ch.getPublishSort() == 0){
                ch.setPublishSort(i + 1);
            }
        }

        List<Chapter> finalChapterList = new ArrayList<>();
        finalChapterList.addAll(publishedChapters);
        finalChapterList.addAll(draftChapters);

        List<Map<String,Object>> resultList = new ArrayList<>();
        for (Chapter chapter : finalChapterList){
            Map<String,Object> chapterInfo = new HashMap<>();
            chapterInfo.put("chapterId",chapter.getId());
            chapterInfo.put("title",chapter.getTitle());
            chapterInfo.put("sort",chapter.getSort());
            chapterInfo.put("publishSort",chapter.getPublishSort());
            chapterInfo.put("status",chapter.getStatus());
            chapterInfo.put("wordCount",chapter.getWordCount());
            chapterInfo.put("draftContent",chapter.getDraftContent());
            chapterInfo.put("brief",chapter.getBrief());
            chapterInfo.put("updateTime",chapter.getUpdateTime());
            resultList.add(chapterInfo);
        }

        return Result.success(resultList,"章节列表查询成功");

    }

    //重新排序已发布章节
    private  void recordPublished(Long novelId) {
        List<Chapter> publishedChapters = chapterRepository.findByNovelIdAndStatusOrderByPublishSortAsc(novelId, Chapter.ChapterStatus.PUBLISHED);
        for (int i = 0; i < publishedChapters.size(); i++){
            Chapter chapter = publishedChapters.get(i);
            chapter.setPublishSort(i + 1);
            chapterRepository.save(chapter);
        }
    }

    private  void recoedUnpublished(Long novelId) {
        List<Chapter> unpublishedChapters = chapterRepository.findByNovelIdAndStatusInOrderBySortAsc(novelId, Arrays.asList(Chapter.ChapterStatus.DRAFT, Chapter.ChapterStatus.OFFLINE));
        for (int i = 0; i < unpublishedChapters.size(); i++){
            Chapter chapter = unpublishedChapters.get(i);
            chapter.setSort(i + 1);
            chapterRepository.save(chapter);
        }
    }

    @Override
    public Result deleteChapter(Long chapterId, Long authorId){
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("1041:章节不存在"));

        Novel novel = novelRepository.findById(chapter.getNovelId())
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));
        if (!novel.getAuthorId().equals(authorId)){
            return Result.error("1032","无权限操作");
        }

        if (chapter.getStatus() != Chapter.ChapterStatus.DRAFT){
            return Result.error("1046","仅草稿状态的章节可删除");
        }

        chapterRepository.delete(chapter);

        recoedUnpublished(novel.getId());

        savedUpdateLog(novel.getId(),chapterId,authorId,ChapterUpdateLog.UpdateType.DELETE);

        return Result.success(null,"章节删除成功");

    }

    @Override
    public Result offlineChapter(Long chapterId, Long authorId){
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("1041;章节不存在"));

        Novel novel = novelRepository.findById(chapter.getNovelId())
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));

        if (!novel.getAuthorId().equals(authorId)){
            return Result.error("1032","无权限操作");
        }

        if (chapter.getStatus() != Chapter.ChapterStatus.PUBLISHED){
            return Result.error("1046","仅已发布章节可下架");
        }

        chapter.setStatus(Chapter.ChapterStatus.OFFLINE);
        chapterRepository.save(chapter);

        recordPublished(novel.getId());
        recoedUnpublished(novel.getId());

        savedUpdateLog(novel.getId(),chapterId,authorId,ChapterUpdateLog.UpdateType.OFFLINE);

        return Result.success(null,"章节下架成功");
    }


    @Override
    public Result restoreChapter(Long chapterId, Long authorId){
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("1041:章节不存在"));
        Novel novel = novelRepository.findById(chapter.getNovelId())
                .orElseThrow(() -> new RuntimeException("1031:小说不存在"));
        if (!novel.getAuthorId().equals(authorId)){
            return Result.error("1032","无权限操作");
        }

        if (chapter.getStatus() != Chapter.ChapterStatus.OFFLINE){
            return Result.error("1046","仅已下架章节可上架");
        }

        chapter.setStatus(Chapter.ChapterStatus.PUBLISHED);
        chapter.setUpdateTime(LocalDateTime.now());
        chapterRepository.save(chapter);

        recordPublished(novel.getId());
        recoedUnpublished(novel.getId());

        savedUpdateLog(novel.getId(),chapterId,authorId,ChapterUpdateLog.UpdateType.PUBLISH);
        return Result.success(null,"章节已重新上架");
    }

    @Override
    public Result getChapterContent(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("1041:章节不存在"));

        if (!Chapter.ChapterStatus.PUBLISHED.equals(chapter.getStatus())) {
            return Result.error("1048", "该章节未发布，无法阅读");
        }

        Map<String, Object> chapterContent = new HashMap<>();
        chapterContent.put("chapterId", chapter.getId());
        chapterContent.put("title", chapter.getTitle());
        chapterContent.put("content", chapter.getContent());
        chapterContent.put("novelId", chapter.getNovelId());
        chapterContent.put("novelTitle", novelRepository.findById(chapter.getNovelId()).get().getTitle()); // 小说标题
        chapterContent.put("wordCount", chapter.getWordCount());

        return Result.success(chapterContent, "章节内容获取成功");
    }

  }
