package com.zlt.aps.mp.factory.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 月计划调整：计划待调整量信息
 *
 * @author ZLT
 * @date 20260606
 */
@ApiModel(value = "调整-结构调整记录对象", description = "调整-结构调整记录对象 ")
@Data
public class MpSkuAdjustInfoVo implements Serializable {
    /**
     * 分厂编号
     */
    @ApiModelProperty(value = "分厂编号，字典：biz_factory_name", name = "factoryCode")
    private String factoryCode;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    private String monthPlanVersion;

    /**
     * 排产计划版本
     */
    @ApiModelProperty(value = "排产计划版本", name = "productionVersion")
    private String productionVersion;

    /**
     * 最新需求计划版本(每次调整后变化)
     */
    @ApiModelProperty(value = "最新需求计划版本(每次调整后变化)", name = "lastMonthPlanVersion")
    private String lastMonthPlanVersion;

    /**
     * 产品结构
     */
    @ApiModelProperty(value = "产品结构", name = "structureName")
    private String structureName;

    /**
     * MES物料编码
     */
    @ApiModelProperty(value = "MES物料编码", name = "mesMaterialCode")
    private String mesMaterialCode;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    private String materialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    private String materialDesc;
    /**
     * 产品状态
     */
    @ApiModelProperty(value = "产品状态", name = "productStatus")
    private String productStatus;
    /**
     * 生胎代码
     */
    @ApiModelProperty(value = "胎胚号", name = "embryoCode")
    private String embryoCode;
    /**
     * 施工阶段 0 无工艺 1 试制 2 量试 3 正式
     */
    @ApiModelProperty(value = "施工阶段 0 无工艺 1 试制 2 量试 3 正式", name = "constructionStage")
    private String constructionStage;
    /**
     * 主花纹
     */
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    private String mainPattern;


    /**
     * 花纹
     */
    @ApiModelProperty(value = "花纹", name = "pattern")
    private String pattern;

    /**
     * 待调整量
     */
    @ApiModelProperty(value = "待调整量", name = "pendingQty")
    private Integer pendingQty;

    /**
     * 获取待调整量的key
     *
     * @return
     */
    public String getPendingQtyKey() {
        String groupKeyFormat = "%s|*|%s";
        return String.format(groupKeyFormat, materialDesc, constructionStage);
    }
}
