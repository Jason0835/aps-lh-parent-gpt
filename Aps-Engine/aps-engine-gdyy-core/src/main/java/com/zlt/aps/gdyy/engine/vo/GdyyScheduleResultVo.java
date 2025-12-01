package com.zlt.aps.gdyy.engine.vo;

import com.zlt.aps.common.core.domain.ApsBaseDto;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 钢带压延排程结果值对象
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-19 10:56:49
 * @Version 1.0
 */
@Data
public class GdyyScheduleResultVo extends ApsBaseDto {

	private static final long serialVersionUID = 1L;

	/** 排程日期 */
	private Date scheduleDate;

	/** 对应的成型批次号 */
	private String cxBatchNo;

	/** 对应的15度裁断批次号 */
	private String cd15BatchNo;

	/** 批次号 **/
	private String batchNo;

	/** 工单号 **/
	private String orderNo;

	/** 钢压大卷编号 */
	private String bigRollCode;

	/**
	 * 机台ID
	 */
	private String machineCode;

	/** 日用参考（个） */
	private Double dayUsed;

	/** 库存（个） */
	private Double stockQty;

	/** 中班（一班16点-24点）计划量 */
	private Double class1Plan;

    /**
     * 一班排产顺序
     */
    private Long class1ProduceOrder;

	/** 中班计划量个数 */
	private Double class1PlanNum;

	/** 中班（一班16点-24点）无库存计划量 */
	private Double class1PlanNoStock;

	/** 中班备注 */
	private String class1Remark;

	/** 夜班（二班0点-8点）计划量 */
	private Double class2Plan;

    /**
     * 二班排产顺序
     */
    private Long class2ProduceOrder;

	/** 中班计划量个数 */
	private Double class2PlanNum;

	/** 夜班（二班0点-8点）无库存计划量 */
	private Double class2PlanNoStock;

	/** 夜班备注 */
	private String class2Remark;

	/** 白班（三班8点-16点）计划量 */
	private Double class3Plan;
    
    /** 钢丝原线代码 */
    private String SteelLineCode;

    /**
     * 三班排产顺序
     */
    private Long class3ProduceOrder;

	/** 中班计划量个数 */
	private Double class3PlanNum;

	/** 夜班（二班0点-8点）无库存计划量 */
	private Double class3PlanNoStock;

	/** 白班（三班8点-16点）备注 */
	private String class3Remark;
	
	/** 库存用量比例 **/
	private BigDecimal stockPlanRate;

	/** 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE */
	private String isRelease;

	/**
	 * 发布成功计数器，每点击一次发布并成功的话，计数器累加
	 */
	private Integer publishSuccessCount;

	/**
	 * 最新发布成功时间
	 */
	private Date newestPublishTime;

	/** 收尾提示标识(0:提示收尾；1:不需要提示) */
	private String markCloseOutTip;

	/** 生产状态:0-未生产；1-生产中；2-生产完成 */
	private String productionStatus;
	
	/** 收尾规格标记（对应成型排程），0：收尾1：非收尾 **/
    private String closeOutSpecFlag;

	/**
	 * 数据来源
	 */
	private String dataSource;

    /**
     * 对应成型一班的计划量
     */
    private Double cxClass1Plan;

    /**
     * 对应成型二班的计划量
     */
    private Double cxClass2Plan;

    /**
     * 对应成型三班的计划量
     */
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的计划量
     */
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的计划量
     */
    private Double cxClass5Plan;

    /**
     * 对应成型次三班的计划量
     */
    private Double cxClass6Plan;
    
    /**
     * 上一天计划量
     */
    private Double lastPlanQty;

    // 辅助方法：获取总计划量
    public Double getTotalPlanQty() {
        return getTotalPlan(this);
    }

	// 辅助方法：获取总计划量
	public static double getTotalPlan(GdyyScheduleResultVo item) {
		return ObjectUtils.defaultIfNull(item.getClass1Plan(), 0D)
				+ ObjectUtils.defaultIfNull(item.getClass2Plan(), 0D)
				+ ObjectUtils.defaultIfNull(item.getClass3Plan(), 0D);
	}
}
