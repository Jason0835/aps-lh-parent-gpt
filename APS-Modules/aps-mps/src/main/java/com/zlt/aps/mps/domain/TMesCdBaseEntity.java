package com.zlt.aps.mps.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 裁断库存
 * @TableName T_MES_CD15_STOCK
 * @TableName T_MES_CD90_STOCK
 */
@Data
public class TMesCdBaseEntity implements Serializable {
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
     * 机台编号（线边库）
     */
    private String machineCode;

    /**
     * 条码（线边库）
     */
    private String barCode;

    /**
     * sap代码
     */
    private String sapMaterialCode;

    /**
     * 库存量
     */
    private BigDecimal stockNum;

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