package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 配方类型对象 t_recipe_type
 * 
 * @author Joran.zhang
 * @date 2022-05-31
 */
@ApiModel(value = "配方类型对象", description = "配方类型对象 ")
@TableName("t_recipe_type")
@KeySequence(value = "seq_t_recipe_type", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class RecipeType extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_T_RECIPE_TYPE */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_RECIPE_TYPE", position = 10)
    private Long id;
    /** 配方类型代号 */
    @Excel(name = "setting.type.recipeTypeCode")
    @ImportValidated(name = "setting.type.recipeTypeCode", isCode=true , maxLength=10,required = true)
    @ApiModelProperty(value = "配方类型代号", position = 20)
    private String recipeTypeCode;
    /** 配方类型名称 */
    @Excel(name = "setting.type.recipeTypeName")
    @ImportValidated(name = "setting.type.recipeTypeName", maxLength=50,required = true)
    @ApiModelProperty(value = "配方类型名称", position = 30)
    private String recipeTypeName;
    /** 备注 */
    @Excel(name = "setting.type.remark")
    @ImportValidated(name = "setting.type.remark", maxLength=300)
    @ApiModelProperty(value = "备注", position = 90)
    private String remark;

}
