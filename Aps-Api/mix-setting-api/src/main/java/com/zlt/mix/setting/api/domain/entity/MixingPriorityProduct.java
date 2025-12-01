package com.zlt.mix.setting.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 炼胶优先排产表对象
 *
 * @author Liam
 * @since 2025/4/17
 */
@ApiModel(value = "炼胶优先排产表对象", description = "炼胶单规格最小排产数对象")
@TableName("t_mixing_priority_product")
@Data
@EqualsAndHashCode(callSuper = true)
public class MixingPriorityProduct extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID", position = 10)
    private Long id;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 20)
    private String mixArea;
    /**
     * 胶料名称
     */
    @ApiModelProperty(value = "胶料名称", position = 30)
    private String glue;
    /**
     * 胶料名称
     */
    @ApiModelProperty(value = "优先排产胶料", position = 30)
    private String priorityGlue;
    /**
     * 备注
     */
    @ApiModelProperty(value = "备注", position = 80)
    private String remark;
}
