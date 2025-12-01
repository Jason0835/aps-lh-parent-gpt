package com.zlt.aps.cd90.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 90度裁断参数信息
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-07
 */
@Data
@TableName("T_CD90_PARAMS")
@ApiModel(value = "Cd90Params对象", description = "90度裁断参数信息")
//@KeySequence(value = "SEQ_PUBLIC",dbType = DbType.ORACLE)
public class Cd90Params extends ApsBaseEntity {

    private static final long serialVersionUID = 1110056585174675868L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_PUBLIC")
    @TableId(value = "ID", type = IdType.AUTO)
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
