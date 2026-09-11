package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Table(name = "user")
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //插入数据无需手动设置uid，自动生成唯一值
    private long uid;

    @Column(nullable = false, unique = true, length = 50)
    private String uname;

    @Column(nullable = false, length = 100)
    private String password;

    private String avatarUrl;

    private String email;

    //READER(读者)、AUTHOR(作者）、ADMIN(管理员)、默认读者
    @Column(nullable = false, length = 20)
    private String role = "READER";


    @Column(unique = true,nullable = true, length = 50)
    private String penName;

}
