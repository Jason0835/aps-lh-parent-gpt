package com.zlt.mix.schedule.engine.service.basicdata.impl;

import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import com.zlt.mix.schedule.engine.mapper.RecipeEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.RecipeEngineService;
import com.zlt.mix.schedule.engine.util.CombinedMapKey;
import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;
import com.zlt.mix.setting.api.domain.entity.MesBasMaterial;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;
import com.zlt.mix.setting.api.domain.entity.RecipeType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 引擎部分配方相关ServiceImpl
 */
@Service
public class RecipeEngineServiceImpl implements RecipeEngineService {

    @Resource
    private RecipeEngineMapper recipeEngineMapper;

    /**
     * 从配方信息主表获取胶料的单车总重map
     * @param glueAreaMachineList  胶料区域机台列表
     * @param recipeType  配方类型
     * @return map，key--密炼区+胶料号+机台编号， value--胶料的单车重量
     */
    public Map<String, Double> mapGlueWeight(List<GlueAreaMachineVo> glueAreaMachineList, String recipeType) {
        Map<String, Double> map = new HashMap<>();
        List<GlueAreaMachineVo> glueWeightList = recipeEngineMapper.listGlueWeight(glueAreaMachineList, recipeType);
        if(!glueWeightList.isEmpty()) {
            for(GlueAreaMachineVo glueWeight : glueWeightList) {
                map.put(glueWeight.getGlue() + glueWeight.getMachineCode(), glueWeight.getWeight());
            }
        }
        return map;
    }

    /**
     * 从配方信息主表获取胶料的最小单车总重map
     *
     * @param glueAreaMachineList 胶料区域机台列表
     * @param recipeType          配方类型
     * @return map，key--胶料号， value--胶料的单车重量
     */
    public Map<String, Double> mapMinGlueWeight(List<GlueAreaMachineVo> glueAreaMachineList, String recipeType) {
        Map<String, Double> map = new HashMap<>();
        // 已按照配方、版本排序的列表
        List<MesPmtRecipe> glueWeightList = recipeEngineMapper.listGlueRecipeWeight(glueAreaMachineList, recipeType);
        if (!glueWeightList.isEmpty()) {
            getWeightMapByRecipe(glueWeightList, map);
        }
        return map;
    }

    /**
     * 根据配方优先级构建 胶料-最小单车重量 的Map
     *
     * @param glueWeightList 配方单车重量
     * @param minWeightMap            胶料-最小单车重量 的Map
     */
    private void getWeightMapByRecipe(List<MesPmtRecipe> glueWeightList, Map<String, Double> minWeightMap) {
        // 根据胶料+机台取最小单车重量，新版本覆盖旧版本
        Map<String, MesPmtRecipe> recipeMap = new HashMap<>();
        for (MesPmtRecipe newPmtRecipe : glueWeightList) {
            String recipeKey = GenerageMapKeyUtils.createMapKey(newPmtRecipe.getRecipeMaterialName(), newPmtRecipe.getRecipeEquipCode());
            recipeMap.put(recipeKey, newPmtRecipe);
        }

        // 再根据物料不区分机台获取最小单车
        for (MesPmtRecipe item : recipeMap.values()) {
            String minKey = item.getRecipeMaterialName();
            Double oldMinWeight = minWeightMap.get(minKey);
            if (oldMinWeight == null) {
                if (item.getLotTotalWeight() != null) {
                    minWeightMap.put(minKey, item.getLotTotalWeight());
                }

                continue;
            }

            if (item.getLotTotalWeight() == null) {
                continue;
            }
            // 相同配方版本，取重量最小的
            Double newWeight = item.getLotTotalWeight();
            if (newWeight.compareTo(oldMinWeight) < 0) {
                minWeightMap.put(minKey, newWeight);
            }
        }
    }

    /**
     * 从配方信息主表获取胶料的单车总重map
     * @param planDate  计划日期
     * @return map，key--密炼区+胶料号+机台编号， value--胶料的单车重量
     */
    public Map<String, Double> mapGlueWeight(Date planDate) {
        Map<String, Double> map = new HashMap<>();
        List<GlueAreaMachineVo> glueWeightList = recipeEngineMapper.listDecomposeGlueWeight(planDate, null);  //默认拿配方类型是ZZ的
        if(!glueWeightList.isEmpty()) {
            for(GlueAreaMachineVo glueWeight : glueWeightList) {
                map.put(glueWeight.getGlue() + glueWeight.getMachineCode(), glueWeight.getWeight());
            }
        }
        return map;
    }

    /**
     * 从配方信息主表获取胶料的单车总重map
     *
     * @param planDate 计划日期
     * @return map，key--胶料号， value--胶料的单车重量
     */
    public Map<String, Double> mapMinGlueWeight(Date planDate) {
        Map<String, Double> map = new HashMap<>();
        List<MesPmtRecipe> glueWeightList = recipeEngineMapper.listDecomposeGlueRecipeWeight(planDate, null);  //默认拿配方类型是ZZ的
        if (!glueWeightList.isEmpty()) {
            getWeightMapByRecipe(glueWeightList, map);
        }
        return map;
    }

    /**
     * 从配方信息表中获取硫磺辅料配方版本map
     * @param areaMaterialList
     * @return map，key--辅料机编号+辅料名称， value--辅料配方版本信息
     */
    public Map<String, MesPmtRecipe> mapLhflRecipeVersionInfo(List<MaterialAreaMachineVo> areaMaterialList) {
        Map<String, MesPmtRecipe> map = new HashMap<>();
        List<String> materialNameList = areaMaterialList.stream().map(r->r.getMaterialName()).collect(Collectors.toList());
        List<MesPmtRecipe> vecipeVersionList = listRecipeVersionInfo(null, materialNameList);  //硫磺辅料配方版本默认拿配方类型是ZZ的
        for(MesPmtRecipe recipe : vecipeVersionList) {
            map.put(recipe.getRecipeEquipCode() + recipe.getRecipeMaterialName(), recipe);
        }
        return map;
    }

    /**
     * 根据物料code从配方主表查询对应的配方版本信息
     * @param recipeType  配方类型
     * @param materialNameList  物料名称列表
     * @return
     */
    public List<MesPmtRecipe> listRecipeVersionInfo(String recipeType, List<String> materialNameList) {
        return recipeEngineMapper.listRecipeVersionInfo(null, materialNameList);
    }

	/**
	 * 加载胶料配方信息
	 * 
	 * @param recipeParams 过滤条件
	 * @return
	 */
    @Override
	public List<MesPmtRecipeVo> listGlueRecipe(MesPmtRecipeVo recipeParams) {
		return recipeEngineMapper.listGlueRecipe(recipeParams);
	}

	/**
	 * 加载胶料配方信息
	 * 
	 * @param recipeParams 过滤条件
	 * @return
	 */
    @Override
	public Map<CombinedMapKey, List<MesPmtRecipeVo>> loadGlueRecipe(MesPmtRecipeVo recipeParams) {
		List<MesPmtRecipeVo> mesPmtRecipeList = this.listGlueRecipe(recipeParams);
		return mesPmtRecipeList.stream().collect(Collectors
				.groupingBy(r -> CombinedMapKey.createKey(r.getRecipeMaterialName(), r.getRecipeEquipCode())));
	}

    /**
     * 获取物料名称和物料编号的map
     * @param materialList
     * @return
     */
	public Map<String, String> mapBasMaterial(List<MaterialAreaMachineVo> materialList) {
        Map<String, String> map = new HashMap<>();
        List<MesBasMaterial> list = recipeEngineMapper.listBasMaterial(materialList);
        if(list != null && !list.isEmpty()) {
            for(MesBasMaterial material : list) {
                map.put(material.getMaterialName(), material.getMaterialCode());
            }
        }
        return map;
    }
	

	/**
	 * 获取配方类型名称和配方类型编号的map
	 * @return
	 */
	public Map<String, String> mapRecipeType() {
		return recipeEngineMapper.listRecipeType().stream()
				.collect(Collectors.toMap(RecipeType::getRecipeTypeCode, RecipeType::getRecipeTypeName));
	}

    /**
     * 查询物料信息
     */
    @Override
    public List<MesBasMaterial> selectListBasMaterial(MesBasMaterial mesBasMaterial) {
        return recipeEngineMapper.selectListBasMaterial(mesBasMaterial);
    }

    /**
     * 查询称重存在塑料胶的记录
     */
    @Override
    public List<MesPmtRecipeVo> listSLGLueRecipe(MesPmtRecipeVo recipeParams) {
        return recipeEngineMapper.listSLGLueRecipe(recipeParams);
    }

    /**
     * 查询对应物料+机台 存在 对应塑胶的配方称重记录
     */
    @Override
    public Map<String, MesPmtRecipeVo> mapSLGLueRecipe(MesPmtRecipeVo recipeParams) {
        List<MesPmtRecipeVo> recipeVoList = recipeEngineMapper.listSLGLueRecipe(recipeParams);
        if (CollectionUtils.isEmpty(recipeVoList)) {
            return Collections.emptyMap();
        }
        Map<String, MesPmtRecipeVo> recipeVoMap = new HashMap<>();
        for (MesPmtRecipeVo itemVo : recipeVoList) {
            if (StringUtils.isNotBlank(itemVo.getRecipeMaterialName()) 
                    && StringUtils.isNotBlank(itemVo.getRecipeEquipCode())) {
                recipeVoMap.put(GenerageMapKeyUtils.createMapKey(itemVo.getRecipeMaterialName(), itemVo.getRecipeEquipCode()), itemVo);
            }
        }
        return recipeVoMap;
    }
}
