package com.zlt.aps.tm.api.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MES胎面库存中间表VO
 * 对应MES中间表 MES_TM_STOCK
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "MES胎面库存", description = "MES胎面库存中间表数据")
public class TmMesStock implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 库存日期，格式：yyyy-MM-dd */
    @ApiModelProperty(value = "库存日期")
    private Date stockDate;

    /** 物料编码（胎面编码） */
    @ApiModelProperty(value = "物料编码")
    private String materialCode;

    /** 可用库存 */
    @ApiModelProperty(value = "可用库存")
    private BigDecimal availableStock;

    /** 版本号 */
    @ApiModelProperty(value = "版本号")
    private String dataVersion;

    /** 分公司编码 */
    @ApiModelProperty(value = "分公司编码")
    private String companyCode;

    /** 分厂编码 */
    @ApiModelProperty(value = "分厂编码")
    private String factoryCode;
}
