package com.commence.novel.repository;

import com.commence.novel.entity.UserReadSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserReadSettingRepository extends JpaRepository<UserReadSetting, Long> {
    Optional<UserReadSetting> findByUserId(String userId);

}
