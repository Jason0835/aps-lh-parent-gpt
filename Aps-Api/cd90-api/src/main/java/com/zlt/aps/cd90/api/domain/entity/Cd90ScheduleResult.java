package com.zlt.aps.cd90.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
 * 90度裁断排程结果对象 t_cd90_schedule_result
 *
 * @author zlt
 * @date 2021-07-06
 */
@Data
@ApiModel(value = "90度裁断排程结果对象", description = "90度裁断排程结果对象 ")
public class Cd90ScheduleResult extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_CD90_SCHEDULE
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 排程日期
     */
    @ImportValidated(required = true, date = true)
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /**
     * 对应的成型批次号
     */
    @ApiModelProperty(value = "对应的成型批次号")
    private String cxBatchNo;

    /**
     * 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号
     */
    @ApiModelProperty(value = "批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String batchNo;

    /**
     * 工单号，自动生成（批次号+4位定长自增序号）
     */
    @ApiModelProperty(value = "工单号，自动生成")
    private String orderNo;

    /**
     * 帘布大卷编号
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.bigRollCode")
    @ApiModelProperty(value = "帘布大卷编号")
    private String bigRollCode;

    @ApiModelProperty(value = "半钢边胶")
    private String edgeGlue;

    /**
     * 帘布代码
     */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.data.column.cd90ScheduleResult.clothCode")
    @ApiModelProperty(value = "帘布代码")
    private String clothCode;

    /**
     * 单耗
     */
    //@Excel(name = "ui.data.column.scheduleResult.supplyTime")
    @ApiModelProperty(value = "单耗")
    private Double unitConsume;

    /**
     * 生产线(机台名称)
     */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.scheduleResult.produceLine")
    @ApiModelProperty(value = "生产线")
    private String machineId;

    /**
     * 库存数量
     */
    //@Excel(name = "ui.data.column.scheduleResult.stockQty")
    @ApiModelProperty(value = "库存数量")
    private Double stockQty;

    /**
     * 库存供应成型时长，单位：小时
     */
    //@Excel(name = "ui.data.column.scheduleResult.supplyTime")
    @ApiModelProperty(value = "库存供应成型时长，单位：小时")
    private Double supplyTime;

    /**
     * 当日日计划量合计
     */
    @ImportValidated(number = true,max = 9999999,min = 0)
    @Excel(name = "ui.data.column.scheduleResult.dailyTotalQty")
    @ApiModelProperty(value = "当日日计划量合计")
    private Double dailyTotalQty;

    /** 当日完成（米） */
    @ApiModelProperty(value = "当日完成（米）")
    private Double totalFinishQty;

    /** 当日完成率 */
    @ApiModelProperty(value = "当日完成率")
    private Double dailyFinishRate;

    /**
     * 中班(12点-24点)计划量
     */
    @ImportValidated(number = true,min = 0,max = 9999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.meter")
    @ApiModelProperty(value = "中班(12点-24点)计划量")
    private Double dayPlanQty;

    /**
     * 中班(12点-24点)完成量
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.dayFinishQty")
    @ApiModelProperty(value = "中班(12点-24点)完成量")
    private Double dayFinishQty;

    /**
     * 中班(12点-24点)生产顺序
     */
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.dayProduceOrder")
    @ApiModelProperty(value = "中班(12点-24点)生产顺序")
    private Long dayProduceOrder;

    /**
     * 中班(12点-24点)完成率
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.dayFinishRate")
    @ApiModelProperty(value = "中班(12点-24点)完成率")
    private Double dayFinishRate;

    /**
     * 中班(12点-24点)系统原因分析
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.daySysAnalysis")
    @ApiModelProperty(value = "中班(12点-24点)系统原因分析")
    private String daySysAnalysis;

    /**
     * 中班(12点-24点)手动输入原因分析
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.dayAnalysis")
    @ApiModelProperty(value = "中班(12点-24点)手动输入原因分析")
    private String dayHandAnalysis;

    /**
     * 夜班(0点-12点)计划量
     */
    @ImportValidated(number = true,min = 0,max = 9999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.nightPlanQty.meter")
    @ApiModelProperty(value = "夜班(0点-12点)计划量")
    private Double nightPlanQty;

    /**
     * 夜班(0点-12点)完成量
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.nightFinishQty")
    @ApiModelProperty(value = "夜班(0点-12点)完成量")
    private Double nightFinishQty;

    /**
     * 夜班(0点-12点)生产顺序
     */
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.nightProduceOrder")
    @ApiModelProperty(value = "夜班(0点-12点)生产顺序")
    private Long nightProduceOrder;

    /**
     * 夜班(0点-12点)完成率
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.nightFinishRate")
    @ApiModelProperty(value = "夜班(0点-12点)完成率")
    private Double nightFinishRate;

    /**
     * 夜班(0点-12点)系统原因分析
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.nightSysAnalysis")
    @ApiModelProperty(value = "夜班(0点-12点)系统原因分析")
    private String nightSysAnalysis;

    /**
     * 夜班(0点-12点)手动输入原因分析
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.nightAnalysis")
    @ApiModelProperty(value = "夜班(0点-12点)手动输入原因分析")
    private String nightHandAnalysis;

    /**
     * 对应成型一班的计划量
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.cxClass1Plan")
    @ApiModelProperty(value = "对应成型一班的计划量")
    private Double cxClass1Plan;

    /**
     * 对应成型二班的计划量
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.cxClass2Plan")
    @ApiModelProperty(value = "对应成型二班的计划量")
    private Double cxClass2Plan;

    /**
     * 对应成型三班的计划量
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.cxClass3Plan")
    @ApiModelProperty(value = "对应成型三班的计划量")
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的计划量
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.cxClass4Plan")
    @ApiModelProperty(value = "对应成型次一班的计划量")
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的计划量
     */
    //@Excel(name = "ui.data.column.cd90ScheduleResult.cxClass5Plan")
    @ApiModelProperty(value = "对应成型次二班的计划量")
    private Double cxClass5Plan;

    /**
     * 是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE
     */
    //@Excel(name = "ui.data.column.scheduleResult.isRelease")
    @ApiModelProperty(value = "是否发布，0--未发布，1--已发布。对应数据字典为：IS_RELEASE")
    private String isRelease;

    /**
     * 删除标识：0--正常，1-删除
     */
    private String delFlag;

    /**
     * 收尾提示标识(0:提示收尾；1:不需要提示)
     */
    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    private String markCloseOutTip;

    /**
     * 生产状态:0-未生产；1-生产中；2-生产完成
     */
    //@Excel(name = "ui.data.column.scheduleResult.productionStatus")
    @ApiModelProperty(value = "生产状态:0-未生产；1-生产中；2-生产完成")
    private String productionStatus;

    @ApiModelProperty(value = "月计划剩余量")
    private Double monthPlanOs;

    @ApiModelProperty(value = "月计划需求量")
    private Double monthPlan;

    private String year;

    private String month;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.data.column.stock.remark", maxLength = 300)
    private String remark;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;

    @ApiModelProperty(value = "排程记录id数组")
    private Long[] ids;

    @ApiModelProperty(value = "调度员是否修改了生产线，0：否，1：是")
    private Integer changeMachine;

    @ApiModelProperty(value = "调度员是否修改了中班计划量，0：否，1：是")
    private Integer changeDayPlan;

    @ApiModelProperty(value = "调度员是否修改了夜班计划量，0：否，1：是")
    private Integer changeNightPlan;

    @ApiModelProperty(value = "发布成功计数器，每点击一次发布并成功的话，计数器累加")
    private Integer publishSuccessCount;

    @ApiModelProperty(value = "工艺")
    private String craft;

    /**
     * 最新发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "最新发布时间")
    private Date newestPublishTime;

    /**
     * 收尾规格标记，0：收尾1：非收尾
     */
    private String closeOutSpecFlag;

    private transient List<Long> ids2;

    @ApiModelProperty(value = "机台名称")
    @TableField(exist = false)
    private String machineName;

    /**
     * 次日夜班计划量
     */
    @ApiModelProperty(value = "次日夜班计划量")
    @TableField(value = "NEXT_DAY_PLAN_QTY")
    private Double nextDayPlanQty = 0D;

    /**
     * 次日夜班计划量(卷)
     */
    @ApiModelProperty(value = "次日夜班计划量(卷)")
    @TableField(exist = false)
    private Double nextDayPlanQtyRollNum = 0D;

    /**
     * 当日完成（卷）
     */
    @ApiModelProperty(value = "当日完成（卷）")
    @TableField(exist = false)
    private Double totalFinishQtyRollNum;

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
        this.dayFinishQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.dayFinishQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.nightFinishQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.nightFinishQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.totalFinishQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.totalFinishQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.cxClass1PlanRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.cxClass1Plan, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.cxClass2PlanRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.cxClass2Plan, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.cxClass3PlanRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.cxClass3Plan, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.cxClass4PlanRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.cxClass4Plan, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.cxClass5PlanRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.cxClass5Plan, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.lastMidPlanQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.lastMidPlanQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.theoreticClassStockQtyRollNum = BigDecimalUtils.div(ObjectUtils.defaultIfNull(this.theoreticClassStockQty, 0D), curlLengthValue).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
