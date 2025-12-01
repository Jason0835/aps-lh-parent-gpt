package com.zlt.aps.gsq.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 钢丝圈预生产库存倍数配置对象 t_gsq_reserve_stock
 * 
 * @author hak
 * @date 2025-02-11
 */
@Data
@ApiModel(value = "钢丝圈预生产库存倍数配置对象", description = "钢丝圈预生产库存倍数配置对象 ")
public class GsqReserveStockDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    private Long id;

    /** 钢丝圈代码 */
    @Excel(name = "ui.data.column.gsq.reserveStock.steelRingCode", sort = 10)
    @ApiModelProperty(value = "钢丝圈代码")
    @ImportValidated(name = "ui.data.column.gsq.reserveStock.steelRingCode", required = true, isCode = true, maxLength = 20)
    private String steelRingCode;

    /** 预生产库存倍数 */
    @Excel(name = "ui.data.column.gsq.reserveStock.reserveStockRate", sort = 20)
    @ApiModelProperty(value = "预生产库存倍数")
    @ImportValidated(name = "ui.data.column.gsq.reserveStock.reserveStockRate", required = true, number = true, min = 0, max = 9999999)
    private BigDecimal reserveStockRate;

    @Excel(name = "ui.common.column.remark", sort = 30)
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
