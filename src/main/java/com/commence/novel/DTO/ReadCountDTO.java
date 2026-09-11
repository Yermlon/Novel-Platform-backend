package com.commence.novel.DTO;

import lombok.Data;

@Data
public class ReadCountDTO {
    private Long totalReadCount;
    private Long uniqueUserCount;

    public ReadCountDTO(Long totalReadCount, Long uniqueUserCount) {
        this.totalReadCount = totalReadCount;
        this.uniqueUserCount = uniqueUserCount;
    }
}
