package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_read_setting", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user", columnNames = "user_id")
})
@Data
@NoArgsConstructor
public class UserReadSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    private Integer fontSize = 17;

    @Column(length = 20)
    private String bgColor = "#f8f6f2";
    @Column(length = 20)
    private String textColor = "#333333";
    @Column(precision = 2, scale = 1)
    private BigDecimal lineHeight = new BigDecimal("1.8");

    private LocalDateTime updateTime = LocalDateTime.now();

}
