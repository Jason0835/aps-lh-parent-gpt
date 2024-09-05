package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 成品库存
 * @TableName T_SAP_STOCK
 */
@Data
public class TSapStock extends ApsBaseEntity {
    /**
     * 主键ID，对应自增序列为：SEQ_NC_STOCK
     */
    private Long id;

    /**
     * 库存日期，格式：yyyy-MM-dd
     */
    private Date stockDate;

    /**
     * SAP品号，外胎物料编号
     */
    private String sapCode;

    /**
     * 库存量
     */
    private Integer stockNum;

    private static final long serialVersionUID = 1L;
}