package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 炼胶生产模式
 *
 * @author Liam
 * @since 2025/4/24
 */
@ApiModel(value = "炼胶优先排产表对象", description = "炼胶单规格最小排产数对象")
@TableName("t_mixing_production_model")
@Data
@EqualsAndHashCode(callSuper = true)
public class MixingProductionModel extends ZltBaseEntity {
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
     * 生产模式名称
     */
    @ApiModelProperty(value = "生产模式名称")
    private String modelName;
    /**
     * 前置胶料
     */
    @ApiModelProperty(value = "前置胶料")
    private String beforeGlue;
    /**
     * 前置数量(车)
     */
    @ApiModelProperty(value = "前置数量(车)")
    private Double beforeQty;
    /**
     * 胶料名称
     */
    @ApiModelProperty(value = "胶料名称")
    private String glue;
    /**
     * 限制机台
     */
    @ApiModelProperty(value = "限制机台")
    private String machineCode;
    /**
     * 后置胶料
     */
    @ApiModelProperty(value = "后置胶料")
    private String afterGlue;
    /**
     * 后置数量(车)
     */
    @ApiModelProperty(value = "后置数量(车)")
    private Double afterQty;
}
