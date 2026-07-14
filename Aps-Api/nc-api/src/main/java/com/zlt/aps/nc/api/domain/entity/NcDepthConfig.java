package com.zlt.aps.nc.api.domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 内衬备库班数与供成型机数配置
 * </p>
 *
 * @author zlt
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_DJ_DEPTH_CONFIG")
@ApiModel(value = "DjDepthConfig对象", description = "内衬备库班数与供成型机数配置")
public class NcDepthConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 供成型机台数（成型工序生产某内衬规格所使用的机台数量）
     */
    @Excel(name = "ui.dj.depthConfig.column.machineQty")
    @ApiModelProperty(value = "供成型机台数")
    @TableField("MACHINE_QTY")
    private Integer machineQty;

    /**
     * 机台范围（数据字典 machine_range，选项：小于、小于等于、等于、大于等于、大于）
     */
    @Excel(name = "ui.dj.depthConfig.column.machineRange", dictType = "machine_range")
    @ApiModelProperty(value = "机台范围，数据字典 machine_range")
    @TableField("MACHINE_RANGE")
    private String machineRange;

    /**
     * 内衬备库班数（该机台数范围对应的排产深度/供应窗口班次数）
     */
    @Excel(name = "ui.dj.depthConfig.column.depthClassQty")
    @ApiModelProperty(value = "内衬备库班数")
    @TableField("DEPTH_CLASS_QTY")
    private BigDecimal depthClassQty;
}
