package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * MES基础库存
 * @TableName T_MES_*_STOCK
 */
@Data
public class TMesStockBaseEntity implements Serializable {
    /**
     * 主键ID，对应自增序列为：SEQ_MES_STOCK
     */
    private Long id;

    /**
     * 库存日期
     */
    private Date stockDate;

    /**
     * 库存物料编号
     */
    private String materialCode;

    /**
     * 胎面代码
     */
    private String sapMaterialCode;

    /**
     * 库存量
     */
    private BigDecimal claimNum;

    /**
     * 未领用库存量
     */
    private BigDecimal unClaimNum;

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
     * 可用库存
     */
    private BigDecimal availableStock;

    /**
     * 删除标识：0--正常，1-删除
     */
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}