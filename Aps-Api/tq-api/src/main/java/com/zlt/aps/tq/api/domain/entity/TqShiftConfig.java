package com.zlt.aps.tq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 胎圈班制配置对象。
 *
 * <p>对齐胎面 TmShiftConfig，承载 1~6 班计划开始/结束时间和开班标识，
 * 用于自动滚动窗口识别（班前 30 分钟触发）。</p>
 *
 * @author APS
 */
@ApiModel(value = "胎圈班制配置对象", description = "胎圈班制配置对象")
@Data
@TableName(value = "T_TQ_SHIFT_CONFIG")
public class TqShiftConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编码。 */
    @Excel(name = "ui.data.column.tq.shiftConfig.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 班次编码。 */
    @Excel(name = "ui.data.column.tq.shiftConfig.shiftCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    @ApiModelProperty(value = "班次编码", name = "shiftCode")
    @TableField(value = "SHIFT_CODE")
    private String shiftCode;

    /** 班次名称。 */
    @Excel(name = "ui.data.column.tq.shiftConfig.shiftName")
    @ImportValidated(required = true, maxLength = 50)
    @ApiModelProperty(value = "班次名称", name = "shiftName")
    @TableField(value = "SHIFT_NAME")
    private String shiftName;

    /** 班次顺序（1~6）。 */
    @Excel(name = "ui.data.column.tq.shiftConfig.shiftOrder")
    @ImportValidated(required = true, digits = true, min = 0, max = 99)
    @ApiModelProperty(value = "班次顺序", name = "shiftOrder")
    @TableField(value = "SHIFT_ORDER")
    private Integer shiftOrder;

    /** 计划开始时间（HH:mm 或 HH:mm:ss）。 */
    @Excel(name = "ui.data.column.tq.shiftConfig.planStartTime")
    @ImportValidated(required = true, maxLength = 8)
    @ApiModelProperty(value = "计划开始时间", name = "planStartTime")
    @TableField(value = "PLAN_START_TIME")
    private String planStartTime;

    /** 计划结束时间。 */
    @Excel(name = "ui.data.column.tq.shiftConfig.planEndTime")
    @ImportValidated(required = true, maxLength = 8)
    @ApiModelProperty(value = "计划结束时间", name = "planEndTime")
    @TableField(value = "PLAN_END_TIME")
    private String planEndTime;

    /** 班次时长（小时）。 */
    @Excel(name = "ui.data.column.tq.shiftConfig.shiftHours")
    @ImportValidated(digits = true, min = 0, max = 24)
    @ApiModelProperty(value = "班次时长（小时）", name = "shiftHours")
    @TableField(value = "SHIFT_HOURS")
    private Integer shiftHours;

    /** 是否跨天（0-否 1-是）。 */
    @Excel(name = "ui.data.column.tq.shiftConfig.crossDayFlag", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否跨天", name = "crossDayFlag")
    @TableField(value = "CROSS_DAY_FLAG")
    private String crossDayFlag;

    /** 是否开班（0-否 1-是）。 */
    @Excel(name = "ui.data.column.tq.shiftConfig.openFlag", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否开班", name = "openFlag")
    @TableField(value = "OPEN_FLAG")
    private String openFlag;

    /** 备注。 */
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
