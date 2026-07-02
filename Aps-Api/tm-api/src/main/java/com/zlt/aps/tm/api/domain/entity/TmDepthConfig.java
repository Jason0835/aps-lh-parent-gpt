package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 备库班数配置
 *
 * @author zlt
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TM_DEPTH_CONFIG")
@ApiModel(value = "TmDepthConfig对象", description = "备库班数配置")
public class TmDepthConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @Excel(name = "ui.tm.depthConfig.column.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 硫化机数量
     */
    @Excel(name = "ui.tm.depthConfig.column.machineQty")
    @ApiModelProperty(value = "硫化机数量")
    @TableField("MACHINE_QTY")
    private Integer machineQty;

    /**
     * 机台范围（数据字典 machine_range，选项：小于、小于等于、等于、大于等于、大于）
     */
    @Excel(name = "ui.tm.depthConfig.column.machineRange", dictType = "machine_range")
    @ApiModelProperty(value = "机台范围，数据字典 machine_range")
    @TableField("MACHINE_RANGE")
    private String machineRange;

    /**
     * 保证班数
     */
    @Excel(name = "ui.tm.depthConfig.column.depthClassQty")
    @ApiModelProperty(value = "保证班数")
    @TableField("DEPTH_CLASS_QTY")
    private BigDecimal depthClassQty;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
