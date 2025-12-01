package com.zlt.aps.xwyy.engine.vo;

import com.zlt.aps.common.core.domain.ApsBaseDto;
import com.zlt.aps.common.core.utils.BigDecimalUtil;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 纤维压延排程结果值对象
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 10:56:49
 * @Version 1.0
 */
@Data
public class XwyyScheduleResultVo extends ApsBaseDto {

	private static final long serialVersionUID = 1L;
	
	/** 主键 **/
	private Long id;
	
	/** 成型批次号 **/
	private String cxBatchNo;

	/** 排程日期 **/
	private Date scheduleDate;

	/** 对应的90度裁断批次号 */
	private String cd90BatchNo;
	
	/** 批次号 **/
	private String batchNo;
	
	/** 工单号 **/
	private String orderNo;

    /** 帘布大卷编号 */
	private String bigRollCode;
	
    /** 原线代码 */
    private String originalLineCode;
	
    /** 原线卷数 */
    private BigDecimal originalLineQtyNum;

    /** 机台ID，多个逗号分割 */
    private String machineId;

    /** 可供成型时长 */
    private Double supplyTime;
	
	/** 日用参考 */
	private Double dayUsed;

    /** 当日预计库存*/
    private Double todayStock;

    /** 当日库存*/
    private BigDecimal todayStockQty;

    /** 前日库存*/
    private Double yesStock;

    /** 中班（一班16点-24点）计划量*/
    private Double dayPlanQty;
    
    /** 中班计划量个数 */
    private Double dayPlanQtyNum;

    /** 中班（一班16点-24点）完成量*/
    private Double dayFinishQty;

	/** 中班手动输入原因分析 */
	private String daySysAnalysis;

	/** 中班原因分析 */
	private String dayHandAnalysis;

	/** 中班排程过程值 */
	private String dayProcessValue;

    /** 夜班（二班0点-8点）计划量*/
    private Double nightPlanQty;
    
    /** 夜班计划量个数 */
    private Double nightPlanQtyNum;

    /** 夜班（二班0点-8点）完成量*/
    private Double nightFinishQty;

	/** 夜班系统原因分析 */
	private String nightSysAnalysis;

	/** 夜班手工输入原因分析 */
    private String nightHandAnalysis;

	/** 夜班排程过程值 */
	private String nightProcessValue;
	/**
	 * 额外计划量标识：0无，1有额外计划量
	 */
	private String extraPlanFlag;

    /** 总合计计划量 */
    private Double totalPlan;
    
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

    /** 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE*/
    private String isRelease;
    
	/** 发布成功计数器，每点击一次发布并成功的话，计数器累加 */
	private Integer publishSuccessCount;

	/** 最新发布成功时间 */
	private Date newestPublishTime;

    /** 收尾提示标识(0:提示收尾；1:不需要提示)*/
    private String markCloseOutTip;

    /** 生产状态:0-未生产；1-生产中；2-生产完成*/
    private String productionStatus;

    /** 数据来源：0>自动排程；1>APS插单；2>导入；**/
    private String dataSource;
     
    /** 是否处理原线破大卷*/
    private boolean breakRollFlag;
    
    /** 原线提醒，0：不提醒，1：提醒*/
    private String originalRemindFlag;
    
    /** 胶料编号 **/
    private String rubberCode;
    
    /** 胶料车数 **/
    private BigDecimal rubberCarNumber;

    /**
     * 原线品牌
     */
    private String originalBrand;

    /**
     * 原线品牌个数
     */
    private BigDecimal originalBrandNum;

    /** 总合计计划卷数 */
    private BigDecimal totalPlanNum;
    
    /**
     * 收尾规格标记，0：收尾1：非收尾
     */
    private String closeOutSpecFlag;
    
    /**
     * 大卷标准长度
     */
    private BigDecimal rollStandardSize;

    @ApiModelProperty(value = "早班生产顺序")
    private Integer dayProduceOrder;

    @ApiModelProperty(value = "夜班生产顺序")
    private Integer nightProduceOrder;
    
    /**
     * 库存用量比例
     */
    private BigDecimal stockPlanRate;
    
    /**
     * 上一天计划量
     */
    private Double lastPlanQty;
}
