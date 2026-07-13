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
    /** 生产定额，单位米/班 */
    @ApiModelProperty(value = "生产定额", name = "quota")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("QUOTA")
    @Excel(name = "ui.data.column.cd15MachineInfo.quota")
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

    /** 是否支持一出二：0--支持，1--不支持 */
    @ApiModelProperty(value = "是否支持一出二", name = "isOutTwo")
    @TableField("IS_OUT_TWO")
    @Excel(name = "ui.data.column.cd15MachineInfo.isOutTwo", dictType = "biz_yes_no")
    private String isOutTwo;

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
}
