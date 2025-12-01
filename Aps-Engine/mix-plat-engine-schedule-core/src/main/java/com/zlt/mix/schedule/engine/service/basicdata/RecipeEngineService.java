package com.zlt.mix.schedule.engine.service.basicdata;

import com.zlt.mix.schedule.engine.util.CombinedMapKey;
import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;
import com.zlt.mix.setting.api.domain.entity.MesBasMaterial;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 引擎部分配方相关Service
 */
public interface RecipeEngineService {

    /**
     * 从配方信息主表获取胶料的单车总重map
     * @param glueAreaMachineList  胶料区域机台列表
     * @param recipeType  配方类型
     * @return map，key--密炼区+胶料号+机台编号， value--胶料的单车重量
     */
    Map<String, Double> mapGlueWeight(List<GlueAreaMachineVo> glueAreaMachineList, String recipeType);
	
    /**
     * 从配方信息主表获取胶料的最小单车总重map
     * @param glueAreaMachineList  胶料区域机台列表
     * @param recipeType  配方类型
     * @return map，key--胶料号， value--胶料的单车重量
     */
    Map<String, Double> mapMinGlueWeight(List<GlueAreaMachineVo> glueAreaMachineList, String recipeType);

    /**
     * 从配方信息主表获取胶料的单车总重map
     * @param planDate  计划日期
     * @return map，key--密炼区+胶料号+机台编号， value--胶料的单车重量
     */
    Map<String, Double> mapGlueWeight(Date planDate);
	
    /**
     * 从配方信息主表获取胶料的单车总重map
     * @param planDate  计划日期
     * @return map，key--胶料号， value--胶料的单车重量
     */
    Map<String, Double> mapMinGlueWeight(Date planDate);

    /**
     * 从配方信息表中获取硫磺辅料配方版本map
     * @param areaMaterialList
     * @return map，key--辅料机编号+辅料名称， value--辅料配方版本信息
     */
    Map<String, MesPmtRecipe> mapLhflRecipeVersionInfo(List<MaterialAreaMachineVo> areaMaterialList);

    /**
     * 根据物料code从配方主表查询对应的配方版本信息
     * @param recipeType  配方类型
     * @param materialCodeList  物料编号列表
     * @return
     */
    List<MesPmtRecipe> listRecipeVersionInfo(String recipeType, List<String> materialCodeList);
    
    /**
	 * 加载胶料配方信息
	 * 
	 * @param recipeParams 过滤条件
	 * @return
	 */
	List<MesPmtRecipeVo> listGlueRecipe(MesPmtRecipeVo recipeParams);
	
	/**
	 * 加载胶料配方信息
	 * 
	 * @param recipeParams 过滤条件
	 * @return 胶料号 + 机台号作为组合key分组的集合
	 */
	Map<CombinedMapKey, List<MesPmtRecipeVo>> loadGlueRecipe(MesPmtRecipeVo recipeParams);

	/**
	 * 获取物料名称和物料编号的map
	 * @param materialList
	 * @return
	 */
	Map<String, String> mapBasMaterial(List<MaterialAreaMachineVo> materialList);
	
	/**
	 * 获取配方类型名称和配方类型编号的map
	 * @return
	 */
	Map<String, String> mapRecipeType();

	/**
	 * 查询物料信息
	 */
	List<MesBasMaterial> selectListBasMaterial(MesBasMaterial mesBasMaterial);

	/**
	 * 查询称重存在塑料胶的记录
	 */
	List<MesPmtRecipeVo> listSLGLueRecipe(MesPmtRecipeVo recipeParams);

	/**
	 * 查询对应物料+机台 存在 对应塑胶的记录
	 */
	Map<String, MesPmtRecipeVo> mapSLGLueRecipe(MesPmtRecipeVo recipeParams);
}
