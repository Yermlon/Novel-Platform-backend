package com.commence.novel.DTO;

import com.commence.novel.entity.User;
import lombok.Data;

@Data
public class RegisterParam {
    private User newUser;
    private String roleType;
    private String code;
}
