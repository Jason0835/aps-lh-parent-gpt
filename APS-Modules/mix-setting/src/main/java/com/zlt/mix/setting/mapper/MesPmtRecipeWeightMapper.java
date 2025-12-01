package com.zlt.mix.setting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipeWeight;

import java.util.List;

/**
 * 配方称量明细Mapper接口
 *
 * @author chen
 * @date 2022-06-01
 */
public interface MesPmtRecipeWeightMapper extends BaseMapper<MesPmtRecipeWeight> {

    /**
     * 查询配方称量明细列表
     *
     * @param mesPmtRecipeWeight 配方称量明细
     * @return 配方称量明细集合
     */
    List<MesPmtRecipeWeight> selectMesPmtRecipeWeightList(MesPmtRecipeWeight mesPmtRecipeWeight);

    /**
     * 根据ID批量更新或新增
     */
    int insertBatch(List<MesPmtRecipeWeight> itemList);
}
