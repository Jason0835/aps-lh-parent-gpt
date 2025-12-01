package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.setting.api.domain.entity.MixingMinProduct;

import java.util.List;

/**
 * 炼胶单规格最小排产数Mapper
 *
 * @author Liam
 * @since 2025/4/11
 */
public interface MixingMinProductEngineMapper {

    /**
     * 加载炼胶单规格最小排产数
     */
    List<MixingMinProduct> selectMixingMinProduct(MixingMinProduct query);
}
