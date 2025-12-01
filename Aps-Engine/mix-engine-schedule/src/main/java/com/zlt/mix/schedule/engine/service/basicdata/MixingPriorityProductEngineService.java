package com.zlt.mix.schedule.engine.service.basicdata;

import java.util.Map;

/**
 * 引擎炼胶优先排产相关Service
 *
 * @author Liam
 * @since 2025/4/17
 */
public interface MixingPriorityProductEngineService {

    /**
     * 加载炼胶优先排产
     *
     * @param mixArea 密炼区
     * @return 炼胶优先排产
     */
    Map<String, String> mapMixingPriorityProduct(String mixArea);
}
