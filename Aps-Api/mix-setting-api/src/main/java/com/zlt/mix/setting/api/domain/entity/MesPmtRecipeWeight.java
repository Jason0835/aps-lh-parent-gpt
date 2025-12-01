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
 * 配方称量明细对象 t_mes_pmt_recipe_weight
 *
 * @author chen
 * @date 2022-06-01
 */
@ApiModel(value = "配方称量明细对象", description = "配方称量明细对象 ")
@TableName("t_mes_pmt_recipe_weight")
// @KeySequence(value = "seq_t_mes_pmt_recipe_weight", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class MesPmtRecipeWeight extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_MES_PMT_RECIPE_WEIGHT
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MES_PMT_RECIPE_WEIGHT", position = 10)
    private Long id;
    /**
     * 父级配方编号
     */
    @Excel(name = "setting.MesPmtRecipeWeight.fatherRecipeId")
    @ImportValidated(name = "setting.MesPmtRecipeWeight.fatherRecipeId", maxLength = 13)
    @ApiModelProperty(value = "父级配方编号", position = 20)
    private String fatherRecipeId;
    /**
     * 配方编号
     */
    @Excel(name = "setting.MesPmtRecipeWeight.recipeId")
    @ImportValidated(name = "setting.MesPmtRecipeWeight.recipeId", maxLength = 13)
    @ApiModelProperty(value = "配方编号", position = 30)
    private String recipeId;
    /**
     * 称量顺序
     */
    @Excel(name = "setting.MesPmtRecipeWeight.weightOrder")
    @ImportValidated(name = "setting.MesPmtRecipeWeight.weightOrder", number = true, min = 0, max = 9999)
    @ApiModelProperty(value = "称量顺序", position = 40)
    private Long weightOrder;
    /**
     * 称重物料代号
     */
    @Excel(name = "setting.MesPmtRecipeWeight.recipeMaterialCode")
    @ImportValidated(name = "setting.MesPmtRecipeWeight.recipeMaterialCode", maxLength = 13)
    @ApiModelProperty(value = "称重物料代号", position = 50)
    private String recipeMaterialCode;
    /**
     * 物料名称
     */
    @Excel(name = "setting.MesPmtRecipeWeight.recipeMaterialName")
    @ImportValidated(name = "setting.MesPmtRecipeWeight.recipeMaterialName", maxLength = 13)
    @ApiModelProperty(value = "物料名称", position = 60)
    private String recipeMaterialName;
    /**
     * 设定重量
     */
    @Excel(name = "setting.MesPmtRecipeWeight.setWeight")
    @ImportValidated(name = "setting.MesPmtRecipeWeight.setWeight", number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "设定重量", position = 70)
    private Double setWeight;
    /**
     * 允许误差
     */
    @Excel(name = "setting.MesPmtRecipeWeight.allowError")
    @ImportValidated(name = "setting.MesPmtRecipeWeight.allowError", number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "允许误差", position = 80)
    private Double allowError;
    /**
     * 备注
     */
    @Excel(name = "setting.MesPmtRecipeWeight.remark")
    @ImportValidated(name = "setting.MesPmtRecipeWeight.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 90)
    private String remark;

}
