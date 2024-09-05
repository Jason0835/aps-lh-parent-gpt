package com.zlt.aps.nc.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 内衬损耗率设定表
 * </p>
 *
 * @author chen
 * @since 2021-07-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_LOSS_SETTING")
@ApiModel(value = "NcLossSetting对象", description = "内衬损耗率设定表")
@KeySequence(value = "SEQ_LOSS_SETTING", clazz = Long.class)
public class NcLossSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_LOSS_SETTING")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "内衬代码")
    @TableField("LINING_CODE")
    private String liningCode;

    @ApiModelProperty(value = "机台id（对应T_NC_MACHINE_INFO表id）")
    @TableField("MACHINE_ID")
    private Long machineId;

    @ApiModelProperty(value = "损耗率(百分比)")
    @TableField("LOSS_RATE")
    private Double lossRate;

}
