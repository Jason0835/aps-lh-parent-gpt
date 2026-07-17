package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;


@ApiModel(value = "胎侧班制配置对象", description = "胎侧班制配置对象")
@Data
@TableName(value = "T_TC_SHIFT_CONFIG")
public class TcShiftConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tc.shiftConfig.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 自动排程日期，历史空日期配置不参与自动排程 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "ui.data.column.tc.shiftConfig.scheduleDate", width = 20, dateFormat = "yyyy-MM-dd")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "排程日期", name = "scheduleDate", required = true)
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    @Excel(name = "ui.data.column.tc.shiftConfig.shiftCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    @ApiModelProperty(value = "班次编码", name = "shiftCode")
    @TableField(value = "SHIFT_CODE")
    private String shiftCode;

    @Excel(name = "ui.data.column.tc.shiftConfig.shiftName")
    @ImportValidated(required = true, maxLength = 50)
    @ApiModelProperty(value = "班次名称", name = "shiftName")
    @TableField(value = "SHIFT_NAME")
    private String shiftName;

    @Excel(name = "ui.data.column.tc.shiftConfig.shiftOrder")
    @ImportValidated(required = true, digits = true, min = 0, max = 99)
    @ApiModelProperty(value = "班次顺序", name = "shiftOrder")
    @TableField(value = "SHIFT_ORDER")
    private Integer shiftOrder;

    @Excel(name = "ui.data.column.tc.shiftConfig.planStartTime")
    @ImportValidated(required = true, maxLength = 8)
    @ApiModelProperty(value = "计划开始时间", name = "planStartTime")
    @TableField(value = "PLAN_START_TIME")
    private String planStartTime;

    @Excel(name = "ui.data.column.tc.shiftConfig.planEndTime")
    @ImportValidated(required = true, maxLength = 8)
    @ApiModelProperty(value = "计划结束时间", name = "planEndTime")
    @TableField(value = "PLAN_END_TIME")
    private String planEndTime;

    @Excel(name = "ui.data.column.tc.shiftConfig.shiftHours")
    @ImportValidated(digits = true, min = 0, max = 24)
    @ApiModelProperty(value = "班次时长（小时）", name = "shiftHours")
    @TableField(value = "SHIFT_HOURS")
    private Integer shiftHours;

    @Excel(name = "ui.data.column.tc.shiftConfig.crossDayFlag", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否跨天", name = "crossDayFlag")
    @TableField(value = "CROSS_DAY_FLAG")
    private String crossDayFlag;

    @Excel(name = "ui.data.column.tc.shiftConfig.openFlag", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否开班", name = "openFlag")
    @TableField(value = "OPEN_FLAG")
    private String openFlag;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
