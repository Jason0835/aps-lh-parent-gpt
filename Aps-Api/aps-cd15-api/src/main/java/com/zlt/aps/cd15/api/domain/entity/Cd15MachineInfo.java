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
 * 斜裁机台基础信息。
 */
@Data
@ApiModel(value = "斜裁机台基础信息", description = "斜裁机台基础信息")
@TableName("t_cd15_machine_info")
public class Cd15MachineInfo extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15MachineInfo.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @ImportExcelValidated(required = true, maxLength = 30)
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd15MachineInfo.machineCode")
    private String machineCode;

    /** 机台名称 */
    @ApiModelProperty(value = "机台名称", name = "machineName")
    @TableField("MACHINE_NAME")
    private String machineName;

    /** 帘布宽度上限 */
    @ApiModelProperty(value = "帘布宽度上限", name = "clothWidthMax")
    @TableField("CLOTH_WIDTH_MAX")
    @Excel(name = "ui.data.column.cd15MachineInfo.clothWidthMax")
    private Double clothWidthMax;

    /** 帘布宽度下限 */
    @ApiModelProperty(value = "帘布宽度下限", name = "clothWidthMin")
    @TableField("CLOTH_WIDTH_MIN")
    @Excel(name = "ui.data.column.cd15MachineInfo.clothWidthMin")
    private Double clothWidthMin;
    /** 历史生产定额保留字段，不参与页面维护、Excel导入导出和自动排程 */
    @ApiModelProperty(value = "历史生产定额保留字段", name = "quota")
    @TableField("QUOTA")
    private Double quota;

    /** 班制 */
    @ApiModelProperty(value = "班制", name = "classShift")
    @TableField("CLASS_SHIFT")
    private String classShift;

    /** 开机班次 */
    @ApiModelProperty(value = "开机班次", name = "openMachineClass")
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("OPEN_MACHINE_CLASS")
    @Excel(name = "ui.data.column.cd15MachineInfo.openMachineClass", dictType = "class_num_three_plan")
    private String openMachineClass;

    /** 是否支持一出二：1--支持，0--不支持 */
    @ApiModelProperty(value = "是否支持一出二", name = "isOutTwo")
    @TableField("IS_OUT_TWO")
//    @Excel(name = "ui.data.column.cd15MachineInfo.isOutTwo", dictType = "biz_yes_no")
    private String isOutTwo;

    /** 是否支持单裁：1--支持，0--不支持 */
    @ApiModelProperty(value = "是否支持单裁", name = "singleCutFlag")
    @ImportExcelValidated(required = true, maxLength = 1)
    @TableField("SINGLE_CUT_FLAG")
    @Excel(name = "ui.data.column.cd15MachineInfo.singleCutFlag", dictType = "biz_yes_no")
    private String singleCutFlag;

    /** 是否支持分裁：1--支持，0--不支持 */
    @ApiModelProperty(value = "是否支持分裁", name = "splitCutFlag")
    @ImportExcelValidated(required = true, maxLength = 1)
    @TableField("SPLIT_CUT_FLAG")
    @Excel(name = "ui.data.column.cd15MachineInfo.splitCutFlag", dictType = "biz_yes_no")
    private String splitCutFlag;

    /** 默认裁断模式：SINGLE、SPLIT、DAILY_OUTPUT */
    @ApiModelProperty(value = "默认裁断模式", name = "defaultCutMode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("DEFAULT_CUT_MODE")
    @Excel(name = "ui.data.column.cd15MachineInfo.defaultCutMode", dictType = "cd15_default_cut_mode")
    private String defaultCutMode;

    /** 历史日产量模式切换阈值保留字段，不参与自动排程 */
    @ApiModelProperty(value = "历史日产量模式切换阈值保留字段，不参与自动排程", name = "dailyOutputModeThreshold")
    @TableField("DAILY_OUTPUT_MODE_THRESHOLD")
//    @Excel(name = "ui.data.column.cd15MachineInfo.dailyOutputModeThreshold")
    private Double dailyOutputModeThreshold;

    /** 单裁班产能力，单位米/班 */
    @ApiModelProperty(value = "单裁班产能力", name = "singleShiftCapacity")
    @TableField("SINGLE_SHIFT_CAPACITY")
    @Excel(name = "ui.data.column.cd15MachineInfo.singleShiftCapacity")
    private Double singleShiftCapacity;

    /** 分裁班产能力，单位米/班 */
    @ApiModelProperty(value = "分裁班产能力", name = "splitShiftCapacity")
    @TableField("SPLIT_SHIFT_CAPACITY")
    @Excel(name = "ui.data.column.cd15MachineInfo.splitShiftCapacity")
    private Double splitShiftCapacity;

    /** 机台状态：1--启用，0--禁用 */
    @ApiModelProperty(value = "机台状态", name = "status")
    @ImportExcelValidated(required = true, maxLength = 1)
    @TableField("STATUS")
    @Excel(name = "ui.data.column.cd15MachineInfo.status", dictType = "sys_enable_disable")
    private String status;

    /** 支持的钢带宽度 */
    @ApiModelProperty(value = "支持的钢带宽度", name = "steelStripWidth")
    @TableField("STEEL_STRIP_WIDTH")
//    @Excel(name = "ui.data.column.cd15MachineInfo.steelStripWidth")
    private Double steelStripWidth;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField("REMARK")
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
