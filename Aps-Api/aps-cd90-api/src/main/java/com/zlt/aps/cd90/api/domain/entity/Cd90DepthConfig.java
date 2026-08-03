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
     * 区间起始机台数（含）
     */
    @ApiModelProperty("区间起始机台数（含）")
    @ImportExcelValidated(required = true)
    @TableField("MIN_MACHINE_QTY")
    @Excel(name = "ui.data.column.cd90DepthConfig.minMachineQty")
    private Integer minMachineQty;

    /**
     * 区间结束机台数（含），空表示无上限
     */
    @ApiModelProperty("区间结束机台数（含），空表示无上限")
    @TableField("MAX_MACHINE_QTY")
    @Excel(name = "ui.data.column.cd90DepthConfig.maxMachineQty")
    private Integer maxMachineQty;

    /**
     * 备库班数（该机台数范围对应的排产深度/供应窗口班次数）
     */
    @ApiModelProperty("备库班数")
    @ImportExcelValidated(required = true)
    @TableField("DEPTH_CLASS_QTY")
    @Excel(name = "ui.data.column.cd90DepthConfig.depthClassQty")
    private BigDecimal depthClassQty;
}
