package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.setting.api.domain.entity.MixingProductionModel;

import java.util.List;

/**
 * 炼胶生产模式Mapper
 *
 * @author Liam
 * @since 2025/4/24
 */
public interface MixingProductionModelEngineMapper {
    /**
     * 查询生产模式
     *
     * @param productionModel 生产模式参数
     * @return 生产模式列表
     */
    List<MixingProductionModel> selectProductionModelList(MixingProductionModel productionModel);
}
