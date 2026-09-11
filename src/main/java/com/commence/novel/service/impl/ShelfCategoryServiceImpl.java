package com.commence.novel.service.impl;

import com.commence.novel.DTO.CategorySortDTO;
import com.commence.novel.entity.NovelCollect;
import com.commence.novel.entity.ShelfCategory;
import com.commence.novel.repository.NovelCollectRepository;
import com.commence.novel.repository.ShelfCategoryRepository;
import com.commence.novel.service.ShelfCategoryService;
import com.commence.novel.utils.Result;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShelfCategoryServiceImpl implements ShelfCategoryService {
    @Autowired
    private ShelfCategoryRepository shelfCategoryRepository;

    @Autowired
    private NovelCollectRepository novelCollectRepository;

    // 默认分组名称（常量）
    private static final String DEFAULT_CATEGORY_NAME = "默认分组";

    @Override
    public Result getCategoryList(Long userId) {
        if (userId == null) {
            return Result.error("1020", "用户ID不能为空");
        }

        // 1. 自动创建默认分组（如果不存在）
        createDefaultCategoryIfNotExists(userId);

        // 2. 查询用户所有分组
        List<ShelfCategory> categoryList = shelfCategoryRepository.findByUserIdOrderBySortAscCreateTimeAsc(userId);

        // 标记默认分组
        categoryList.forEach(category -> {
            if (DEFAULT_CATEGORY_NAME.equals(category.getName())) {
                category.setDefault(true);
            }
        });

        return Result.success(categoryList, "查询书架分类列表成功");
    }

    @Override
    public Result addCategory(Long userId, String categoryName) {
        if (userId == null) {
            return Result.error("1020", "用户ID不能为空");
        }
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return Result.error("1070", "分类名称不能为空");
        }

        String trimName = categoryName.trim();

        // 禁止创建同名默认分组
        if (DEFAULT_CATEGORY_NAME.equals(trimName)) {
            return Result.error("1072", "不能创建同名的默认分组");
        }

        // 检查同名分组
        boolean exists = shelfCategoryRepository.existsByUserIdAndName(userId, trimName);
        if (exists) {
            return Result.error("1071", "已存在同名的书架分类，请勿重复创建");
        }

        // 自动创建默认分组（确保默认分组存在）
        createDefaultCategoryIfNotExists(userId);

        // ==========获取当前用户最大sort值，新增分组sort=maxSort+1 ==========
        Integer maxSort = shelfCategoryRepository.findMaxSortByUserId(userId);
        if (maxSort == null) {
            maxSort = 0; // 初始值
        }

        // 新增分组
        ShelfCategory category = new ShelfCategory();
        category.setUserId(userId);
        category.setName(trimName);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setSort(maxSort + 1); // 递增排序值
        ShelfCategory savedCategory = shelfCategoryRepository.save(category);

        return Result.success(savedCategory, "新增分组成功");
    }

    // ========== 修改分组名称 ==========
    @Override
    public Result updateCategoryName(Long userId, Long categoryId, String newName) {
        // 参数校验
        if (userId == null) {
            return Result.error("1020", "用户ID不能为空");
        }
        if (categoryId == null) {
            return Result.error("1073", "分组ID不能为空");
        }
        if (newName == null || newName.trim().isEmpty()) {
            return Result.error("1070", "分类名称不能为空");
        }

        String trimNewName = newName.trim();

        // 1. 查询分组（并校验归属）
        Optional<ShelfCategory> categoryOpt = shelfCategoryRepository.findByIdAndUserId(categoryId, userId);
        if (categoryOpt.isEmpty()) {
            return Result.error("1074", "分组不存在或无权操作");
        }

        ShelfCategory category = categoryOpt.get();

        // 2. 禁止修改默认分组名称
        if (DEFAULT_CATEGORY_NAME.equals(category.getName())) {
            return Result.error("1075", "默认分组不能修改名称");
        }

        // 3. 禁止修改为默认分组名称
        if (DEFAULT_CATEGORY_NAME.equals(trimNewName)) {
            return Result.error("1072", "不能将分组名称修改为默认分组");
        }

        // 4. 检查新名称是否重复
        if (!category.getName().equals(trimNewName) && shelfCategoryRepository.existsByUserIdAndName(userId, trimNewName)) {
            return Result.error("1071", "已存在同名的书架分类，请勿重复修改");
        }

        // 5. 修改名称
        category.setName(trimNewName);
        category.setUpdateTime(LocalDateTime.now());
        shelfCategoryRepository.save(category);

        return Result.success(category, "分组名称修改成功");
    }

    // ========== 调整分组排序 ==========
    @Override
    public Result adjustCategorySort(Long userId, List<CategorySortDTO> categorySortList) {
        // 1. 参数校验
        if (userId == null) return Result.error("1020", "用户ID不能为空");
        if (categorySortList == null || categorySortList.isEmpty()) {
            return Result.error("1082", "排序数据不能为空");
        }

        // 2. 查询该用户所有非默认分组（按名称过滤）
        String DEFAULT_GROUP_NAME = "默认分组"; // 常量定义，建议抽离到配置类
        List<ShelfCategory> allCategories = shelfCategoryRepository.findByUserIdAndNameNotOrderBySortAsc(userId, DEFAULT_GROUP_NAME);

        if (allCategories.isEmpty()) {
            return Result.error("1083", "该用户暂无可排序的分组");
        }

        // 3. 校验所有待排序分组是否属于当前用户，且不是默认分组
        for (CategorySortDTO dto : categorySortList) {
            if (dto.getCategoryId() == null || dto.getSort() == null) {
                return Result.error("1084", "分组ID和排序值不能为空");
            }
            // 校验分组归属
            boolean exists = allCategories.stream()
                    .anyMatch(c -> c.getId().equals(dto.getCategoryId()));
            if (!exists) {
                return Result.error("1074", "分组不存在或无权操作：" + dto.getCategoryId());
            }
        }

        // 4. 临时更新排序值
        for (CategorySortDTO dto : categorySortList) {
            ShelfCategory category = allCategories.stream()
                    .filter(c -> c.getId().equals(dto.getCategoryId()))
                    .findFirst().orElse(null);
            if (category != null) {
                category.setSort(dto.getSort());
                category.setUpdateTime(LocalDateTime.now());
            }
        }

        // 5. 重新整理排序值为连续序号（避免断层/重复）
        List<ShelfCategory> sortedCategories = allCategories.stream()
                .sorted(Comparator.comparingInt(ShelfCategory::getSort))
                .collect(Collectors.toList());
        for (int i = 0; i < sortedCategories.size(); i++) {
            ShelfCategory category = sortedCategories.get(i);
            category.setSort(i + 1); // 排序值从1开始连续
            shelfCategoryRepository.save(category);
        }

        return Result.success(null, "分组排序调整成功");
    }

    // ========== 删除分组 ==========
    @Override
    public Result deleteCategory(Long userId, Long categoryId) {
        // 参数校验
        if (userId == null) {
            return Result.error("1020", "用户ID不能为空");
        }
        if (categoryId == null) {
            return Result.error("1073", "分组ID不能为空");
        }

        // 1. 查询分组（并校验归属）
        Optional<ShelfCategory> categoryOpt = shelfCategoryRepository.findByIdAndUserId(categoryId, userId);
        if (categoryOpt.isEmpty()) {
            return Result.error("1074", "分组不存在或无权操作");
        }

        ShelfCategory category = categoryOpt.get();

        // 2. 禁止删除默认分组
        if (DEFAULT_CATEGORY_NAME.equals(category.getName())) {
            return Result.error("1077", "默认分组不能删除");
        }

        // 3. 查询用户的默认分组
        Optional<ShelfCategory> defaultCategoryOpt = shelfCategoryRepository.findByUserIdAndName(userId, DEFAULT_CATEGORY_NAME);
        if (defaultCategoryOpt.isEmpty()) {
            return Result.error("1078", "默认分组不存在，无法删除当前分组");
        }
        Long defaultCategoryId = defaultCategoryOpt.get().getId();

        // 4. 将该分组下的所有收藏记录移入默认分组
        // 4.1 查询该用户该分组下的所有收藏记录
        List<NovelCollect> collectList = novelCollectRepository.findByUserIdAndCategoryId(userId, categoryId);
        // 4.2 批量更新这些记录的 categoryId 为默认分组ID
        if (!collectList.isEmpty()) {
            collectList.forEach(collect -> {
                collect.setCategoryId(defaultCategoryId); // 替换为默认分组ID
            });
            novelCollectRepository.saveAll(collectList); // 批量保存更新
        }

        // 5. 删除当前分组
        shelfCategoryRepository.deleteByIdAndUserId(categoryId, userId);

        return Result.success(null, "分组删除成功，该分组下的小说已移入默认分组");
    }

    // ========== 自动创建默认分组 ==========
    private void createDefaultCategoryIfNotExists(Long userId) {
        boolean defaultExists = shelfCategoryRepository.existsByUserIdAndName(userId, DEFAULT_CATEGORY_NAME);
        if (!defaultExists) {
            ShelfCategory defaultCategory = new ShelfCategory();
            defaultCategory.setUserId(userId);
            defaultCategory.setName(DEFAULT_CATEGORY_NAME);
            defaultCategory.setCreateTime(LocalDateTime.now());
            defaultCategory.setUpdateTime(LocalDateTime.now());
            defaultCategory.setSort(-1); // 默认分组排序值设为-1，确保排在最前面
            shelfCategoryRepository.save(defaultCategory);
        }
    }


}
