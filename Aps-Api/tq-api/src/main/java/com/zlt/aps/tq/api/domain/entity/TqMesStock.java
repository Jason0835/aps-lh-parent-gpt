package com.zlt.aps.tq.api.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * MES胎圈库存中间表VO
 * 对应MES中间表 MES_TQ_STOCK
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "MES胎圈库存", description = "MES胎圈库存中间表数据")
public class TqMesStock implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 库存日期，格式：yyyy-MM-dd */
    @ApiModelProperty(value = "库存日期")
    private Date stockDate;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码")
    private String materialCode;

    /** 可用库存 */
    @ApiModelProperty(value = "可用库存")
    private BigDecimal availableStock;
}
