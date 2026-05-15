package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SkuMoldCapacityAllocateLog.java
 * 描    述：排产过程_计划模具受限日志表 T_MP_MOLD_CAPACITY_LOG
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20260515
 */

@Data
@TableName(value = "T_MP_MOLD_CAPACITY_LOG")
@ApiModel(value = "排产过程_计划模具受限日志对象", description = "排产过程_计划模具受限日志对象")
public class MpSkuMoldCapacityAllocateLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 计划类型 01 正常 02 订单预测 03 实单模拟
     */
    @ApiModelProperty(value = "计划类型", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;

    /**
     * 物料编码
     */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 物料描述
     */
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(value = "MATERIAL_DESC")
    private String materialDesc;

    /**
     * 产品结构
     */
    @ApiModelProperty(value = "产品结构", name = "structureName")
    @TableField(value = "STRUCTURE_NAME")
    private String structureName;

    /**
     * 主花纹
     */
    @ApiModelProperty(value = "主花纹", name = "mainPattern")
    @TableField(value = "MAIN_PATTERN")
    private String mainPattern;

    /**
     * 日硫化量(单模)
     */
    @ApiModelProperty(value = "日硫化量", name = "dayVulcanizationQty")
    @TableField(value = "DAY_VULCANIZATION_QTY")
    private Integer dayVulcanizationQty;

    /**
     * 模具最大产能
     */
    @ApiModelProperty(value = "模具最大产能", name = "maxMoldCapacity")
    @TableField(value = "MAX_MOLD_CAPACITY")
    private Integer maxMoldCapacity;
    /**
     * 总优先级需求量
     */
    @ApiModelProperty(value = "总优先级需求量", name = "maxHeightQty")
    @TableField(value = "MAX_HEIGHT_QTY")
    private Integer maxHeightQty;
    /**
     * 高优先级-分摊量
     */
    @ApiModelProperty(value = "高优先级-分摊量", name = "heightQty")
    @TableField(value = "HEIGHT_QTY")
    private Integer heightQty;

    /**
     * 总净需求
     */
    @ApiModelProperty(value = "总净需求", name = "maxNetQty")
    @TableField(value = "MAX_NET_QTY")
    private Integer maxNetQty;

    /**
     * 排产净需求-分摊量
     */
    @ApiModelProperty(value = "排产净需求-分摊量", name = "netQty")
    @TableField(value = "NET_QTY")
    private Integer netQty;
}
