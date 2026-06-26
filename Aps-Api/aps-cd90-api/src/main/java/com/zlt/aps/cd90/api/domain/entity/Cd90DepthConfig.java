package com.zlt.aps.cd90.api.domain.entity;

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

@Data
@ApiModel(value = "直裁备库班数与供成型机数配置", description = "直裁备库班数与供成型机数配置")
@TableName("t_cd90_depth_config")
public class Cd90DepthConfig extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @ApiModelProperty("工厂编码")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd90DepthConfig.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /**
     * 供成型机台数（成型工序生产某直裁胎体规格所使用的机台数量）
     */
    @ApiModelProperty("供成型机台数")
    @ImportExcelValidated(required = true)
    @TableField("MACHINE_QTY")
    @Excel(name = "ui.data.column.cd90DepthConfig.machineQty")
    private Integer machineQty;

    /**
     * 机台范围（数据字典machine_range，选项：LT-小于、LE-小于等于、EQ-等于、GE-大于等于、GT-大于）
     */
    @ApiModelProperty("机台范围")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("MACHINE_RANGE")
    @Excel(name = "ui.data.column.cd90DepthConfig.machineRange", dictType = "machine_range")
    private String machineRange;

    /**
     * 备库班数（该机台数范围对应的排产深度/供应窗口班次数）
     */
    @ApiModelProperty("备库班数")
    @ImportExcelValidated(required = true)
    @TableField("DEPTH_CLASS_QTY")
    @Excel(name = "ui.data.column.cd90DepthConfig.depthClassQty")
    private BigDecimal depthClassQty;
}