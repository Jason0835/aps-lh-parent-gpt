package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <p>
 * 成型排产限制表
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-16
 */
@Data
@TableName("T_CX_SCHEDULE_LIMIT")
@ApiModel(value = "CxScheduleLimit对象", description = "成型排产限制表")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class CxScheduleLimit extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID",type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "成型机台机型类型，数据来源数据字典。如一次法：1；二次法：2；")
    @TableField("MACHINE_TYPE")
    private String machineType;

    @ApiModelProperty(value = "外胎规格尺寸信息")
    @TableField("SPEC_DIMENSION")
    private BigDecimal specDimension;

    @ApiModelProperty(value = "胎胚平均库存在硫化班产数（下限）")
    @TableField("TIRE_AVG_LH_STOCK_MINIMUN")
    private BigDecimal tireAvgLhStockMinimun;

    @ApiModelProperty(value = "胎胚平均库存在硫化班产数（上限）")
    @TableField("TIRE_AVE_LH_STOCK_MAXIMUN")
    private BigDecimal tireAveLhStockMaximun;

    @ApiModelProperty(value = "最大硫化班次")
    @TableField("MAX_LH_CLASS")
    private BigDecimal maxLhClass;
}
