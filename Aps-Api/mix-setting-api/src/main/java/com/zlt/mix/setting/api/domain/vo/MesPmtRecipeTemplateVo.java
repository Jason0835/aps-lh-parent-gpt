package com.zlt.mix.setting.api.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.common.annotation.ImportExcelValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 配方信息对象导入模板
 *
 * @author Liam
 * @since 2025/3/6
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MesPmtRecipeTemplateVo extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_MES_PMT_RECIPE
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MES_PMT_RECIPE", position = 10)
    private Long id;
    /**
     * 配方编号
     */
    @Excel(name = "setting.MesPmtRecipe.recipeId")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.recipeId", required = true, maxLength = 13)
    @ApiModelProperty(value = "配方编号", position = 20)
    private String recipeId;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    // @Excel(name = "setting.MesPmtRecipe.mixArea")
    // @ImportExcelValidated(name = "setting.MesPmtRecipe.mixArea", maxLength = 10)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 30)
    private String mixArea;
    /**
     * 机台编号
     */
    @Excel(name = "setting.MesPmtRecipe.recipeEquipCode")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.recipeEquipCode", required = true, maxLength = 30)
    @ApiModelProperty(value = "机台编号", position = 40)
    private String recipeEquipCode;
    /**
     * 机台名称
     */
    @Excel(name = "setting.MesPmtRecipe.machineName")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.machineName")
    @ApiModelProperty(value = "机台名称", position = 160)
    @TableField(exist = false)
    private String machineName;
    /**
     * 物料代号
     */
    @Excel(name = "setting.MesPmtRecipe.recipeMaterialCode")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.recipeMaterialCode", required = true, maxLength = 13)
    @ApiModelProperty(value = "物料代号", position = 50)
    private String recipeMaterialCode;
    /**
     * 物料名称
     */
    @Excel(name = "setting.MesPmtRecipe.recipeMaterialName")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.recipeMaterialName", maxLength = 50)
    @ApiModelProperty(value = "物料名称", position = 60)
    private String recipeMaterialName;
    /**
     * 配方版本号
     */
    @Excel(name = "setting.MesPmtRecipe.recipeVersionId")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.recipeVersionId", required = true, number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "配方版本号", position = 70)
    private String recipeVersionId;
    /**
     * 配方阶段（投产、试制，对应数据字典：PRODUCT_STAGE投产、试制）
     */
    @Excel(name = "setting.MesPmtRecipe.productStage",dictType = "PRODUCT_STAGE")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.productStage", maxLength = 1)
    @ApiModelProperty(value = "配方阶段（投产、试制，对应数据字典：PRODUCT_STAGE投产、试制）", position = 90)
    private String productStage;
    /**
     * 配方类型(对应T_RECIPE_TYPE表的RECIPE_TYPE_CODE)
     */
    @Excel(name = "setting.MesPmtRecipe.recipeType")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.recipeType", required = true, number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "配方类型(对应T_RECIPE_TYPE表的RECIPE_TYPE_CODE)", position = 80)
    private String recipeType;
    /**
     * 夏季炼胶时间(秒)
     */
    @Excel(name = "setting.MesPmtRecipe.summerMixTime")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.summerMixTime", required = true, number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "夏季炼胶时间(秒)", position = 100)
    private Long summerMixTime;
    /**
     * 单车总重
     */
    @Excel(name = "setting.MesPmtRecipe.lotTotalWeight")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.lotTotalWeight", required = true, number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "单车总重", position = 120)
    private Double lotTotalWeight;
    /**
     * 配方状态( 0 废止，1 在用， 2 未审核;对应字典：RECIPE_STATE)
     */
    @Excel(name = "setting.MesPmtRecipe.recipeState", dictType = "RECIPE_STATE")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.recipeState", required = true, maxLength = 1)
    @ApiModelProperty(value = "配方状态( 0 废止，1 在用， 2 未审核;对应字典：RECIPE_STATE)", position = 130)
    private String recipeState;
    /**
     * 审核标志 (0 未审核，1 已审核;对应字典：AUDIT_FLAG)
     */
    @Excel(name = "setting.MesPmtRecipe.auditFlag", dictType = "AUDIT_FLAG")
    @ImportExcelValidated(name = "setting.MesPmtRecipe.auditFlag", required = true, maxLength = 1)
    @ApiModelProperty(value = "审核标志 (0 未审核，1 已审核;对应字典：AUDIT_FLAG)", position = 140)
    private String auditFlag;
    
    /*
    ----- 子表字段start -----
     */

    /**
     * 称量顺序
     */
    @Excel(name = "setting.MesPmtRecipeWeight.weightOrder")
    @ImportExcelValidated(name = "setting.MesPmtRecipeWeight.weightOrder", required = true, number = true, min = 0, max = 9999)
    @ApiModelProperty(value = "称量顺序", position = 40)
    private Long weightOrder;

    /**
     * 称重物料代号
     */
    @Excel(name = "setting.MesPmtRecipeWeight.recipeMaterialCode")
    @ImportExcelValidated(name = "setting.MesPmtRecipeWeight.recipeMaterialCode", required = true, maxLength = 13)
    @ApiModelProperty(value = "称重物料代号", position = 50)
    private String recipeMaterialCodeSub;

    /**
     * 物料名称
     */
    @Excel(name = "setting.MesPmtRecipeWeight.recipeMaterialName")
    @ImportExcelValidated(name = "setting.MesPmtRecipeWeight.recipeMaterialName", maxLength = 50)
    @ApiModelProperty(value = "物料名称", position = 60)
    private String recipeMaterialNameSub;

    /**
     * 设定重量
     */
    @Excel(name = "setting.MesPmtRecipeWeight.setWeight")
    @ImportExcelValidated(name = "setting.MesPmtRecipeWeight.setWeight", required = true, number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "设定重量", position = 70)
    private Double setWeight;

    /**
     * 允许误差
     */
    @Excel(name = "setting.MesPmtRecipeWeight.allowError")
    @ImportExcelValidated(name = "setting.MesPmtRecipeWeight.allowError", required = true, number = true, min = 0, max = 999999999)
    @ApiModelProperty(value = "允许误差", position = 80)
    private Double allowError;
}
