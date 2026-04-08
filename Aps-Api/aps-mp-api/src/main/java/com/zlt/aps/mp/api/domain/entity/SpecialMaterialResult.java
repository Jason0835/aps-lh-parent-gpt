package com.zlt.aps.mp.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SpecialMaterialResult.java
 * 描    述：S2-0604.排产结果-特殊材料排产结果对象 t_mp_special_material_result
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-04-08
 */

@Data
@EqualsAndHashCode(callSuper=false)
@TableName(value = "T_MP_SPECIAL_MATERIAL_RESULT")
@ApiModel(value = "S2-0604.排产结果-特殊材料排产结果对象", description = "S2-0604.排产结果-特殊材料排产结果对象 ")
public class SpecialMaterialResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 需求计划版本
     */
    @ApiModelProperty(value = "需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 排产版本号
     */
    @ApiModelProperty(value = "排产版本号", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

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
     * 生产实际排产量
     */
    @ApiModelProperty(value = "生产实际排产量", name = "totalQty")
    @TableField(value = "TOTAL_QTY")
    private Long totalQty;

    /**
     * 标准长
     */
    @ApiModelProperty(value = "标准长", name = "standardLength")
    @TableField(value = "STANDARD_LENGTH")
    private Long standardLength;
}