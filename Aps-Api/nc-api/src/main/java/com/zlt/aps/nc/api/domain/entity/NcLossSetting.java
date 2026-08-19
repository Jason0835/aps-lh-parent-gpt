package com.zlt.aps.nc.api.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 内衬损耗率设定表
 * </p>
 *
 * @author chen
 * @since 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_LOSS_SETTING")
@ApiModel(value = "NcLossSetting对象", description = "内衬损耗率设定表")
public class NcLossSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.loss.liningCode", sort = 10)
    @ApiModelProperty(value = "内衬代码")
    @TableField("LINING_CODE")
    private String liningCode;

    @ApiModelProperty(value = "机台编号")
    @TableField("MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.loss.line", sort = 20)
    @ImportExcelValidated(name = "ui.data.column.loss.line", maxLength = 30)
    @ApiModelProperty(value = "机台")
    @TableField(exist = false)
    private String machineName;

    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%", sort = 30)
    @ImportExcelValidated(name = "ui.data.column.loss.lossRate", required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "损耗率(百分比)")
    @TableField("LOSS_RATE")
    private BigDecimal lossRate;

    @Excel(name = "ui.data.column.info.remark")
    @ImportExcelValidated(name = "ui.data.column.info.remark", maxLength = 100)
    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;

}
