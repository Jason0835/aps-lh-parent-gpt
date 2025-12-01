package com.zlt.mix.schedule.engine.service.basicdata.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.mapper.MixingGlueRecipeMapEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.MixingGlueRecipeMapEngineService;
import com.zlt.mix.setting.api.domain.entity.MixingGlueRecipeMap;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 引擎胶料配方映射表ServiceImpl
 *
 * @author Liam
 * @since 2025/4/27
 */
@Service
public class MixingGlueRecipeMapEngineServiceImpl implements MixingGlueRecipeMapEngineService {
    @Resource
    private MixingGlueRecipeMapEngineMapper mixingGlueRecipeMapEngineMapper;

    /**
     * 查询胶料配方映射的胶料名称Map
     *
     * @param mixArea 密炼区
     * @return 胶料配方映射的胶料名称Map
     */
    @Override
    public Map<String, String> mapGlueRecipe(String mixArea) {
        MixingGlueRecipeMap query = new MixingGlueRecipeMap();
        query.setMixArea(mixArea);
        query.setReverseDayStockTag(GlueEngineConstants.YES_OR_NO_NO_0);
        List<MixingGlueRecipeMap> list = mixingGlueRecipeMapEngineMapper.selectGlueRecipeMapList(query);
        return list.stream()
                .filter(v -> StringUtils.isNotBlank(v.getGlue()) && StringUtils.isNotBlank(v.getMapGlue()))
                .collect(Collectors.toMap(MixingGlueRecipeMap::getGlueRecipeMapKey, MixingGlueRecipeMap::getMapGlue, (v1, v2) -> v1));
    }

    /**
     * 只查询胶料映射的Map
     *
     * @param mixArea 密炼区
     * @return 胶料映射的Map
     */
    @Override
    public Map<String, String> mapGlueRecipeOnlyGlue(String mixArea) {
        MixingGlueRecipeMap query = new MixingGlueRecipeMap();
        query.setMixArea(mixArea);
        query.setReverseDayStockTag(GlueEngineConstants.YES_OR_NO_NO_0);
        List<MixingGlueRecipeMap> list = mixingGlueRecipeMapEngineMapper.selectGlueRecipeMapList(query);
        return list.stream()
                .filter(v -> StringUtils.isNotBlank(v.getGlue()) && StringUtils.isNotBlank(v.getMapGlue()))
                .collect(Collectors.toMap(MixingGlueRecipeMap::getGlue, MixingGlueRecipeMap::getMapGlue, (v1, v2) -> v1));
    }

    /**
     * 查询胶料配方映射的反转白班计划量的Map
     *
     * @param mixArea 密炼区
     * @return 胶料配方映射的反转库存Map
     */
    @Override
    public Map<String, String> mapReserveGlueRecipe(String mixArea) {
        MixingGlueRecipeMap query = new MixingGlueRecipeMap();
        query.setMixArea(mixArea);
        query.setReverseDayStockTag(GlueEngineConstants.YES_OR_NO_YES_1);
        List<MixingGlueRecipeMap> list = mixingGlueRecipeMapEngineMapper.selectGlueRecipeMapList(query);
        return list.stream()
                .filter(v -> StringUtils.isNotBlank(v.getGlue()) && StringUtils.isNotBlank(v.getMapGlue()))
                .collect(Collectors.toMap(MixingGlueRecipeMap::getMapGlue, MixingGlueRecipeMap::getGlue, (v1, v2) -> v1));
    }
}
