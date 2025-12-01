package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 排程参数（硫磺辅料排程设置）对象 t_lhfl_schedule_params
 *
 * @author Liam
 * @date 2022-04-06
 */
@ApiModel(value = "排程参数（硫磺辅料排程设置）对象", description = "排程参数（硫磺辅料排程设置）对象 ")
@TableName("t_lhfl_schedule_params")
@KeySequence(value = "seq_t_lhfl_schedule_params", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class LhflScheduleParams extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID", position = 10)
    private Long id;
    /**
     * 密炼区(0表示默认参数配置，对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.lhflScheduleParams.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.lhflScheduleParams.mixArea", maxLength = 10)
    @ApiModelProperty(value = "密炼区(0表示默认参数配置，对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /**
     * 参数code
     */
    @Excel(name = "setting.lhflScheduleParams.paramCode")
    @ImportValidated(name = "setting.lhflScheduleParams.paramCode", maxLength = 16)
    @ApiModelProperty(value = "参数code", position = 30)
    private String paramCode;
    /**
     * 参数名称
     */
    @Excel(name = "setting.lhflScheduleParams.paramName")
    @ImportValidated(name = "setting.lhflScheduleParams.paramName", maxLength = 16)
    @ApiModelProperty(value = "参数名称", position = 40)
    private String paramName;
    /**
     * 参数值
     */
    @Excel(name = "setting.lhflScheduleParams.paramValue")
    @ImportValidated(name = "setting.lhflScheduleParams.paramValue", maxLength = 16)
    @ApiModelProperty(value = "参数值", position = 50)
    private String paramValue;
    /**
     * 参数值对应的正则表达式
     */
    @ApiModelProperty(value = "参数值对应的正则表达式", position = 60)
    private String regularExpression;
    /**
     * 参数值根据正则表达式校验是失败后的错误提示
     */
    @ApiModelProperty(value = "参数值根据正则表达式校验是失败后的错误提示", position = 70)
    private String errorTips;
    /**
     * 备注
     */
    @Excel(name = "setting.lhflScheduleParams.remark")
    @ImportValidated(name = "setting.lhflScheduleParams.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 80)
    private String remark;

}
