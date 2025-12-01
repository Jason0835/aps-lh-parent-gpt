package com.zlt.mix.schedule.engine.mapper;

import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MaterialAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;
import com.zlt.mix.setting.api.domain.entity.MesBasMaterial;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;
import com.zlt.mix.setting.api.domain.entity.RecipeType;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 引擎模块配方相关mapper
 */
public interface RecipeEngineMapper {

    /**
     * 从配方信息主表获取胶料的单车总重列表
     * @param glueAreaMachineList 终炼胶+密炼区+机台列表
     * @param recipeType 配方类型
     * @return
     */
    List<GlueAreaMachineVo> listGlueWeight(@Param("glueAreaMachineList") List<GlueAreaMachineVo> glueAreaMachineList, @Param("recipeType") String recipeType);

	/**
	 * 从配方信息主表获取胶料的单车总重列表
	 * 
	 * @param glueAreaMachineList 终炼胶+密炼区+机台列表
	 * @param recipeType 配方类型
	 * @return
	 */
	List<MesPmtRecipe> listGlueRecipeWeight(@Param("glueAreaMachineList") List<GlueAreaMachineVo> glueAreaMachineList, @Param("recipeType") String recipeType);

    /**
     * 从配方信息主表获取胶料的单车总重列表
     * @param planDate 计划日期
     * @param recipeType 配方类型
     * @return
     */
    List<GlueAreaMachineVo> listDecomposeGlueWeight(@Param("planDate") Date planDate, @Param("recipeType") String recipeType);
	
    /**
     * 从配方信息主表获取胶料的单车总重列表
     * @param planDate 计划日期
     * @param recipeType 配方类型
     * @return
     */
    List<MesPmtRecipe> listDecomposeGlueRecipeWeight(@Param("planDate") Date planDate, @Param("recipeType") String recipeType);

    /**
     * 根据物料code从配方主表查询对应的配方版本信息
     * @param recipeType  配方类型
     * @param materialNameList  物料名称列表
     * @return
     */
    List<MesPmtRecipe> listRecipeVersionInfo(@Param("recipeType") String recipeType, @Param("materialNameList") List<String> materialNameList);

	/**
	 * 查询胶料配方信息
	 * 
	 * @param mixArea 密炼区
	 * @return
	 */
	List<MesPmtRecipeVo> listGlueRecipe(@Param("params") MesPmtRecipeVo params);

	/**
	 * 根据物料名称，查询出物料编号
	 * @param materialList
	 * @return
	 */
	List<MesBasMaterial> listBasMaterial(@Param("materialList") List<MaterialAreaMachineVo> materialList);

	/**
	 * 查询配方类型
	 * @return
	 */
	List<RecipeType> listRecipeType();
	
	/**
	 * 获取有配置硫磺辅料机台的配方
	 * 
	 * @param mixArea 密炼区
	 * @param materialList 物料编号列表
	 * @return
	 */
	List<MesPmtRecipeVo> listLhflMachineRecipe(@Param("mixArea") String mixArea, @Param("materialList") List<String> materialList);

	/**
	 * 查询物料信息
	 */
	List<MesBasMaterial> selectListBasMaterial(MesBasMaterial mesBasMaterial);

	/**
	 * 查询称重存在塑炼的配方
	 */
	List<MesPmtRecipeVo> listSLGLueRecipe(MesPmtRecipeVo recipeParams);
}
