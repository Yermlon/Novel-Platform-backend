package com.commence.novel.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChapterDTO {
    @NotNull(message = "小说ID不能为空")
    private Long novelId;
    private String title;
    private  String content;
    private Integer sort;
    private String brief;
}
