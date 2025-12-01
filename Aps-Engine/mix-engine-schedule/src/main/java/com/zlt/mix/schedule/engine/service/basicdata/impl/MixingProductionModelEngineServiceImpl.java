package com.zlt.mix.schedule.engine.service.basicdata.impl;

import com.zlt.mix.schedule.engine.mapper.MixingProductionModelEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.MixingProductionModelEngineService;
import com.zlt.mix.setting.api.domain.entity.MixingProductionModel;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 引擎炼胶生产模式Service
 *
 * @author Liam
 * @since 2025/4/24
 */
@Service
public class MixingProductionModelEngineServiceImpl implements MixingProductionModelEngineService {
    @Resource
    private MixingProductionModelEngineMapper modelEngineMapper;

    /**
     * 查询生产模式
     *
     * @param productionModel 生产模式参数
     * @return 生产模式列表
     */
    @Override
    public List<MixingProductionModel> selectProductionModelList(MixingProductionModel productionModel) {
        return modelEngineMapper.selectProductionModelList(productionModel);
    }
}
