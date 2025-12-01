package com.zlt.mix.schedule.engine.vo;

import java.math.BigDecimal;

import com.zlt.mix.setting.api.domain.entity.MesPmtRecipeWeight;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 配方称重表
 * 
 * @author hakimryan
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MesPmtRecipeWeightVo extends MesPmtRecipeWeight {
	private static final long serialVersionUID = 5060950063517211702L;
	/**
	 * 物料大类
	 */
	private String majorType;
	/**
	 * 换算比率,子物料比父级物料的单车总重
	 */
	private BigDecimal conversionRatio;
}
