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
 * 提醒设备对象 t_remind_setting
 *
 * @author Gim
 * @date 2022-03-23
 */
@ApiModel(value = "提醒设备对象", description = "提醒设备对象 ")
@TableName("t_remind_setting")
@KeySequence(value = "seq_t_remind_setting", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class RemindSetting extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_REMIND_SETTING
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_REMIND_SETTING", position = 10)
    private Long id;
    /**
     * 栏位CODE（对应数据字典REMIND_CODE）
     */
    @Excel(name = "setting.remindSetting.fieldCode", dictType = "REMIND_CODE")
    @ImportValidated(name = "setting.remindSetting.fieldCode", required = true, maxLength = 30)
    @ApiModelProperty(value = "栏位CODE（对应数据字典REMIND_CODE）", position = 20)
    private String fieldCode;
    /**
     * 栏位值
     */
    @Excel(name = "setting.remindSetting.fieldValue")
    @ImportValidated(name = "setting.remindSetting.fieldValue", required = true, maxLength = 40)
    @ApiModelProperty(value = "栏位值", position = 30)
    private String fieldValue;
    /**
     * 文字颜色
     */
    @Excel(name = "setting.remindSetting.wordColor")
    @ImportValidated(name = "setting.remindSetting.wordColor", isCode = true, maxLength = 10)
    @ApiModelProperty(value = "文字颜色", position = 40)
    private String wordColor;
    /**
     * 背景颜色
     */
    @Excel(name = "setting.remindSetting.backgroundColor")
    @ImportValidated(name = "setting.remindSetting.backgroundColor", isCode = true, maxLength = 10)
    @ApiModelProperty(value = "背景颜色", position = 50)
    private String backgroundColor;
    /**
     * 提示信息
     */
    @Excel(name = "setting.remindSetting.tips")
    @ImportValidated(name = "setting.remindSetting.tips", maxLength = 300)
    @ApiModelProperty(value = "提示信息", position = 60)
    private String tips;
    /**
     * 备注
     */
    @Excel(name = "setting.remindSetting.remark")
    @ImportValidated(name = "setting.remindSetting.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 70)
    private String remark;

}
