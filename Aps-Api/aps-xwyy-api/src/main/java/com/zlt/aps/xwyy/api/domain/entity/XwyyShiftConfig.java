package com.zlt.aps.xwyy.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "纤维压延班次配置", description = "纤维压延班次配置")
@TableName("t_xwyy_shift_config")
public class XwyyShiftConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("班次编码")
    @TableField("SHIFT_CODE")
    @Excel(name = "ui.data.column.xwyyShiftConfig.shiftCode")
    private String shiftCode;

    @ApiModelProperty("班次名称")
    @TableField("SHIFT_NAME")
    @Excel(name = "ui.data.column.xwyyShiftConfig.shiftName")
    private String shiftName;

    @ApiModelProperty("班次序号")
    @TableField("SHIFT_ORDER")
    @Excel(name = "ui.data.column.xwyyShiftConfig.shiftOrder")
    private Integer shiftOrder;

    @ApiModelProperty("开始时间（HH:mm:ss格式）")
    @TableField("START_TIME")
    @Excel(name = "ui.data.column.xwyyShiftConfig.startTime")
    private String startTime;

    @ApiModelProperty("结束时间（HH:mm:ss格式）")
    @TableField("END_TIME")
    @Excel(name = "ui.data.column.xwyyShiftConfig.endTime")
    private String endTime;

    @ApiModelProperty("班次时长（小时）")
    @TableField("SHIFT_HOURS")
    @Excel(name = "ui.data.column.xwyyShiftConfig.shiftHours")
    private Integer shiftHours;

    @ApiModelProperty("是否跨天：0-否 1-是")
    @TableField("IS_CROSS_DAY")
    @Excel(name = "ui.data.column.xwyyShiftConfig.isCrossDay", dictType = "biz_yes_no")
    private Integer isCrossDay;

    @ApiModelProperty("排程天数：1-第一天 2-第二天 3-第三天")
    @TableField("SCHEDULE_DAY")
    @Excel(name = "ui.data.column.xwyyShiftConfig.scheduleDay")
    private Integer scheduleDay;

    @ApiModelProperty("当天班次序号：该天第几个班")
    @TableField("DAY_SHIFT_ORDER")
    @Excel(name = "ui.data.column.xwyyShiftConfig.dayShiftOrder")
    private Integer dayShiftOrder;

    @ApiModelProperty("对应结果表字段：CLASS1~CLASS8")
    @TableField("CLASS_FIELD")
    @Excel(name = "ui.data.column.xwyyShiftConfig.classField")
    private String classField;

    @ApiModelProperty("工厂编号")
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.xwyyShiftConfig.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    @ApiModelProperty("是否启用：0-禁用 1-启用")
    @TableField("IS_ACTIVE")
    @Excel(name = "ui.data.column.xwyyShiftConfig.isActive", dictType = "sys_enable_disable")
    private Integer isActive;
}
