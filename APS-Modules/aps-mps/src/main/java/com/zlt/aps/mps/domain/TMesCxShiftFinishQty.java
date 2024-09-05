package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 成型排程各班次完成量回报接口
 * @TableName T_MES_CX_SHIFT_FINISH_QTY
 */
@Data
public class TMesCxShiftFinishQty implements Serializable {
    /**
     * 主键ID，对应序列SEQ_CX_FINISH_QTY
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
     * 一班(中班)完成量
     */
    private Integer sapClass1FinishQty = 0;

    /**
     * 二班(夜班)完成量
     */
    private Integer sapClass2FinishQty = 0;

    /**
     * 三班(白班)完成量
     */
    private Integer sapClass3FinishQty = 0;

    /**
     * 四班(次日一班)完成量
     */
    private Integer sapClass4FinishQty = 0;

    /**
     * 五班(次日二班)完成量
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

    /**
     * 备注
     */
    private String remark;

    /**
     * 版本号
     */
    private String dataVersion;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 删除标识：0--正常，1-删除
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}