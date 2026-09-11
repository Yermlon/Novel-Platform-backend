package com.commence.novel.repository;

import com.commence.novel.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    Optional<Announcement> findFirstByStatusOrderByPublishedTimeDesc(String status);

    List<Announcement> findAllByOrderByPublishedTimeDesc();
}
