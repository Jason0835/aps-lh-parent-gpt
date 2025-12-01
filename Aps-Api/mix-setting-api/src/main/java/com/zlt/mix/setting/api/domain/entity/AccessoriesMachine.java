package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import com.zlt.mix.setting.api.domain.dto.MachineOrderDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 硫磺辅料与机台对应对象 t_accessories_machine
 *
 * @author Liam
 * @date 2022-04-18
 */
@ApiModel(value = "硫磺辅料与机台对应对象", description = "硫磺辅料与机台对应对象 ")
@TableName("t_accessories_machine")
@KeySequence(value = "seq_t_accessories_machine", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class AccessoriesMachine extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_ACCESSORIES_MACHINE
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_ACCESSORIES_MACHINE", position = 10)
    private Long id;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.accessoriesMachine.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.accessoriesMachine.mixArea", required = true, maxLength = 10)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /**
     * 物料名称
     */
    @Excel(name = "setting.accessoriesMachine.materialName")
    @ImportValidated(name = "setting.accessoriesMachine.materialName", required = true, maxLength = 50)
    @ApiModelProperty(value = "物料名称", position = 30)
    private String materialName;
    /**
     * 生产机台编号（对应的是小料机台编号）
     */

    @ApiModelProperty(value = "生产机台编号（对应的是小料机台编号）", position = 40)
    private String machineCode;

    /**
     * 回显机台名称，作为页面显示和机台导入导出
     */
    @Excel(name = "setting.accessoriesMachine.machineCode")
    @ImportValidated(name = "setting.accessoriesMachine.machineCode", required = true)
    @TableField(exist = false)
    private String machineName;
    /**
     * 备注
     */
    @Excel(name = "setting.accessoriesMachine.remark")
    @ImportValidated(name = "setting.accessoriesMachine.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 50)
    private String remark;

    /**
     * 班制(如1--长白班，2--两班制，3--三班制；对应数据字典LH_CLASS_SHIFT)
     */
    @ApiModelProperty(value = "班制(如1--长白班，2--两班制，3--三班制；对应数据字典LH_CLASS_SHIFT)", position = 50)
    @TableField(exist = false)
    private Integer classShift;

    @ApiModelProperty(value = "机台顺序", position = 60)
    private String machineOrder;

    @ApiModelProperty(value = "用于接收前端机台编号及顺序集合", position = 70)
    @TableField(exist = false)
    private List<MachineOrderDto> machineOrderList;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    @TableField(exist = false)
    private Date scheduleDate;

    @ApiModelProperty(value = "班制(如1--长白班，2--两班制，3--三班制；对应数据字典LH_CLASS_SHIFT)")
    @TableField(exist = false)
    private Integer nowClassShift;
}
