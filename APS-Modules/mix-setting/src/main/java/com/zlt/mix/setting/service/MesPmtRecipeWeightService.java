package com.zlt.mix.setting.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipeWeight;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 配方称量明细Service接口
 *
 * @author chen
 * @date 2022-06-01
 */
public interface MesPmtRecipeWeightService extends IService<MesPmtRecipeWeight> {
    /**
     * 查询配方称量明细列表
     *
     * @param mesPmtRecipeWeight 配方称量明细
     * @return 配方称量明细集合
     */
    List<MesPmtRecipeWeight> selectMesPmtRecipeWeightList(MesPmtRecipeWeight mesPmtRecipeWeight);
}
