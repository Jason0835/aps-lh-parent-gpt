package com.zlt.aps.xwyy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 纤维压延损耗率设定表
 * </p>
 *
 * @author chen
 * @since 2021-07-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_XWYY_LOSS_SETTING")
@ApiModel(value = "XwyyLossSetting对象", description = "纤维压延损耗率设定表")
@KeySequence(value = "SEQ_LOSS_SETTING", clazz = Long.class)
public class XwyyLossSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_LOSS_SETTING")
    @TableId(value = "ID",type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "帘布大卷编号")
    @TableField("BIG_ROLL_CODE")
    private String bigRollCode;

    @ApiModelProperty(value = "机台id")
    @TableField("MACHINE_ID")
    private Long machineId;

    @ApiModelProperty(value = "损耗率(百分比)")
    @TableField("LOSS_RATE")
    private Double lossRate;
}
