package com.zlt.aps.template.cx;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 成型排产限制对象 cx_schedule_limit
 *
 * @author zlt
 * @date 2021-06-16
 */
@Data
@ApiModel(value = "CxScheduleLimit对象", description = "成型排产限制信息")
public class CxScheduleLimitTemp extends ApsBaseDto {
    private static final long serialVersionUID = 1L;


    /** 成型机台机型类型，数据来源数据字典。如一次法：1；二次法：2； */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.cx.machine.type",dictType = "MACHINE_TYPE")
    @ApiModelProperty(value = "成型机台机型类型", position = 20)
    private String machineType;

    /**
     * 外胎规格尺寸信息
     */
    @ImportValidated(required = true, min = 0, max = 9999999, number = true)
    @Excel(name = "ui.data.column.cx.limit.specDimension")
    @ApiModelProperty(value = "外胎规格尺寸信息", position = 30)
    private BigDecimal specDimension;

    /**
     * 胎胚平均库存在硫化班产数（下限）
     */
    @ImportValidated(required = true, min = 0, max = 99, number = true)
    @Excel(name = "ui.data.column.cx.limit.tireAvgLhStockMinimun", width = 32)
    @ApiModelProperty(value = "胎胚平均库存在硫化班产数(下限)", position = 40)
    private BigDecimal tireAvgLhStockMinimun;

    /**
     * 胎胚平均库存在硫化班产数（上限）
     */
    @ImportValidated(required = true, min = 0, max = 99, number = true)
    @Excel(name = "ui.data.column.cx.limit.tireAveLhStockMaximun", width = 32)
    @ApiModelProperty(value = "胎胚平均库存在硫化班产数(上限)", position = 50)
    private BigDecimal tireAveLhStockMaximun;

    /**
     * 最大硫化班次
     */
    @ImportValidated(required = true, min = 0, max = 99, number = true)
    @Excel(name = "ui.data.column.cx.limit.maxLhClass")
    @ApiModelProperty(value = "最大硫化班次", position = 60)
    private BigDecimal maxLhClass;

    /**
     * 备注
     */
    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
