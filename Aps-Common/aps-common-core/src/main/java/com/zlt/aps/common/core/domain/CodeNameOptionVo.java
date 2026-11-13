package com.zlt.aps.common.core.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 编码名称下拉选项。
 *
 * <p>统一为前端下拉组件提供code/name字段，避免各业务页面重复转换下拉数据结构。</p>
 */
@Data
@ApiModel(value = "编码名称下拉选项")
public class CodeNameOptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务编码。
     */
    @ApiModelProperty(value = "业务编码")
    private String code;

    /**
     * 业务名称。
     */
    @ApiModelProperty(value = "业务名称")
    private String name;
}
