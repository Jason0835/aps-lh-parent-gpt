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

    /** 区间起始机台数（含） */
    @ApiModelProperty("区间起始机台数（含）")
    @ImportExcelValidated(required = true)
    @TableField("MIN_MACHINE_QTY")
    @Excel(name = "ui.data.column.cd15DepthConfig.minMachineQty")
    private Integer minMachineQty;

    /** 区间结束机台数（含，空表示无上限） */
    @ApiModelProperty("区间结束机台数（含，空表示无上限）")
    @TableField("MAX_MACHINE_QTY")
    @Excel(name = "ui.data.column.cd15DepthConfig.maxMachineQty")
    private Integer maxMachineQty;

    /** 备库班数 */
    @ApiModelProperty("备库班数")
    @ImportExcelValidated(required = true)
    @TableField("DEPTH_CLASS_QTY")
    @Excel(name = "ui.data.column.cd15DepthConfig.depthClassQty")
    private BigDecimal depthClassQty;
}
