package com.commence.novel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//全局跨域配置
@Configuration
public class GlobalCorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") //添加映射路径
                        .allowedOrigins("http://localhost:8080","http://192.168.2.4:8080")  //开放哪些ip、端口、域名的访问权限
                        .allowCredentials(true)  //是否允许发送Cookie信息
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  //开放哪些Http方法，允许跨域访问
                        .allowedHeaders("*")  //允许Http请求中的携带哪些header信息
                        .exposedHeaders("*");  //暴露哪些头部信息
            }
        };
    }
}