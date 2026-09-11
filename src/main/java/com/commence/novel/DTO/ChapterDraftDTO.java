package com.commence.novel.DTO;

import lombok.Data;

@Data
public class ChapterDraftDTO {
    private String draftContent;
    private String title;
    private String brief;
    private boolean isAuto;
}
