package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 斜裁备库班数与供成型机数配置。
 */
@Data
@ApiModel(value = "斜裁备库班数与供成型机数配置", description = "斜裁备库班数与供成型机数配置")
@TableName("t_cd15_depth_config")
public class Cd15DepthConfig extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty("工厂编码")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15DepthConfig.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 供成型机台数 */
    @ApiModelProperty("供成型机台数")
    @ImportExcelValidated(required = true)
    @TableField("MACHINE_QTY")
    @Excel(name = "ui.data.column.cd15DepthConfig.machineQty")
    private Integer machineQty;

    /** 机台范围 */
    @ApiModelProperty("机台范围")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("MACHINE_RANGE")
    @Excel(name = "ui.data.column.cd15DepthConfig.machineRange", dictType = "machine_range")
    private String machineRange;

    /** 备库班数 */
    @ApiModelProperty("备库班数")
    @ImportExcelValidated(required = true)
    @TableField("DEPTH_CLASS_QTY")
    @Excel(name = "ui.data.column.cd15DepthConfig.depthClassQty")
    private BigDecimal depthClassQty;
}
