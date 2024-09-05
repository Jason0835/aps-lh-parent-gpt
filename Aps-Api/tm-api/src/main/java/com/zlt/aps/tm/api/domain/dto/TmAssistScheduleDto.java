package com.zlt.aps.tm.api.domain.dto;

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
 * 胎面外协排程结果对象 t_tm_assist_schedule
 *
 * @author chen
 * @date 2022-02-15
 */
@ApiModel(value = "胎面外协排程结果对象", description = "胎面外协排程结果对象 ")
@Data
@EqualsAndHashCode(callSuper = true)
public class TmAssistScheduleDto extends ApsBaseEntity {

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
    @Excel(name = "ui.data.column.quota.treadCode")
    @ApiModelProperty(value = "胎面代码", position = 24)
    private String treadCode;

    /**
     * 胶料代码
     */
    @ApiModelProperty(value = "胶料代码", position = 25)
    //@Excel(name = "胶料代码")
    private String glueCode;

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
    @Excel(name = "ui.data.column.scheduleResult.dayPlanQty.meter")
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
    @Excel(name = "ui.data.column.scheduleResult.dayProduceOrder")
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
    @Excel(name = "ui.data.column.scheduleResult.dayAnalysis")
    @ApiModelProperty(value = "中班(12点-24点)手动输入原因分析", position = 38)
    private String dayHandAnalysis;

    /**
     * 夜班(0点-12点)计划量
     */
    @ImportValidated(number = true,max = 9999999,min = 0,digits=true)
    @Excel(name = "ui.data.column.scheduleResult.nightPlanQty.meter")
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
    @Excel(name = "ui.data.column.scheduleResult.nightProduceOrder")
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
    @Excel(name = "ui.data.column.scheduleResult.nightAnalysis")
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

    @ApiModelProperty(value = "补强/封口胶")
    private String reinforceSealGlue;
}
