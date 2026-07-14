package com.zlt.aps.nc.api.domain.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 内衬班制配置
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_SHIFT_CONFIG")
@ApiModel(value = "DjShiftConfig对象", description = "内衬班制配置")
public class NcShiftConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @Excel(name = "ui.data.column.dj.shiftConfig.factoryCode")
    @ApiModelProperty(value = "工厂编号")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /** 班次编码（如 01=夜班, 02=早班, 03=中班） */
    @Excel(name = "ui.data.column.dj.shiftConfig.shiftCode")
    @ApiModelProperty(value = "班次编码")
    @TableField("SHIFT_CODE")
    private String shiftCode;

    /** 班次名称（如 夜班、早班、中班） */
    @Excel(name = "ui.data.column.dj.shiftConfig.shiftName")
    @ApiModelProperty(value = "班次名称")
    @TableField("SHIFT_NAME")
    private String shiftName;

    /** 班次顺序（1,2,3...），用于确定班次循环顺序 */
    @Excel(name = "ui.data.column.dj.shiftConfig.shiftOrder")
    @ApiModelProperty(value = "班次顺序")
    @TableField("SHIFT_ORDER")
    private Integer shiftOrder;

    /** 计划开始时间（HH:mm:ss） */
    @Excel(name = "ui.data.column.dj.shiftConfig.planStartTime")
    @ApiModelProperty(value = "计划开始时间")
    @TableField("PLAN_START_TIME")
    private String planStartTime;

    /** 计划结束时间（HH:mm:ss） */
    @Excel(name = "ui.data.column.dj.shiftConfig.planEndTime")
    @ApiModelProperty(value = "计划结束时间")
    @TableField("PLAN_END_TIME")
    private String planEndTime;

    /** 是否跨天（0-否，1-是） */
    @Excel(name = "ui.data.column.dj.shiftConfig.crossDayFlag", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否跨天")
    @TableField("CROSS_DAY_FLAG")
    private String crossDayFlag;

    /** 是否开班（0-否，1-是） */
    @Excel(name = "ui.data.column.dj.shiftConfig.openFlag", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否开班")
    @TableField("OPEN_FLAG")
    private String openFlag;
}
