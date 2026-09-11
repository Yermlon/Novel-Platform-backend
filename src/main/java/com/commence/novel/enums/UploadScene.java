package com.commence.novel.enums;

public enum UploadScene {
    USER_AVATAR("user_avatar", "用户头像", 2),    // 2MB
    NOVEL_COVER("novel_cover", "小说封面", 5),    // 5MB
    CAROUSEL_IMAGE("carousel", "轮播图", 8);      // 8MB

    private final String code;
    private final String desc;
    private final int maxSizeMB;

    UploadScene(String code, String desc, int maxSizeMB) {
        this.code = code;
        this.desc = desc;
        this.maxSizeMB = maxSizeMB;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
    public int getMaxSizeMB() { return maxSizeMB; }
    public long getMaxSizeBytes() { return (long) maxSizeMB * 1024 * 1024; }
}
