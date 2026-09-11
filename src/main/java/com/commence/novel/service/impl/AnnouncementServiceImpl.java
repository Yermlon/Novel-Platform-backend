package com.commence.novel.service.impl;

import com.commence.novel.entity.Announcement;
import com.commence.novel.repository.AnnouncementRepository;
import com.commence.novel.service.AnnouncementService;
import com.commence.novel.utils.Result;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {
    @Autowired
    private AnnouncementRepository announcementRepository;

    @Override
    public Result getLatestPublishedAnnouncement(){
        Optional<Announcement> opt = announcementRepository.findFirstByStatusOrderByPublishedTimeDesc("PUBLISHED");
        Announcement announcement = opt.orElse(null);
        return Result.success(announcement,"查询最新公告成功");
    }

    @Override
    public Result getAllAnnouncementList() {
        // 按发布时间降序，返回所有公告
        List<Announcement> announcementList = announcementRepository.findAllByOrderByPublishedTimeDesc();
        return Result.success(announcementList, "查询所有公告列表成功");
    }

    @Override
    public Result publishAnnouncement(Announcement announcement) {
        if (ObjectUtils.isEmpty(announcement.getContent())) {
            return Result.error("1060","公告内容不能为空");
        }

        announcement.setStatus("PUBLISHED");
        announcement.setPublishedTime(LocalDateTime.now());
        announcement.setCreatedTime(LocalDateTime.now());
        Announcement newAnnouncement = announcementRepository.save(announcement);

        return Result.success(newAnnouncement,"发布公告成功");
    }

    @Override
    public Result editAnnouncement(Long id, Announcement announcement) {
        Announcement existAnnouncement = announcementRepository.findById(id).orElse(null);
        if (existAnnouncement == null) {
            return Result.error("1061","公告不存在");
        }

        if (ObjectUtils.isEmpty(existAnnouncement.getContent())) {
            return Result.error("1060","公告不能为空");
        }

        existAnnouncement.setContent(announcement.getContent());
        existAnnouncement.setStatus(announcement.getStatus());
        if ("PUBLISHED".equals(existAnnouncement.getStatus())) {
            existAnnouncement.setPublishedTime(LocalDateTime.now());
        }
        Announcement updatedAnnouncement = announcementRepository.save(existAnnouncement);

        return Result.success(updatedAnnouncement,"编辑公告成功");
    }

    @Override
    public Result deleteAnnouncement(Long id) {
        Announcement existAnnouncement = announcementRepository.findById(id).orElse(null);
        if (existAnnouncement == null) {
            return Result.error("1061","公告不存在");
        }

        announcementRepository.delete(existAnnouncement);
        return Result.success(null,"删除公告成功");
    }
}
