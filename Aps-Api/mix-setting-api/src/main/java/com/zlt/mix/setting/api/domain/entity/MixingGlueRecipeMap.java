package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import com.zlt.mix.common.core.utils.GenerageMapKeyUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Liam
 * @since 2025/4/27
 */
@ApiModel(value = "胶料配方映射表", description = "胶料配方映射表")
@TableName("t_mixing_glue_recipe_map")
@Data
@EqualsAndHashCode(callSuper = true)
public class MixingGlueRecipeMap extends ZltBaseEntity {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID")
    private Long id;
    /**
     * 密炼区
     */
    @ApiModelProperty(value = "密炼区")
    private String mixArea;
    /**
     * 胶料名称
     */
    @ApiModelProperty(value = "胶料名称")
    private String glue;
    /**
     * 配方类型
     */
    @ApiModelProperty(value = "配方类型")
    private String recipeType;
    /**
     * 映射胶料名称
     */
    @ApiModelProperty(value = "胶料名称")
    private String mapGlue;
    /**
     * 映射配方类型
     */
    @ApiModelProperty(value = "映射配方类型")
    private Double mapRecipeType;
    /**
     * 胶料名称
     */
    @ApiModelProperty(value = "胶料名称")
    private String reverseDayStockTag;

    /**
     * 获取映射字段的的key
     *
     * @return 映射字段的的key
     */
    public String getGlueRecipeMapKey() {
        return GenerageMapKeyUtils.createMapKey(getGlue(), getRecipeType());
    }
}
