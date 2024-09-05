package com.zlt.aps.template.xwyy;

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
 * 纤维压延库存信息对象
 *
 * @author zlt
 * @date 2021-05-31
 */
@Data
@ApiModel(value = "纤维压延库存信息对象", description = "纤维压延库存信息对象")
public class XwyyStockTemp {

    @Excel(name = "ui.data.column.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期", position = 20)
    private String stockDate;

    @ApiModelProperty(value = "库存物料编号", position = 30)
    @Excel(name = "ui.data.column.xwyy.quota.bigRollCode")
    private String materialCode;

    @ApiModelProperty(value = "库存量", position = 40)
    @Excel(name = "ui.data.column.stock.stockNum")
    private BigDecimal stockNum;

    @ApiModelProperty(value = "修正数量", position = 50)
    @Excel(name = "ui.data.column.stock.modifyNum")
    private BigDecimal modifyNum;

    @ApiModelProperty(value = "不良数量", position = 60)
    @Excel(name = "ui.data.column.stock.badNum")
    private BigDecimal badNum;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.data.column.stock.remark")
    private String remark;
}
