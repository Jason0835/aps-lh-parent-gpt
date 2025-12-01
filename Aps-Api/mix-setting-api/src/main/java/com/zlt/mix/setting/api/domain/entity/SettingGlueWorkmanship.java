package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分厂胶料工艺实体 seq_t_glue_workmanship
 *
 * @author Liam
 * @date 2022-03-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@KeySequence(value = "SEQ_T_GLUE_WORKMANSHIP", dbType = DbType.ORACLE)
@TableName("T_GLUE_WORKMANSHIP")
@ApiModel(value = "SettingGlueWorkmanShip对象", description = "分厂胶料工艺")
public class SettingGlueWorkmanship extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_WORKMANSHIP")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "分厂号（对应数据字典FACTORY）")
    @TableField("FACTORY_CODE")
    @Excel(name = "setting.workmanship.factoryCode", dictType = "FACTORY")
    @ImportValidated(name = "setting.workmanship.factoryCode", required = true, isCode = true, maxLength = 30)
    private String factoryCode;

    @ApiModelProperty(value = "胶料号")
    @TableField("GLUE")
    @Excel(name = "setting.workmanship.glue")
    @ImportValidated(name = "setting.workmanship.glue", required = true, maxLength = 30)
    private String glue;

    @ApiModelProperty(value = "刀数")
    @TableField("KNIFE_NUM")
    @Excel(name = "setting.workmanship.knifeNum")
    @ImportValidated(name = "setting.workmanship.knifeNum", required = true, digits = true, min = 0, max = 999999)
    private Long knifeNum;

    @ApiModelProperty(value = "每桌车数")
    @TableField("CAR_NUM")
    @Excel(name = "setting.workmanship.carNum")
    @ImportValidated(name = "setting.workmanship.carNum", required = true, digits = true, min = 0, max = 999999)
    private Long carNum;

    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @Excel(name = "ui.remark")
    @ImportValidated(name = "ui.remark", maxLength = 300)
    private String remark;

    @ApiModelProperty(value = "删除标识：0--正常，1-删除")
    @TableLogic(value = ZltConstant.DEL_FLAG_NORMAL, delval = ZltConstant.DEL_FLAG_DEL)
    @TableField("DEL_FLAG")
    private String delFlag;

}



