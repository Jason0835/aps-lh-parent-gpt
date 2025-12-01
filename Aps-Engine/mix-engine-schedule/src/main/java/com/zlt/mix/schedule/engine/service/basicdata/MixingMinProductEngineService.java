package com.zlt.mix.schedule.engine.service.basicdata;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 引擎炼胶单规格最小排产数相关Service
 *
 * @author Liam
 * @since 2025/4/11
 */
public interface MixingMinProductEngineService {
    /**
     * 加载炼胶单规格最小排产数
     *
     * @param mixArea 密炼取
     * @return 炼胶单规格最小排产数
     */
    Map<String, BigDecimal> mapMixingMinProduct(String mixArea);
}
