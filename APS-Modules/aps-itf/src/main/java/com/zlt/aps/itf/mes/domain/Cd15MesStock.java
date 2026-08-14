package com.zlt.aps.itf.mes.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MES斜裁库存对象查询模型，映射MES_CD15_STOCK。
 */
@Data
public class Cd15MesStock implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 库存日期。 */
    private Date stockDate;
    /** 物料编码。 */
    private String materialCode;
    /** 库存量。 */
    private BigDecimal stockNum;
    /** 可用库存。 */
    private BigDecimal availableStock;
    /** 数据版本。 */
    private String dataVersion;
}
