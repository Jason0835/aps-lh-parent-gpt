package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 胎侧备库班数配置
 *
 * @author zlt
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TC_DEPTH_CONFIG")
@ApiModel(value = "TcDepthConfig对象", description = "胎侧备库班数配置")
public class TcDepthConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @Excel(name = "ui.tc.depthConfig.column.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 区间起始硫化机数量（包含），同一工厂首段必须从 1 开始
     */
    @Excel(name = "ui.tc.depthConfig.column.minMachineQty")
    @ApiModelProperty(value = "区间起始硫化机数量（包含）")
    @TableField("MIN_MACHINE_QTY")
    private Integer minMachineQty;

    /**
     * 区间结束硫化机数量（包含），为空表示无上限且只允许用于末段
     */
    @Excel(name = "ui.tc.depthConfig.column.maxMachineQty")
    @ApiModelProperty(value = "区间结束硫化机数量（包含），为空表示无上限")
    @TableField("MAX_MACHINE_QTY")
    private Integer maxMachineQty;

    /**
     * 保证班数
     */
    @Excel(name = "ui.tc.depthConfig.column.depthClassQty")
    @ApiModelProperty(value = "保证班数")
    @TableField("DEPTH_CLASS_QTY")
    private BigDecimal depthClassQty;

}
