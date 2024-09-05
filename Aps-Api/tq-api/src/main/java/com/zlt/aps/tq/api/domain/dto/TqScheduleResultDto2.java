package com.zlt.aps.tq.api.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 胎圈排程结果对象 t_tq_schedule_result
 * 
 * @author chen
 * @date 2021-06-24
 */
@Data
@ApiModel(value = "胎圈排程结果对象", description = "胎圈排程结果对象 ")
public class TqScheduleResultDto2 extends ApsBaseDto
{
    private static final Long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_TQ_SCHEDULE */
    private Long id;

    /** 排程日期 */
    @ImportValidated(required = true, date = true)
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /** 对应的成型批次号 */
    @ApiModelProperty(value = "对应的成型批次号")
    private String cxBatchNo;

    /** 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号 */
    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String batchNo;

    /** 工单号，自动生成（批次号+4位定长自增序号） */
    @ApiModelProperty(value = "工单号，自动生成")
    private String orderNo;

    /** 胎圈代码 */
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.tq.scheduleResult.beadCode")
    @ApiModelProperty(value = "胎圈代码")
    private String beadCode;

    /** 钢丝圈代码 */
//    @Excel(name = "ui.data.column.gsq.scheduleResult.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    /** 三角胶代码 */
//    @Excel(name = "ui.data.column.tq.scheduleResult.triangleGlueCode")
    @ApiModelProperty(value = "三角胶代码")
    private String triangleGlueCode;

    /** 胶料代码 */
//    @Excel(name = "ui.data.column.scheduleResult.glueCode")
    @ApiModelProperty(value = "胶料代码")
    private String glueCode;

    /** 口型板代码 */
//    @Excel(name = "ui.data.column.scheduleResult.mouthPlateCode")
    @ApiModelProperty(value = "口型板代码")
    private String mouthPlateCode;

    /** 尺寸 */
//    @Excel(name = "ui.data.column.scheduleResult.specSize")
    @ApiModelProperty(value = "尺寸")
    private String specSize;

    /**
     * 生产线(机台名称)
     */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.scheduleResult.produceLine")
    @ApiModelProperty(value = "生产线")
    private String machineId;

    /** 单耗 */
//    @Excel(name = "ui.data.column.scheduleResult.unitConsume")
    @ApiModelProperty(value = "单耗")
    private Double unitConsume;

    /** 库存数量 */
//    @Excel(name = "ui.data.column.scheduleResult.stockQty")
    @ApiModelProperty(value = "库存数量")
    private Double stockQty;

    /** 库存供应成型时长，单位：小时 */
//    @Excel(name = "ui.data.column.scheduleResult.supplyTime")
    @ApiModelProperty(value = "库存供应成型时长，单位：小时")
    private Double supplyTime;

    /**
     * 当日日计划量合计
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.dailyTotalQty")
    @ApiModelProperty(value = "当日日计划量合计")
    private Double dailyTotalQty;

    /** 中班(16点-24点)计划量(条) */
    @ImportValidated( min = 0, max = 99999999,digits = true)
    @Excel(name = "ui.data.column.scheduleResult.PlanQtyNum2")
    @ApiModelProperty(value = "中班(16点-24点)计划量(个)")
    private Double midPlanQty;

    /** 中班(16点-24点)完成量 */
//    @Excel(name = "ui.data.column.scheduleResult.dayFinishQty")
    @ApiModelProperty(value = "中班(16点-24点)完成量")
    private Double midFinishQty;

    /** 中班(16点-24点)生产顺序 */
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.productSort1")
    @ApiModelProperty(value = "中班(16点-24点)生产顺序")
    private Integer midProduceOrder;

    /** 中班(16点-24点)完成率 */
//    @Excel(name = "ui.data.column.scheduleResult.dayFinishRate")
    @ApiModelProperty(value = "中班(16点-24点)完成率")
    private Double midFinishRate;

    /** 中班(16点-24点)系统原因分析 */
//    @Excel(name = "ui.data.column.scheduleResult.daySysAnalysis")
    @ApiModelProperty(value = "中班(16点-24点)系统原因分析")
    private String midSysAnalysis;

    /** 中班(16点-24点)手动输入原因分析 */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.analysis2")
    @ApiModelProperty(value = "中班(16点-24点)手动输入原因分析")
    private String midHandAnalysis;

    /** 夜班(0点-8点)计划量(条) */
    @ImportValidated( min = 0, max = 99999999,digits = true)
    @Excel(name = "ui.data.column.scheduleResult.PlanQtyNum3")
    @ApiModelProperty(value = "夜班(0点-8点)计划量(条)")
    private Double nightPlanQty;

    /** 夜班(0点-8点)完成量 */
//    @Excel(name = "ui.data.column.scheduleResult.nightFinishQty")
    @ApiModelProperty(value = "夜班(0点-8点)完成量")
    private Double nightFinishQty;

    /** 夜班(0点-8点)生产顺序 */
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.productSort2")
    @ApiModelProperty(value = "夜班(0点-8点)生产顺序")
    private Integer nightProduceOrder;

    /** 夜班(0点-8点)完成率 */
//    @Excel(name = "ui.data.column.scheduleResult.nightFinishRate")
    @ApiModelProperty(value = "夜班(0点-8点)完成率")
    private Double nightFinishRate;

    /** 夜班(0点-8点)系统原因分析 */
//    @Excel(name = "ui.data.column.scheduleResult.nightSysAnalysis")
    @ApiModelProperty(value = "夜班(0点-8点)系统原因分析")
    private String nightSysAnalysis;

    /** 夜班(0点-8点)手动输入原因分析 */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.analysis3")
    @ApiModelProperty(value = "夜班(0点-8点)手动输入原因分析")
    private String nightHandAnalysis;

    /** 白班(8点-16点)计划量(条) */
    @ImportValidated( min = 0, max = 99999999,digits = true)
    @Excel(name = "ui.data.column.scheduleResult.PlanQtyNum5")
    @ApiModelProperty(value = "白班(8点-16点)计划量(条)")
    private Double dayPlanQty;

    /** 白班(8点-16点)完成量 */
//    @Excel(name = "ui.data.column.scheduleResult.midFinishQty")
    @ApiModelProperty(value = "白班(8点-16点)完成量")
    private Double dayFinishQty;

    /** 白班(8点-16点)生产顺序 */
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.productSort3")
    @ApiModelProperty(value = "白班(8点-16点)生产顺序")
    private Integer dayProduceOrder;

    /** 白班(8点-16点)完成率 */
//    @Excel(name = "ui.data.column.scheduleResult.midFinishRate")
    @ApiModelProperty(value = "白班(8点-16点)完成率")
    private Double dayFinishRate;

    /** 白班(8点-16点)系统原因分析 */
//    @Excel(name = "ui.data.column.scheduleResult.midSysAnalysis")
    @ApiModelProperty(value = "白班(8点-16点)系统原因分析")
    private String daySysAnalysis;

    /** 白班(8点-16点)手动输入原因分析 */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.analysis6")
    @ApiModelProperty(value = "白班(8点-16点)手动输入原因分析")
    private String dayHandAnalysis;

    /** 次日中班(16点-24点)计划量(条) */
    @ImportValidated( min = 0, max = 99999999,digits = true)
    @Excel(name = "ui.data.column.scheduleResult.PlanQtyNum4")
    @ApiModelProperty(value = "次日中班(16点-24点)计划量(条)")
    private Double nextMidPlanQty;

    /** 次日中班(16点-24点)完成量 */
//    @Excel(name = "ui.data.column.scheduleResult.nextMidFinishQty")
    @ApiModelProperty(value = "次日中班(16点-24点)完成量")
    private Double nextMidFinishQty;

    /** 次日中班(16点-24点)生产顺序 */
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.productSort4")
    @ApiModelProperty(value = "次日中班(16点-24点)生产顺序")
    private Integer nextMidProduceOrder;

    /** 次日中班(16点-24点)完成率 */
//    @Excel(name = "ui.data.column.scheduleResult.nextMidFinishRate")
    @ApiModelProperty(value = "次日中班(16点-24点)完成率")
    private Double nextMidFinishRate;

    /** 次日中班(16点-24点)系统原因分析 */
//    @Excel(name = "ui.data.column.scheduleResult.nextMidSysAnalysis")
    @ApiModelProperty(value = "次日中班(16点-24点)系统原因分析")
    private String nextMidSysAnalysis;

    /** 次日中班(16点-24点)手动输入原因分析 */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.analysis4")
    @ApiModelProperty(value = "次日中班(16点-24点)手动输入原因分析")
    private String nextMidHandAnalysis;

    /** 对应成型一班的计划量 */
//    @Excel(name = "ui.data.column.scheduleResult.cxClass1Plan")
    @ApiModelProperty(value = "对应成型一班的计划量")
    private Double cxClass1Plan;

    /** 对应成型二班的计划量 */
//    @Excel(name = "ui.data.column.scheduleResult.cxClass2Plan")
    @ApiModelProperty(value = "对应成型二班的计划量")
    private Double cxClass2Plan;

    /** 对应成型三班的计划量 */
//    @Excel(name = "ui.data.column.scheduleResult.cxClass3Plan")
    @ApiModelProperty(value = "对应成型三班的计划量")
    private Double cxClass3Plan;

    /** 对应成型次一班的计划量 */
//    @Excel(name = "ui.data.column.scheduleResult.cxClass4Plan")
    @ApiModelProperty(value = "对应成型次一班的计划量")
    private Double cxClass4Plan;

    /** 对应成型次二班的计划量 */
//    @Excel(name = "ui.data.column.scheduleResult.cxClass5Plan")
    @ApiModelProperty(value = "对应成型次二班的计划量")
    private Double cxClass5Plan;

    /** 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE */
//    @Excel(name = "ui.data.column.scheduleResult.isRelease", dictType = "IS_RELEASE")
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。")
    private String isRelease;

    /** 收尾提示标识(0:提示收尾；1:不需要提示) */
    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    private String markCloseOutTip;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

    /** 月计划需求量 */
    @ApiModelProperty(value = "月计划需求量", position = 95)
    private String monthPlan;

    /** 月计划需求量 */
    @ApiModelProperty(value = "月计划剩余量", position = 100)
    private String monthPlanOs;

    @ApiModelProperty(value = "关联汇总表中年份", position = 600)
    private String year;

    @ApiModelProperty(value = "关联汇总表中月份", position = 600)
    private String month;

//    @Excel(name = "ui.data.column.scheduleResult.productionStatus", dictType = "PRODUCTION_STATUS")
    @ApiModelProperty(value = "生产状态")
    private String productionStatus;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

}
