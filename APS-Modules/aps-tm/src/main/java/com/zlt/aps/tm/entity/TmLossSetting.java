package com.zlt.aps.tm.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 胎面损耗率设定表
 * </p>
 *
 * @author chen
 * @since 2021-07-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TM_LOSS_SETTING")
@ApiModel(value = "TmLossSetting对象", description = "胎面损耗率设定表")
@KeySequence(value = "SEQ_LOSS_SETTING", clazz = Long.class)
public class TmLossSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_LOSS_SETTING")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "胎面代码")
    @TableField("TREAD_CODE")
    private String treadCode;

    @ApiModelProperty(value = "机台id（对应T_TM_MACHINE_INFO表id）")
    @TableField("MACHINE_ID")
    private Long machineId;

    @ApiModelProperty(value = "损耗率")
    @TableField("LOSS_RATE")
    private Double lossRate;

}
