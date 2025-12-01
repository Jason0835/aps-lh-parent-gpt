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

/**
 * 硫磺辅料耗损率设定对象 t_lhfl_lossrate_setting
 * 
 * @author Joran.zhang
 * @date 2022-05-23
 */
@ApiModel(value = "硫磺辅料耗损率设定对象", description = "硫磺辅料耗损率设定对象 ")
@TableName("t_lhfl_lossrate_setting")
@KeySequence(value = "seq_lhfl_lossrate_setting", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class LhflLossSetting extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_LHFL_LOSSRATE_SETTING
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_LHFL_LOSSRATE_SETTING", position = 10)
    private Long id;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.lhflLossSetting.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.lhflLossSetting.mixArea", maxLength = 10)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /**
     * 物料名称
     */
    @Excel(name = "setting.lhflLossSetting.materialName")
    @ImportValidated(name = "setting.lhflLossSetting.materialName", maxLength = 50)
    @ApiModelProperty(value = "物料名称", position = 30)
    private String materialName;
    /**
     * 生产机台编号（对应的是小料机台编号）
     */
    //@Excel(name = "setting.lhflLossSetting.machineCode")
    @ImportValidated(name = "setting.lhflLossSetting.machineCode", maxLength = 30)
    @ApiModelProperty(value = "生产机台编号（对应的是小料机台编号）", position = 40)
    private String machineCode;

    /**
     * 机台名称
     */
    @TableField(exist = false)
    @Excel(name = "setting.lhflLossSetting.machineName")
    @ImportValidated(name = "setting.lhflLossSetting.machineName", maxLength=30)
    private String machineName;


    /** 损耗率 */
    @Excel(name = "setting.lhflLossSetting.lossRate")
    @ImportValidated(name = "setting.lhflLossSetting.lossRate", maxLength=6,min = 0.00,max = 9999.99,number = true,required = true)
    @ApiModelProperty(value = "损耗率", position = 50)
    private BigDecimal lossRate;
    /** 备注 */
    @Excel(name = "setting.lhflLossSetting.remark")
    @ImportValidated(name = "setting.lhflLossSetting.remark", maxLength=300)
    @ApiModelProperty(value = "备注", position = 60)
    private String remark;

}
