package com.commence.novel.service;

import com.commence.novel.entity.Carousel;
import com.commence.novel.utils.Result;

public interface CarouselService {
    //查询所有状态的轮播图（管理端）
    Result getAllCarouselList();

    //新增轮播图
    Result addCarousel(Carousel carousel);

    //编辑轮播图
    Result editCarousel(Long id, Carousel carousel);

    //删除轮播图
    Result deleteCarousel(Long id);
}
