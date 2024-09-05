package com.zlt.aps.cd15.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 15度裁断损耗率设定表
 * </p>
 *
 * @author chen
 * @since 2021-07-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_CD15_LOSS_SETTING")
@ApiModel(value = "Cd15LossSetting对象", description = "15度裁断损耗率设定表")
@KeySequence(value = "SEQ_LOSS_SETTING", clazz = Long.class)
public class Cd15LossSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_LOSS_SETTING")
    @TableId(value = "ID",type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "钢带代码")
    @TableField("STEEL_STRIP_CODE")
    private String steelStripCode;

    @ApiModelProperty(value = "机台id（对应T_CD15_MACHINE_INFO表id）")
    @TableField("MACHINE_ID")
    private Long machineId;

    @ApiModelProperty(value = "损耗率(百分比)")
    @TableField("LOSS_RATE")
    private Double lossRate;

}
