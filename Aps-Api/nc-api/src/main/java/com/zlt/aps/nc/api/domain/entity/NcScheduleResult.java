package com.zlt.aps.nc.api.domain.entity;

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
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.ObjectUtils;

import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 内衬胶排程结果对象 nc_schedule_result
 *
 * @author zlt
 * @date 2026-06-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "内衬胶排程结果对象", description = "内衬胶排程结果对象 ")
@TableName("T_NC_SCHEDULE_RESULT")
public class NcScheduleResult extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd")
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
    @ImportValidated(required = true,maxLength = 20,isCode = true)
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

    @ApiModelProperty(value = "胶料代码(完整没有截取的)")
    @TableField("WHOLE_GLUE_CODE")
    private String wholeGlueCode;

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
    @ImportValidated(required = true,maxLength = 20)
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
    @ImportValidated(number = true,max = 9999999,min = 0,digits=true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty")
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
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
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
    @ImportValidated(number = true,max = 9999999,min = 0,digits=true)
    @Excel(name = "ui.data.column.scheduleResult.nightPlanQty")
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
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
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

    @TableField(exist = false)
    private Long[] ids;

    /**
     * 供应时长查询条件
     */
    @TableField(exist = false)
    private transient Double supplyStartTime;
    @TableField(exist = false)
    private transient Double supplyEndTime;

    @ApiModelProperty(value = "生产状态")
    @TableField("PRODUCTION_STATUS")
    private String productionStatus;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.data.column.stock.remark", maxLength = 300)
    private String remark;

    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    @TableField("MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    @TableField("DATA_SOURCE")
    private String dataSource;

    @ApiModelProperty(value = "调度员是否修改了生产线，0：否，1：是")
    @TableField("CHANGE_MACHINE")
    private Integer changeMachine;

    @ApiModelProperty(value = "调度员是否修改了中班计划量，0：否，1：是")
    @TableField("CHANGE_DAY_PLAN")
    private Integer changeDayPlan;

    @ApiModelProperty(value = "调度员是否修改了夜班计划量，0：否，1：是")
    @TableField("CHANGE_NIGHT_PLAN")
    private Integer changeNightPlan;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    @TableField("PUBLISH_SUCCESS_COUNT")
    private Integer publishSuccessCount;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    @TableField("NEWEST_PUBLISH_TIME")
    private Date newestPublishTime;

    @TableField(exist = false)
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
     * 次日夜班计划量
     */
    @ApiModelProperty(value = "次日夜班计划量")
    @TableField(value = "NEXT_DAY_PLAN_QTY")
    private Double nextDayPlanQty = 0D;

    /**
     * 次日夜班顺序
     */
    @ApiModelProperty(value = "次日夜班顺序")
    @TableField(value = "NEXT_DAY_PRODUCE_ORDER")
    private Integer nextDayProduceOrder;

    /**
     * 次日夜班计划量(卷)
     */
    @ApiModelProperty(value = "次日夜班计划量(卷)")
    @TableField(exist = false)
    private Double nextDayPlanQtyRollNum = 0D;

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
        this.nextDayPlanQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.nextDayPlanQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
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
