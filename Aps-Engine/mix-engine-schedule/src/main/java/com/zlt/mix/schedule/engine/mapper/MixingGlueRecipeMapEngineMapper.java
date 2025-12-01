package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.setting.api.domain.entity.MixingGlueRecipeMap;

import java.util.List;

/**
 * 胶料配方映射表Mapper
 *
 * @author Liam
 * @since 2025/4/27
 */
public interface MixingGlueRecipeMapEngineMapper {
    /**
     * 查询胶料配方映射列表
     *
     * @param query 查询条件
     * @return 胶料配方映射列表
     */
    List<MixingGlueRecipeMap> selectGlueRecipeMapList(MixingGlueRecipeMap query);
}
