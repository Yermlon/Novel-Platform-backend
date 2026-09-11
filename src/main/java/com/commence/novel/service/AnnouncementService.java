package com.commence.novel.service;

import com.commence.novel.entity.Announcement;
import com.commence.novel.utils.Result;

public interface AnnouncementService {
    //查询最新的公告
    Result getLatestPublishedAnnouncement();

    // 新增：查询所有公告（给管理后台用）
    Result getAllAnnouncementList();

    //发布公告
    Result publishAnnouncement(Announcement announcement);

    //编辑公告
    Result editAnnouncement(Long id, Announcement announcement);

    //删除公告
    Result deleteAnnouncement(Long id);
}
