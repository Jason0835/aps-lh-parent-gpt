package com.zlt.aps.lh.api.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 文字示方更新入参对象。
 *
 * @author Codex
 */
@Data
@ApiModel(value = "文字示方更新入参")
public class LhGenerateTextMouldPlanDTO implements Serializable {

    private static final long serialVersionUID = -3450678844871646003L;

    @ApiModelProperty(value = "硫化排程结果主键", required = true)
    private Long id;

    @ApiModelProperty(value = "分厂编号")
    private String factoryCode;

    @ApiModelProperty(value = "是否确认替换同机台同日未发布换模计划")
    private Boolean confirmReplace;
}
