package com.zlt.mix.schedule.engine.service.basicdata.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.schedule.engine.mapper.MixingPriorityProductEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.MixingPriorityProductEngineService;
import com.zlt.mix.setting.api.domain.entity.MixingPriorityProduct;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 引擎炼胶优先排产相关ServiceImpl
 *
 * @author Liam
 * @since 2025/4/17
 */
@Service
public class MixingPriorityProductEngineServiceImpl implements MixingPriorityProductEngineService {
    @Resource
    private MixingPriorityProductEngineMapper mixingPriorityProductEngineMapper;

    /**
     * 加载炼胶优先排产
     *
     * @param mixArea 密炼区
     * @return 胶料-优先胶料
     */
    @Override
    public Map<String, String> mapMixingPriorityProduct(String mixArea) {
        MixingPriorityProduct query = new MixingPriorityProduct();
        query.setMixArea(mixArea);
        return mixingPriorityProductEngineMapper.selectMixingPriorityProduct(query).stream()
                .filter(v -> StringUtils.isNotBlank(v.getGlue()) && StringUtils.isNotBlank(v.getPriorityGlue()))
                .collect(Collectors.toMap(MixingPriorityProduct::getGlue, MixingPriorityProduct::getPriorityGlue, (v1, v2) -> v1));
    }
}
