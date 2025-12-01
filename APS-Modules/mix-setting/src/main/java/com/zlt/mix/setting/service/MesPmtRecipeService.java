package com.zlt.mix.setting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;
import com.zlt.mix.setting.api.domain.vo.MesPmtRecipeTemplateVo;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方信息Service接口
 *
 * @author chen
 * @date 2022-06-01
 */
public interface MesPmtRecipeService extends IService<MesPmtRecipe> {
    /**
     * 查询配方信息列表
     *
     * @param mesPmtRecipe 配方信息
     * @return 配方信息集合
     */
    List<MesPmtRecipe> selectMesPmtRecipeList(MesPmtRecipe mesPmtRecipe);

    /**
     * 根据机台名称和胶料名称查询配方信息
     *
     * @param mesPmtRecipe 机台名称和胶料名称
     * @return 配方集合
     */
    List<MesPmtRecipe> selectMesPmtRecipeByParams(MesPmtRecipe mesPmtRecipe);

    /**
     * 根据密炼区、胶料名称，查询对应配方的机台信息
     *
     * @param mesPmtRecipe 密炼区、胶料名称
     * @return 对应配方的机台信息
     */
    ArrayList<MesPmtRecipe> selectMesPmtRecipeMachine(MesPmtRecipe mesPmtRecipe);

    /**
     * 导入配方数据
     */
    AjaxResult importData(List<MesPmtRecipeTemplateVo> list, boolean updateSupport, Long importLogId);
}
