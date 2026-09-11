package com.commence.novel.DTO;

import lombok.Data;

@Data
public class UserDTO {
    private Long uid;
    private String uname;
    private String password;
    private String email;
    private String role;
    private String penName;
    private String avatarUrl;
}
