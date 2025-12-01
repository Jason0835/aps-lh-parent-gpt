package com.zlt.mix.schedule.engine.service.basicdata;

import java.util.Map;

/**
 * 引擎胶料配方映射表Service
 *
 * @author Liam
 * @since 2025/4/27
 */
public interface MixingGlueRecipeMapEngineService {
    /**
     * 查询胶料配方映射的胶料名称Map
     *
     * @param mixArea 密炼区
     * @return 胶料配方映射的胶料名称Map
     */
    Map<String, String> mapGlueRecipe(String mixArea);

    /**
     * 只查询胶料映射的Map
     *
     * @param mixArea 密炼区
     * @return 胶料映射的Map
     */
    Map<String,String> mapGlueRecipeOnlyGlue(String mixArea);

    /**
     * 查询胶料配方映射的反转白班计划量的Map
     *
     * @param mixArea 密炼区
     * @return 胶料配方映射的反转库存Map
     */
    Map<String, String> mapReserveGlueRecipe(String mixArea);
}
