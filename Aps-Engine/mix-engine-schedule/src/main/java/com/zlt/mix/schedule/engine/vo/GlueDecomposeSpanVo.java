package com.zlt.mix.schedule.engine.vo;

import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 胶料分解计划和跨区接收信息VO
 * 
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueDecomposeSpanVo extends GlueDecomposePlan {
	private static final long serialVersionUID = -3984494548416822207L;

	/**
	 * 接收表ID
	 */
	private Long receiveId;

	/**
	 * 发送Id
	 */
	private Long sendId;

	/**
	 * 被委托密炼区
	 */
	private String entrustedMixArea;

	/**
	 * 被委托密炼区对应的机台
	 */
	private String receiveMachineCode;
}
