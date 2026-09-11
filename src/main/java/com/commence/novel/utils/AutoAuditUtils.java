package com.commence.novel.utils;

import com.commence.novel.entity.SensitiveWord;
import com.commence.novel.repository.SensitiveWordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 自动审核工具类（改造后：敏感词持久化，重启不丢失）
 */
@Component // 改为Spring组件，支持依赖注入
public class AutoAuditUtils {
    // 内存敏感词集合（供审核逻辑使用）
    private static final Set<String> SENSITIVE_WORDS = new HashSet<>();

    // 注入JPA的敏感词仓库（非静态，通过@PostConstruct初始化）
    @Autowired
    private SensitiveWordRepository sensitiveWordRepository;

    // ========== 启动时初始化：从数据库加载所有敏感词 ==========
    @PostConstruct
    public void initSensitiveWords() {
        try {
            // 清空内存原有数据
            SENSITIVE_WORDS.clear();

            // 从数据库查询所有敏感词并加载到内存
            List<SensitiveWord> dbWordList = sensitiveWordRepository.findAll();
            for (SensitiveWord sw : dbWordList) {
                SENSITIVE_WORDS.add(sw.getWord());
            }

            // 兜底：如果数据库为空，添加基础敏感词并同步到数据库
            if (SENSITIVE_WORDS.isEmpty()) {
                addSensitiveWord("敏感词");
                addSensitiveWord("涉黄");
            }
            System.out.println("✅ 敏感词初始化完成，共加载：" + SENSITIVE_WORDS.size() + "个");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ 敏感词初始化失败，使用基础内存词库");
            // 兜底：仅加载基础词（不入库）
            SENSITIVE_WORDS.add("敏感词");
            SENSITIVE_WORDS.add("涉黄");
        }
    }

    // ========== 核心：章节自动审核逻辑（完全保留原有逻辑） ==========
    public static AuditResult autoAudit(String content, String title, String brief) {
        AuditResult result = new AuditResult();

        if (!basicChapterCheck(content, title, result)) {
            return result;
        }

        if (!sensitiveWordCheck(content, title, brief, result)) {
            return result;
        }

        if (!customChapterCheck(content, result)) {
            return result;
        }

        result.setPass(true);
        result.setReason("自动审核通过");
        return result;
    }

    // ========== 核心：评论自动审核逻辑（完全保留原有逻辑） ==========
    public static AuditResult autoAuditComment(String content) {
        AuditResult result = new AuditResult();

        // 评论基础校验
        if (!basicCommentCheck(content, result)) {
            return result;
        }

        // 评论敏感词校验
        if (!sensitiveWordCheckForComment(content, result)) {
            return result;
        }

        // 评论自定义校验（纯表情/无意义内容）
        if (!customCommentCheck(content, result)) {
            return result;
        }

        result.setPass(true);
        result.setReason("评论自动审核通过");
        return result;
    }

    // ========== 章节基础校验 ==========
    private static boolean basicChapterCheck(String content, String title, AuditResult result) {
        if (StringUtils.isEmpty(title)) {
            result.setPass(false);
            result.setReason("章节标题不能为空");
            return false;
        }
        if (StringUtils.isEmpty(content)) {
            result.setPass(false);
            result.setReason("章节内容不能为空");
            return false;
        }
        if (title.length() > 20) {
            result.setPass(false);
            result.setReason("章节标题长度不能超过20字");
            return false;
        }
        if (content.length() < 200) {
            result.setPass(false);
            result.setReason("章节内容不能少于200字");
            return false;
        }
        return true;
    }

    // ========== 章节敏感词校验 ==========
    private static boolean sensitiveWordCheck(String content, String title, String brief, AuditResult result) {
        String allContent = title + content + (brief == null ? "" : brief);
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (allContent.contains(sensitiveWord)) {
                result.setPass(false);
                result.setReason("内容包含敏感词「" + sensitiveWord + "」，请修改后重新发布");
                return false;
            }
        }
        return true;
    }

    // ========== 章节自定义校验 ==========
    private static boolean customChapterCheck(String content, AuditResult result) {
        // 修复正则：原有replace用法错误，需要用replaceAll
        String pureText = content.replaceAll("[\\p{Punct}\\s]", "");
        if (pureText.length() < 200) {
            result.setPass(false);
            result.setReason("章节有效内容过少，请补充后重新发布");
            return false;
        }
        return true;
    }

    // ========== 评论基础校验 ==========
    private static boolean basicCommentCheck(String content, AuditResult result) {
        if (StringUtils.isEmpty(content)) {
            result.setPass(false);
            result.setReason("评论内容不能为空");
            return false;
        }
        if (content.length() > 500) { // 评论长度限制
            result.setPass(false);
            result.setReason("评论内容不能超过500字");
            return false;
        }
        return true;
    }

    // ========== 评论敏感词校验 ==========
    private static boolean sensitiveWordCheckForComment(String content, AuditResult result) {
        String checkContent = content.toLowerCase().replace("　", " "); // 全角转半角
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (checkContent.contains(sensitiveWord.toLowerCase())) {
                result.setPass(false);
                result.setReason("评论包含敏感词「" + sensitiveWord + "」，请重新发布,");
                return false;
            }
        }
        return true;
    }

    // ========== 评论自定义校验 ==========
    private static boolean customCommentCheck(String content, AuditResult result) {
        // 规则1：纯表情/空白内容（无意义评论）
        String trimContent = content.trim();
        if (trimContent.matches("^[\\p{So}\\s]+$")) { // \\p{So} 匹配emoji符号
            result.setPass(false);
            result.setReason("评论内容为纯表情/空白，无有效信息.");
            return false;
        }

        // 规则2：有效文字过少（可选）
        String pureText = trimContent.replaceAll("[\\p{Punct}\\s\\p{So}]", ""); // 移除标点/空格/emoji
        if (pureText.length() < 2 && !trimContent.matches("^[\\p{So}]{1,5}$")) { // 至少2个文字，或1-5个表情
            result.setPass(false);
            result.setReason("评论有效内容过少，请补充后发布.");
            return false;
        }

        return true;
    }

    // ========== 动态添加敏感词（同步数据库+内存） ==========
    public static void addSensitiveWord(String word) {
        if (StringUtils.hasText(word)) {
            String trimWord = word.trim();
            // 获取Spring容器中的工具类实例（调用Repository）
            AutoAuditUtils instance = SpringContextUtils.getBean(AutoAuditUtils.class);

            // 检查数据库是否已存在（避免重复）
            Optional<SensitiveWord> existWord = instance.sensitiveWordRepository.findByWord(trimWord);
            if (existWord.isPresent()) {
                System.err.println("⚠️ 敏感词已存在：" + trimWord);
                return;
            }

            // 同步到数据库
            SensitiveWord sw = new SensitiveWord();
            sw.setWord(trimWord);
            sw.setType("DEFAULT");
            sw.setCreateTime(LocalDateTime.now());
            sw.setUpdateTime(LocalDateTime.now());
            instance.sensitiveWordRepository.save(sw);

            // 同步到内存
            SENSITIVE_WORDS.add(trimWord);
        }
    }

    // ========== 删除敏感词（同步数据库+内存） ==========
    public static void removeSensitiveWord(String word) {
        if (StringUtils.hasText(word)) {
            String trimWord = word.trim();
            // 获取Spring容器中的工具类实例
            AutoAuditUtils instance = SpringContextUtils.getBean(AutoAuditUtils.class);

            // 从数据库删除
            instance.sensitiveWordRepository.deleteByWord(trimWord);

            // 从内存移除
            SENSITIVE_WORDS.remove(trimWord);
        }
    }

    // ========== 获取所有敏感词（返回新集合，避免外部修改） ==========
    public static Set<String> getSensitiveWords() {
        return new HashSet<>(SENSITIVE_WORDS);
    }

    // ========== 审核结果封装类（原有逻辑不变） ==========
    public static class AuditResult {
        private boolean pass;
        private String reason;

        public boolean isPass() { return pass; }
        public void setPass(boolean pass) { this.pass = pass; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}