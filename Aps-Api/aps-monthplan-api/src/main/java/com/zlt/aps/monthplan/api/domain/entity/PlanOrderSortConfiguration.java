package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：PlanOrderSortConfiguration.java
 * 描    述：业务排序配置对象 t_mdm_plan_order_config
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-17
 */

@Data
@TableName(value = "T_MDM_PLAN_ORDER_CONFIG")
@ApiModel(value = "业务排序配置对象", description = "业务排序配置对象 ")
public class PlanOrderSortConfiguration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编码 默认AH01
     */
    @Excel(name = "ui.data.column.businessSortConfiguration.factoryCode")
    @ApiModelProperty(value = "分厂编码 默认AH01", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 业务类型 01 库存冲销 02 月度计划排产
     */
    @Excel(name = "ui.data.column.businessSortConfiguration.businessType")
    @ApiModelProperty(value = "业务类型 01 库存冲销 02 月度计划排产", name = "businessType")
    @TableField(value = "BUSINESS_TYPE")
    private String businessType;

    /**
     * 层级 1 第一顺序 2 第二顺序 3 第三顺序
     */
    @Excel(name = "ui.data.column.businessSortConfiguration.hierarchy")
    @ApiModelProperty(value = "层级 1 第一顺序 2 第二顺序 3 第三顺序", name = "hierarchy")
    @TableField(value = "HIERARCHY")
    private Integer hierarchy;

    /**
     * 配置项编码
     */
    @Excel(name = "ui.data.column.businessSortConfiguration.optionCode")
    @ApiModelProperty(value = "配置项编码", name = "optionCode")
    @TableField(value = "OPTION_CODE")
    private String optionCode;

    /**
     * 配置项说明
     */
    @Excel(name = "ui.data.column.businessSortConfiguration.optionName")
    @ApiModelProperty(value = "配置项说明", name = "optionName")
    @TableField(value = "OPTION_NAME")
    private String optionName;

    /**
     * 排序方式 1 升序 2 降序
     */
    @Excel(name = "ui.data.column.businessSortConfiguration.sortOrder")
    @ApiModelProperty(value = "排序方式 1 升序 2 降序", name = "sortOrder")
    @TableField(value = "SORT_ORDER")
    private Integer sortOrder;

    /**
     * 优先级值
     */
    @Excel(name = "ui.data.column.businessSortConfiguration.priority")
    @ApiModelProperty(value = "优先级值", name = "priority")
    @TableField(value = "PRIORITY")
    private Integer priority;

}