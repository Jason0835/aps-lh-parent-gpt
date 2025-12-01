package com.zlt.mix.schedule.engine.service.basicdata;

import com.zlt.mix.setting.api.domain.entity.MixingProductionModel;

import java.util.List;

/**
 * 引擎炼胶生产模式Service
 *
 * @author Liam
 * @since 2025/4/24
 */
public interface MixingProductionModelEngineService {
    /**
     * 查询生产模式
     *
     * @param productionModel 生产模式参数
     * @return 生产模式列表
     */
    List<MixingProductionModel> selectProductionModelList(MixingProductionModel productionModel);
}
