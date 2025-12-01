package com.zlt.aps.template.tm;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎面库存信息对象 t_tm_stock
 *
 * @author zlt
 * @date 2021-05-25
 */
@Data
@ApiModel(value = "t_tm_stock胎面库存信息对象", description = "胎面库存信息对象")
public class TmStockTemp extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;


    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期", position = 20)
    @ImportValidated(name = "ui.data.column.stock.stockDate", date = true)
    private Date stockDate;


    /**
     * 库存物料编号
     */
    @ApiModelProperty(value = "库存物料编号", position = 30)
    @Excel(name = "ui.data.column.quota.treadCode")
    @ImportValidated(name = "ui.data.column.quota.treadCode", required = true, isCode = true, maxLength = 50)
    private String materialCode;

    /**
     * 库存量
     */
    @ApiModelProperty(value = "库存量", position = 40)
    @Excel(name = "ui.data.column.stock.stockNum")
    @ImportValidated(name = "ui.data.column.stock.stockNum", number = true, min = 0, max = 999999)
    private BigDecimal stockNum;

    /**
     * 修正数量
     */
    @ApiModelProperty(value = "修正数量", position = 50)
    @Excel(name = "ui.data.column.stock.modifyNum")
    @ImportValidated(name = "ui.data.column.stock.modifyNum", number = true, min = -999999, max = 999999)
    private BigDecimal modifyNum;

    /**
     * 不良数量
     */
    @ApiModelProperty(value = "不良数量", position = 60)
    @Excel(name = "ui.data.column.stock.badNum")
    @ImportValidated(name = "ui.data.column.stock.badNum", number = true, min = 0, max = 999999)
    private BigDecimal badNum;

    /**
     * 库存量(卷)
     */
    @ApiModelProperty(value = "库存量(卷)", position = 70)
    @Excel(name = "ui.data.column.stock.rollStockNum", scale = 1)
    @ImportValidated(name = "ui.data.column.stock.rollStockNum", number = true, min = 0, max = 999999)
    private BigDecimal rollStockNum;

    /**
     * 修正数量(卷)
     */
    @ApiModelProperty(value = "修正数量(卷)", position = 80)
    @Excel(name = "ui.data.column.stock.rollModifyNum")
    @ImportValidated(name = "ui.data.column.stock.rollModifyNum", number = true, min = -999999, max = 999999)
    private BigDecimal rollModifyNum;

    /**
     * 不良数量(卷)
     */
    @ApiModelProperty(value = "不良数量(卷)", position = 90)
    @Excel(name = "ui.data.column.stock.rollBadNum")
    @ImportValidated(name = "ui.data.column.stock.rollBadNum", number = true, min = 0, max = 999999)
    private BigDecimal rollBadNum;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.data.column.stock.remark", maxLength = 300)
    private String remark;

}
