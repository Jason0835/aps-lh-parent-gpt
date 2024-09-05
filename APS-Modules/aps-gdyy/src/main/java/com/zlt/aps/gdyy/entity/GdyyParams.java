package com.zlt.aps.gdyy.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 钢带压延参数信息
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-02
 */
@Data
@TableName("T_GDYY_PARAMS")
@ApiModel(value = "GdyyParams对象", description = "钢带压延参数信息")
@KeySequence(value = "SEQ_PUBLIC", clazz = Long.class)
public class GdyyParams extends ApsBaseEntity {

    private static final long serialVersionUID = 1110056585174675868L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "参数code")
    @TableField("PARAM_CODE")
    private String paramCode;

    @ApiModelProperty(value = "参数名称")
    @TableField("PARAM_NAME")
    private String paramName;

    @ApiModelProperty(value = "参数值")
    @TableField("PARAM_VALUE")
    private String paramValue;

    @ApiModelProperty(value = "参数值对应的正则表达式")
    @TableField("REGULAR_EXPRESSION")
    private String regularExpression;

    @ApiModelProperty(value = "参数值根据正则表达式校验是失败后的错误提示")
    @TableField("ERROR_TIPS")
    private String errorTips;
}
