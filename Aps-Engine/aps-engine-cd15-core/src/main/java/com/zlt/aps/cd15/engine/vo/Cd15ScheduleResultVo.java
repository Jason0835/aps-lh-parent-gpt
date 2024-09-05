package com.zlt.aps.cd15.engine.vo;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseDto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 15度裁断排程结果值对象
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-8 10:56:49
 * @Version 1.0
 */
@Data
public class Cd15ScheduleResultVo extends ApsBaseDto {

	private static final long serialVersionUID = 1L;
	/** 主键ID **/
	private Long id;

	/** 排程日期 */
	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
	private Date scheduleDate;

	/** 对应的成型批次号 */
	private String cxBatchNo;

	/** 批次号 **/
	private String batchNo;

	/** 工单号 **/
	private String orderNo;

	/** 钢压大卷编号 */
	private String bigRollCode;

	/** 机台ID,多个逗号分割 */
	private String machineId;

	/** 1#钢带代码 */
	private String steelStripCode1;

	/** 2#钢带代码 */
	private String steelStripCode2;

	/** 1#钢带单耗 */
	private Double unitConsume1;

	/** 1#钢带库存数量 */
	private Double stock1Qty1;

	/** 2#钢带库存数量 */
	private Double stock1Qty2;

	/** 1#钢带库存供应成型时长，单位：小时 */
	private Double supplyTime1;

	/** 1#钢带中班(12点-24点)计划量 */
	private Double dayPlanQty1;
	
	/** 1#钢带中班(12点-24点)完成量 */
    private Double dayFinishQty1;
	
	/** 1#钢带中班(12点-24点)完成率 */
    private Double dayFinishRate1;

	/** 1#钢带中班(12点-24点)生产顺序 */
	private Long dayProduceOrder1;

	/** 1#钢带中班(12点-24点)手动输入原因分析 */
	private String dayHandAnalysis1;

	/** 中班系统原因分析 */
	private String daySysAnalysis1;

	/** 1#钢带夜班(0点-12点)计划量 */
	private Double nightPlanQty1;
	
	/** 1#钢带夜班(0点-12点)完成量 */
    private Double nightFinishQty1;
	
	/** 1#钢带夜班(0点-12点)完成率 */
    private Double nightFinishRate1;

	/** 1#钢带夜班(0点-12点)生产顺序 */
	private Long nightProduceOrder1;

	/** 夜班系统原因分析 */
	private String nightSysAnalysis1;

	/** 1#钢带夜班(0点-12点)手动输入原因分析 */
	private String nightHandAnalysis1;

	/** 1#钢带对应成型一班的计划量 */
	private Double cxClass1Plan;

	/** 1#钢带对应成型二班的计划量 */
	private Double cxClass2Plan;

	/** 1#钢带对应成型三班的计划量 */
	private Double cxClass3Plan;

	/** 1#钢带对应成型次一班的计划量 */
	private Double cxClass4Plan;

	/** 1#钢带对应成型次二班的计划量 */
	private Double cxClass5Plan;

	/**
	 * 月计划剩余量
	 */
	private Double monthPlanOs;

	/**
	 * 月计划需求量
	 */
	private Double monthPlan;

	/**
	 * 裁断角度
	 */
	private Double cuttingAngle;

	/**
	 * 生产状态:0-未生产；1-生产中；2-生产完成
	 */
	private String productionStatus;

	/**
	 * 收尾提示标识(0:提示收尾；1:不需要提示)
	 */
	public String markCloseOutTip;

	/**
	 * 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE
	 */
	private String isRelease;

	/**
	 * 发布成功计数器，每点击一次发布并成功的话，计数器累加
	 */
	private Integer publishSuccessCount;

	/**
	 * 最新发布成功时间
	 */
	private Date newestPublishTime;

	/**
	 * 数据来源
	 */
	private String dataSource;

	/**
	 * 工艺
	 */
	private String craft1;

	/**
	 * 工艺
	 */
	private String craft2;
	
	/**
	 * 半钢边胶
	 */
	private String edgeGlue;
	
	/**
	 * 总计划量
	 */
	private BigDecimal totalPlanQty;
    
    /**
     * 收尾规格标记，0：收尾1：非收尾
     */
    private String closeOutSpecFlag;

	@Override
	public String toString() {
		return "Cd15ScheduleResultVo [scheduleDate=" + scheduleDate + ", cxBatchNo=" + cxBatchNo + ", batchNo="
				+ batchNo + ", orderNo=" + orderNo + ", bigRollCode=" + bigRollCode + ", machineId=" + machineId
				+ ", steelStripCode1=" + steelStripCode1 + ", steelStripCode2=" + steelStripCode2 + ", unitConsume1="
				+ unitConsume1 + ", stock1Qty1=" + stock1Qty1 + ", stock1Qty2=" + stock1Qty2 + ", supplyTime1="
				+ supplyTime1 + ", dayPlanQty1=" + dayPlanQty1 + ", dayProduceOrder1=" + dayProduceOrder1
				+ ", nightPlanQty1=" + nightPlanQty1 + ", nightProduceOrder1=" + nightProduceOrder1 + ", cxClass1Plan="
				+ cxClass1Plan + ", cxClass2Plan=" + cxClass2Plan + ", cxClass3Plan=" + cxClass3Plan + ", cxClass4Plan="
				+ cxClass4Plan + ", cxClass5Plan=" + cxClass5Plan + ", monthPlanOs=" + monthPlanOs + ", monthPlan="
				+ monthPlan + ", cuttingAngle=" + cuttingAngle + ", productionStatus=" + productionStatus
				+ ", markCloseOutTip=" + markCloseOutTip + ", isRelease=" + isRelease + "]";
	}

}
