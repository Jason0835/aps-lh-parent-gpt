package com.zlt.aps.cx.api.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 成型月结库存信息对象 t_cx_month_stock
 *
 * @author chen
 * @date 2021-06-17
 */
@Data
@ApiModel(value = "CxMonthStockDto对象", description = "成型月结库存信息")
public class CxMonthStockDto extends ApsBaseDto {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 库存所属月份：yyyy-MM
     */
    @JsonFormat(pattern = "yyyy-MM")
    @ImportValidated(required = true,date = true)
    @Excel(name = "ui.data.column.cx.monthStock.stockMonth", width = 30, dateFormat = "yyyy-MM")
    @ApiModelProperty(value = "库存所属月份", position = 10)
    private Date stockMonth;

    /**
     * 施工版本信息
     */
    @Excel(name = "ui.data.column.productStatus.bomDataVersion")
    @ImportValidated(required = true,maxLength = 30)
    private  String bomDataVersion;
    /**
     * 胎胚代码
     */
    @ImportValidated(isCode = true, maxLength = 30, required = true)
    @Excel(name = "ui.data.column.cx.monthStock.embryoCode")
    @ApiModelProperty(value = "胎胚代码", position = 20)
    private String embryoCode;

    /**
     * 库存量
     */
    @ImportValidated(min = 0, max = 999999999, digits = true, required = true)
    @Excel(name = "ui.data.column.cx.monthStock.stockNum")
    @ApiModelProperty(value = "库存量", position = 30)
    private String stockNum;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 300)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

    //接收前端查询条件开始日期
    @ApiModelProperty(value = "查询条件开始月份", position = 40)
    private String beginStockMonth;

    //接收前端查询条件结束日期
    @ApiModelProperty(value = "查询条件结束月份", position = 50)
    private String endStockMonth;


}
