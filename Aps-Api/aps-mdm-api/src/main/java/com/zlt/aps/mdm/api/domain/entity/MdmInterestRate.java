package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmInterestRate.java
 * 描    述：利率优先等级配置对象 t_mdm_interest_rate
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-03
 */

@ApiModel(value = "利率优先等级配置对象", description = "利率优先等级配置对象 ")
@Data
@TableName(value = "T_MDM_INTEREST_RATE")
public class MdmInterestRate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmInterestRate.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 利率值下限
     */
    @ImportExcelValidated(required = true, min = 0, max = 9.99)
    @Excel(name = "ui.data.column.mdmInterestRate.valueMin")
    @ApiModelProperty(value = "利率值下限", name = "valueMin")
    @TableField(value = "VALUE_MIN")
    private BigDecimal valueMin;

    /**
     * 利率值上限
     */
    @ImportExcelValidated(required = true, min = 0, max = 9.99)
    @Excel(name = "ui.data.column.mdmInterestRate.valueMax")
    @ApiModelProperty(value = "利率值上限", name = "valueMax")
    @TableField(value = "VALUE_MAX")
    private BigDecimal valueMax;

    /**
     * 优先等级值(可正负)
     */
    @ImportExcelValidated(required = true, digits = true)
    @Excel(name = "ui.data.column.mdmInterestRate.priority")
    @ApiModelProperty(value = "优先等级值(可正负)", name = "priority")
    @TableField(value = "PRIORITY")
    private Integer priority;

    /**
     * 版本号
     */
//    @Excel(name = "ui.data.column.mdmInterestRate.version")
    @ApiModelProperty(value = "版本号", name = "version")
    @TableField(value = "VERSION")
    private Long version;

    @ApiModelProperty(value = "备注", hidden = true)
    @TableField(exist = false)
    private String remark;
}