package com.zlt.mix.schedule.engine.vo;

import com.zlt.mix.setting.api.domain.entity.MesPmtRecipe;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方信息对象
 * 
 * @author hakimryan
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MesPmtRecipeVo extends MesPmtRecipe {
	private static final long serialVersionUID = 2926883066751628380L;
	/**
	 * 称重明细
	 */
	private List<MesPmtRecipeWeightVo> recipeWeightList = new ArrayList<>();
	/**
	 * 物料大类
	 */
	private String majorType;
	/**
	 * 物料sapCode
	 */
	private String sapMaterialCode;
	/**
	 * 最少停放时长
	 */
	private Long minParkTime;
	/**
	 * 是否修改，查询用
	 */
	private Boolean isModify;
	/**
	 * 是否只要被忽略的配方
	 */
	private Boolean isOnlySkip;
	/**
	 * 物料名称列表，用于查询
	 */
	private List<String> recipeMaterialNameList;
}
