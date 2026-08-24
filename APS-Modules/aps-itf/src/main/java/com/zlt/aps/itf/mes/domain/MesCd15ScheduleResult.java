package com.zlt.aps.itf.mes.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 斜裁排程结果 MES 中间表宽表数据。
 */
@Data
public class MesCd15ScheduleResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期。 */
    private Date scheduleDate;
    /** 分裁批次号。 */
    private String splitBatchNo;
    /** 斜裁批次号。 */
    private String batchNo;
    /** 工单号。 */
    private String orderNo;
    /** 钢压大卷编号。 */
    private String bigRollCode;
    /** 机台编码。 */
    private String machineCode;
    /** 库排编码。 */
    private String stockCode;
    /** 成型胎胚物料描述。 */
    private String embryoSpecDesc;
    /** 钢带代码。 */
    private String steelStripCode1;
    /** 钢带物料号。 */
    private String materialCode1;
    /** 钢带单耗。 */
    private BigDecimal unitConsume1;
    /** 钢带库存数量。 */
    private BigDecimal stock1Qty1;
    /** 裁断角度。 */
    private BigDecimal cuttingAngle;
    /** 钢带库存供应成型时长，单位小时。 */
    private BigDecimal supplyTime1;
    /** 夜班计划量。 */
    private BigDecimal nightPlanQty1;
    /** 夜班生产顺序。 */
    private Integer nightProduceOrder1;
    /** 夜班系统原因分析。 */
    private String nightSysAnalysis1;
    /** 夜班人工原因分析。 */
    private String nightHandAnalysis1;
    /** 夜班示方类型。 */
    private String nightExampleType;
    /** 夜班示方号。 */
    private String nightExampleNo;
    /** 夜班备注。 */
    private String nightRemark;
    /** 早班计划量。 */
    private BigDecimal dayPlanQty1;
    /** 早班生产顺序。 */
    private Integer dayProduceOrder1;
    /** 早班系统原因分析。 */
    private String daySysAnalysis1;
    /** 早班人工原因分析。 */
    private String dayHandAnalysis1;
    /** 早班示方类型。 */
    private String dayExampleType;
    /** 早班示方号。 */
    private String dayExampleNo;
    /** 早班备注。 */
    private String dayRemark;
    /** 中班计划量。 */
    private BigDecimal midPlanQty1;
    /** 中班生产顺序。 */
    private Integer midProduceOrder1;
    /** 中班系统原因分析。 */
    private String midSysAnalysis1;
    /** 中班人工原因分析。 */
    private String midHandAnalysis1;
    /** 中班示方类型。 */
    private String midExampleType;
    /** 中班示方号。 */
    private String midExampleNo;
    /** 中班备注。 */
    private String midRemark;
    /** 对应成型一班计划量。 */
    private BigDecimal cxClass1Plan;
    /** 对应成型二班计划量。 */
    private BigDecimal cxClass2Plan;
    /** 对应成型三班计划量。 */
    private BigDecimal cxClass3Plan;
    /** 对应成型四班计划量，MES 字段名为 CX_CLASS5_PLAN。 */
    private BigDecimal cxClass5Plan;
    /** 收尾标识。 */
    private String markCloseOutTip;
    /** 备注。 */
    private String remark;
    /** 删除标识。 */
    private String delFlag;
    /** 创建者。 */
    private String createBy;
    /** 创建时间。 */
    private Date createTime;
    /** 更新者。 */
    private String updateBy;
    /** 更新时间。 */
    private Date updateTime;
    /** 数据版本号。 */
    private String dataVersion;
    /** 分公司编码。 */
    private String companyCode;
    /** 厂别。 */
    private String factoryCode;
}
