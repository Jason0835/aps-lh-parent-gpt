package com.zlt.mix.schedule.engine.vo;

import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 胶料分解记录
 * 
 * @author hakimryan
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueDecomposePlanVo extends GlueDecomposePlan {
	private static final long serialVersionUID = -3984494548416822207L;
	/**
	 * 物料大类
	 */
	private String majorType;
	/**
	 * 层级，终炼胶为第一层，一段母胶为最后一层
	 */
	private Integer level;
	// /**
	//  * 上级胶料分解记录
	//  */
	// private GlueDecomposePlanVo upGlueDecomposePlan;
	/**
	 * 上级胶料分级记录，可能有多个
	 */
	private List<GlueDecomposePlanVo> father = new ArrayList<>();
	/**
	 * 子胶料分解记录，可能有多个
	 */
	private List<GlueDecomposePlanVo> children = new ArrayList<>();
	/**
	 * 需求量（包含安全库存）
	 */
	private Double requireQty;
	/**
	 * 机台顺序
	 */
	private Integer machineOrder;
	/**
	 * 是否通过分解产生的计划
	 */
	private boolean decomposeFlag;
	/**
	 * 选配方优先级
	 */
	private Double recipeOrder;
}
