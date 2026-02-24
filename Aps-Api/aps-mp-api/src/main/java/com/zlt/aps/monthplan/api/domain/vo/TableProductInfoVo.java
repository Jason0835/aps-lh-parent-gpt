package com.zlt.aps.monthplan.api.domain.vo;

import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 物料基础信息-前端对
 *
 * @author ZLT
 * @date 20250912
 */
@Data
@ApiModel(value = "物料基础信息列表查询结果Vo", description = "物料基础信息列表查询结果Vo")
public class TableProductInfoVo extends MdmMaterialInfo {
    /**
     * 模具号多个以,隔开
     */
    @ApiModelProperty(value = "模具号多个以,隔开", name = "mouldNo")
    private String mouldNo;
    /**
     * 规格代号多个以,隔开
     */
    @ApiModelProperty(value = "规格代号多个以,隔开", name = "specCode")
    private String specCode;
    /**
     * 模具数量
     */
    @ApiModelProperty(value = "模具数量", name = "mouldNumber")
    private Integer mouldNumber;
    /**
     * 胚胎代码 多个以,隔开
     */
    @ApiModelProperty(value = "胚胎代码 多个以,隔开", name = "embryoCode")
    private String embryoCode;
    /**
     * 施工规格代号 多个以,隔开
     */
    @ApiModelProperty(value = "施工规格代号 多个以,隔开", name = "constructionSpecCode")
    private String constructionSpecCode;
    /**
     * 施工代号 多个以,隔开
     */
    @ApiModelProperty(value = "施工代号 多个以,隔开", name = "constructionCode")
    private String constructionCode;
    /**
     * 是看查询模具未配置数据 0-否，1-是
     */
    @ApiModelProperty(value = "是看查询模具未配置数据，0-否，1-是", name = "isMouldNullData")
    private String isMouldNullData;
    /**
     * 是否查询施工未配置数据 0-否，1-是
     */
    @ApiModelProperty(value = "是否查询施工未配置数据，0-否，1-是", name = "isConstructionNullData")
    private String isConstructionNullData;
}
