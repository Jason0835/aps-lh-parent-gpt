package com.zlt.aps.tm.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * 胎面排程结果对象 tm_schedule_result
 *
 * @author zlt
 * @date 2021-06-17
 */
@ApiModel(value = "胎面排程结果对象", description = "胎面排程结果对象 ")
public class TmScheduleResult2 extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_TM_SCHEDULE
     */
    private Long id;

    /**
     * 排程日期
     */
    @ApiModelProperty(value = "排程日期", position = 10)
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.scheduleResult.scheduleDate",dateFormat = "yyyy-MM-dd")
    @ImportValidated(required = true, date = true)
    private Date scheduleDate;

    /**
     * 对应的成型批次号
     */
    @ApiModelProperty(value = "对应的成型批次号", position = 15)
    //@Excel(name = "对应的成型批次号")
    private String cxBatchNo;

    /**
     * 批次号，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号
     */
    @ApiModelProperty(value = "批次号", position = 20)
    //@Excel(name = "批次号")
    private String batchNo;

    /**
     * 工单号，自动生成（批次号+4位定长自增序号）
     */
    @ApiModelProperty(value = "工单号", position = 21)
    //@Excel(name = "工单号，自动生成", readConverterExp = "批=次号+4位定长自增序号")
    private String orderNo;

    /**
     * 规格描述信息
     */
    @ApiModelProperty(value = "规格描述", position = 22)
    //@Excel(name = "规格描述")
    private String specDesc;

    /**
     * 施工代码，即胎胚代码
     */
    @ApiModelProperty(value = "排程日期", position = 23)
    //@Excel(name = "施工代码，即胎胚代码")
    private String workCode;

    /**
     * 胎面代码
     */
    @ImportValidated(required = true,maxLength = 20,isCode = true)
    @Excel(name = "ui.construction.tread")
    @ApiModelProperty(value = "胎面代码", position = 24)
    private String treadCode;

    /**
     * 胶料代码
     */
    @ApiModelProperty(value = "胶料代码", position = 25)
    //@Excel(name = "胶料代码")
    private String glueCode;

    @ApiModelProperty(value = "胶料代码(完整没有截取的)")
    private String wholeGlueCode;

    /**
     * 胶料序号
     */
    @ApiModelProperty(value = "胶料序号", position = 26)
    //@Excel(name = "胶料序号")
    private String glueSeq;

    /**
     * 口型板代码
     */
    @ApiModelProperty(value = "口型板代码", position = 27)
    //@Excel(name = "口型板代码")
    private String mouthPlateCode;

    /**
     * 单耗
     */
    @ApiModelProperty(value = "单耗", position = 28)
    //@Excel(name = "单耗")
    private Double unitConsume;

    /**
     * 生产线(机台名称)
     */
    @ImportValidated(required = true,maxLength = 20)
    @Excel(name = "ui.data.column.scheduleResult.produceLine")
    @ApiModelProperty(value = "生产线", position = 29)
    private String machineId;

    /**
     * 库存数量
     */
    @ApiModelProperty(value = "库存数量", position = 31)
    //@Excel(name = "库存数量")
    private Double stockQty;

    /**
     * 库存供应成型时长，单位：小时
     */
    @ApiModelProperty(value = "库存供应成型时长", position = 32)
    //@Excel(name = "库存供应成型时长")
    private Double supplyTime;

    /**
     * 当日日计划量合计
     */
    @Excel(name = "ui.data.column.scheduleResult.dailyTotalQty")
    @ApiModelProperty(value = "当日日计划量合计")
    private Double dailyTotalQty;

    /**
     * 中班(12点-24点)计划量
     */
    @ImportValidated(number = true,max = 9999999,min = 0,digits=true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.planMeter2")
    @ApiModelProperty(value = "中班(12点-24点)计划量", position = 33)
    private Double dayPlanQty;

    /**
     * 中班(12点-24点)完成量
     */
    @ApiModelProperty(value = "中班(12点-24点)完成量", position = 34)
    //@Excel(name = "中班(12点-24点)完成量")
    private Double dayFinishQty;

    /**
     * 中班(12点-24点)生产顺序
     */
    @ApiModelProperty(value = "中班(12点-24点)生产顺序", position = 35)
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.productSort1")
    private Long dayProduceOrder;

    /**
     * 中班(12点-24点)完成率
     */
    @ApiModelProperty(value = "中班(12点-24点)完成率", position = 36)
    //@Excel(name = "中班(12点-24点)完成率")
    private Double dayFinishRate;

    /**
     * 中班(12点-24点)系统原因分析
     */
    @ApiModelProperty(value = "中班(12点-24点)系统原因分析", position = 37)
    private String daySysAnalysis;

    /**
     * 中班(12点-24点)手动输入原因分析
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.analysis2")
    @ApiModelProperty(value = "中班(12点-24点)手动输入原因分析", position = 38)
    private String dayHandAnalysis;

    /**
     * 夜班(0点-12点)计划量
     */
    @ImportValidated(number = true,max = 9999999,min = 0,digits=true)
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.planMeter3")
    @ApiModelProperty(value = "夜班(0点-12点)计划量", position = 39)
    private Double nightPlanQty;

    /**
     * 夜班(0点-12点)完成量
     */
    @ApiModelProperty(value = "夜班(0点-12点)完成量", position = 40)
    //@Excel(name = "夜班(0点-12点)完成量")
    private Double nightFinishQty;

    /**
     * 夜班(0点-12点)生产顺序
     */
    @ApiModelProperty(value = "夜班(0点-12点)生产顺序", position = 41)
    @ImportValidated(number = true,min = 0,max = 999999,isInteger = true)
    @Excel(name = "ui.data.column.scheduleResult.productSort2")
    private Long nightProduceOrder;

    /**
     * 夜班(0点-12点)完成率
     */
    @ApiModelProperty(value = "夜班(0点-12点)完成率", position = 42)
    //@Excel(name = "夜班(0点-12点)完成率")
    private Double nightFinishRate;

    /**
     * 夜班(0点-12点)系统原因分析
     */
    @ApiModelProperty(value = "夜班(0点-12点)系统原因分析", position = 43)
    //@Excel(name = "夜班(0点-12点)系统原因分析")
    private String nightSysAnalysis;

    /**
     * 夜班(0点-12点)手动输入原因分析
     */
    @ImportValidated(maxLength = 100)
    @Excel(name = "ui.data.column.scheduleResult.analysis3")
    @ApiModelProperty(value = "夜班(0点-12点)手动输入原因分析", position = 44)
    private String nightHandAnalysis;

    @ApiModelProperty(value = "预计划", position = 44)
    private Double prePlanQty;

    /**
     * 对应成型一班的计划量
     */
    @ApiModelProperty(value = "对应成型一班的计划量", position = 45)
    //@Excel(name = "对应成型一班的计划量")
    private Double cxClass1Plan;

    /**
     * 对应成型二班的计划量
     */
    @ApiModelProperty(value = "对应成型二班的计划量", position = 46)
    //@Excel(name = "对应成型二班的计划量")
    private Double cxClass2Plan;

    /**
     * 对应成型三班的计划量
     */
    @ApiModelProperty(value = "对应成型三班的计划量", position = 47)
    //@Excel(name = "对应成型三班的计划量")
    private Double cxClass3Plan;

    /**
     * 对应成型次一班的计划量
     */
    @ApiModelProperty(value = "对应成型次一班的计划量", position = 48)
    //@Excel(name = "对应成型次一班的计划量")
    private Double cxClass4Plan;

    /**
     * 对应成型次二班的计划量
     */
    @ApiModelProperty(value = "对应成型次二班的计划量", position = 49)
    //@Excel(name = "对应成型次二班的计划量")
    private Double cxClass5Plan;

    @ApiModelProperty(value = "月计划需求量", position = 50)
    private Double monthPlan;

    @ApiModelProperty(value = "月计划剩余量", position = 51)
    private Double monthPlanOs;

    private String year;

    private String month;

    private String isRelease;

    private String delFlag;

    @ApiModelProperty(value = "生产状态")
    private String productionStatus;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.data.column.stock.remark", maxLength = 300)
    private String remark;

    @ApiModelProperty(value = "收尾提示标识(0:提示收尾；1:不需要提示)")
    private String markCloseOutTip;

    @ApiModelProperty(value = "数据来源：0>自动排程；1>APS插单；2>导入；")
    private String dataSource;





    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getMarkCloseOutTip() {
        return markCloseOutTip;
    }

    public void setMarkCloseOutTip(String markCloseOutTip) {
        this.markCloseOutTip = markCloseOutTip;
    }

    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getProductionStatus() {
        return productionStatus;
    }

    public void setProductionStatus(String productionStatus) {
        this.productionStatus = productionStatus;
    }

    public String getIsRelease() { return isRelease; }

    public void setIsRelease(String isRelease) { this.isRelease = isRelease; }

    public String getYear() { return year; }

    public void setYear(String year) { this.year = year; }

    public String getMonth() { return month; }

    public void setMonth(String month) { this.month = month; }

    public Double getMonthPlan() { return monthPlan; }

    public void setMonthPlan(Double monthPlan) { this.monthPlan = monthPlan; }

    public Double getMonthPlanOs() {
        return monthPlanOs;
    }

    public void setMonthPlanOs(Double monthPlanOs) {
        this.monthPlanOs = monthPlanOs;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setScheduleDate(Date scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public Date getScheduleDate() {
        return scheduleDate;
    }

    public void setCxBatchNo(String cxBatchNo) {
        this.cxBatchNo = cxBatchNo;
    }

    public String getCxBatchNo() {
        return cxBatchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setSpecDesc(String specDesc) {
        this.specDesc = specDesc;
    }

    public String getSpecDesc() {
        return specDesc;
    }

    public void setWorkCode(String workCode) {
        this.workCode = workCode;
    }

    public String getWorkCode() {
        return workCode;
    }

    public void setTreadCode(String treadCode) {
        this.treadCode = treadCode;
    }

    public String getTreadCode() {
        return treadCode;
    }

    public void setGlueCode(String glueCode) {
        this.glueCode = glueCode;
    }

    public String getGlueCode() {
        return glueCode;
    }

    public void setGlueSeq(String glueSeq) {
        this.glueSeq = glueSeq;
    }

    public String getGlueSeq() {
        return glueSeq;
    }

    public void setMouthPlateCode(String mouthPlateCode) {
        this.mouthPlateCode = mouthPlateCode;
    }

    public String getMouthPlateCode() {
        return mouthPlateCode;
    }

    public void setUnitConsume(Double unitConsume) {
        this.unitConsume = unitConsume;
    }

    public Double getUnitConsume() {
        return unitConsume;
    }

    public String getMachineId() { return machineId; }

    public void setMachineId(String machineId) { this.machineId = machineId; }

    public void setStockQty(Double stockQty) {
        this.stockQty = stockQty;
    }

    public Double getStockQty() {
        return stockQty;
    }

    public void setSupplyTime(Double supplyTime) {
        this.supplyTime = supplyTime;
    }

    public Double getSupplyTime() {
        return supplyTime;
    }

    public void setDayPlanQty(Double dayPlanQty) {
        this.dayPlanQty = dayPlanQty;
    }

    public Double getDayPlanQty() {
        return dayPlanQty;
    }

    public void setDayFinishQty(Double dayFinishQty) {
        this.dayFinishQty = dayFinishQty;
    }

    public Double getDayFinishQty() {
        return dayFinishQty;
    }

    public void setDayProduceOrder(Long dayProduceOrder) {
        this.dayProduceOrder = dayProduceOrder;
    }

    public Long getDayProduceOrder() {
        return dayProduceOrder;
    }

    public void setDayFinishRate(Double dayFinishRate) {
        this.dayFinishRate = dayFinishRate;
    }

    public Double getDayFinishRate() {
        return dayFinishRate;
    }

    public void setDaySysAnalysis(String daySysAnalysis) {
        this.daySysAnalysis = daySysAnalysis;
    }

    public String getDaySysAnalysis() {
        return daySysAnalysis;
    }

    public void setDayHandAnalysis(String dayHandAnalysis) {
        this.dayHandAnalysis = dayHandAnalysis;
    }

    public String getDayHandAnalysis() {
        return dayHandAnalysis;
    }

    public void setNightPlanQty(Double nightPlanQty) {
        this.nightPlanQty = nightPlanQty;
    }

    public Double getNightPlanQty() {
        return nightPlanQty;
    }

    public void setNightFinishQty(Double nightFinishQty) {
        this.nightFinishQty = nightFinishQty;
    }

    public Double getNightFinishQty() {
        return nightFinishQty;
    }

    public void setNightProduceOrder(Long nightProduceOrder) {
        this.nightProduceOrder = nightProduceOrder;
    }

    public Long getNightProduceOrder() {
        return nightProduceOrder;
    }

    public void setNightFinishRate(Double nightFinishRate) {
        this.nightFinishRate = nightFinishRate;
    }

    public Double getNightFinishRate() {
        return nightFinishRate;
    }

    public void setNightSysAnalysis(String nightSysAnalysis) {
        this.nightSysAnalysis = nightSysAnalysis;
    }

    public String getNightSysAnalysis() {
        return nightSysAnalysis;
    }

    public void setNightHandAnalysis(String nightHandAnalysis) {
        this.nightHandAnalysis = nightHandAnalysis;
    }

    public String getNightHandAnalysis() {
        return nightHandAnalysis;
    }

    public void setCxClass1Plan(Double cxClass1Plan) {
        this.cxClass1Plan = cxClass1Plan;
    }

    public Double getCxClass1Plan() {
        return cxClass1Plan;
    }

    public void setCxClass2Plan(Double cxClass2Plan) {
        this.cxClass2Plan = cxClass2Plan;
    }

    public Double getCxClass2Plan() {
        return cxClass2Plan;
    }

    public void setCxClass3Plan(Double cxClass3Plan) {
        this.cxClass3Plan = cxClass3Plan;
    }

    public Double getCxClass3Plan() {
        return cxClass3Plan;
    }

    public void setCxClass4Plan(Double cxClass4Plan) {
        this.cxClass4Plan = cxClass4Plan;
    }

    public Double getCxClass4Plan() {
        return cxClass4Plan;
    }

    public void setCxClass5Plan(Double cxClass5Plan) {
        this.cxClass5Plan = cxClass5Plan;
    }

    public Double getCxClass5Plan() {
        return cxClass5Plan;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public Double getDailyTotalQty() {
        return dailyTotalQty;
    }

    public void setDailyTotalQty(Double dailyTotalQty) {
        this.dailyTotalQty = dailyTotalQty;
    }

    @Override
    public String toString() {
        return "TmScheduleResult{" +
                "id=" + id +
                ", scheduleDate=" + scheduleDate +
                ", cxBatchNo='" + cxBatchNo + '\'' +
                ", batchNo='" + batchNo + '\'' +
                ", orderNo='" + orderNo + '\'' +
                ", specDesc='" + specDesc + '\'' +
                ", workCode='" + workCode + '\'' +
                ", treadCode='" + treadCode + '\'' +
                ", glueCode='" + glueCode + '\'' +
                ", glueSeq='" + glueSeq + '\'' +
                ", mouthPlateCode='" + mouthPlateCode + '\'' +
                ", unitConsume=" + unitConsume +
                ", machineId='" + machineId + '\'' +
                ", stockQty=" + stockQty +
                ", supplyTime=" + supplyTime +
                ", dayPlanQty=" + dayPlanQty +
                ", dayFinishQty=" + dayFinishQty +
                ", dayProduceOrder=" + dayProduceOrder +
                ", dayFinishRate=" + dayFinishRate +
                ", daySysAnalysis='" + daySysAnalysis + '\'' +
                ", dayHandAnalysis='" + dayHandAnalysis + '\'' +
                ", nightPlanQty=" + nightPlanQty +
                ", nightFinishQty=" + nightFinishQty +
                ", nightProduceOrder=" + nightProduceOrder +
                ", nightFinishRate=" + nightFinishRate +
                ", nightSysAnalysis='" + nightSysAnalysis + '\'' +
                ", nightHandAnalysis='" + nightHandAnalysis + '\'' +
                ", cxClass1Plan=" + cxClass1Plan +
                ", cxClass2Plan=" + cxClass2Plan +
                ", cxClass3Plan=" + cxClass3Plan +
                ", cxClass4Plan=" + cxClass4Plan +
                ", cxClass5Plan=" + cxClass5Plan +
                ", monthPlan=" + monthPlan +
                ", monthPlanOs=" + monthPlanOs +
                ", year='" + year + '\'' +
                ", month='" + month + '\'' +
                ", isRelease='" + isRelease + '\'' +
                ", delFlag='" + delFlag + '\'' +
                ", productionStatus='" + productionStatus + '\'' +
                ", remark='" + remark + '\'' +
                ", markCloseOutTip='" + markCloseOutTip + '\'' +
                '}';
    }
}
