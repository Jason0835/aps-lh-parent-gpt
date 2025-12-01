package com.zlt.aps.cd15.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 15度裁断钢压大卷与机台的映射表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_CD15_MACHINE_ROLL_MAPPING")
@ApiModel(value = "Cd15MachineRollMapping对象", description = "15度裁断钢压大卷与机台的映射表")
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class Cd15MachineRollMapping extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "钢压大卷编号")
    @TableField("BIG_ROLL_CODE")
    private String bigRollCode;


    @ApiModelProperty(value = "机台id（对应T_CD15_MACHINE_INFO表id）")
    @TableField("MACHINE_ID")
    private Long machineId;

}
