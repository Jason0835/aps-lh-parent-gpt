package com.zlt.aps.gsq.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Date;
import java.util.List;

/**
 * 钢丝圈排程结果对象 t_gsq_schedule_result
 *
 * @author chen
 * @date 2021-06-18
 */
@Data
@ApiModel(value="GsqScheduleResultDto对象", description="钢丝圈排程结果信息")
public class GsqScheduleResultDto extends ApsBaseDto
{
    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_GSQ_SCHEDULE */
    private Long id;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期", position = 10)
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate")
    @ImportValidated(required = true, date = true)
    private Date scheduleDate;

    /** 对应的成型批次号 */
    @ApiModelProperty(value = "成型批次号", position = 20)
    private String tqBatchNo;

    /** 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号 */
    @ApiModelProperty(value = "批次号", position = 30)
    private String batchNo;

    /** 工单号，自动生成（批次号+4位定长自增序号） */
    @ApiModelProperty(value = "工单号", position = 40)
    private String orderNo;

    /** 钢丝类型 */
//    @Excel(name = "ui.data.column.scheduleResult.steelType", sort = 10)
    @ApiModelProperty(value = "钢丝类型", position = 50)
    private String steelType;

    /** 钢丝圈代码 */
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.gsq.scheduleResult.steelRingCode", sort = 20)
    @ApiModelProperty(value = "钢丝圈代码", position = 60)
    private String steelRingCode;

    @ApiModelProperty(value = "寸口", position = 65)
    private String dimension;

    /** 排列 */
//    @Excel(name = "ui.data.column.gsq.scheduleResult.rank", sort = 30)
    @ApiModelProperty(value = "排列", position = 70)
    private String rank;

    /**
     * 生产线(机台名称)
     */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.scheduleResult.produceLine")
    @ApiModelProperty(value = "生产线", position = 80)
    private String machineId;

    /** 月计划需求量 */
//    @Excel(name = "ui.data.column.scheduleResult.monthPlan", sort = 50)
    @ApiModelProperty(value = "月计划需求量", position = 95)
    private String monthPlan;

    /** 月计划需求量 */
//    @Excel(name = "ui.data.column.scheduleResult.monthPlanOs", sort = 60)
    @ApiModelProperty(value = "月计划剩余量", position = 100)
    private String monthPlanOs;

    /** 单耗 */
    @ApiModelProperty(value = "单耗", position = 105)
    private Double unitConsume;

    /** 库存数量 */
//    @Excel(name = "ui.data.column.scheduleResult.stockQty", sort = 70)
    @ApiModelProperty(value = "库存数量", position = 110)
    private Double stockQty;

    /** 库存供应成型时长，单位：小时 */
//    @Excel(name = "ui.data.column.scheduleResult.supplyTime", sort = 80)
    @ApiModelProperty(value = "库存供应成型时长，单位：小时", position = 120)
    private Double supplyTime;

    /**
     * 当日日计划量合计
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.dailyTotalQty")
    @ApiModelProperty(value = "当日日计划量合计")
    private Double dailyTotalQty;

    /** 中班(16点-24点)计划量(条) */
    @ImportValidated(min = 0, max = 99999999,digits = true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.individual")
    @ApiModelProperty(value = "中班(16点-24点)计划量(条)", position = 130)
    private Double midPlanQty;

    /** 中班(16点-24点)完成量 */
//    @Excel(name = "ui.data.column.scheduleResult.midFinishQty")
    @ApiModelProperty(value = "中班(16点-24点)完成量", position = 140)
    private Double midFinishQty;

    /** 中班(16点-24点)生产顺序 */
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayProduceOrder")
    @ApiModelProperty(value = "中班(16点-24点)生产顺序", position = 150)
    private Integer midProduceOrder;

    /** 中班(16点-24点)完成率 */
//    @Excel(name = "ui.data.column.scheduleResult.midFinishRate")
    @ApiModelProperty(value = "中班(16点-24点)完成率", position = 160)
    private Double midFinishRate;

    /** 中班(16点-24点)系统原因分析 */
//    @Excel(name = "ui.data.column.scheduleResult.midSysAnalysis")
    @ApiModelProperty(value = "中班(16点-24点)系统原因分析", position = 170)
    private String midSysAnalysis;

    /** 中班(16点-24点)手动输入原因分析 */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.dayAnalysis")
    @ApiModelProperty(value = "中班(16点-24点)手动输入原因分析", position = 180)
    private String midHandAnalysis;

    /** 夜班(0点-8点)计划量(条) */
    @ImportValidated( min = 0, max = 99999999,digits = true)
    @Excel(name = "ui.data.column.scheduleResult.nightPlanQty.individual")
    @ApiModelProperty(value = "夜班(0点-8点)计划量(条)", position = 190)
    private Double nightPlanQty;

    /** 夜班(0点-8点)完成量 */
//    @Excel(name = "ui.data.column.scheduleResult.nightFinishQty")
    @ApiModelProperty(value = "夜班(0点-8点)完成量", position = 200)
    private Double nightFinishQty;

    /** 夜班(0点-8点)生产顺序 */
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.nightProduceOrder")
    @ApiModelProperty(value = "夜班(0点-8点)生产顺序",position = 210)
    private Integer nightProduceOrder;

    /** 夜班(0点-8点)完成率 */
//    @Excel(name = "ui.data.column.scheduleResult.nightFinishRate")
    @ApiModelProperty(value = "夜班(0点-8点)完成率", position = 220)
    private Double nightFinishRate;

    /** 夜班(0点-8点)系统原因分析 */
//    @Excel(name = "ui.data.column.scheduleResult.nightSysAnalysis")
    @ApiModelProperty(value = "夜班(0点-8点)系统原因分析", position = 230)
    private String nightSysAnalysis;

    /** 夜班(0点-8点)手动输入原因分析 */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.nightAnalysis")
    @ApiModelProperty(value = "夜班(0点-8点)手动输入原因分析", position = 240)
    private String nightHandAnalysis;

    /** 白班(8点-16点)计划量(条) */
    @ImportValidated( min = 0, max = 99999999,digits = true)
    @Excel(name = "ui.data.column.scheduleResult.midPlanQty.individual")
    @ApiModelProperty(value = "白班(8点-16点)计划量(条)", position = 250)
    private Double dayPlanQty;

    /** 白班(8点-16点)完成量 */
//    @Excel(name = "ui.data.column.scheduleResult.dayFinishQty")
    @ApiModelProperty(value = "白班(8点-16点)完成量", position = 260)
    private Double dayFinishQty;

    /** 白班(8点-16点)生产顺序 */
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.midProduceOrder")
    @ApiModelProperty(value = "白班(8点-16点)生产顺序", position = 270)
    private Integer dayProduceOrder;

    /** 白班(8点-16点)完成率 */
//    @Excel(name = "ui.data.column.scheduleResult.dayFinishRate")
    @ApiModelProperty(value = "白班(8点-16点)完成率", position = 280)
    private Double dayFinishRate;

    /** 白班(8点-16点)系统原因分析 */
//    @Excel(name = "ui.data.column.scheduleResult.daySysAnalysis")
    @ApiModelProperty(value = "白班(8点-16点)系统原因分析", position = 290)
    private String daySysAnalysis;

    /** 白班(8点-16点)手动输入原因分析 */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.midAnalysis")
    @ApiModelProperty(value = "白班(8点-16点)手动输入原因分析", position = 300)
    private String dayHandAnalysis;

    /** 对应成型一班的计划量 */
//    @Excel(name = "ui.data.column.scheduleResult.cxClass1Plan", sort = 190)
    @ApiModelProperty(value = "对应成型一班的计划量", position = 310)
    private Double cxClass1Plan;

    /** 对应成型二班的计划量 */
//    @Excel(name = "ui.data.column.scheduleResult.cxClass2Plan", sort = 200)
    @ApiModelProperty(value = "对应成型二班的计划量", position = 320)
    private Double cxClass2Plan;

    /** 对应成型三班的计划量 */
//    @Excel(name = "ui.data.column.scheduleResult.cxClass3Plan", sort = 210)
    @ApiModelProperty(value = "对应成型三班的计划量", position = 330)
    private Double cxClass3Plan;

    /** 对应成型次一班的计划量 */
//    @Excel(name = "ui.data.column.scheduleResult.cxClass4Plan", sort = 220)
    @ApiModelProperty(value = "对应成型次一班的计划量", position = 340)
    private Double cxClass4Plan;

    /** 对应成型次二班的计划量 */
//    @Excel(name = "ui.data.column.scheduleResult.cxClass5Plan", sort = 230)
    @ApiModelProperty(value = "对应成型次二班的计划量", position = 350)
    private Double cxClass5Plan;

    /**
     * 收尾提示标识(0:提示收尾；1:不需要提示)
     */
    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    private String markCloseOutTip;

    @ApiModelProperty(value = "关联汇总表中年份", position = 600)
    private String year;

    @ApiModelProperty(value = "关联汇总表中月份", position = 600)
    private String month;

//    @Excel(name = "ui.data.column.scheduleResult.isRelease", dictType = "IS_RELEASE")
    @ApiModelProperty(value = "是否发布")
    private String isRelease;

//    @Excel(name = "ui.data.column.scheduleResult.productionStatus", dictType = "PRODUCTION_STATUS")
    @ApiModelProperty(value = "生产状态")
    private String productionStatus;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

    @ApiModelProperty(value = "颜色类型")
    private String colorType;

    @ApiModelProperty(value = "颜色代码")
    private String colorCode;

    @ApiModelProperty(value = "排程记录id数组")
    private Long[] ids;

    @ApiModelProperty(value = "调度员是否修改了生产线，0：否，1：是")
    private Integer changeMachine;

    @ApiModelProperty(value = "调度员是否修改了中班计划量，0：否，1：是")
    private Integer changeMidPlan;

    @ApiModelProperty(value = "调度员是否修改了夜班计划量，0：否，1：是")
    private Integer changeNightPlan;

    @ApiModelProperty(value = "调度员是否修改了白班计划量，0：否，1：是")
    private Integer changeDayPlan;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    private Date newestPublishTime;

    private transient List<Long> ids2;

    @ApiModelProperty(value = "机台名称")
    @TableField(exist = false)
    private String machineName;

    /**
     * 昨日早班计划量
     */
    @ApiModelProperty(value = "昨日早班计划量")
    @TableField(value = "LAST_MID_PLAN_QTY")
    private Double lastMidPlanQty = 0D;

    /**
     * 理论交班库存=成型消耗量
     */
    @ApiModelProperty(value = "成型消耗量")
    @TableField(exist = false)
    private Double cxConsumeQty = 0D;

    /**
     * 理论交班库存=昨日早班计划+库存+夜班计划-(成型昨日早班消耗量+成型夜班消耗量)
     */
    @ApiModelProperty(value = "理论交班库存")
    @TableField(exist = false)
    private Double theoreticClassStockQty = 0D;

    /**
     * 理论昨日早班计划量
     */
    public void calculateTheoreticClassLastDayPlanQty() {
        Double lastMidPlanQty = ObjectUtils.defaultIfNull(this.lastMidPlanQty, 0D);
        Double stockQty = ObjectUtils.defaultIfNull(this.stockQty, 0D);
        Double midPlanQty = ObjectUtils.defaultIfNull(this.midPlanQty, 0D);
        Double cxConsumeQty = ObjectUtils.defaultIfNull(this.cxConsumeQty, 0D);
        this.theoreticClassStockQty = lastMidPlanQty + stockQty + midPlanQty - cxConsumeQty;
    }
}
