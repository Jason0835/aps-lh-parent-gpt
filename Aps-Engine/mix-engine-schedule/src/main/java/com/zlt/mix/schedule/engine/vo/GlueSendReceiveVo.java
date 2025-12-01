package com.zlt.mix.schedule.engine.vo;

import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 胶料分解计划和跨区接收信息VO
 * 
 */
@Data
public class GlueSendReceiveVo  {

	public GlueSendReceiveVo() {
	}

	public GlueSendReceiveVo(List<GlueSpanSend> sendList, List<GlueSpanReceive> receiveList, List<GlueDecomposePlan> glueDecomposePlanList) {
		this.sendList = sendList;
		this.receiveList = receiveList;
		this.glueDecomposePlanList = glueDecomposePlanList;
	}

	/**
	 * 发送列表
	 */
	private List<GlueSpanSend> sendList;

	/**
	 * 接收列表
	 */
	private List<GlueSpanReceive> receiveList;

	/**
	 * 分解胶料列表
	 */
	private List<GlueDecomposePlan> glueDecomposePlanList;
}
