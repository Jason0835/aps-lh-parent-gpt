package com.zlt.aps.factory.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工厂月度排产物料施工信息对象
 *
 * @author ZLT
 * @date 20251209
 */
@Data
public class MonthPlanProductConstructionInfoVo implements Serializable {
    /**
     * 工厂编码
     */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    private String factoryCode;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编号", name = "materialCode")
    private String materialCode;

    /**
     * 规格代号
     */
    @ApiModelProperty(value = "规格代号", name = "specCode")
    private String specCode;

    /**
     * 施工代号
     */
    @ApiModelProperty(value = "施工代号", name = "constructionCode")
    private String constructionCode;

    /**
     * 是否零度材料
     */
    @ApiModelProperty(value = "是否零度材料", name = "isZeroRack")
    private String isZeroRack;
    /**
     * 胎胚号
     */
    @ApiModelProperty(value = "胎胚号", name = "embryoCode")
    private String embryoCode;

    /**
     * 成型法
     */
    @ApiModelProperty(value = "成型法", name = "mouldMethod")
    private String mouldMethod;

    /**
     * 夏季机械硫化时间--单位秒
     */
    @ApiModelProperty(value = "夏季机械硫化时间", name = "curingTime")
    private Integer curingTime;

    /**
     * 冬季机械硫化时间--单位秒
     */
    @ApiModelProperty(value = "冬季机械硫化时间", name = "curingTime2")
    private Integer curingTime2;

    /**
     * 模具型腔
     */
    @ApiModelProperty(value = "模具型腔", name = "mouldCavity")
    private String mouldCavity;

    /**
     * 合模压力
     */
    @ApiModelProperty(value = "合模压力", name = "mouldClampingPressure")
    private BigDecimal mouldClampingPressure;
}
