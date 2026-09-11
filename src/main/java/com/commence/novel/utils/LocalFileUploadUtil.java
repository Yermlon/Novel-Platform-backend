package com.commence.novel.utils;

import com.commence.novel.enums.UploadScene;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class LocalFileUploadUtil {
    @Value("${file.upload.root-path:D:/实验报告/毕设/novel/upload/}")
    private String rootUploadPath;

    @Value("${file.access.domain:http://localhost:8081}")
    private String accessDomain;

    public String upload(MultipartFile file, UploadScene scene) throws IOException {
        // 1. 空文件校验
        if (file.isEmpty()) {
            throw new IllegalArgumentException(scene.getDesc() + "不能为空");
        }
        // 2. 大小校验
        if (file.getSize() > scene.getMaxSizeBytes()) {
            throw new IllegalArgumentException(scene.getDesc() + "大小不能超过" + scene.getMaxSizeMB() + "MB");
        }
        // 3. 类型校验
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件格式错误，仅支持jpg/png/jpeg/webp");
        }
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (!suffix.matches("\\.(jpg|png|jpeg|webp)$")) {
            throw new IllegalArgumentException("仅支持jpg/png/jpeg/webp格式的图片");
        }

        File rootDir = new File(rootUploadPath);
        if (!rootDir.exists()) {
            rootDir.mkdirs(); // 递归创建根目录（包括不存在的父目录）
        }

        // 4. 按场景创建目录
        String sceneDirPath = rootUploadPath + scene.getCode();
        File sceneDir = new File(sceneDirPath);
        if (!sceneDir.exists()) {
            sceneDir.mkdirs();
        }
        // 5. 生成唯一文件名
        String fileName = UUID.randomUUID().toString() + suffix;
        File destFile = new File(sceneDir, fileName);
        // 6. 保存文件
        file.transferTo(destFile);
        // 7. 返回URL
        return accessDomain + "/upload/" + scene.getCode() + "/" + fileName;
    }
}
