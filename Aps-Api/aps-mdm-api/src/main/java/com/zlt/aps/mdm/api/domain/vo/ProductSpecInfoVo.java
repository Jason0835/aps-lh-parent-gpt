package com.zlt.aps.mdm.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 物料编码对应的施工规格信息对象
 *
 * @author ZLT
 * @date 20250415
 */
@Data
@ApiModel(value = "物料编码对应的施工规格信息对象", description = "物料编码对应的施工规格信息对象")
public class ProductSpecInfoVo implements Serializable {

    /**
     * 硫化规格代号
     */
    @ApiModelProperty(value = "硫化规格代号", name = "specCode")
    private String specCode;

    /**
     * 施工代号，可转换成施工阶段
     */
    @ApiModelProperty(value = "施工代号", name = "constructionCode")
    private String constructionCode;

    /**
     * 生胎代码
     */
    @ApiModelProperty(value = "生胎代码", name = "embryoCode")
    private String embryoCode;
    /**
     * 合模压力
     */
    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    private BigDecimal mouldClampingPressure;
    /**
     * 模具行腔
     */
    @ApiModelProperty(value = "模具行腔", name = "moldCavity")
    private String moldCavity;
    /**
     * 成型法: MACHINE_TYPE
     * 1-1次法
     * 2-2次法
     */
    @ApiModelProperty(value = "成型法 1-1次法 2-2次法", name = "mouldMethod")
    private String mouldMethod;
}
