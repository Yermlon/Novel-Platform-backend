package com.commence.novel.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class StatisticsDao {

    @PersistenceContext
    private EntityManager entityManager;

    // 登录用户日活
    public List<Map<String, Object>> countLoginUserDAU() {
        String sql = """
            SELECT 
                behavior_date AS `date`,
                CAST(COUNT(DISTINCT user_id) AS UNSIGNED) AS activeUserCount
            FROM (
                -- 子查询：先格式化日期，避免 GROUP BY 冲突
                SELECT 
                    DATE_FORMAT(active_time, '%Y-%m-%d') AS behavior_date,
                    user_id
                FROM (
                    -- 阅读行为
                    SELECT read_time AS active_time, user_id FROM novel_read 
                    WHERE read_time >= DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
                      AND user_id IS NOT NULL
                    UNION ALL
                    -- 评论行为
                    SELECT create_time AS active_time, user_id FROM novel_comment 
                    WHERE create_time >= DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
                      AND user_id IS NOT NULL 
                      AND is_deleted = 0
                    UNION ALL
                    -- 收藏行为
                    SELECT collect_time AS active_time, user_id FROM novel_collect 
                    WHERE collect_time >= DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
                      AND user_id IS NOT NULL
                    UNION ALL
                    -- 评论点赞行为
                    SELECT create_time AS active_time, user_id FROM novel_comment_like 
                    WHERE create_time >= DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
                      AND user_id IS NOT NULL
                ) AS raw_behavior
            ) AS active_behavior
            GROUP BY behavior_date
            ORDER BY `date` ASC
            """;
        Query query = entityManager.createNativeQuery(sql);
        query.unwrap(org.hibernate.query.NativeQuery.class)
                .setResultTransformer(org.hibernate.transform.AliasToEntityMapResultTransformer.INSTANCE);
        return query.getResultList();
    }

    // 登录用户月活
    public List<Map<String, Object>> countLoginUserMAU() {
        String sql = """
            SELECT 
                behavior_month AS `month`,
                CAST(COUNT(DISTINCT user_id) AS UNSIGNED) AS activeUserCount
            FROM (
                -- 子查询：先格式化月份，避免 GROUP BY 冲突
                SELECT 
                    CONCAT(YEAR(active_time), '-', LPAD(MONTH(active_time), 2, '0')) AS behavior_month,
                    user_id
                FROM (
                    -- 阅读行为
                    SELECT read_time AS active_time, user_id FROM novel_read 
                    WHERE read_time >= DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 6 MONTH)
                      AND user_id IS NOT NULL
                    UNION ALL
                    -- 评论行为
                    SELECT create_time AS active_time, user_id FROM novel_comment 
                    WHERE create_time >= DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 6 MONTH)
                      AND user_id IS NOT NULL 
                      AND is_deleted = 0
                    UNION ALL
                    -- 收藏行为
                    SELECT collect_time AS active_time, user_id FROM novel_collect 
                    WHERE collect_time >= DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 6 MONTH)
                      AND user_id IS NOT NULL
                    UNION ALL
                    -- 评论点赞行为
                    SELECT create_time AS active_time, user_id FROM novel_comment_like 
                    WHERE create_time >= DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 6 MONTH)
                      AND user_id IS NOT NULL
                ) AS raw_behavior
            ) AS active_behavior
            GROUP BY behavior_month
            ORDER BY `month` ASC
            """;
        Query query = entityManager.createNativeQuery(sql);
        query.unwrap(org.hibernate.query.NativeQuery.class)
                .setResultTransformer(org.hibernate.transform.AliasToEntityMapResultTransformer.INSTANCE);
        return query.getResultList();
    }

    // 整体访问量
    public List<Map<String, Object>> countTotalVisit() {
        String sql = """
        SELECT 
            DATE_FORMAT(visit_time, '%Y-%m-%d') AS `date`,
            CAST(COUNT(*) AS UNSIGNED) AS visitCount
        FROM visit_log  -- 改成访问日志表
        WHERE visit_time >= DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
        GROUP BY DATE_FORMAT(visit_time, '%Y-%m-%d')
        ORDER BY `date` ASC
        """;
        Query query = entityManager.createNativeQuery(sql);
        query.unwrap(org.hibernate.query.NativeQuery.class)
                .setResultTransformer(org.hibernate.transform.AliasToEntityMapResultTransformer.INSTANCE);
        return query.getResultList();
    }
}