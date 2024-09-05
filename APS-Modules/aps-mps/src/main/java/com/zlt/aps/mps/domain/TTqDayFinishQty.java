package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 胎圈排程计划每日各班完成量
 * @TableName T_TQ_DAY_FINISH_QTY
 */
@Data
public class TTqDayFinishQty extends ApsBaseEntity {
    /**
     * 主键ID，对应序列SEQ_FINISH_QTY
     */
    private Long id;

    /**
     * 排程时间
     */
    private Date scheduleDate;

    /**
     * 胎圈代码
     */
    private String beadCode;

    /**
     * 中班(16点-24点)完成量
     */
    private BigDecimal midFinishQty = BigDecimal.ZERO;

    /**
     * 夜班(0点-8点)完成量
     */
    private BigDecimal nightFinishQty = BigDecimal.ZERO;

    /**
     * 白班(8点-16点)完成量
     */
    private BigDecimal dayFinishQty = BigDecimal.ZERO;

    /**
     * 工单号
     */
    private String orderNo;

    private static final long serialVersionUID = 1L;
}