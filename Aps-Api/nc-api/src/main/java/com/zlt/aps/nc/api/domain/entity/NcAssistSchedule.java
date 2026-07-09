package com.zlt.aps.nc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 内衬胶外协排程结果对象 t_nc_assist_schedule
 *
 * @author zlt
 * @date 2026-02-15
 */
@ApiModel(value = "内衬胶外协排程结果对象", description = "内衬胶外协排程结果对象 ")
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_ASSIST_SCHEDULE")
public class NcAssistSchedule extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate", dateFormat = "yyyy-MM-dd")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "排程日期")
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    /**
     * 对应的成型批次号
     */
    @ApiModelProperty(value = "对应的成型批次号")
    @TableField("CX_BATCH_NO")
    private String cxBatchNo;

    /**
     * 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号
     */
    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    @TableField("BATCH_NO")
    private String batchNo;

    /**
     * 工单号，自动生成（批次号+4位定长自增序号）
     */
    @ApiModelProperty(value = "工单号，自动生成")
    @TableField("ORDER_NO")
    private String orderNo;

    /**
     * 内衬代码
     */
    @ImportValidated(required = true, maxLength = 20, isCode = true)
    @Excel(name = "ui.data.column.quota.liningCode")
    @ApiModelProperty(value = "内衬代码")
    @TableField("LINING_CODE")
    private String liningCode;

    /**
     * 胶料代码
     */
    //@Excel(name = "ui.data.column.scheduleResult.glueCode")
    @ApiModelProperty(value = "胶料代码")
    @TableField("GLUE_CODE")
    private String glueCode;

    /**
     * 胶料序号
     */
    //@Excel(name = "ui.data.column.scheduleResult.glueSeq}")
    @ApiModelProperty(value = "胶料序号")
    @TableField("GLUE_SEQ")
    private String glueSeq;

    /**
     * 口型板代码
     */
    @ApiModelProperty(value = "口型板代码")
    @TableField("MOUTH_PLATE_CODE")
    private String mouthPlateCode;

    /**
     * 单耗
     */
    @ApiModelProperty(value = "单耗")
    @TableField("UNIT_CONSUME")
    private Double unitConsume;


    /**
     * 生产线(机台名称)
     */
    @ImportValidated(required = true, maxLength = 20)
    @Excel(name = "ui.data.column.scheduleResult.produceLine")
    @ApiModelProperty(value = "生产线")
    @TableField("MACHINE_ID")
    private String machineId;

    /**
     * 库存数量
     */
    //@Excel(name = "ui.data.column.scheduleResult.stockQty")
    @ApiModelProperty(value = "库存数量")
    @TableField("STOCK_QTY")
    private Double stockQty;

    /**
     * 库存供应成型时长，单位：小时
     */
    //@Excel(name = "ui.data.column.scheduleResult.supplyTime")
    @ApiModelProperty(value = "库存供应成型时长，单位：小时")
    @TableField("SUPPLY_TIME")
    private Double supplyTime;

    /**
     * 当日日计划量合计
     */
    @Excel(name = "ui.data.column.scheduleResult.dailyTotalQty")
    @ApiModelProperty(value = "当日日计划量合计")
    @TableField("DAILY_TOTAL_QTY")
    private Double dailyTotalQty;

    /**
     * 中班(12点-24点)计划量
     */
    @ImportValidated(number = true, max = 9999999, min = 0, digits = true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.meter")
    @ApiModelProperty(value = "中班(12点-24点)计划量")
    @TableField("DAY_PLAN_QTY")
    private Double dayPlanQty;

    /**
     * 中班(12点-24点)完成量
     */
    ////@Excel(name = "中班(12点-24点)完成量")
    @ApiModelProperty(value = "中班(12点-24点)完成量")
    @TableField("DAY_FINISH_QTY")
    private Double dayFinishQty;

    /**
     * 中班(12点-24点)生产顺序
     */
    @ImportValidated(number = true, min = 0, max = 999999, isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayProduceOrder")
    @ApiModelProperty(value = "中班(12点-24点)生产顺序")
    @TableField("DAY_PRODUCE_ORDER")
    private Long dayProduceOrder;

    /**
     * 中班(12点-24点)完成率
     */
    ////@Excel(name = "中班(12点-24点)完成率")
    @ApiModelProperty(value = "中班(12点-24点)完成率")
    @TableField("DAY_FINISH_RATE")
    private Double dayFinishRate;

    /**
     * 中班(12点-24点)系统原因分析
     */
    ////@Excel(name = "中班(12点-24点)系统原因分析")
    @ApiModelProperty(value = "中班(12点-24点)系统原因分析")
    @TableField("DAY_SYS_ANALYSIS")
    private String daySysAnalysis;

    /**
     * 中班(12点-24点)手动输入原因分析
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.dayAnalysis")
    @ApiModelProperty(value = "中班(12点-24点)手动输入原因分析")
    @TableField("DAY_HAND_ANALYSIS")
    private String dayHandAnalysis;

    /**
     * 夜班(0点-12点)计划量
     */
    @ImportValidated(number = true, max = 9999999, min = 0, digits = true)
    @Excel(name = "ui.data.column.scheduleResult.nightPlanQty.meter")
    @ApiModelProperty(value = "夜班(0点-12点)计划量")
    @TableField("NIGHT_PLAN_QTY")
    private Double nightPlanQty;

    /**
     * 夜班(0点-12点)完成量
     */
    ////@Excel(name = "夜班(0点-12点)完成量")
    @ApiModelProperty(value = "夜班(0点-12点)完成量")
    @TableField("NIGHT_FINISH_QTY")
    private Double nightFinishQty;

    /**
     * 夜班(0点-12点)生产顺序
     */
    @ImportValidated(number = true, min = 0, max = 999999, isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.nightProduceOrder")
    @ApiModelProperty(value = "夜班(0点-12点)生产顺序")
    @TableField("NIGHT_PRODUCE_ORDER")
    private Long nightProduceOrder;

    /**
     * 夜班(0点-12点)完成率
     */
    ////@Excel(name = "夜班(0点-12点)完成率")
    @ApiModelProperty(value = "夜班(0点-12点)完成率")
    @TableField("NIGHT_FINISH_RATE")
    private Double nightFinishRate;

    /**
     * 夜班(0点-12点)系统原因分析
     */
    ////@Excel(name = "夜班(0点-12点)系统原因分析")
    @ApiModelProperty(value = "夜班(0点-12点)系统原因分析")
    @TableField("NIGHT_SYS_ANALYSIS")
    private String nightSysAnalysis;

    /**
     * 夜班(0点-12点)手动输入原因分析
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.nightAnalysis")
    @ApiModelProperty(value = "夜班(0点-12点)手动输入原因分析")
    @TableField("NIGHT_HAND_ANALYSIS")
    private String nightHandAnalysis;

    @ApiModelProperty(value = "预计划", position = 44)
    @TableField("PRE_PLAN_QTY")
    private Double prePlanQty;

    /**
     * 对应成型一班的计划量
     */
    ////@Excel(name = "对应成型一班的计划量")
    @ApiModelProperty(value = "对应成型一班的计划量")
    @TableField("CX_CLASS1_PLAN")
    private Double cxClass1Plan;

    /**
     * 对应成型二班的计划量
     */
    ////@Excel(name = "对应成型二班的计划量")
    @ApiModelProperty(value = "对应成型二班的计划量")
    @TableField("CX_CLASS2_PLAN")
    private Double cxClass2Plan;

    /**
     * 对应成型三班的计划量
     */
    ////@Excel(name = "对应成型三班的计划量")
    @ApiModelProperty(value = "对应成型三班的计划量")
    @TableField("CX_CLASS3_PLAN")
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的计划量
     */
    ////@Excel(name = "对应成型次一班的计划量")
    @ApiModelProperty(value = "对应成型次一班的计划量")
    @TableField("CX_CLASS4_PLAN")
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的计划量
     */
    ////@Excel(name = "对应成型次二班的计划量")
    @ApiModelProperty(value = "对应成型次二班的计划量")
    @TableField("CX_CLASS5_PLAN")
    private Double cxClass5Plan;

    /**
     * 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE
     */
    ////@Excel(name = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    @TableField("IS_RELEASE")
    private String isRelease;

    @ApiModelProperty(value = "月计划需求量")
    @TableField("MONTH_PLAN")
    private Double monthPlan;

    @ApiModelProperty(value = "月计划剩余量")
    @TableField("MONTH_PLAN_OS")
    private Double monthPlanOs;

    @TableField("YEAR")
    private String year;

    @TableField("MONTH")
    private String month;

    @ApiModelProperty(value = "生产状态")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.data.column.stock.remark", maxLength = 300)
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    @TableField("MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    @TableField("DATA_SOURCE")
    private String dataSource;
}
