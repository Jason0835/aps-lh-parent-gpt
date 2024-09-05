package com.zlt.aps.template.cx;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * @ClassName CxStock
 * @Description TODO
 * @Author Joran.Zhang
 * @Date ${Date} ${Time}
 * @Version 1.0
 **/
@Data
@TableName("T_CX_STOCK")
@ApiModel(value = "CxStock对象", description = "成型库存信息")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class CxStockTemp extends ApsBaseEntity {

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ImportValidated(required = true, date = true)
    @Excel(name = "ui.data.column.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期")
    @TableField("STOCK_DATE")
    private Date stockDate;

    @Excel(name = "ui.data.column.productStatus.bomDataVersion")
    @TableField("bom_Data_Version")
    @ImportValidated(required = true,maxLength = 30)
    private  String bomDataVersion;


    @ApiModelProperty(value = "胎胚代码")
    @TableField("EMBRYO_CODE")
    @ImportValidated(required = true, maxLength = 50, isCode = true)
    @Excel(name = "ui.data.column.stock.embryoCode")
    private String embryoCode;
    
    /**
     * 排程使用库存
     */
    @Excel(name = "ui.data.column.stock.scheduleUseStock")
    private Long scheduleUseStock;

    @ApiModelProperty(value = "库存量(可用)")
    @TableField("STOCK_NUM")
    @ImportValidated(required = true, number = true, min = 0, max = 999999)
    @Excel(name = "ui.data.column.stock.stockNumAvailable")
    private Long stockNum;

    @ApiModelProperty(value = "不可用库存")
    @TableField("UNAVAILABLE_STOCK")
    @ImportValidated(digits = true, min = 0, max = 999999)
    @Excel(name = "ui.data.column.stock.unavailableStock")
    private Long unavailableStock;

    @ApiModelProperty(value = "修正数量")
    @TableField("MODIFY_NUM")
    @ImportValidated(number = true, min = -999999, max = 999999)
    @Excel(name = "ui.data.column.stock.modifyNum")
    private Long modifyNum;

    @ApiModelProperty(value = "不良数量")
    @TableField("BAD_NUM")
    @ImportValidated(number = true, min = 0, max = 999999)
    @Excel(name = "ui.data.column.stock.badNum")
    private String badNum;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.data.column.stock.remark")
    private String remark;

}
