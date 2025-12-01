package com.zlt.mix.schedule.engine.vo;

import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 胶料跨区接收值对象
 *
 * @author hak
 * @date 2022-08-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueSpanReceiveVo extends GlueSpanReceive {
	private static final long serialVersionUID = 4441000249845760363L;

	/**
	 * 配方重量
	 */
	private Double lotTotalWeight;

	/**
	 * 物料大类
	 */
	private String majorType;
}
