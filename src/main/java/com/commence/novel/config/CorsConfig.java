package com.commence.novel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

//前后端跨域配置类
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 👇 允许你的前端 Vue 地址
        config.addAllowedOrigin("http://localhost:8080");

        // 允许携带 Cookie 或 Token
        config.setAllowCredentials(true);

        // 允许所有请求方法（GET、POST、PUT、DELETE 等）
        config.addAllowedMethod("*");

        // 允许所有请求头
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 👇 对所有请求路径生效，包括上传的图片
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}