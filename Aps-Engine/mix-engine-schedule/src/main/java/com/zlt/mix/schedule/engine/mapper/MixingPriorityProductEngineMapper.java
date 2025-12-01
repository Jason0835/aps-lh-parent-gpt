package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.setting.api.domain.entity.MixingPriorityProduct;

import java.util.List;

/**
 * 炼胶优先排产Mapper
 *
 * @author Liam
 * @since 2025/4/17
 */
public interface MixingPriorityProductEngineMapper {
    /**
     * 查询炼胶优先排产列表
     */
    List<MixingPriorityProduct> selectMixingPriorityProduct(MixingPriorityProduct query);
}
