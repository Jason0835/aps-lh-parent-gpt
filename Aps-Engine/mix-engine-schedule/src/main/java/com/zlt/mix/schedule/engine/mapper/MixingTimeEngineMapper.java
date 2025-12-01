package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.setting.api.domain.entity.MixingTime;

import java.util.List;

/**
 * 炼胶间隔时间相关Mapper
 */
public interface MixingTimeEngineMapper {

    /**
     * 加载胶料间隔时间
     */
    List<MixingTime> listMixingTime(MixingTime query);
}
