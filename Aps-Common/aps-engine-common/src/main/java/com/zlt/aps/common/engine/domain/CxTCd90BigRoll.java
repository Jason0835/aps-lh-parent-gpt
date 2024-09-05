package com.zlt.aps.common.engine.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 帘布大卷信息维护表
 * @TableName T_CD90_BIG_ROLL
 */
@Data
public class CxTCd90BigRoll extends ApsBaseEntity {
    /**
     * 主键ID，对应自增序列为：SEQ_PUBLIC
     */
    private Long id;

    /**
     * 帘布大卷编号
     */
    private String bigRollCode;

    /**
     * 布卷长度。此帘布大卷一卷的最大长度，单位：米。
     */
    private BigDecimal clothLength;

    /**
     * 折合生产条数。一卷大概能生产的胎胚数量，单位：条。
     */
    private Integer convertProduceNum;

    /**
     * 实际卷取标准。此钢压大卷实际卷取的长度，单位：米。
     */
    private BigDecimal actClothLength;

    private static final long serialVersionUID = 1L;
}