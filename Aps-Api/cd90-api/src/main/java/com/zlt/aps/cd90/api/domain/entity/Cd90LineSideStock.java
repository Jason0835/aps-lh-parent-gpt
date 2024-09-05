package com.zlt.aps.cd90.api.domain.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 90°裁断线边库存信息对象
 *
 * @author hak
 * @date 2023-03-03
 */
@Data
@EqualsAndHashCode(callSuper=false)
@ApiModel(value = "90°裁断线边库存信息对象", description = "90°裁断线边库存信息对象")
public class Cd90LineSideStock extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_CD90_LINE_SIDE_STOCK
     */
    @ApiModelProperty(value = "主键ID", position = 10)
    private Long id;

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期", position = 20)
    @ImportValidated(required = true, date = true)
    private Date stockDate;

    @ApiModelProperty(value = "查询库存的开始日期yyyy-MM-dd", position = 21)
    private String startTime;

    @ApiModelProperty(value = "查询库存的结束日期yyyy-MM-dd", position = 22)
    private String endTime;

    /**
     * 大卷条码
     */
    @ApiModelProperty(value = "库存物料编号", position = 30)
    @Excel(name = "ui.data.column.badStock.roll.barcode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String barCode;

    /**
     * 库存物料编号
     */
    @ApiModelProperty(value = "库存物料编号", position = 30)
    @Excel(name = "ui.data.column.badStock.materialCode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String materialCode;

    /**
     * 机台编号
     */
    @ApiModelProperty(value = "机台编号", position = 40)
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    private String machineCode;
    
    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.machine.machineName")
    private String machineName;

    /**
     * 库存量
     */
    @ApiModelProperty(value = "库存量", position = 50)
    @Excel(name = "ui.data.column.stock.stockNum")
    @ImportValidated(required = true, number = true, min = 0, max = 999999)
    private BigDecimal stockNum;
}
