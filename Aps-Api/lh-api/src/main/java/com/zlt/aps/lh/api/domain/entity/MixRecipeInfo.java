package com.zlt.aps.lh.api.domain.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 密炼配方信息对象 t_mix_recipe_info
 * 
 * @author zlt
 * @date 2021-11-09
 */
@ApiModel(value = "密炼配方信息对象", description = "密炼配方信息对象 ")
@Data
public class MixRecipeInfo extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 分厂编号 */
    @Excel(name = "ui.data.column.recipeInfo.factoryCode")
    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    /** 配方编码 */
    @Excel(name = "ui.data.column.recipeInfo.recipeCode")
    @ApiModelProperty(value = "配方编码")
    private String recipeCode;

    /** 炼胶时间 */
    @Excel(name = "ui.data.column.recipeInfo.mixingTime")
    @ApiModelProperty(value = "炼胶时间")
    private Long mixingTime;

    /** 间隔时间 */
    @Excel(name = "ui.data.column.recipeInfo.intervalTime")
    @ApiModelProperty(value = "间隔时间")
    private Long intervalTime;

    /** 删除标识 */
    private String delFlag;





}
