package com.commence.novel.service;

import com.commence.novel.entity.NovelsType;
import com.commence.novel.utils.Result;

public interface NovelsTypeService {
    // 新增小说类型
    Result addNovelsType(NovelsType novelsType);

    // 编辑小说类型
    Result editNovelsType(Integer typeId, NovelsType novelsType);

    // 禁用/启用小说类型
    Result changeNovelsTypeStatus(Integer typeId, Integer status);

    // 查询所有小说类型（带状态）
    Result listAllNovelsType();

}
