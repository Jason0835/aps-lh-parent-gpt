package com.zlt.aps.gsq.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;

/**
 * <p>
 * 钢丝圈颜色提示信息表
 * </p>
 *
 */
@Data
@Getter
@TableName("T_GSQ_STEEL_TYPE_COLOR")
@ApiModel(value = "GsqSteelTypeColor对象", description = "钢带大卷颜色提示信息表")
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class GsqSteelTypeColor extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "钢丝类型")
    @TableField("STEEL_TYPE")
    private String steelType;

    @ApiModelProperty(value = "颜色类型；0-字体颜色，1-背景颜色（对应数据字典BIG_ROLL_COLOR）")
    @TableField("COLOR_TYPE")
    private String colorType;

    @ApiModelProperty(value = "颜色代码，例如：#000000")
    @TableField("COLOR_CODE")
    private String colorCode;

    @ApiModelProperty(value = "状态，0--启用，1--禁用。")
    @TableField("STATUS")
    private String status;
}
