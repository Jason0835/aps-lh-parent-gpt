package com.zlt.aps.factory.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.common.core.annotation.Excel;
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
     * 制造示方书号
     */
    @ApiModelProperty(value = "制造示方书号", name = "embryoNo")
    private String embryoNo;

    /**
     * 文字示方书号
     */
    @ApiModelProperty(value = "文字示方书号", name = "textNo")
    private String textNo;

    /**
     * 硫化示方书号
     */
    @ApiModelProperty(value = "硫化示方书号", name = "lhNo")
    private String lhNo;

    /**
     * 胎胚号
     */
    @ApiModelProperty(value = "胎胚号", name = "embryoCode")
    private String embryoCode;

    /**
     * 主物料(胎胚描述)
     */
    @ApiModelProperty(value = "主物料(胎胚描述)", name = "mainMaterialDesc")
    private String mainMaterialDesc;

    /**
     * 产品状态
     */
    @ApiModelProperty(value = "产品状态", name = "productStatus")
    private String productStatus;

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
