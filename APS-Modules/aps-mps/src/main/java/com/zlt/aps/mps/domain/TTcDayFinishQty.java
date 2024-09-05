package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 胎侧排程计划每日各班完成量
 * @TableName T_TC_DAY_FINISH_QTY
 */
@Data
public class TTcDayFinishQty extends ApsBaseEntity {
    /**
     * 主键ID，对应序列SEQ_FINISH_QTY
     */
    private Long id;

    /**
     * 排程时间
     */
    private Date scheduleDate;

    /**
     * 胎侧代码
     */
    private String sidewallCode;

    /**
     * 中班(12点-24点)完成量
     */
    private BigDecimal dayFinishQty = BigDecimal.ZERO;

    /**
     * 夜班(0点-12点)完成量
     */
    private BigDecimal nightFinishQty = BigDecimal.ZERO;

    /**
     * 工单号
     */
    private String orderNo;

    private static final long serialVersionUID = 1L;
}