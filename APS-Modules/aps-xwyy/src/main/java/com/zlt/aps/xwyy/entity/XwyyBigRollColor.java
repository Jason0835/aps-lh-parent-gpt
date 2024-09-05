package com.zlt.aps.xwyy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.Date;

import static org.apache.ibatis.type.JdbcType.DATE;

/**
 * <p>
 * 帘布大卷颜色提示信息表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
@Data
@Getter
@TableName("T_XWYY_BIG_ROLL_COLOR")
@ApiModel(value = "XwyyBigRollColor对象", description = "帘布大卷颜色提示信息表")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class XwyyBigRollColor extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC", position = 10)
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "帘布大卷编号")
    @TableField("BIG_ROLL_CODE")
    private String bigRollCode;

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
