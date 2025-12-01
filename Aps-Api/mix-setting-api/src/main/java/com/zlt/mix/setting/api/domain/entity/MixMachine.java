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
 * 密炼机台信息对象 t_mix_machine
 *
 * @author Gim
 * @date 2022-03-22
 */
@ApiModel(value = "密炼机台信息对象", description = "密炼机台信息对象 ")
@TableName("t_mix_machine")
@KeySequence(value = "seq_t_mix_machine", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class MixMachine extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_MIX_MACHINE
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MIX_MACHINE", position = 1)
    private Long id;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.machine.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.machine.mixArea", required = true, maxLength = 10)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 2)
    private String mixArea;
    /**
     * 机台编号
     */
    @Excel(name = "setting.machine.machineCode")
    @ImportValidated(name = "setting.machine.machineCode", required = true, maxLength = 30, isCode = true)
    @ApiModelProperty(value = "机台编号", position = 3)
    private String machineCode;
    /**
     * 机台名称
     */
    @Excel(name = "setting.machine.machineName")
    @ImportValidated(name = "setting.machine.machineName", required = true, maxLength = 40)
    @ApiModelProperty(value = "机台名称", position = 4)
    private String machineName;

    /**
     * 是否有硫磺秤(0--否，1--是)
     */
    // @Excel(name = "setting.machine.haveSulfurSteelyard", dictType = "IS_HAVE")
    // @ImportValidated(name = "setting.machine.haveSulfurSteelyard", maxLength = 10)
    @ApiModelProperty(value = "是否有硫磺秤(0--否，1--是)", position = 5)
    private String haveSulfurSteelyard;

    /**
     * 是否有辅料秤(0--否，1--是)
     */
    // @Excel(name = "setting.machine.haveMaterialSteelyard", dictType = "IS_HAVE")
    // @ImportValidated(name = "setting.machine.haveMaterialSteelyard", maxLength = 10)
    @ApiModelProperty(value = "是否有辅料秤(0--否，1--是)", position = 6)
    private String haveMaterialSteelyard;

    /**
     * 机台状态
     */
    @Excel(name = "setting.machine.status", dictType = "STATUS")
    @ImportValidated(name = "setting.machine.status", maxLength = 10)
    @ApiModelProperty(value = "机台状态", position = 7)
    private String status;

    /**
     * 夜班状态
     */
    @Excel(name = "setting.machine.nightStatus", dictType = "STATUS")
    @ImportValidated(name = "setting.machine.nightStatus", maxLength = 10)
    @ApiModelProperty(value = "夜班状态", position = 20)
    private String midStatus;

    /**
     * 白班状态
     */
    @Excel(name = "setting.machine.dayStatus", dictType = "STATUS")
    @ImportValidated(name = "setting.machine.dayStatus", maxLength = 10)
    @ApiModelProperty(value = "白班状态", position = 30)
    private String nightStatus;

    /**
     * 白班状态
     */
    // @Excel(name = "setting.machine.dayStatus", dictType = "STATUS")
    // @ImportValidated(name = "setting.machine.dayStatus", maxLength = 10)
    // @ApiModelProperty(value = "白班状态", position = 30)
    private String dayStatus;

    /**
     * 备注
     */
    @Excel(name = "setting.machine.remark")
    @ImportValidated(name = "setting.machine.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 8)
    private String remark;



}
