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
@TableName("T_NC_DEPTH_CONFIG")
@ApiModel(value = "NcDepthConfig对象", description = "内衬备库班数与供成型机数配置")
public class NcDepthConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 区间起始机台数（含），第1条必须为 1
     */
    @Excel(name = "ui.dj.depthConfig.column.minMachineQty")
    @ApiModelProperty(value = "区间起始机台数（含）")
    @TableField("MIN_MACHINE_QTY")
    private Integer minMachineQty;

    /**
     * 区间结束机台数（含），NULL 表示无上限（仅末行允许）
     */
    @Excel(name = "ui.dj.depthConfig.column.maxMachineQty")
    @ApiModelProperty(value = "区间结束机台数（含），NULL 表示无上限")
    @TableField("MAX_MACHINE_QTY")
    private Integer maxMachineQty;

    /**
     * 内衬备库班数（该机台数范围对应的排产深度/供应窗口班次数）
     */
    @Excel(name = "ui.dj.depthConfig.column.depthClassQty")
    @ApiModelProperty(value = "内衬备库班数")
    @TableField("DEPTH_CLASS_QTY")
    private BigDecimal depthClassQty;
}
