package com.zlt.aps.lh.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 硫化损耗率设定
 * </p>
 *
 * @author chen
 * @since 2021-07-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_LH_LOSS_SETTING")
@ApiModel(value = "LhLossSetting对象", description = "硫化损耗率设定")
@KeySequence(value = "SEQ_LOSS_SETTING", clazz = Long.class)
public class LhLossSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_LOSS_SETTING")
    @TableId(value = "ID",type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "SAP品号信息")
    @TableField("SAP_CODE")
    private String sapCode;

    @ApiModelProperty(value = "机台编号（对应T_LH_MACHINE_INFO表编号）")
    @TableField("MACHINE_CODE")
    private String machineCode;

    @ApiModelProperty(value = "损耗率")
    @TableField("LOSS_RATE")
    private Double lossRate;
}
