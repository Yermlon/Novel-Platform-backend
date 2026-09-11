package com.commence.novel.DTO;

import lombok.Data;

@Data
public class UserReadProgressDTO {
    private String userId;
    private String novelId;
    private String chapterId;
    private Integer progress;
}
