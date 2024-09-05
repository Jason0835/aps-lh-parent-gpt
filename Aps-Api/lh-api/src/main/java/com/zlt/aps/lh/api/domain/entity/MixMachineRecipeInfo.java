package com.zlt.aps.lh.api.domain.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 机台和配方对应及下车重量对象 t_mix_machine_recipe_info
 * 
 * @author zlt
 * @date 2021-11-09
 */
@ApiModel(value = "机台和配方对应及下车重量对象", description = "机台和配方对应及下车重量对象 ")
@Data
public class MixMachineRecipeInfo extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.recipe.factoryCode")
    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    /** 配方编码 */
    @Excel(name = "ui.data.column.recipe.recipeCode")
    @ApiModelProperty(value = "配方编码")
    private String recipeCode;

    /** 配方时间 */
    @Excel(name = "ui.data.column.recipe.recipeTime")
    @ApiModelProperty(value = "配方时间")
    private Long recipeTime;

    /** 下车重量 */
    @Excel(name = "ui.data.column.recipe.issueWeight")
    @ApiModelProperty(value = "下车重量")
    private BigDecimal issueWeight;

    /** 生产机台 */
    @Excel(name = "ui.data.column.recipe.machineCode")
    @ApiModelProperty(value = "生产机台")
    private String machineCode;

    /** 工艺停放时间（小时） */
    @Excel(name = "ui.data.column.recipe.stopTime", readConverterExp = "小=时")
    @ApiModelProperty(value = "工艺停放时间")
    private Long stopTime;

    /** 终炼胶库存 */
    @Excel(name = "ui.data.column.recipe.finalStockNum")
    @ApiModelProperty(value = "终炼胶库存")
    private Long finalStockNum;

    /** 母炼胶库存 */
    @Excel(name = "ui.data.column.recipe.masterStockNum")
    @ApiModelProperty(value = "母炼胶库存")
    private Long masterStockNum;

    /** 返回胶库存 */
    @Excel(name = "ui.data.column.recipe.returnStockNum")
    @ApiModelProperty(value = "返回胶库存")
    private Long returnStockNum;

    /** 不合格胶库存（车数）,以保管员或者MES库存为准 */
    @Excel(name = "ui.data.column.recipe.badStockNum", readConverterExp = "车=数")
    @ApiModelProperty(value = "不合格胶库存")
    private Long badStockNum;

    /** 终炼胶库存 */
    @Excel(name = "ui.data.column.recipe.finalMaterialStockNum")
    @ApiModelProperty(value = "终炼胶库存")
    private Long finalMaterialStockNum;

    /** 母炼胶库存 */
    @Excel(name = "ui.data.column.recipe.masterMaterialStockNum")
    @ApiModelProperty(value = "母炼胶库存")
    private Long masterMaterialStockNum;

    /** 安全库存 */
    @Excel(name = "ui.data.column.recipe.safetyStockNum")
    @ApiModelProperty(value = "安全库存")
    private Long safetyStockNum;

    /** 品名 */
    @Excel(name = "ui.data.column.recipe.productName")
    @ApiModelProperty(value = "品名")
    private String productName;

    /** 删除标识 */
    @ApiModelProperty(value = "品名")
    private String delFlag;





}
