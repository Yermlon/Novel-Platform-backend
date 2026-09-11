package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class NovelComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long novelId;
    private Long userId;
    private String content;
    private Long parentId;
    private Integer likeCount = 0;
    private Integer isAuthor = 0;
    private Boolean isTop;

    @Column(name = "is_deleted", columnDefinition = "BIT(1) DEFAULT 0")
    @JdbcTypeCode(SqlTypes.BOOLEAN)
    private Boolean isDeleted;

    @Column(nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 审核状态：PUBLISHED(已发布), BLOCKED(已屏蔽), PENDING(待审核)
     */
    @Column(name = "status", columnDefinition = "VARCHAR(20) DEFAULT 'PUBLISHED'")
    private String status;

    /**
     * 审核备注（记录违规原因）
     */
    @Column(name = "review_remark", columnDefinition = "VARCHAR(255)")
    private String reviewRemark;

    /**
     * 审核时间
     */
    @Column(name = "review_time")
    private LocalDateTime reviewTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        // 默认审核状态为 PUBLISHED（已发布）
        if (this.status == null) {
            this.status = "PUBLISHED";
        }
    }
}
