package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 硫化排程各班次完成量回报接口
 * @TableName T_MES_LH_SHIFT_FINISH_QTY
 */
@Data
public class TMesLhShiftFinishQty implements Serializable {
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
    private String lhMachineCode;

    /**
     * SAP品号
     */
    private String sapCode;

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