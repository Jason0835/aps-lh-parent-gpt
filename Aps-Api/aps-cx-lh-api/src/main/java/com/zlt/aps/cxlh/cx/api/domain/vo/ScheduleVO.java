package com.zlt.aps.cxlh.cx.api.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 排程视图对象
 */
@Data
public class ScheduleVO {

    /** 成型批次号 */
    private String cxBatchNo;

    /** 排程日期 */
    private Date scheduleDate;

    /** 结构编码 */
    private String structureCode;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 胎胚编码 */
    private String embryoCode;

    /** 成型机编码 */
    private String cxMachineCode;

    /** 成型机名称 */
    private String cxMachineName;

    /** 早班计划量 */
    private BigDecimal morningQty;

    /** 中班计划量 */
    private BigDecimal afternoonQty;

    /** 夜班计划量 */
    private BigDecimal nightQty;

    /** 总计划量 */
    private BigDecimal totalQty;

    /** 早班顺位 */
    private Integer morningOrder;

    /** 中班顺位 */
    private Integer afternoonOrder;

    /** 夜班顺位 */
    private Integer nightOrder;

    /** 优先级 */
    private Integer priority;

    /** 试制标识 */
    private Boolean trialFlag;

    /** 紧急收尾标识 */
    private Boolean urgentFinish;

    /** 库存可供时长 */
    private BigDecimal inventoryDuration;

    /** 生产状态 */
    private String productionStatus;

    /** 胎面预警标识 */
    private Boolean treadWarning;
}
