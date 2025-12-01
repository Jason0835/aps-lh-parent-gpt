package com.zlt.aps.xwyy.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 纤维压延预生产库存倍数配置对象 t_xwyy_reserve_stock
 * 
 * @author hak
 * @date 2025-02-11
 */
@Data
@ApiModel(value = "纤维压延预生产库存倍数配置对象", description = "纤维压延预生产库存倍数配置对象 ")
public class XwyyReserveStockDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    private Long id;

    /** 纤维大卷编号 */
    @Excel(name = "ui.data.column.xwyy.reserveStock.bigRollCode", sort = 10)
    @ApiModelProperty(value = "纤维大卷编号")
    @ImportValidated(name = "ui.data.column.xwyy.reserveStock.bigRollCode", required = true, isCode = true, maxLength = 20)
    private String bigRollCode;

    /** 预生产库存倍数 */
    @Excel(name = "ui.data.column.xwyy.reserveStock.reserveStockRate", sort = 20)
    @ApiModelProperty(value = "预生产库存倍数")
    @ImportValidated(name = "ui.data.column.xwyy.reserveStock.reserveStockRate", required = true, number = true, min = 0, max = 9999999)
    private BigDecimal reserveStockRate;

    @Excel(name = "ui.common.column.remark", sort = 30)
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
