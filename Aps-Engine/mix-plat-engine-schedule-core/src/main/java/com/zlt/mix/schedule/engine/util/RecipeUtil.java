package com.zlt.mix.schedule.engine.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

import com.github.pagehelper.util.StringUtil;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeWeightVo;

/**
 * 配方工具
 * 
 * @author hakimryan
 *
 */
public class RecipeUtil {
	/**
	 * 不合格胶的正则表达式
	 */
	private final static String PATTERN_BHG_GLUE = "\\w*(-B|-H|-L|-D)";
	/**
	 * 洗胶的正则表达式
	 */
	private final static String PATTERN_WASH_GLUE = "X\\w*";

	/**
	 * 获取配方类型的优先级，数值越大优先级越高
	 * 
	 * @param recipeType 配方类型
	 * @return
	 */
	public static Integer getRecipeTypePriority(String recipeType) {
		if (recipeType == null) {
			return 0;
		}
		boolean isC2Z = recipeType.startsWith(GlueEngineConstants.RECIPE_TYPE_C2Z);
		boolean isZZ = recipeType.startsWith(GlueEngineConstants.RECIPE_TYPE_ZZ);
		boolean isS = recipeType.startsWith(GlueEngineConstants.RECIPE_TYPE_S); // 试制标识开头
		boolean isCS = recipeType.startsWith(GlueEngineConstants.RECIPE_TYPE_CS); // 掺胶试制标识开头
		if (isC2Z) {
			// C2Z的最优先
			return 5;
		} else if (isS) {
			// 试制的最后
			return 1;
		} else if (isCS) {
			// 掺胶试制次之
			return 2;
		} else if (isZZ) {
			// ZZ再试制之前
			return 3;
		} else {
			// 其余的在中间
			return 4;
		}
	}

	/**
	 * 获取胶料的真正物料类型，主要是为了对不合格胶的特殊处理
	 * 
	 * @param glueCode  胶料编号
	 * @param majorType 物料类型
	 * @return
	 */
	public static String getMajorType(String glueCode, String majorType) {
		if (glueCode == null || majorType == null) {
			return majorType;
		}
		if (GlueEngineConstants.SCHEDULE_MAJOR_TYPE.contains(majorType)) {
			// 使用正则表达式匹配是否不合格胶的胶料代码
			if (Pattern.matches(PATTERN_BHG_GLUE, glueCode)) {
				// 匹配上则直接返回不合格胶的类型代码
				return GlueEngineConstants.MAJOR_TYPE_BHG;
			}
			// 洗胶代码
			if (Pattern.matches(PATTERN_WASH_GLUE, glueCode)) {
				return GlueEngineConstants.MAJOR_TYPE_WASH;
			}
		}
		return majorType;
	}

	/**
	 * 获取胶料的真正物料类型<br/>
	 * 会特别区分出不合格胶、洗胶、掺胶类型
	 * 
	 * @param glueCode  胶料代码
	 * @param majorType 胶料类型
	 * @return
	 */
	public static String getMajorType(String glueCode, String majorType, BigDecimal setWeight,
			BigDecimal maxSetWeight) {
		// 先区分是否不合格胶
		String realMajorType = getMajorType(glueCode, majorType);
		// 如果物料类型是终炼母炼，但又不是同一系列的胶，则判断物料类型为掺胶
		if (GlueEngineConstants.SCHEDULE_MAJOR_TYPE.contains(realMajorType) && setWeight.compareTo(maxSetWeight) != 0) {
			return GlueEngineConstants.MAJOR_TYPE_MIX;
		} else {
			return realMajorType;
		}
	}

	/**
	 * 检查是否下级母炼胶
	 * 
	 * @param glueCode
	 * @param upGlueCode
	 * @return
	 */
	public static boolean checkIsSubGlue(String glueCode, String upGlueCode) {
		if (StringUtil.isEmpty(glueCode) || StringUtil.isEmpty(upGlueCode)) {
			return false;
		}
		// 如果下段胶的胶料号一样（自己炼自己），也是掺胶
		if (glueCode.equals(upGlueCode)) {
			return false;
		}
		// 终胶编号
		String finalGludeCode;
		// 下级以斜杠分隔，胶前半部分代码一致
		if (upGlueCode.contains("/")) {
			finalGludeCode = upGlueCode.substring(0, upGlueCode.indexOf("/"));
		} else {
			finalGludeCode = upGlueCode;
		}
		return glueCode.startsWith(finalGludeCode);
	}

	/**
	 * 获取称重配方中类型为终炼或母炼胶的配方重量，没有终炼母炼胶则返回0
	 * 
	 * @param recipeWeightList
	 * @return
	 */
	public static BigDecimal getMaxSetWeight(List<MesPmtRecipeWeightVo> recipeWeightList) {
		BigDecimal maxSetWeight = recipeWeightList.stream()
				.filter(r -> GlueEngineConstants.SCHEDULE_MAJOR_TYPE.contains(r.getMajorType()))
				.map(r -> BigDecimalUtil.valueOfZero(r.getSetWeight())).max(BigDecimal::compareTo)
				.orElse(BigDecimal.ZERO);
		return maxSetWeight;
	}
}
