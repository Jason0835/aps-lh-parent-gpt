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
 * @author Liam
 * @date 2022-03-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
@KeySequence(value = "SEQ_T_FORMULA_INFO", dbType = DbType.ORACLE)
@TableName("T_FORMULA_INFO")
@ApiModel(value = "SettingFormulaInfo对象", description = "配方信息")
public class SettingFormulaInfo extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_FORMULA_INFO")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "胶料名称")
    @TableField("GLUE")
    @Excel(name = "setting.formulaInfo.glue")
    @ImportValidated(name = "setting.formulaInfo.glue", required = true, maxLength = 30)
    private String glue;

    @ApiModelProperty(value = "配方重量(KG)")
    @TableField("WEIGHT")
    @Excel(name = "setting.formulaInfo.weight")
    @ImportValidated(name = "setting.formulaInfo.weight", required = true, number = true, min = 0, max = 9999999.999)
    private Double weight;

    @ApiModelProperty(value = "胶料类型(对应数据字典，GLUE_TYPE)")
    @TableField("GLUE_TYPE")
    @Excel(name = "setting.formulaInfo.glueType", dictType = "GLUE_TYPE")
    @ImportValidated(name = "setting.formulaInfo.glueType", required = true, isCode = true, maxLength = 60)
    private String glueType;


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
