package com.zlt.aps.gsq.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 钢丝圈班制配置对象
 *
 * <p>用于配置每个工厂的6班次开始时间、结束时间等信息，供自动滚动更新窗口识别使用。
 * 6班次制对应关系：</p>
 * <ol>
 *   <li>1班：D日 中班（D为排程日期前一天）</li>
 *   <li>2班：D+1日 夜班</li>
 *   <li>3班：D+1日 早班</li>
 *   <li>4班：D+1日 中班</li>
 *   <li>5班：D+2日 夜班</li>
 *   <li>6班：D+2日 早班</li>
 * </ol>
 *
 * @author APS
 */
@ApiModel(value = "钢丝圈班制配置对象", description = "钢丝圈班制配置对象")
@Data
@TableName(value = "T_GSQ_SHIFT_CONFIG")
public class GsqShiftConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @Excel(name = "ui.data.column.gsq.shiftConfig.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 班次编码 */
    @Excel(name = "ui.data.column.gsq.shiftConfig.shiftCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    @ApiModelProperty(value = "班次编码", name = "shiftCode")
    @TableField(value = "SHIFT_CODE")
    private String shiftCode;

    /** 班次名称 */
    @Excel(name = "ui.data.column.gsq.shiftConfig.shiftName")
    @ImportValidated(required = true, maxLength = 50)
    @ApiModelProperty(value = "班次名称", name = "shiftName")
    @TableField(value = "SHIFT_NAME")
    private String shiftName;

    /** 班次顺序（1~6） */
    @Excel(name = "ui.data.column.gsq.shiftConfig.shiftOrder")
    @ImportValidated(required = true, digits = true, min = 1, max = 6)
    @ApiModelProperty(value = "班次顺序", name = "shiftOrder")
    @TableField(value = "SHIFT_ORDER")
    private Integer shiftOrder;

    /** 计划开始时间（HH:mm 或 HH:mm:ss） */
    @Excel(name = "ui.data.column.gsq.shiftConfig.planStartTime")
    @ImportValidated(required = true, maxLength = 8)
    @ApiModelProperty(value = "计划开始时间", name = "planStartTime")
    @TableField(value = "PLAN_START_TIME")
    private String planStartTime;

    /** 计划结束时间（HH:mm 或 HH:mm:ss） */
    @Excel(name = "ui.data.column.gsq.shiftConfig.planEndTime")
    @ImportValidated(required = true, maxLength = 8)
    @ApiModelProperty(value = "计划结束时间", name = "planEndTime")
    @TableField(value = "PLAN_END_TIME")
    private String planEndTime;

    /** 班次时长（小时） */
    @Excel(name = "ui.data.column.gsq.shiftConfig.shiftHours")
    @ImportValidated(digits = true, min = 0, max = 24)
    @ApiModelProperty(value = "班次时长（小时）", name = "shiftHours")
    @TableField(value = "SHIFT_HOURS")
    private Integer shiftHours;

    /** 是否跨天（biz_yes_no 字典） */
    @Excel(name = "ui.data.column.gsq.shiftConfig.crossDayFlag", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否跨天", name = "crossDayFlag")
    @TableField(value = "CROSS_DAY_FLAG")
    private String crossDayFlag;

    /** 是否开班（biz_yes_no 字典，1-开班，0-停用） */
    @Excel(name = "ui.data.column.gsq.shiftConfig.openFlag", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否开班", name = "openFlag")
    @TableField(value = "OPEN_FLAG")
    private String openFlag;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
