package com.commence.novel.config;

import com.commence.novel.interceptor.JwtInterceptor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//注册jwt拦截器 + 配置文件上传
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Value("${file.upload.root-path:D:/实验报告/毕设/novel/upload/}")
    private String rootUploadPath;

    @PostConstruct
    public void init() {
        System.out.println("===== 文件上传根路径配置 =====");
        System.out.println("配置的根路径：" + rootUploadPath);
        String fixedPath = fixUploadPath(rootUploadPath);
        System.out.println("修正后的路径：" + fixedPath);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/upload/**" // 关键：放行图片访问，不被JWT拦截
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String fixedUploadPath = fixUploadPath(rootUploadPath);
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + fixedUploadPath);
    }

    private String fixUploadPath(String path) {
        if (path == null || path.isEmpty()) {
            return "D:/实验报告/毕设/novel/upload/";
        }
        if (path.contains("\\")) {
            return path.endsWith("\\") ? path : path + "\\";
        } else {
            return path.endsWith("/") ? path : path + "/";
        }
    }
}