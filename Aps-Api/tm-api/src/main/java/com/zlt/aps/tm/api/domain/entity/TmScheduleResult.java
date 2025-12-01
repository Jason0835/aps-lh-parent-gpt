package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 胎面排程结果对象 tm_schedule_result
 *
 * @author zlt
 * @date 2021-06-17
 */
@Data
@ApiModel(value = "胎面排程结果对象", description = "胎面排程结果对象 ")
@TableName(value = "T_TM_SCHEDULE_RESULT")
//@KeySequence(value = "SEQ_TM_SCHEDULE")
public class TmScheduleResult extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 排程日期
     */
    @ApiModelProperty(value = "排程日期", position = 10)
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd")
    @ImportValidated(required = true, date = true)
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /**
     * 对应的成型批次号
     */
    @ApiModelProperty(value = "对应的成型批次号", position = 15)
    //@Excel(name = "对应的成型批次号")
    @TableField(value = "CX_BATCH_NO")
    private String cxBatchNo;

    /**
     * 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号
     */
    @ApiModelProperty(value = "批次号", position = 20)
    //@Excel(name = "批次号")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /**
     * 工单号，自动生成（批次号+4位定长自增序号）
     */
    @ApiModelProperty(value = "工单号", position = 21)
    //@Excel(name = "工单号，自动生成", readConverterExp = "批=次号+4位定长自增序号")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /**
     * 规格描述信息
     */
    @ApiModelProperty(value = "规格描述", position = 22)
    //@Excel(name = "规格描述")
    @TableField(value = "SPEC_DESC")
    private String specDesc;

    /**
     * 施工代码，即胎胚代码
     */
    @ApiModelProperty(value = "排程日期", position = 23)
    //@Excel(name = "施工代码，即胎胚代码")
    @TableField(value = "WORK_CODE")
    private String workCode;

    /**
     * 胎面代码
     */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.quota.treadCode")
    @ApiModelProperty(value = "胎面代码", position = 24)
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    /**
     * 胶料代码
     */
    @ApiModelProperty(value = "胶料代码", position = 25)
    //@Excel(name = "胶料代码")
    @TableField(value = "GLUE_CODE")
    private String glueCode;

    @ApiModelProperty(value = "胶料代码(完整没有截取的)")
    @TableField(value = "WHOLE_GLUE_CODE")
    private String wholeGlueCode;

    /**
     * 胶料序号
     */
    @ApiModelProperty(value = "胶料序号", position = 26)
    //@Excel(name = "胶料序号")
    @TableField(value = "GLUE_SEQ")
    private String glueSeq;

    /**
     * 口型板代码
     */
    @ApiModelProperty(value = "口型板代码", position = 27)
    //@Excel(name = "口型板代码")
    @TableField(value = "MOUTH_PLATE_CODE")
    private String mouthPlateCode;

    /**
     * 单耗
     */
    @ApiModelProperty(value = "单耗", position = 28)
    //@Excel(name = "单耗")
    @TableField(value = "UNIT_CONSUME")
    private Double unitConsume;

    /**
     * 生产线(机台名称)
     */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.scheduleResult.produceLine")
    @ApiModelProperty(value = "生产线", position = 29)
    @TableField(value = "MACHINE_ID")
    private String machineId;

    /**
     * 库存数量
     */
    @ApiModelProperty(value = "库存数量", position = 31)
    //@Excel(name = "库存数量")
    @TableField(value = "STOCK_QTY")
    private Double stockQty;

    /**
     * 库存供应成型时长，单位：小时
     */
    @ApiModelProperty(value = "库存供应成型时长", position = 32)
    //@Excel(name = "库存供应成型时长")
    @TableField(value = "SUPPLY_TIME")
    private Double supplyTime;

    /**
     * 当日日计划量合计
     */
    @Excel(name = "ui.data.column.scheduleResult.dailyTotalQty")
    @ApiModelProperty(value = "当日日计划量合计")
    @TableField(exist = false)
    private Double dailyTotalQty;

    /**
     * 中班(12点-24点)计划量
     */
    @ImportValidated(number = true,max = 9999999,min = 0,digits=true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty")
    @ApiModelProperty(value = "中班(12点-24点)计划量", position = 33)
    @TableField(value = "DAY_PLAN_QTY")
    private Double dayPlanQty;

    /**
     * 中班(12点-24点)完成量
     */
    @ApiModelProperty(value = "中班(12点-24点)完成量", position = 34)
    //@Excel(name = "中班(12点-24点)完成量")
    @TableField(value = "DAY_FINISH_QTY")
    private Double dayFinishQty;

    /**
     * 中班(12点-24点)生产顺序
     */
    @ApiModelProperty(value = "中班(12点-24点)生产顺序", position = 35)
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayProduceOrder")
    @TableField(value = "DAY_PRODUCE_ORDER")
    private Long dayProduceOrder;

    /**
     * 中班(12点-24点)完成率
     */
    @ApiModelProperty(value = "中班(12点-24点)完成率", position = 36)
    //@Excel(name = "中班(12点-24点)完成率")
    @TableField(value = "DAY_FINISH_RATE")
    private Double dayFinishRate;

    /**
     * 中班(12点-24点)系统原因分析
     */
    @ApiModelProperty(value = "中班(12点-24点)系统原因分析", position = 37)
    @TableField(value = "DAY_SYS_ANALYSIS")
    private String daySysAnalysis;

    /**
     * 中班(12点-24点)手动输入原因分析
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.dayAnalysis")
    @ApiModelProperty(value = "中班(12点-24点)手动输入原因分析", position = 38)
    @TableField(value = "DAY_HAND_ANALYSIS")
    private String dayHandAnalysis;

    /**
     * 夜班(0点-12点)计划量
     */
    @ImportValidated(number = true,max = 9999999,min = 0,digits=true)
    @Excel(name = "ui.data.column.scheduleResult.nightPlanQty")
    @ApiModelProperty(value = "夜班(0点-12点)计划量", position = 39)
    @TableField(value = "NIGHT_PLAN_QTY")
    private Double nightPlanQty;

    /**
     * 夜班(0点-12点)完成量
     */
    @ApiModelProperty(value = "夜班(0点-12点)完成量", position = 40)
    //@Excel(name = "夜班(0点-12点)完成量")
    @TableField(value = "NIGHT_FINISH_QTY")
    private Double nightFinishQty;

    /**
     * 夜班(0点-12点)生产顺序
     */
    @ApiModelProperty(value = "夜班(0点-12点)生产顺序", position = 41)
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.nightProduceOrder")
    @TableField(value = "NIGHT_PRODUCE_ORDER")
    private Long nightProduceOrder;

    /**
     * 夜班(0点-12点)完成率
     */
    @ApiModelProperty(value = "夜班(0点-12点)完成率", position = 42)
    //@Excel(name = "夜班(0点-12点)完成率")
    @TableField(value = "NIGHT_FINISH_RATE")
    private Double nightFinishRate;

    /**
     * 夜班(0点-12点)系统原因分析
     */
    @ApiModelProperty(value = "夜班(0点-12点)系统原因分析", position = 43)
    //@Excel(name = "夜班(0点-12点)系统原因分析")
    @TableField(value = "NIGHT_SYS_ANALYSIS")
    private String nightSysAnalysis;

    /**
     * 夜班(0点-12点)手动输入原因分析
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.nightAnalysis")
    @ApiModelProperty(value = "夜班(0点-12点)手动输入原因分析", position = 44)
    @TableField(value = "NIGHT_HAND_ANALYSIS")
    private String nightHandAnalysis;

    @ApiModelProperty(value = "预计划", position = 44)
    @TableField(value = "PRE_PLAN_QTY")
    private Double prePlanQty;

    /**
     * 对应成型一班的计划量
     */
    @ApiModelProperty(value = "对应成型一班的计划量", position = 45)
    //@Excel(name = "对应成型一班的计划量")
    @TableField(value = "CX_CLASS1_PLAN")
    private Double cxClass1Plan;

    /**
     * 对应成型二班的计划量
     */
    @ApiModelProperty(value = "对应成型二班的计划量", position = 46)
    //@Excel(name = "对应成型二班的计划量")
    @TableField(value = "CX_CLASS2_PLAN")
    private Double cxClass2Plan;

    /**
     * 对应成型三班的计划量
     */
    @ApiModelProperty(value = "对应成型三班的计划量", position = 47)
    //@Excel(name = "对应成型三班的计划量")
    @TableField(value = "CX_CLASS3_PLAN")
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的计划量
     */
    @ApiModelProperty(value = "对应成型次一班的计划量", position = 48)
    //@Excel(name = "对应成型次一班的计划量")
    @TableField(value = "CX_CLASS4_PLAN")
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的计划量
     */
    @ApiModelProperty(value = "对应成型次二班的计划量", position = 49)
    //@Excel(name = "对应成型次二班的计划量")
    @TableField(value = "CX_CLASS5_PLAN")
    private Double cxClass5Plan;

    @ApiModelProperty(value = "月计划需求量", position = 50)
    @TableField(exist = false)
    private Double monthPlan;

    @ApiModelProperty(value = "月计划剩余量", position = 51)
    @TableField(exist = false)
    private Double monthPlanOs;

    @TableField(exist = false)
    private String year;

    @TableField(exist = false)
    private String month;

    @TableField(exist = false)
    private Long[] ids;

    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /**
     * 供应时长查询条件
     */
    @TableField(exist = false)
    private transient Double supplyStartTime;
    @TableField(exist = false)
    private transient Double supplyEndTime;

    @ApiModelProperty(value = "生产状态")
    @TableField(value = "PRODUCTION_STATUS")
    private String productionStatus;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.data.column.stock.remark", maxLength = 300)
    @TableField(value = "REMARK")
    private String remark;

    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    @TableField(value = "MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    @TableField(value = "DATA_SOURCE")
    private String dataSource;

    @ApiModelProperty(value = "补强/封口胶")
    @TableField(value = "REINFORCE_SEAL_GLUE")
    private String reinforceSealGlue;

    @ApiModelProperty(value = "调度员是否修改了生产线，0：否，1：是")
    @TableField(exist = false)
    private Integer changeMachine;

    @ApiModelProperty(value = "调度员是否修改了中班计划量，0：否，1：是")
    @TableField(exist = false)
    private Integer changeDayPlan;

    @ApiModelProperty(value = "调度员是否修改了夜班计划量，0：否，1：是")
    @TableField(exist = false)
    private Integer changeNightPlan;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    @TableField(value = "PUBLISH_SUCCESS_COUNT")
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    @TableField(value = "NEWEST_PUBLISH_TIME")
    private Date newestPublishTime;

    /**
     * 昨日早班计划量
     */
    @ApiModelProperty(value = "昨日早班计划量")
    @TableField(value = "LAST_MID_PLAN_QTY")
    private Double lastMidPlanQty = 0D;

    /**
     * 卷曲长度
     */
    @ApiModelProperty(value = "卷曲长度")
    @TableField(exist = false)
    private Double curlLength;

    /**
     * 理论交班库存=昨日早班计划+库存+夜班计划-(成型昨日早班消耗量+成型夜班消耗量)
     */
    @ApiModelProperty(value = "理论交班库存")
    @TableField(exist = false)
    private Double theoreticClassStockQty = 0D;

    /**
     * 理论交班库存=成型消耗量
     */
    @ApiModelProperty(value = "成型消耗量")
    @TableField(exist = false)
    private Double cxConsumeQty = 0D;

    @TableField(exist = false)
    private transient List<Long> ids2;

    @ApiModelProperty(value = "机台名称")
    @TableField(exist = false)
    private String machineName;

    /**
     * 库存数量(卷)
     */
    @ApiModelProperty(value = "库存数量(卷)")
    @TableField(exist = false)
    private Double stockQtyRollNum = 0D;

    @ApiModelProperty(value = "月计划剩余量(卷)", position = 51)
    @TableField(exist = false)
    private Double monthPlanOsRollNum = 0D;

    /**
     * 当日日计划量合计(卷)
     */
    @ApiModelProperty(value = "当日日计划量合计(卷)")
    @TableField(exist = false)
    private Double dailyTotalQtyRollNum = 0D;

    /**
     * 夜班计划量(卷)
     */
    @ApiModelProperty(value = "夜班计划量(卷)")
    @TableField(exist = false)
    private Double dayPlanQtyRollNum = 0D;

    /**
     * 早班计划量(卷)
     */
    @ApiModelProperty(value = "早班计划量(卷)")
    @TableField(exist = false)
    private Double nightPlanQtyRollNum = 0D;

    /**
     * 预计划量(卷)
     */
    @ApiModelProperty(value = "预计划量(卷)")
    @TableField(exist = false)
    private Double prePlanQtyRollNum = 0D;

    /**
     * 昨日早班计划用量(卷)
     */
    @ApiModelProperty(value = "昨日早班计划用量(卷)")
    @TableField(exist = false)
    private Double cxClass1PlanRollNum = 0D;

    /**
     * 夜班计划用量(卷)
     */
    @ApiModelProperty(value = "夜班计划用量(卷)")
    @TableField(exist = false)
    private Double cxClass2PlanRollNum = 0D;

    /**
     * 早班计划用量(卷)
     */
    @ApiModelProperty(value = "早班计划用量(卷)")
    @TableField(exist = false)
    private Double cxClass3PlanRollNum = 0D;

    /**
     * 次日夜班计划用量(卷)
     */
    @ApiModelProperty(value = "次日夜班计划用量(卷)")
    @TableField(exist = false)
    private Double cxClass4PlanRollNum = 0D;

    /**
     * 次日早班计划用量(卷)
     */
    @ApiModelProperty(value = "次日早班计划用量(卷)")
    @TableField(exist = false)
    private Double cxClass5PlanRollNum = 0D;

    /**
     * 昨日早班计划量(卷)
     */
    @ApiModelProperty(value = "昨日早班计划量(卷)")
    @TableField(exist = false)
    private Double lastMidPlanQtyRollNum = 0D;

    /**
     * 理论交接班库存(卷)
     */
    @ApiModelProperty(value = "理论交接班库存(卷)")
    @TableField(exist = false)
    private Double theoreticClassStockQtyRollNum = 0D;

    /**
     * 中班(12点-24点)完成量(卷)
     */
    @ApiModelProperty(value = "中班(12点-24点)完成量(卷)", position = 34)
    @TableField(exist = false)
    private Double dayFinishQtyRollNum = 0D;

    /**
     * 夜班(0点-12点)完成量(卷)
     */
    @ApiModelProperty(value = "夜班(0点-12点)完成量(卷)", position = 40)
    @TableField(exist = false)
    private Double nightFinishQtyRollNum = 0D;

    /**
     * 批量转机台参数-原机台ID
     */
    @ApiModelProperty(value = "原机台ID")
    @TableField(exist = false)
    private String sourceMachineId;

    /**
     * 批量转机台参数-目标机台ID
     */
    @ApiModelProperty(value = "目标机台ID")
    @TableField(exist = false)
    private String targetMachineId;

    /**
     * 批量转机台参数-班次，字典：CLASS_NUM_THREE
     */
    @ApiModelProperty(value = "班次，字典：CLASS_NUM_THREE")
    @TableField(exist = false)
    private Integer classShift;

    /**
     * 理论昨日早班计划量
     */
    public void calculateTheoreticClassLastDayPlanQty() {
        Double lastMidPlanQty = ObjectUtils.defaultIfNull(this.lastMidPlanQty, 0D);
        Double stockQty = ObjectUtils.defaultIfNull(this.stockQty, 0D);
        Double dayPlanQty = ObjectUtils.defaultIfNull(this.dayPlanQty, 0D);
        Double cxConsumeQty = ObjectUtils.defaultIfNull(this.cxConsumeQty, 0D);
        this.theoreticClassStockQty = lastMidPlanQty + stockQty + dayPlanQty - cxConsumeQty;
    }

    /**
     * 计算计划量对应卷数
     */
    public void calculatePlanQty() {
        Double curlLengthValue = this.curlLength;
        if (curlLengthValue == null || curlLengthValue <= 0) {
            return;
        }
        this.stockQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.stockQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.monthPlanOsRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.monthPlanOs, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.dailyTotalQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.dailyTotalQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.dayPlanQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.dayPlanQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.nightPlanQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.nightPlanQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.prePlanQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.prePlanQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.dayFinishQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.dayFinishQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.nightFinishQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.nightFinishQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.cxClass1PlanRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.cxClass1Plan, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.cxClass2PlanRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.cxClass2Plan, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.cxClass3PlanRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.cxClass3Plan, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.cxClass4PlanRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.cxClass4Plan, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.cxClass5PlanRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.cxClass5Plan, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.lastMidPlanQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.lastMidPlanQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.theoreticClassStockQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.theoreticClassStockQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
