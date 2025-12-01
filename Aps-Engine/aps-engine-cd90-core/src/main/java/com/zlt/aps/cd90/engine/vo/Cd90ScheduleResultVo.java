package com.zlt.aps.cd90.engine.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import com.zlt.aps.common.core.utils.BigDecimalUtil;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 90度裁断排程结果值对象
 *
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 10:56:49
 * @Version 1.0
 */
@Data
public class Cd90ScheduleResultVo extends ApsBaseDto {

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

	/** 帘布代码 */
	private String clothCode;

	/** 单耗 */
	private Double unitConsume;

	/** 机台ID,多个逗号分割 */
	private String machineId;

	/** 库存数量 */
	private Double stockQty;

	/** 库存供应成型时长，单位：小时 */
	private Double supplyTime;

	/** 中班(12点-24点)计划量 */
	private Double dayPlanQty;

	/** 中班(12点-24点)完成量 */
    private Double dayFinishQty;

	/** 中班(12点-24点)完成率 */
    private Double dayFinishRate;

	/** 中班(12点-24点)生产顺序 */
	private Long dayProduceOrder;

	/** 中班(12点-24点)手动输入原因分析 */
	private String dayHandAnalysis;

	/** 中班系统原因分析 */
	private String daySysAnalysis;

	/** 夜班(0点-12点)计划量 */
	private Double nightPlanQty;

	/** 夜班(0点-12点)完成量 */
    private Double nightFinishQty;

	/** 夜班(0点-12点)完成率 */
    private Double nightFinishRate;

	/** 夜班(0点-12点)生产顺序 */
	private Long nightProduceOrder;

	/** 夜班(0点-12点)手动输入原因分析 */
	private String nightHandAnalysis;

	/** 夜班系统原因分析 */
	private String nightSysAnalysis;

	/** 对应成型一班的计划量 */
	private Double cxClass1Plan;

	/** 对应成型二班的计划量 */
	private Double cxClass2Plan;

	/** 对应成型三班的计划量 */
	private Double cxClass3Plan;

	/** 对应成型次一班的计划量 */
	private Double cxClass4Plan;

	/** 对应成型次二班的计划量 */
	private Double cxClass5Plan;

	/** 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE */
	private String isRelease;

	/** 收尾提示标识(0:提示收尾；1:不需要提示) */
	private String markCloseOutTip;

	/** 生产状态:0-未生产；1-生产中；2-生产完成 */
	private String productionStatus;

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
	private String craft;

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

    /**
     * 预计库存，晚班（19点）的剩余库存，仅用于计算可供时长
     */
    private Double planStockQty;

    /**
     * 上一天早班计划
     */
    private Double lastMidPlanQty;

    /**
     * 夜班与早班的交接班库存
     */
    private double classStock;

    /**
     * 库存供需比例，交接班库存/成型一天需求量
     */
    private double supplyDemandRatio;

    /**
	 * 次日夜班计划
     */
    private double nextDayPlanQty;

	/**
     * 剩余量
     */
    private double surplusQty;

	/**
     * 层数
     */
    private Integer layers;

	/**
     * 供应成型规格数
     */
    private Integer specCount;

	/**
     * 是否夜班生产规格
     */
    private Boolean isNightSpec;

	/**
     * 是否大需求量规格
     */
    private Boolean isLargeDemandSpec;

    /**
     * 隔天程序需求量，虚拟字段，用于排序
     * @return
     */
    public Double getNextDayCxPlanQty() {
        return BigDecimalUtil.add(this.cxClass3Plan, this.cxClass4Plan);
    }

	@Override
	public String toString() {
		return "Cd90ScheduleResultVo [scheduleDate=" + scheduleDate + ", cxBatchNo=" + cxBatchNo + ", batchNo="
				+ batchNo + ", orderNo=" + orderNo + ", bigRollCode=" + bigRollCode + ", clothCode=" + clothCode
				+ ", unitConsume=" + unitConsume + ", machineId=" + machineId + ", stockQty=" + stockQty
				+ ", supplyTime=" + supplyTime + ", dayPlanQty=" + dayPlanQty + ", dayProduceOrder=" + dayProduceOrder
				+ ", nightPlanQty=" + nightPlanQty + ", nightProduceOrder=" + nightProduceOrder + ", cxClass1Plan="
				+ cxClass1Plan + ", cxClass2Plan=" + cxClass2Plan + ", cxClass3Plan=" + cxClass3Plan + ", cxClass4Plan="
				+ cxClass4Plan + ", cxClass5Plan=" + cxClass5Plan + ", isRelease=" + isRelease + ", markCloseOutTip="
				+ markCloseOutTip + ", productionStatus=" + productionStatus + "]";
	}
}
