package com.zlt.mix.schedule.engine.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方分解VO
 * 
 * @author carl
 *
 */
@Data
public class DecomposeRecipeVo  {

	/**
	 * 机台code
	 */
	private String machineCode;

	/**
	 * 胶料名称
	 */
	private String glue;

	/**
	 * 子胶名称
	 */
	private String sonGlue;

	/**
	 * 配方类型
	 */
	private String recipeType;

	/**
	 * 配方版本号
	 */
	private String recipeVersionId;

	/**
	 * 投产阶段
	 */
	private String productStage;

}
