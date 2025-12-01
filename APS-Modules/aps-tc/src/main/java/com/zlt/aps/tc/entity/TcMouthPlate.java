package com.zlt.aps.tc.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 胎侧口型板信息维护
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TC_MOUTH_PLATE")
@ApiModel(value = "TcMouthPlate对象", description = "胎侧口型板信息维护")
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class TcMouthPlate extends ApsBaseEntity {

    private static final long serialVersionUID = 1110056585174675867L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "口型板编号。一个口型板编号可以对应多个机台。", position = 20)
    @TableField("MOUTH_PLATE_CODE")
    private String mouthPlateCode;

    @ApiModelProperty(value = "机台id", position = 30)
    @TableField("MACHINE_ID")
    private Long machineId;

    @ApiModelProperty(value = "状态，0--启用，1--禁用。", position = 40)
    @TableField("STATUS")
    private String status;

    @TableField(exist = false)
    @ApiModelProperty(value = "机台名称", position = 50)
    private String machineName;
}
