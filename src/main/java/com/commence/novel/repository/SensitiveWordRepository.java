package com.commence.novel.repository;

import com.commence.novel.entity.SensitiveWord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Long> {
    // 根据敏感词内容查询
    Optional<SensitiveWord> findByWord(String word);
    // 根据敏感词内容删除
    void deleteByWord(String word);
}