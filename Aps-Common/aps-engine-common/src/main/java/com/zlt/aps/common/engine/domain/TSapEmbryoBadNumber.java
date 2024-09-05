package com.zlt.aps.common.engine.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * SAP胎胚不良数
 * @TableName T_SAP_EMBRYO_BAD_NUMBER
 */
@Data
public class TSapEmbryoBadNumber extends ApsBaseEntity {
    /**
     * 主键ID，对应自增序列为：SEQ_NC_STOCK
     */
    private Long id;

    /**
     * 不良日期，格式：yyyy-MM-dd
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date badDate;

    /**
     * SAP品号，外胎物料编号
     */
    private String sapCode;

    /**
     * 胎胚代码
     */
    private String embryoCode;

    /**
     * 不良数
     */
    private Integer badNum;

    /**
     * 施工版本
     */
    private String bomDataVersion;

    private static final long serialVersionUID = 1L;
}