package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 小料机台信息对象 t_lhfl_machine
 *
 * @author Liam
 * @date 2022-04-18
 */
@ApiModel(value = "小料机台信息对象", description = "小料机台信息对象 ")
@TableName("t_lhfl_machine")
@KeySequence(value = "seq_t_lhfl_machine", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class LhflMachine extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_MIX_MACHINE
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MIX_MACHINE", position = 10)
    private Long id;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.lhflMachine.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.lhflMachine.mixArea", maxLength = 10, required = true)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /**
     * 机台编号
     */
    @Excel(name = "setting.lhflMachine.machineCode")
    @ImportValidated(name = "setting.lhflMachine.machineCode", maxLength = 30, required = true, isCode = true)
    @ApiModelProperty(value = "机台编号", position = 30)
    private String machineCode;
    /**
     * 机台名称
     */
    @Excel(name = "setting.lhflMachine.machineName")
    @ImportValidated(name = "setting.lhflMachine.machineName", maxLength = 40, required = true)
    @ApiModelProperty(value = "机台名称", position = 40)
    private String machineName;
    /**
     * 班制(如1--长白班，2--两班制，3--三班制；对应数据字典LH_CLASS_SHIFT)
     */
    @Excel(name = "setting.lhflMachine.classShift", dictType = "LH_CLASS_SHIFT")
    @ImportValidated(name = "setting.lhflMachine.classShift", number = true, min = 0, max = 999, required = true)
    @ApiModelProperty(value = "班制(如1--长白班，2--两班制，3--三班制；对应数据字典LH_CLASS_SHIFT)", position = 50)
    private Integer classShift;
    /**
     * 产能(车/小时)
     */
    @Excel(name = "setting.lhflMachine.capacity")
    @ImportValidated(name = "setting.lhflMachine.capacity", number = true, min = 0, max = 9999999, required = true ,digits=true)
    @ApiModelProperty(value = "产能(车/小时)", position = 60)
    private BigDecimal capacity;


    /**
     * 机台状态
     */
    @Excel(name = "setting.machine.status", dictType = "STATUS")
    @ImportValidated(name = "setting.machine.status", maxLength = 10)
    @ApiModelProperty(value = "机台状态", position = 70)
    private String status;

    /**
     * 备注
     */
    @Excel(name = "setting.lhflMachine.remark")
    @ImportValidated(name = "setting.lhflMachine.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 80)
    private String remark;

    /**
     * 中班状态
     */
    @Excel(name = "setting.machine.midStatus", dictType = "STATUS")
    @ImportValidated(name = "setting.machine.midStatus", maxLength = 10)
    @ApiModelProperty(value = "中班状态", position = 10)
    private String midStatus;

    /**
     * 夜班状态
     */
    @Excel(name = "setting.machine.nightStatus", dictType = "STATUS")
    @ImportValidated(name = "setting.machine.nightStatus", maxLength = 10)
    @ApiModelProperty(value = "夜班状态", position = 20)
    private String nightStatus;

    /**
     * 白班状态
     */
    @Excel(name = "setting.machine.dayStatus", dictType = "STATUS")
    @ImportValidated(name = "setting.machine.dayStatus", maxLength = 10)
    @ApiModelProperty(value = "白班状态", position = 30)
    private String dayStatus;
    
	/**
	 * 中班开班时间
	 */
    @TableField(exist = false)
	private Date midClassStartTime;

	/**
	 * 夜班开班时间
	 */
    @TableField(exist = false)
	private Date nightClassStartTime;

	/**
	 * 白班开班时间
	 */
    @TableField(exist = false)
	private Date dayClassStartTime;
}
