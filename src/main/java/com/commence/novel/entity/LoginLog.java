package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "login_log")
public class LoginLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //日志id

    @Column(nullable = false, length = 50)
    private String uname;
    private Long userId; //关联用户id

    @Column(nullable = false, length = 50)
    private String ip;

    @Column(nullable = false, length = 10)
    private String status; //登录状态

    @Column(length = 200)
    private String message; //备注信息

    @Column(nullable = false)
    private LocalDateTime loginTime; //登录时间
}
