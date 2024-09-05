package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 成型排程(8点-12点)完成量
 * @TableName T_CX_PART_FINISH_QTY
 */
@Data
public class TCxPartFinishQty extends ApsBaseEntity {
    /**
     * 主键ID，对应序列SEQ_CX_FINISH_QTY
     */
    private Long id;

    /**
     * 日期
     */
    private Date statDate;

    /**
     * 胎胚代码
     */
    private String embryoCode;

    /**
     * 完成量(8点-12点)
     */
    private Integer finishQty;


    /**
     * 施工信息版本
     */
    private String bomDataVersion;

    private static final long serialVersionUID = 1L;
}