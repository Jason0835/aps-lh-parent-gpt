package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@ApiModel(value = "胎面班制配置对象", description = "胎面班制配置对象")
@Data
@TableName(value = "T_TM_SHIFT_CONFIG")
public class TmShiftConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tm.shiftConfig.factoryCode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tm.shiftConfig.scheduleDate")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    @Excel(name = "ui.data.column.tm.shiftConfig.shiftCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    @ApiModelProperty(value = "班次编码", name = "shiftCode")
    @TableField(value = "SHIFT_CODE")
    private String shiftCode;

    @Excel(name = "ui.data.column.tm.shiftConfig.shiftName")
    @ImportValidated(required = true, maxLength = 50)
    @ApiModelProperty(value = "班次名称", name = "shiftName")
    @TableField(value = "SHIFT_NAME")
    private String shiftName;

    @Excel(name = "ui.data.column.tm.shiftConfig.shiftOrder")
    @ImportValidated(required = true, digits = true, min = 0, max = 99)
    @ApiModelProperty(value = "班次顺序", name = "shiftOrder")
    @TableField(value = "SHIFT_ORDER")
    private Integer shiftOrder;

    @Excel(name = "ui.data.column.tm.shiftConfig.planStartTime")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "计划开始时间", name = "planStartTime")
    @TableField(value = "PLAN_START_TIME")
    private Date planStartTime;

    @Excel(name = "ui.data.column.tm.shiftConfig.planEndTime")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "计划结束时间", name = "planEndTime")
    @TableField(value = "PLAN_END_TIME")
    private Date planEndTime;

    @Excel(name = "ui.data.column.tm.shiftConfig.crossDayFlag", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否跨天", name = "crossDayFlag")
    @TableField(value = "CROSS_DAY_FLAG")
    private String crossDayFlag;

    @Excel(name = "ui.data.column.tm.shiftConfig.openFlag", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否开班", name = "openFlag")
    @TableField(value = "OPEN_FLAG")
    private String openFlag;
}
