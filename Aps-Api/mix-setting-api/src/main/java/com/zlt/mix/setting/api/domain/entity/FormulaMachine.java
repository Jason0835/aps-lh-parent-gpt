package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import com.zlt.mix.setting.api.domain.dto.MachineOrderDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 配方与机台对应对象 t_formula_machine
 *
 * @author Gim
 * @date 2022-03-28
 */
@ApiModel(value = "配方与机台对应对象", description = "配方与机台对应对象 ")
@TableName("t_formula_machine")
@KeySequence(value = "seq_t_formula_machine", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class FormulaMachine extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_FORMULA_MACHINE
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_FORMULA_MACHINE", position = 10)
    private Long id;

    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.formulaMachine.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.formulaMachine.mixArea", maxLength = 10, required = true)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /**
     * 胶料名称
     */
    @Excel(name = "setting.formulaMachine.glue")
    @ImportValidated(name = "setting.formulaMachine.glue", maxLength = 30, required = true)
    @ApiModelProperty(value = "胶料名称", position = 30)
    private String glue;
    /**
     * 生产机台编号
     */
    @ApiModelProperty(value = "生产机台编号", position = 40)
    private String machineCode;

    /**
     * 回显机台名称，作为页面显示和机台导入导出
     */
    @Excel(name = "setting.formulaMachine.machineCode")
    @ImportValidated(name = "setting.formulaMachine.machineCode", required = true)
    @TableField(exist = false)
    private String machineName;

    /**
     * 备注
     */
    @Excel(name = "setting.formulaMachine.remark")
    @ImportValidated(name = "setting.formulaMachine.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 50)
    private String remark;

    @ApiModelProperty(value = "机台顺序", position = 60)
    private String machineOrder;

    @ApiModelProperty(value = "用于接收前端机台编号及顺序集合", position = 70)
    @TableField(exist = false)
    private List<MachineOrderDto> machineOrderList;
}
