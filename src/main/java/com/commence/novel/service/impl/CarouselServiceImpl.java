package com.commence.novel.service.impl;

import com.commence.novel.entity.Carousel;
import com.commence.novel.repository.CarouselRepository;
import com.commence.novel.service.CarouselService;
import com.commence.novel.utils.Result;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CarouselServiceImpl implements CarouselService {
    @Autowired
    private CarouselRepository carouselRepository;

    @Override
    public Result getAllCarouselList() {
        // 按排序升序、更新时间降序排列，包含所有状态
        List<Carousel> carouselList = carouselRepository.findAllByOrderBySortAscUpdateTimeDesc();
        return Result.success(carouselList, "查询所有轮播图列表成功");
    }


    @Override
    public Result addCarousel(Carousel carousel){
        if (ObjectUtils.isEmpty(carousel.getTitle())){
            return Result.error("1050","轮播图标题不能为空");
        }
        if (ObjectUtils.isEmpty(carousel.getImageUrl())){
            return Result.error("1050","轮播图图片地址不能为空");
        }

        carousel.setCreateTime(LocalDateTime.now());
        carousel.setUpdateTime(LocalDateTime.now());
        carousel.setStatus(ObjectUtils.isEmpty(carousel.getStatus()) ? "ENABLED" : carousel.getStatus());
        Carousel newCarousel = carouselRepository.save(carousel);

        return Result.success(newCarousel,"新增轮播图成功");
    }

    @Override
    public Result editCarousel(Long id, Carousel carousel){
        Carousel existCarousel = carouselRepository.findById(id).orElse(null);
        if (existCarousel == null){
            return Result.error("1051","轮播图不存在");
        }

        if (!"ENABLED".equals(carousel.getStatus()) && !"DISABLED".equals(carousel.getStatus())){
            return Result.error("1054","轮播图状态只能是ENABLED或DISABLED");
        }

        existCarousel.setTitle(carousel.getTitle());
        existCarousel.setImageUrl(carousel.getImageUrl());
        existCarousel.setNovelId(carousel.getNovelId());
        existCarousel.setSort(carousel.getSort());
        existCarousel.setStatus(carousel.getStatus());
        existCarousel.setUpdateTime(LocalDateTime.now());
        Carousel updatedCarousel = carouselRepository.save(existCarousel);

        return Result.success(updatedCarousel,"编辑轮播图成功");
    }

    @Override
    public Result deleteCarousel(Long id){
        Carousel existsCarousel = carouselRepository.findById(id).orElse(null);
        if (existsCarousel == null){
            return Result.error("1051","轮播图不存在");
        }

        if (!"DISABLED".equals(existsCarousel.getStatus())){
            return Result.error("1055","仅禁用状态的轮播图可删除，启用状态请先禁用");
        }

        carouselRepository.delete(existsCarousel);
        return Result.success(null,"删除轮播图成功");
    }
}
