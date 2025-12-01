package com.zlt.aps.tc.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 胎侧损耗率设定表
 * </p>
 *
 * @author chen
 * @since 2021-07-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TC_LOSS_SETTING")
@ApiModel(value = "TcLossSetting对象", description = "胎侧损耗率设定表")
//@KeySequence(value = "SEQ_LOSS_SETTING",dbType = DbType.ORACLE)
public class TcLossSetting extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_LOSS_SETTING")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "胎侧代码")
    @TableField("SIDEWALL_CODE")
    private String sidewallCode;

    @ApiModelProperty(value = "机台id（对应T_TC_MACHINE_INFO表id）")
    @TableField("MACHINE_ID")
    private Long machineId;

    @ApiModelProperty(value = "损耗率(百分比)")
    @TableField("LOSS_RATE")
    private Double lossRate;

}
