package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 炼胶时间信息对象 t_mixing_time
 *
 * @author Liam
 * @date 2022-03-31
 */
@ApiModel(value = "炼胶时间信息对象", description = "炼胶时间信息对象 ")
@TableName("t_mixing_time")
// @KeySequence(value = "seq_t_mixing_time", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class MixingTime extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_MIXING_TIME
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MIXING_TIME", position = 10)
    private Long id;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "setting.mixingTime.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "setting.mixingTime.mixArea", maxLength = 10, required = true, isCode = true)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /**
     * 胶料名称
     */
    @Excel(name = "setting.mixingTime.glue")
    @ImportValidated(name = "setting.mixingTime.glue", maxLength = 30, required = true)
    @ApiModelProperty(value = "胶料名称", position = 30)
    private String glue;
    /**
     * 机台编号
     */
    @Excel(name = "setting.mixingTime.machineCode")
    @ImportValidated(name = "setting.mixingTime.machineCode", maxLength = 30, isCode = true)
    @ApiModelProperty(value = "机台编号", position = 40)
    private String machineCode;
    /**
     * 冬季炼胶时间
     */
    // @Excel(name = "setting.mixingTime.winterMixTime")
    // @ImportValidated(name = "setting.mixingTime.winterMixTime", digits = true, min = 0, max = 9999999999L , required = true)
    @ApiModelProperty(value = "冬季炼胶时间", position = 50)
    private Long winterMixTime;
    /**
     * 夏季炼胶时间(车/秒)
     */
    // @Excel(name = "setting.mixingTime.summerMixTime")
    // @ImportValidated(name = "setting.mixingTime.summerMixTime", digits = true, min = 0, max = 9999999999L , required = true)
    @ApiModelProperty(value = "夏季炼胶时间(车/秒)", position = 60)
    private Long summerMixTime;
    /**
     * 间隔时间(秒)
     */
    @Excel(name = "setting.mixingTime.intervalTime")
    @ImportValidated(name = "setting.mixingTime.intervalTime", digits = true, min = 0, max = 999999999L , required = true)
    @ApiModelProperty(value = "间隔时间(秒)", position = 70)
    private Long intervalTime;
    /**
     * 备注
     */
    @Excel(name = "setting.mixingTime.remark")
    @ImportValidated(name = "setting.mixingTime.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 80)
    private String remark;

}
