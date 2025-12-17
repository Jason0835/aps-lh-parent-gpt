package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 原材料预警配置
 */
@ApiModel(value = "原材料预警配置", description = "原材料预警配置")
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "T_RAW_WARNING_CONFIG")
public class RawWarningConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @Excel(name = "工厂编码")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 原材料编码
     */
    @Excel(name = "原材料编码")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "原材料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 原材料名称
     */
    @Excel(name = "原材料名称")
    @ImportExcelValidated(required = true, maxLength = 100)
    @ApiModelProperty(value = "原材料名称", name = "materialName")
    @TableField(value = "MATERIAL_NAME")
    private String materialName;

    /**
     * 预警类型：1-用量偏差预警 2-新材料预警
     */
    @Excel(name = "预警类型", dictType = "raw_warning_type")
    @ApiModelProperty(value = "预警类型", name = "warningType")
    @TableField(value = "WARNING_TYPE")
    private String warningType;

    /**
     * 偏差上限（百分比），例如0.1表示10%
     */
    @Excel(name = "偏差上限")
    @ApiModelProperty(value = "偏差上限", name = "deviationUpper")
    @TableField(value = "DEVIATION_UPPER")
    private BigDecimal deviationUpper;

    /**
     * 偏差下限（百分比），例如-0.1表示-10%
     */
    @Excel(name = "偏差下限")
    @ApiModelProperty(value = "偏差下限", name = "deviationLower")
    @TableField(value = "DEVIATION_LOWER")
    private BigDecimal deviationLower;

    /**
     * 是否启用：0-禁用 1-启用
     */
    @Excel(name = "是否启用", dictType = "sys_yes_no")
    @ApiModelProperty(value = "是否启用", name = "enabled")
    @TableField(value = "ENABLED")
    private Integer enabled;

    /**
     * 预警级别：1-低 2-中 3-高
     */
    @Excel(name = "预警级别", dictType = "warning_level")
    @ApiModelProperty(value = "预警级别", name = "warningLevel")
    @TableField(value = "WARNING_LEVEL")
    private String warningLevel;

    /**
     * 通知方式：多个用逗号分隔，email,sms,wechat
     */
    @Excel(name = "通知方式")
    @ApiModelProperty(value = "通知方式", name = "notifyTypes")
    @TableField(value = "NOTIFY_TYPES")
    private String notifyTypes;
}