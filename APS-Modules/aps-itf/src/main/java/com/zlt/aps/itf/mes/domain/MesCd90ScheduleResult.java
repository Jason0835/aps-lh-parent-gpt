package com.zlt.aps.itf.mes.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 直裁排程结果 MES 中间表宽表数据。
 */
@Data
public class MesCd90ScheduleResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期。 */
    private Date scheduleDate;
    /** 对应成型批次号。 */
    private String cxBatchNo;
    /** 直裁批次号。 */
    private String batchNo;
    /** 工单号。 */
    private String orderNo;
    /** 大卷编号。 */
    private String bigRollCode;
    /** 产出物类别。 */
    private String outputType;
    /** 产出物代码。 */
    private String outputCode;
    /** 产出物料号。 */
    private String outputMaterialCode;
    /** 成型胎胚物料描述。 */
    private String embryoSpecDesc;
    /** 单耗。 */
    private BigDecimal unitConsume;
    /** 机台编码。 */
    private String machineCode;
    /** 库排编码。 */
    private String stockCode;
    /** 库存数量。 */
    private BigDecimal stockQty;
    /** 库存供应成型时长，单位小时。 */
    private BigDecimal supplyTime;
    /** 夜班计划量。 */
    private BigDecimal nightPlanQty;
    /** 夜班生产顺序。 */
    private Integer nightProduceOrder;
    /** 夜班系统原因分析。 */
    private String nightSysAnalysis;
    /** 夜班人工原因分析。 */
    private String nightHandAnalysis;
    /** 夜班示方类型。 */
    private String nightExampleType;
    /** 夜班示方号。 */
    private String nightExampleNo;
    /** 夜班备注。 */
    private String nightRemark;
    /** 早班计划量。 */
    private BigDecimal dayPlanQty;
    /** 早班生产顺序。 */
    private Integer dayProduceOrder;
    /** 早班系统原因分析。 */
    private String daySysAnalysis;
    /** 早班人工原因分析。 */
    private String dayHandAnalysis;
    /** 早班示方类型。 */
    private String dayExampleType;
    /** 早班示方号。 */
    private String dayExampleNo;
    /** 早班备注。 */
    private String dayRemark;
    /** 中班计划量。 */
    private BigDecimal midPlanQty;
    /** 中班生产顺序。 */
    private Integer midProduceOrder;
    /** 中班系统原因分析。 */
    private String midSysAnalysis;
    /** 中班人工原因分析。 */
    private String midHandAnalysis;
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
    /** 对应成型四班计划量。 */
    private BigDecimal cxClass4Plan;
    /** 备注。 */
    private String remark;
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
