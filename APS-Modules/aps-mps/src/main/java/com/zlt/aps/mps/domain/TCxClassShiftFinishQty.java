package com.zlt.aps.mps.domain;

import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 成型排程各班次完成量
 * @TableName T_CX_CLASS_SHIFT_FINISH_Qty
 */
@Data
public class TCxClassShiftFinishQty extends ApsBaseEntity {
    /**
     * 主键ID，对应序列SEQ_CX_FINISH_Qty
     */
    private Long id;

    /**
     * 成型排程工单号，自动生成，批次号+4位定长自增序号
     */
    private String orderNo;

    /**
     * 排程日期
     */
    private Date scheduleDate;

    /**
     * 成型机台编号
     */
    private String cxMachineCode;

    /**
     * SAP品号
     */
    private String sapCode;

    /**
     * SAP一班(中班)完成量
     */
    private Integer sapClass1FinishQty = 0;

    /**
     * SAP二班(中班)完成量
     */
    private Integer sapClass2FinishQty = 0;

    /**
     * SAP三班(中班)完成量
     */
    private Integer sapClass3FinishQty = 0;

    /**
     * SAP四班(次日一班)完成量
     */
    private Integer sapClass4FinishQty = 0;

    /**
     * SAP五班(次日一班)完成量
     */
    private Integer sapClass5FinishQty = 0;

    /**
     * 胎胚代码
     */
    private String embryoCode;

    /**
     * 一班(中班)完成量
     */
    private Integer class1FinishQty = 0;

    /**
     * 二班(夜班)完成量
     */
    private Integer class2FinishQty = 0;

    /**
     * 三班(白班)完成量
     */
    private Integer class3FinishQty = 0;

    /**
     * 四班(次日一班)完成量
     */
    private Integer class4FinishQty = 0;

    /**
     * 五班(次日二班)完成量
     */
    private Integer class5FinishQty = 0;



    private static final long serialVersionUID = 1L;
}