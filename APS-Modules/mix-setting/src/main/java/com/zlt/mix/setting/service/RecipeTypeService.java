package com.zlt.mix.setting.service;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.RecipeType;

/**
 * 配方类型Service接口
 * 
 * @author Joran.zhang
 * @date 2022-05-31
 */
public interface RecipeTypeService  extends IService<RecipeType>
{
    /**
     * 查询配方类型列表
     * 
     * @param recipeType 配方类型
     * @return 配方类型集合
     */
    List<RecipeType> selectRecipeTypeList(RecipeType recipeType);

    /**
     * 保存配方类型信息（id为空则新增，id不为空则修改）
     *
     * @param recipeType
     */
    void saveRecipeType(RecipeType recipeType);

    /**
     * 批量删除配方类型
     * 
     * @param ids 需要删除的配方类型ID
     * @return 结果
     */
    int deleteRecipeTypeByIds(Long[] ids);

    /**
     * 校验配方类型唯一性
     */
    String checkRecipeTypeUnique(RecipeType recipeType);

    /**
     * 导入配方类型数据
     */
    AjaxResult importData(List<RecipeType> list, boolean updateSupport, Long importLogId);
}
