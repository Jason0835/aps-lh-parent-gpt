package com.zlt.aps.cd15.api.domain.entity;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 15度裁断排程结果对象 t_cd15_assist_schedule
 * 
 * @author zlt
 * @date 2022-02-11
 */
@ApiModel(value = "15度裁断外协排程结果对象", description = "15度裁断外协排程结果对象 ")
@Data
public class Cd15ScheduleAssist extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_CD15_SCHEDULE */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 排程日期 */
    @ImportValidated(required = true, date = true)
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /** 对应的成型批次号 */
    @ApiModelProperty(value = "对应的成型批次号")
    private String cxBatchNo;

    /** 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号 */
    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String batchNo;

    /** 工单号，自动生成（批次号+4位定长自增序号） */
    @ApiModelProperty(value = "工单号")
    private String orderNo;

    /** 钢压大卷编号 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.bigRollCode")
    @ApiModelProperty(value = "钢压大卷编号")
    private String bigRollCode;

    /**
     * 生产线(机台名称)
     */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.scheduleResult.produceLine")
    @ApiModelProperty(value = "生产线")
    private String machineId;


    /** 1#钢带代码 */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.cd15ScheduleResult.steelStripCode1")
    @ApiModelProperty(value = "1#钢带代码")
    private String steelStripCode1;

    /** 2#钢带代码 */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.cd15ScheduleResult.steelStripCode2")
    @ApiModelProperty(value = "2#钢带代码")
    private String steelStripCode2;

    /** 1#钢带单耗 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.unitConsume1")
    @ApiModelProperty(value = "1#钢带单耗")
    private Double unitConsume1;

    /** 1#钢带库存数量 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.stock1Qty1")
    @ApiModelProperty(value = "1#钢带库存数量")
    private Double stock1Qty1;

    /** 2#钢带库存数量 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.stock1Qty2")
    @ApiModelProperty(value = "2#钢带库存数量")
    private Double stock1Qty2;

    /** 1#钢带库存供应成型时长，单位：小时 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.supplyTime1")
    @ApiModelProperty(value = "1#钢带库存供应成型时长，单位：小时")
    private Double supplyTime1;

    /**
     * 当日日计划量合计
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.dailyTotalQty")
    @ApiModelProperty(value = "当日日计划量合计")
    private Double dailyTotalQty;

    /** 1#钢带中班(12点-24点)计划量 */
    @ImportValidated(number = true,min = 0,max = 9999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.meter")
    @ApiModelProperty(value = "1#钢带中班(12点-24点)计划量")
    private Double dayPlanQty1;

    /** 1#钢带中班(12点-24点)完成量 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.dayFinishQty1")
    @ApiModelProperty(value = "1#钢带中班(12点-24点)完成量")
    private Double dayFinishQty1;

    /** 1#钢带中班(12点-24点)生产顺序 */
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayProduceOrder")
    @ApiModelProperty(value = "1#钢带中班(12点-24点)生产顺序")
    private Long dayProduceOrder1;

    /** 1#钢带中班(12点-24点)完成率 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.dayFinishRate1")
    @ApiModelProperty(value = "1#钢带中班(12点-24点)完成率")
    private Double dayFinishRate1;

    /** 1#钢带中班(12点-24点)系统原因分析 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.daySysAnalysis1")
    @ApiModelProperty(value = "1#钢带中班(12点-24点)系统原因分析")
    private String daySysAnalysis1;

    /** 1#钢带中班(12点-24点)手动输入原因分析 */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.dayAnalysis")
    @ApiModelProperty(value = "1#钢带中班(12点-24点)手动输入原因分析")
    private String dayHandAnalysis1;

    /** 1#钢带夜班(0点-12点)计划量 */
    @ImportValidated(number = true,min = 0,max = 9999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.nightPlanQty.meter")
    @ApiModelProperty(value = "1#钢带夜班(0点-12点)计划量")
    private Double nightPlanQty1;

    /** 1#钢带夜班(0点-12点)完成量 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.nightFinishQty1")
    @ApiModelProperty(value = "1#钢带夜班(0点-12点)完成量")
    private Double nightFinishQty1;

    /** 1#钢带夜班(0点-12点)生产顺序 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.nightProduceOrder1")
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.nightProduceOrder")
    @ApiModelProperty(value = "1#钢带夜班(0点-12点)生产顺序")
    private Long nightProduceOrder1;

    /** 1#钢带夜班(0点-12点)完成率 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.nightFinishRate1")
    @ApiModelProperty(value = "1#钢带夜班(0点-12点)完成率")
    private Double nightFinishRate1;

    /** 1#钢带夜班(0点-12点)系统原因分析 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.nightSysAnalysis1")
    @ApiModelProperty(value = "1#钢带夜班(0点-12点)系统原因分析")
    private String nightSysAnalysis1;

    /** 1#钢带夜班(0点-12点)手动输入原因分析 */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.nightAnalysis")
    @ApiModelProperty(value = "1#钢带夜班(0点-12点)手动输入原因分析")
    private String nightHandAnalysis1;

    /** 1#钢带对应成型一班的计划量 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.cxClass1Plan")
    @ApiModelProperty(value = "1#钢带对应成型一班的计划量")
    private Double cxClass1Plan;

    /** 1#钢带对应成型二班的计划量 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.cxClass2Plan")
    @ApiModelProperty(value = "1#钢带对应成型二班的计划量")
    private Double cxClass2Plan;

    /** 1#钢带对应成型三班的计划量 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.cxClass3Plan")
    @ApiModelProperty(value = "1#钢带对应成型三班的计划量")
    private Double cxClass3Plan;

    /** 1#钢带对应成型次一班的计划量 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.cxClass4Plan")
    @ApiModelProperty(value = "1#钢带对应成型次一班的计划量")
    private Double cxClass4Plan;

    /** 1#钢带对应成型次二班的计划量 */
    //@Excel(name = "ui.data.column.cd15ScheduleResult.cxClass5Plan")
    @ApiModelProperty(value = "1#钢带对应成型次二班的计划量")
    private Double cxClass5Plan;

    /** 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE */
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    private String isRelease;

    /** 收尾提示标识(0:提示收尾；1:不需要提示) */
    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    private String markCloseOutTip;

    /** 生产状态:0-未生产；1-生产中；2-生产完成 */
    @ApiModelProperty(value = "生产状态:0-未生产；1-生产中；2-生产完成")
    private String productionStatus;

    /** 删除标识：0--正常，1-删除 */
    private String delFlag;

    @ApiModelProperty(value = "月计划剩余量")
    private Double monthPlanOs;

    @ApiModelProperty(value = "月计划需求量")
    private Double monthPlan;

    @ApiModelProperty(value = "裁断角度")
    private Double cuttingAngle;

    private String year;

    private String month;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.data.column.stock.remark", maxLength = 300)
    private String remark;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("scheduleDate", getScheduleDate())
            .append("cxBatchNo", getCxBatchNo())
            .append("batchNo", getBatchNo())
            .append("orderNo", getOrderNo())
            .append("bigRollCode", getBigRollCode())
            .append("machineId", getMachineId())
            .append("steelStripCode1", getSteelStripCode1())
            .append("steelStripCode2", getSteelStripCode2())
            .append("unitConsume1", getUnitConsume1())
            .append("stock1Qty1", getStock1Qty1())
            .append("stock1Qty2", getStock1Qty2())
            .append("supplyTime1", getSupplyTime1())
            .append("dayPlanQty1", getDayPlanQty1())
            .append("dayFinishQty1", getDayFinishQty1())
            .append("dayProduceOrder1", getDayProduceOrder1())
            .append("dayFinishRate1", getDayFinishRate1())
            .append("daySysAnalysis1", getDaySysAnalysis1())
            .append("dayHandAnalysis1", getDayHandAnalysis1())
            .append("nightPlanQty1", getNightPlanQty1())
            .append("nightFinishQty1", getNightFinishQty1())
            .append("nightProduceOrder1", getNightProduceOrder1())
            .append("nightFinishRate1", getNightFinishRate1())
            .append("nightSysAnalysis1", getNightSysAnalysis1())
            .append("nightHandAnalysis1", getNightHandAnalysis1())
            .append("cxClass1Plan", getCxClass1Plan())
            .append("cxClass2Plan", getCxClass2Plan())
            .append("cxClass3Plan", getCxClass3Plan())
            .append("cxClass4Plan", getCxClass4Plan())
            .append("cxClass5Plan", getCxClass5Plan())
            .append("isRelease", getIsRelease())
            .append("markCloseOutTip", getMarkCloseOutTip())
            .append("productionStatus", getProductionStatus())
            .append("remark", getRemark())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }

}
