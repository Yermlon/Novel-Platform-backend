package com.commence.novel.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Carousel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    private Long novelId;

    private Integer sort = 0;
    private  String status = "ENABLED";
    private LocalDateTime createTime =  LocalDateTime.now();
    private LocalDateTime updateTime =  LocalDateTime.now();

}
