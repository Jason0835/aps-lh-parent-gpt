package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 密炼参数信息对象 t_schedule_params
 *
 * @author Liam
 * @date 2022-03-11
 */
@Data
@TableName("T_SCHEDULE_PARAMS" )
@ApiModel(value = "SettingScheduleParams对象", description = "密炼参数信息" )
@KeySequence(value = "seq_t_schedule_params", dbType = DbType.ORACLE)
public class SettingScheduleParams extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "ID")
    @ApiModelProperty(value = "主键ID", position = 10)
    private Long id;

    @Excel(name = "setting.machine.mixArea", dictType = "MIX_AREA")
    @TableField("MIX_AREA")
    @NotBlank(message = "密炼区不能为空")
    @ApiModelProperty(value = "密炼区(0表示默认参数配置，对应数据字典code：MIX_AREA)", position = 15)
    private String mixArea;

    @Excel(name = "ui.data.column.paramsCode")
    @TableField("PARAM_CODE")
    @NotBlank(message = "参数代码不能为空")
    @ApiModelProperty(value = "参数code", position = 20)
    private String paramCode;

    @Excel(name = "ui.data.column.paramsName")
    @TableField("PARAM_NAME")
    @ApiModelProperty(value = "参数名称", position = 30)
    private String paramName;

    @Excel(name = "ui.data.column.paramsValue")
    @TableField("PARAM_VALUE")
    @ApiModelProperty(value = "参数值", position = 40)
    private String paramValue;

    @ApiModelProperty(value = "参数值对应的正则表达式", position = 50)
    @TableField("REGULAR_EXPRESSION")
    private String regularExpression;

    @ApiModelProperty(value = "参数值根据正则表达式校验是失败后的错误提示", position = 60)
    @TableField("ERROR_TIPS")
    private String errorTips;

    @Excel(name = "ui.remark")
    @TableField("REMARK")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

    @ApiModelProperty(value = "删除标识：0--正常，1-删除", position = 600)
    @TableLogic(value = ZltConstant.DEL_FLAG_NORMAL, delval = ZltConstant.DEL_FLAG_DEL)
    @TableField("DEL_FLAG")
    private String delFlag;


}
