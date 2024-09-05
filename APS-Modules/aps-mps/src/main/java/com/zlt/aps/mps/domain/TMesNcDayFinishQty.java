package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 内衬每日完成量回报
 * @TableName T_MES_NC_DAY_FINISH_QTY
 */
@Data
public class TMesNcDayFinishQty implements Serializable {
    /**
     * 主键ID，对应序列SEQ_MES_FINISH_QTY
     */
    private Long id;

    /**
     * 工单号
     */
    private String orderNo;

    /**
     * 排程时间
     */
    private Date scheduleDate;

    /**
     * 内衬代码
     */
    private String liningCode;

    /**
     * 胎面代码
     */
    private String sapMaterialCode;

    /**
     * 中班(12点-24点)完成量
     */
    private BigDecimal dayFinishQty = BigDecimal.ZERO;

    /**
     * 夜班(0点-12点)完成量
     */
    private BigDecimal nightFinishQty = BigDecimal.ZERO;

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