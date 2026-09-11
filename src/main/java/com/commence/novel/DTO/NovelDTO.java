package com.commence.novel.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class NovelDTO {
    @NotBlank(message = "小说名不能为空")
    @Length(max=15,message = "小说名长度不能超过15个字符")
    private String title;

    private String coverUrl;

    @Length(max= 300,message = "小说简介长度不能超过300个字符")
    private String intro;

    @NotNull(message = "小说分类不能为空")
    private Integer typeId;

    private String updateStatus;

    @Length(max=500, message = "驳回原因长度不能超过500个字符")
    private String rejectReason;



}
