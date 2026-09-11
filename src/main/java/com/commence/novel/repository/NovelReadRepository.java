package com.commence.novel.repository;

import com.commence.novel.entity.NovelRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface NovelReadRepository extends JpaRepository<NovelRead, Long> {
    Optional<NovelRead> findFirstByUserIdAndNovelIdOrderByReadTimeDesc(Long userId, Long novelId);

    List<NovelRead> findByUserIdOrderByReadTimeDesc(Long userId);

    List<NovelRead> findByNovelId(Long novelId);

    @Query(value = """
        SELECT DATE_FORMAT(read_time, '%Y-%m-%d') AS date, COUNT(*) AS readCount 
        FROM novel_read 
        WHERE novel_id = :novelId 
        AND read_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
        GROUP BY DATE_FORMAT(read_time, '%Y-%m-%d')
        ORDER BY date ASC
        """, nativeQuery = true)
    List<Map<String, Object>> countByDay(@Param("novelId") Long novelId);

    @Query(value = """
        SELECT 
            CONCAT(YEAR(read_time), '年第', WEEKOFYEAR(read_time), '周') AS week,
            COUNT(*) AS readCount,
            YEAR(read_time) AS year_num,
            WEEKOFYEAR(read_time) AS week_num
        FROM novel_read
        WHERE novel_id =  :novelId
            AND read_time >= DATE_SUB(NOW(), INTERVAL 4 WEEK)
        GROUP BY week, year_num, week_num
        ORDER BY year_num ASC, week_num ASC
        """, nativeQuery = true)
    List<Map<String, Object>> countByWeek(@Param("novelId") Long novelId);

    @Query(value = """
        SELECT
          CONCAT(YEAR(read_time), '年', MONTH(read_time), '月') AS month,
          COUNT(*) AS readCount,
          YEAR(read_time) AS year_num,
          MONTH(read_time) AS month_num
        FROM novel_read
        WHERE novel_id = :novelId
          AND read_time >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
        GROUP BY month, year_num, month_num
        ORDER BY year_num ASC, month_num ASC
        """, nativeQuery = true)
    List<Map<String, Object>> countByMonth(@Param("novelId") Long novelId);

}
