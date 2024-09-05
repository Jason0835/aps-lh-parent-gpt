package com.zlt.aps.xwyy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 纤维压延排程结果表
 * </p>
 *
 * @author chen
 * @since 2021-07-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_XWYY_SCHEDULE_RESULT")
@ApiModel(value = "XwyyScheduleResult对象", description = "纤维压延排程结果表")
@KeySequence(value = "SEQ_XWYY_SCHEDULE", clazz = Long.class)
public class XwyyScheduleResult extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID",type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "排程日期")
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    @ApiModelProperty(value = "对应的90度裁断批次号")
    @TableField("CD90_BATCH_NO")
    private String cd90BatchNo;

    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    @TableField("BATCH_NO")
    private String batchNo;

    @ApiModelProperty(value = "工单号，自动生成（批次号+4位定长自增序号）")
    @TableField("ORDER_NO")
    private String orderNo;

    @ApiModelProperty(value = "帘布大卷编号")
    @TableField("BIG_ROLL_CODE")
    private String bigRollCode;

    @ApiModelProperty(value = "原线代码")
    @TableField("ORIGINAL_LINE_CODE")
    private String originalLineCode;

    @ApiModelProperty(value = "机台ID，多个逗号分割")
    @TableField("MACHINE_ID")
    private String machineId;

    @ApiModelProperty(value = "库存供应成型时长，单位：小时")
    @TableField("SUPPLY_TIME")
    private Double supplyTime;

    @ApiModelProperty(value = "中班(12点-24点)计划量")
    @TableField("DAY_PLAN_QTY")
    private Double dayPlanQty;

    @ApiModelProperty(value = "中班计划量个数")
    @TableField("DAY_PLAN_QTY_NUM")
    private Double dayPlanQtyNum;

    @ApiModelProperty(value = "中班(12点-24点)完成量")
    @TableField("DAY_FINISH_QTY")
    private Double dayFinishQty;

    @ApiModelProperty(value = "中班(12点-24点)系统原因分析")
    @TableField("DAY_SYS_ANALYSIS")
    private String daySysAnalysis;

    @ApiModelProperty(value = "中班(12点-24点)手动输入原因分析")
    @TableField("DAY_HAND_ANALYSIS")
    private String dayHandAnalysis;

    @ApiModelProperty(value = "中班排程过程值")
    @TableField("DAY_PROCESS_VALUE")
	private String dayProcessValue;

    @ApiModelProperty(value = "夜班(0点-12点)计划量")
    @TableField("NIGHT_PLAN_QTY")
    private Double nightPlanQty;

    @ApiModelProperty(value = "夜班计划量个数")
    @TableField("NIGHT_PLAN_QTY_NUM")
    private Double nightPlanQtyNum;

    @ApiModelProperty(value = "夜班(0点-12点)完成量")
    @TableField("NIGHT_FINISH_QTY")
    private Double nightFinishQty;

    @ApiModelProperty(value = "夜班(0点-12点)系统原因分析")
    @TableField("NIGHT_SYS_ANALYSIS")
    private String nightSysAnalysis;

    @ApiModelProperty(value = "夜班(0点-12点)手动输入原因分析")
    @TableField("NIGHT_HAND_ANALYSIS")
    private String nightHandAnalysis;

    @ApiModelProperty(value = "夜班排程过程值")
    @TableField("NIGHT_PROCESS_VALUE")
	private String nightProcessValue;

    @ApiModelProperty(value = "前日库存")
    @TableField("YES_STOCK")
    private Double yesStock;

    @ApiModelProperty(value = "当日库存")
    @TableField("TODAY_STOCK")
    private Double todayStock;

    @ApiModelProperty(value = "日用参考（个）")
    @TableField("DAY_USED")
    private Double dayUsed;

    @ApiModelProperty(value = "白班外厂应支")
    @TableField("DAY_OUT")
    private Double dayOut;

    @ApiModelProperty(value = "2厂早班计划量")
    @TableField("FAC2_CLASS1_PLAN")
    private Double fac2Class1Plan;

    @ApiModelProperty(value = "2厂中班计划量")
    @TableField("FAC2_CLASS2_PLAN")
    private Double fac2Class2Plan;

    @ApiModelProperty(value = "2厂晚班计划量")
    @TableField("FAC2_CLASS3_PLAN")
    private Double fac2Class3Plan;

    @ApiModelProperty(value = "2厂合计计划量")
    @TableField("FAC2_TOTAL_PLAN")
    private Double fac2TotalPlan;

    @ApiModelProperty(value = "5厂早班计划量")
    @TableField("FAC5_CLASS1_PLAN")
    private Double fac5Class1Plan;

    @ApiModelProperty(value = "5厂中班计划量")
    @TableField("FAC5_CLASS2_PLAN")
    private Double fac5Class2Plan;

    @ApiModelProperty(value = "5厂晚班计划量")
    @TableField("FAC5_CLASS3_PLAN")
    private Double fac5Class3Plan;

    @ApiModelProperty(value = "5厂合计计划量")
    @TableField("FAC5_TOTAL_PLAN")
    private Double fac5TotalPlan;

    @ApiModelProperty(value = "总合计计划量")
    @TableField("TOTAL_PLAN")
    private Double totalPlan;

    @ApiModelProperty(value = "对应成型一班的计划量")
    @TableField("CX_CLASS1_PLAN")
    private Double cxClass1Plan;

    @ApiModelProperty(value = "对应成型二班的计划量")
    @TableField("CX_CLASS2_PLAN")
    private Double cxClass2Plan;

    @ApiModelProperty(value = "对应成型三班的计划量")
    @TableField("CX_CLASS3_PLAN")
    private Double cxClass3Plan;

    @ApiModelProperty(value = "对应成型次一班的计划量")
    @TableField("CX_CLASS4_PLAN")
    private Double cxClass4Plan;

    @ApiModelProperty(value = "对应成型次二班的计划量")
    @TableField("CX_CLASS5_PLAN")
    private Double cxClass5Plan;

    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    @TableField("IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    @TableField("MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

    @ApiModelProperty(value = "收尾规格标记（对应成型排程），0：收尾1：非收尾")
    @TableField("CLOSE_OUT_SPEC_FLAG")
    private String closeOutSpecFlag;

    @ApiModelProperty(value = "生产状态:0-未生产；1-生产中；2-生产完成")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    @ApiModelProperty(value = "关联汇总表年份", position = 600)
    @TableField(exist = false)
    private String year;

    @ApiModelProperty(value = "关联汇总表月份", position = 600)
    @TableField(exist = false)
    private String month;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

    @ApiModelProperty(value = "排程记录id数组")
    @TableField(exist = false)
    private Long[] ids;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    private Date newestPublishTime;

    /**
     * 原线提醒，0：不提醒，1：提醒
     */
    @ApiModelProperty(value = "原线提醒，0：不提醒，1：提醒")
    private Integer originalRemindFlag;

    /**
     * 原线规格名称
     */
    @ApiModelProperty(value = "原线规格名称")
    @TableField(exist = false)
    private String originalLineName;

    @ApiModelProperty(value = "标准米长")
    @TableField(exist = false)
    private BigDecimal actClothLength;

    /**
     * 原线卷数
     */
    @ApiModelProperty(value = "原线卷数")
    @TableField("ORIGINAL_LINE_QTY_NUM")
    private String originalLineQtyNum;

    /**
     * 原线长度
     */
    @ApiModelProperty(value = "原线长度")
    @TableField(exist = false)
    private String originalLineLength;

    /**
     * 多规格共用原线情况，需要更新的帘布大卷代号
     */
    @TableField(exist = false)
    private String maxBigRollCode;

    /**
     * 多规格共用原线且原线品牌相同的情况，需要更新的帘布大卷代号
     */
    @TableField(exist = false)
    private String maxBigRollCodeBrand;

    /**
     * 胶料号
     */
    @ApiModelProperty(value = "胶料号")
    @TableField("RUBBER_CODE")
    private String rubberCode;

    /**
     * 胶料车数
     */
    @ApiModelProperty(value = "胶料车数")
    @TableField("RUBBER_CAR_NUMBER")
    private BigDecimal rubberCarNumber;


    /**
     * 原线品牌
     */
    @ApiModelProperty(value = "原线品牌")
    @TableField("ORIGINAL_BRAND")
    private String originalBrand;

    /**
     * 原线品牌个数
     */
    @ApiModelProperty(value = "原线品牌个数")
    @TableField("ORIGINAL_BRAND_NUM")
    private BigDecimal originalBrandNum;
}
