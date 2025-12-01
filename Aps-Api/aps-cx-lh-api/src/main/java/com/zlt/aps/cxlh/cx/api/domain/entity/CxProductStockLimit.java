package com.zlt.aps.cxlh.cx.api.domain.entity;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 成型投产班次库存限定设置对象 t_cx_product_stock_limit
 * 
 * @author zlt
 * @date 2022-01-07
 */
@ApiModel(value = "成型投产班次库存限定设置对象", description = "成型投产班次库存限定设置对象 ")
@Data
public class CxProductStockLimit extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 限制名称 */
    @Excel(name = "ui.data.column.shiftLimit.limitName")
    @ApiModelProperty(value = "限制名称")
    private String limitName;

    /** 胎胚类型 */
    @Excel(name = "ui.data.column.shiftLimit.type")
    @ApiModelProperty(value = "胎胚类型")
    private String type;

    /** 限制类型 */
    @Excel(name = "ui.data.column.shiftLimit.limitType")
    @ApiModelProperty(value = "限制类型")
    private String limitType;

    /** 库存数 */
    @Excel(name = "ui.data.column.shiftLimit.stockNum")
    @ApiModelProperty(value = "库存数")
    private Long stockNum;

    /** 班数 */
    @Excel(name = "ui.data.column.shiftLimit.shiftParams")
    @ApiModelProperty(value = "班数")
    private Double shiftParams;

    /** 删除标识（0未删除；1已删除） */
    @ApiModelProperty(value = "班数")
    private String delFlag;





}
