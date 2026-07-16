package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 班次配置。
 */
@Data
@ApiModel(value = "班次配置", description = "班次配置")
@TableName("t_cd15_shift_config")
public class Cd15ShiftConfig extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @ImportExcelValidated(required = true, maxLength = 64)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15ShiftConfig.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 班次编码 */
    @ApiModelProperty(value = "班次编码", name = "shiftCode")
    @ImportExcelValidated(required = true, maxLength = 64)
    @TableField("SHIFT_CODE")
    @Excel(name = "ui.data.column.cd15ShiftConfig.shiftCode")
    private String shiftCode;

    /** 班次名称 */
    @ApiModelProperty(value = "班次名称", name = "shiftName")
    @TableField("SHIFT_NAME")
    @Excel(name = "ui.data.column.cd15ShiftConfig.shiftName")
    private String shiftName;

    /** 班次序号 */
    @ApiModelProperty(value = "班次序号", name = "shiftOrder")
    @TableField("SHIFT_ORDER")
    @Excel(name = "ui.data.column.cd15ShiftConfig.shiftOrder")
    private Integer shiftOrder;

    /** 开始时间（HH:mm:ss格式） */
    @ApiModelProperty(value = "开始时间", name = "startTime")
    @ImportExcelValidated(required = true, maxLength = 32)
    @TableField("START_TIME")
    @Excel(name = "ui.data.column.cd15ShiftConfig.startTime")
    private String startTime;

    /** 结束时间（HH:mm:ss格式） */
    @ApiModelProperty(value = "结束时间", name = "endTime")
    @ImportExcelValidated(required = true, maxLength = 32)
    @TableField("END_TIME")
    @Excel(name = "ui.data.column.cd15ShiftConfig.endTime")
    private String endTime;

    /** 班次时长（小时） */
    @ApiModelProperty(value = "班次时长", name = "shiftHours")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("SHIFT_HOURS")
    @Excel(name = "ui.data.column.cd15ShiftConfig.shiftHours")
    private Integer shiftHours;

    /** 是否跨天：0-否 1-是 */
    @ApiModelProperty(value = "是否跨天", name = "isCrossDay")
    @TableField("IS_CROSS_DAY")
    @Excel(name = "ui.data.column.cd15ShiftConfig.isCrossDay", dictType = "biz_yes_no")
    private Integer isCrossDay;

    /** 排程天数：1-第一天 2-第二天 3-第三天 */
    @ApiModelProperty(value = "排程天数", name = "scheduleDay")
    @TableField("SCHEDULE_DAY")
    @Excel(name = "ui.data.column.cd15ShiftConfig.scheduleDay")
    private Integer scheduleDay;

    /** 当天班次序号：该天第几个班 */
    @ApiModelProperty(value = "当天班次序号", name = "dayShiftOrder")
    @TableField("DAY_SHIFT_ORDER")
    @Excel(name = "ui.data.column.cd15ShiftConfig.dayShiftOrder")
    private Integer dayShiftOrder;

    /** 对应结果表字段：CLASS1~CLASS8 */
    @ApiModelProperty(value = "对应结果表字段", name = "classField")
    @TableField("CLASS_FIELD")
    @Excel(name = "ui.data.column.cd15ShiftConfig.classField")
    private String classField;

    /** 是否启用：0-禁用 1-启用 */
    @ApiModelProperty(value = "是否启用", name = "isActive")
    @ImportExcelValidated(required = true, maxLength = 1)
    @TableField("IS_ACTIVE")
    @Excel(name = "ui.data.column.cd15ShiftConfig.isActive", dictType = "sys_enable_disable")
    private Integer isActive;
}