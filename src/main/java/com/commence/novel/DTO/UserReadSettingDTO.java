package com.commence.novel.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserReadSettingDTO {
    private String userId;
    private Integer fontSize;
    private String bgColor;
    private String textColor;
    private BigDecimal lineHeight;
}
