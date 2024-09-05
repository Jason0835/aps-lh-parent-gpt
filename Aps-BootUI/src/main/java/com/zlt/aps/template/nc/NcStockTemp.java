package com.zlt.aps.template.nc;

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
 * 内衬库存信息对象
 *
 * @author zlt
 * @date 2021-05-31
 */
@Data
@ApiModel(value = "内衬库存信息对象", description = "内衬库存信息对象")
public class NcStockTemp extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;


    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportValidated(name = "ui.data.column.stock.stockDate", required = true, date = true)
    @ApiModelProperty(value = "库存日期", position = 20)
    private Date stockDate;


    /**
     * 库存物料编号
     */
    @ApiModelProperty(value = "库存物料编号", position = 30)
    @Excel(name = "ui.data.column.quota.liningCode")
    @ImportValidated(name = "ui.data.column.quota.liningCode", required = true, maxLength = 50, isCode = true)
    private String materialCode;

    /**
     * 库存量
     */
    @ApiModelProperty(value = "库存量", position = 40)
    @Excel(name = "ui.data.column.stock.stockNum")
    @ImportValidated(name = "ui.data.column.stock.stockNum", required = true, number = true, min = 0, max = 999999)
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

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.data.column.stock.remark", maxLength = 300)
    private String remark;

}
