package com.commence.novel.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PwdTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456"; // 明文密码
        String encodePassword = encoder.encode(rawPassword);
        System.out.println(encodePassword); // 这行打印出来的，就是你数据库要用的！
    }
}
