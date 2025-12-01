package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 配方信息对象 t_mes_pmt_recipe
 *
 * @author chen
 * @date 2022-06-01
 */
@ApiModel(value = "配方信息对象", description = "配方信息对象 ")
@TableName("t_mes_pmt_recipe")
// @KeySequence(value = "seq_t_mes_pmt_recipe", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class MesPmtRecipe extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_MES_PMT_RECIPE
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MES_PMT_RECIPE", position = 10)
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 配方编号
     */
    @Excel(name = "setting.MesPmtRecipe.recipeId")
    @ImportValidated(name = "setting.MesPmtRecipe.recipeId", required = true, maxLength = 13)
    @ApiModelProperty(value = "配方编号", position = 20)
    private String recipeId;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    // @Excel(name = "setting.MesPmtRecipe.mixArea")
    // @ImportValidated(name = "setting.MesPmtRecipe.mixArea", maxLength = 10)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 30)
    private String mixArea;
    /**
     * 机台编号
     */
    @Excel(name = "setting.MesPmtRecipe.recipeEquipCode")
    @ImportValidated(name = "setting.MesPmtRecipe.recipeEquipCode", required = true, maxLength = 30)
    @ApiModelProperty(value = "机台编号", position = 40)
    private String recipeEquipCode;
    /**
     * 机台名称
     */
    @Excel(name = "setting.MesPmtRecipe.machineName")
    @ImportValidated(name = "setting.MesPmtRecipe.machineName")
    @ApiModelProperty(value = "机台名称", position = 160)
    @TableField(exist = false)
    private String machineName;
    /**
     * 物料代号
     */
    @Excel(name = "setting.MesPmtRecipe.recipeMaterialCode")
    @ImportValidated(name = "setting.MesPmtRecipe.recipeMaterialCode", required = true, maxLength = 13)
    @ApiModelProperty(value = "物料代号", position = 50)
    private String recipeMaterialCode;
    /**
     * 物料名称
     */
    @Excel(name = "setting.MesPmtRecipe.recipeMaterialName")
    @ImportValidated(name = "setting.MesPmtRecipe.recipeMaterialName", maxLength = 50)
    @ApiModelProperty(value = "物料名称", position = 60)
    private String recipeMaterialName;
    /**
     * 配方版本号
     */
    @Excel(name = "setting.MesPmtRecipe.recipeVersionId")
    @ImportValidated(name = "setting.MesPmtRecipe.recipeVersionId", required = true, number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "配方版本号", position = 70)
    private String recipeVersionId;
    /**
     * 配方阶段（投产、试制，对应数据字典：PRODUCT_STAGE投产、试制）
     */
    @Excel(name = "setting.MesPmtRecipe.productStage",dictType = "PRODUCT_STAGE")
    @ImportValidated(name = "setting.MesPmtRecipe.productStage", maxLength = 1)
    @ApiModelProperty(value = "配方阶段（投产、试制，对应数据字典：PRODUCT_STAGE投产、试制）", position = 90)
    private String productStage;
    /**
     * 配方类型(对应T_RECIPE_TYPE表的RECIPE_TYPE_CODE)
     */
    @Excel(name = "setting.MesPmtRecipe.recipeType")
    @ImportValidated(name = "setting.MesPmtRecipe.recipeType", required = true, number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "配方类型(对应T_RECIPE_TYPE表的RECIPE_TYPE_CODE)", position = 80)
    private String recipeType;
    /**
     * 配方类型名称(对应T_RECIPE_TYPE表关联出来的RECIPE_TYPE_NAME)
     */
    // @Excel(name = "setting.MesPmtRecipe.recipeTypeName")
    // @ImportValidated(name = "setting.MesPmtRecipe.recipeTypeName")
    @ApiModelProperty(value = "配方类型名称(对应T_RECIPE_TYPE表关联出来的RECIPE_TYPE_NAME)", position = 80)
    @TableField(exist = false)
    private String recipeTypeName;
    /**
     * 夏季炼胶时间(秒)
     */
    @Excel(name = "setting.MesPmtRecipe.summerMixTime")
    @ImportValidated(name = "setting.MesPmtRecipe.summerMixTime", required = true, number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "夏季炼胶时间(秒)", position = 100)
    private Long summerMixTime;
    /**
     * 冬季炼胶时间(秒)
     */
    // @Excel(name = "setting.MesPmtRecipe.winterMixTime")
    // @ImportValidated(name = "setting.MesPmtRecipe.winterMixTime", number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "冬季炼胶时间(秒)", position = 110)
    private Long winterMixTime;
    /**
     * 单车总重
     */
    @Excel(name = "setting.MesPmtRecipe.lotTotalWeight")
    @ImportValidated(name = "setting.MesPmtRecipe.lotTotalWeight", required = true, number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "单车总重", position = 120)
    private Double lotTotalWeight;
    /**
     * 配方状态( 0 废止，1 在用， 2 未审核;对应字典：RECIPE_STATE)
     */
    @Excel(name = "setting.MesPmtRecipe.recipeState", dictType = "RECIPE_STATE")
    @ImportValidated(name = "setting.MesPmtRecipe.recipeState", required = true, maxLength = 1)
    @ApiModelProperty(value = "配方状态( 0 废止，1 在用， 2 未审核;对应字典：RECIPE_STATE)", position = 130)
    private String recipeState;
    /**
     * 审核标志 (0 未审核，1 已审核;对应字典：AUDIT_FLAG)
     */
    @Excel(name = "setting.MesPmtRecipe.auditFlag", dictType = "AUDIT_FLAG")
    @ImportValidated(name = "setting.MesPmtRecipe.auditFlag", required = true, maxLength = 1)
    @ApiModelProperty(value = "审核标志 (0 未审核，1 已审核;对应字典：AUDIT_FLAG)", position = 140)
    private String auditFlag;
    /**
     * 备注
     */
    // @Excel(name = "setting.MesPmtRecipe.remark")
    // @ImportValidated(name = "setting.MesPmtRecipe.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 150)
    private String remark;


}
