package com.commence.novel.service.impl;

import com.commence.novel.entity.NovelsType;
import com.commence.novel.repository.NovelsTypeRepository;
import com.commence.novel.service.NovelsTypeService;
import com.commence.novel.utils.Result;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class NovelsTypeServiceImpl implements NovelsTypeService {

    @Autowired
    private NovelsTypeRepository novelsTypeRepository;

    @Override
    public Result addNovelsType(NovelsType novelsType) {
        // 参数校验
        if (StringUtils.isBlank(novelsType.getTypeName())) {
            return Result.error("1070", "小说类型名称不能为空");
        }

        // 校验类型名是否重复
        Optional<NovelsType> existType = novelsTypeRepository.findByTypeName(novelsType.getTypeName());
        if (existType.isPresent()) {
            return Result.error("1071", "该小说类型已存在");
        }

        // 设置默认值
        if (novelsType.getStatus() == null) {
            novelsType.setStatus(1); // 默认启用
        }
        novelsType.setCreateTime(new Date());
        novelsType.setUpdateTime(new Date());

        NovelsType savedType = novelsTypeRepository.save(novelsType);
        return Result.success(savedType, "新增小说类型成功");
    }

    @Override
    public Result editNovelsType(Integer typeId, NovelsType novelsType) {
        // 校验ID
        if (typeId == null) {
            return Result.error("1072", "小说类型ID不能为空");
        }

        // 校验类型是否存在
        Optional<NovelsType> existType = novelsTypeRepository.findById(typeId);
        if (existType.isEmpty()) {
            return Result.error("1073", "该小说类型不存在");
        }

        NovelsType typeToUpdate = existType.get();

        // 校验类型名是否重复（排除自身）
        if (StringUtils.isNotBlank(novelsType.getTypeName())) {
            Optional<NovelsType> nameExist = novelsTypeRepository.findByTypeName(novelsType.getTypeName());
            if (nameExist.isPresent() && !nameExist.get().getTypeId().equals(typeId)) {
                return Result.error("1071", "该小说类型已存在");
            }
            typeToUpdate.setTypeName(novelsType.getTypeName());
        }

        // 更新状态（如果传了）
        if (novelsType.getStatus() != null) {
            typeToUpdate.setStatus(novelsType.getStatus());
        }

        typeToUpdate.setUpdateTime(new Date());
        NovelsType updatedType = novelsTypeRepository.save(typeToUpdate);
        return Result.success(updatedType, "编辑小说类型成功");
    }

    @Override
    public Result changeNovelsTypeStatus(Integer typeId, Integer status) {
        // 校验参数
        if (typeId == null) {
            return Result.error("1072", "小说类型ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("1074", "状态只能是0（禁用）或1（启用）");
        }

        // 校验类型是否存在
        Optional<NovelsType> existType = novelsTypeRepository.findById(typeId);
        if (existType.isEmpty()) {
            return Result.error("1073", "该小说类型不存在");
        }

        NovelsType typeToUpdate = existType.get();
        typeToUpdate.setStatus(status);
        typeToUpdate.setUpdateTime(new Date());
        novelsTypeRepository.save(typeToUpdate);

        String msg = status == 1 ? "启用小说类型成功" : "禁用小说类型成功";
        return Result.success(null, msg);
    }

    @Override
    public Result listAllNovelsType() {
        List<NovelsType> typeList = novelsTypeRepository.findAll();
        return Result.success(typeList, "查询所有小说类型成功");
    }

}