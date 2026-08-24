package com.zlt.aps.cd15.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 斜裁排程结果下发 MES 数据。 */
@Data
public class Cd15ScheduleResultIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 斜裁批次号。 */
    private String cd15BatchNo;
    /** 工单号；分裁组合两条数据共用。 */
    private String orderNo;
    /** 分裁组合号；分裁组合两条数据共用。 */
    private String groupNo;
    /** 当前班次排班日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date scheduleDate;
    /** 斜裁机台编码。 */
    private String machineCode;
    /** 钢带代码。 */
    private String steelStripCode;
    /** 钢带物料号。 */
    private String materialCode;
    /** 成型胎胚物料描述，多个规格使用斜杠拼接。 */
    private String embryoSpecDesc;
    /** GDYY 大卷代码。 */
    private String bigRollCode;
    /** 库排号。 */
    private String storageLaneCode;
    /** 裁断角度。 */
    private String cuttingAngle;
    /** 裁断模式：SINGLE 或 SPLIT。 */
    private String cutMode;
    /** 班次名称。 */
    private String shiftName;
    /** CLASS 字段。 */
    private String classField;
    /** 排程天数。 */
    private Integer scheduleDay;
    /** 当天班次序号。 */
    private Integer dayShiftOrder;
    /** 当前班次计划量。 */
    private Double planQty;
    /** 当前班次成型需求量。 */
    private Double cxPlanQty;
    /** 当前班次完成量。 */
    private Double finishQty;
    /** 当前班次生产顺序。 */
    private Integer produceOrder;
    /** 当前班次完成率。 */
    private Double finishRate;
    /** 当前班次系统分析。 */
    private String analysis;
    /** 当前班次人工分析。 */
    private String analysisInput;
    /** 斜裁宽度，单位毫米。 */
    private BigDecimal craftWidth;
    /** 单耗，单位毫米每条。 */
    private BigDecimal unitConsumeMillimeter;
    /** 单耗，单位米每条。 */
    private BigDecimal unitConsume;
    /** 卷曲长度，单位米。 */
    private BigDecimal curlLength;
    /** GDYY 大卷幅宽，单位毫米。 */
    private BigDecimal cordWidth;
    /** 数据来源。 */
    private String sourceType;
    /** 生产状态。 */
    private String productionStatus;
    /** 钢带库存数量。 */
    private Double stockQty;
    /** 钢带库存供应成型时长，单位小时。 */
    private Double supplyTime;
    /** 当前班次示方类型。 */
    private String exampleType;
    /** 当前班次示方号。 */
    private String exampleNo;
    /** 当前班次备注。 */
    private String shiftRemark;
    /** 对应成型一班计划量。 */
    private Double cxClass1Plan;
    /** 对应成型二班计划量。 */
    private Double cxClass2Plan;
    /** 对应成型三班计划量。 */
    private Double cxClass3Plan;
    /** 对应成型四班计划量。 */
    private Double cxClass4Plan;
    /** 收尾标识。 */
    private String markCloseOutTip;
    /** 排程备注。 */
    private String remark;
    /** 是否用于清除已发布的原班次计划。 */
    private Boolean clearExistingPlan;
    /** 工厂编码。 */
    private String factoryCode;
    /** 发布追踪号。 */
    private String publishTraceId;
}
