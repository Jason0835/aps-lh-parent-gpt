package com.zlt.mix.setting.api.domain.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 胶料损耗率设定对象 t_glue_loss_setting
 * 
 * @author Joran.zhang
 * @date 2022-05-23
 */
@ApiModel(value = "胶料损耗率设定对象", description = "胶料损耗率设定对象 ")
@TableName("t_glue_loss_setting")
@KeySequence(value = "SEQ_GLUE_LOSS_SETTING", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueLossSetting extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_GLUE_LOSS_SETTING */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_GLUE_LOSS_SETTING", position = 10)
    private Long id;
    /** 密炼区(对应数据字典code：MIX_AREA) */
    @Excel(name = "setting.glueLossSetting.mixArea",dictType = "MIX_AREA")
    @ImportValidated(name = "setting.glueLossSetting.mixArea", maxLength=10)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /** 胶料名称 */
    @Excel(name = "setting.glueLossSetting.glue")
    @ImportValidated(name = "setting.glueLossSetting.glue", maxLength=30)
    @ApiModelProperty(value = "胶料名称", position = 30)
    private String glue;
    /** 机台编号（对应T_MIX_MACHINE表编号） */

    @ImportValidated(name = "setting.glueLossSetting.machineCode", maxLength=10)
    @ApiModelProperty(value = "机台编号（对应T_MIX_MACHINE表编号）", position = 40)
    private String machineCode;

    /**
     * 机台名称
     */
    @TableField(exist = false)
    @Excel(name = "setting.glueLossSetting.machineName")
    @ImportValidated(name = "setting.glueLossSetting.machineName", maxLength=30)
    private String machineName;

    /** 损耗率 */
    @Excel(name = "setting.glueLossSetting.lossRate")
    @ImportValidated(name = "setting.glueLossSetting.lossRate",min = 0.00, maxLength=9999,number = true,max = 9999.99,required = true)
    @ApiModelProperty(value = "损耗率", position = 50)
    private BigDecimal lossRate;
    /** 备注 */
    @Excel(name = "setting.glueLossSetting.remark")
    @ImportValidated(name = "setting.glueLossSetting.remark", maxLength=300)
    @ApiModelProperty(value = "备注", position = 110)
    private String remark;

}
