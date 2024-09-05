package com.zlt.aps.cx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 规格字体颜色设置表 2021-08-18添加
 * </p>
 *
 * @author chen
 * @since 2021-08-21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_CX_SPEC_COLOR")
@ApiModel(value="CxSpecColor对象", description="规格字体颜色设置表")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class CxSpecColor extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID",type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "规格型号")
    @TableField("SPEC_DESC")
    private String specDesc;

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
