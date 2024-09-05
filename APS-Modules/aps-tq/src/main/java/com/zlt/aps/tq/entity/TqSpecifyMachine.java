package com.zlt.aps.tq.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 胎圈定点机台表
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TQ_SPECIFY_MACHINE")
@ApiModel(value = "TqSpecifyMachine对象", description = "胎圈定点机台表")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class TqSpecifyMachine extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "胎圈代码")
    @TableField("BEAD_CODE")
    private String beadCode;

    @ApiModelProperty(value = "机台id（对应T_TQ_MACHINE_INFO表id）")
    @TableField("MACHINE_ID")
    private Long machineId;

    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线")
    @TableField("LINE_TYPE")
    private String lineType;

    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业")
    @TableField("JOB_TYPE")
    private String jobType;
}
