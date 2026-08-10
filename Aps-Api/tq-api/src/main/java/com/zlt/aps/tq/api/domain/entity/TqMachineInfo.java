package com.zlt.aps.tq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_tq_machine_info")
@ApiModel(value = "胎圈机台信息对象", description = "胎圈机台信息对象")
public class TqMachineInfo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "机台编号", position = 20)
    @Excel(name = "ui.data.column.machine.machineCode")
    @TableField("MACHINE_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String machineCode;

    @ApiModelProperty(value = "机台名称", position = 30)
    @Excel(name = "ui.data.column.machine.machineName")
    @TableField("MACHINE_NAME")
    @ImportValidated(required = true, maxLength = 60)
    private String machineName;

    @ApiModelProperty(value = "班制，对应数据字典LH_CLASS_SHIFT", position = 40)
    @Excel(name = "ui.data.column.machine.classShift", dictType = "LH_CLASS_SHIFT")
    @TableField("CLASS_SHIFT")
    @ImportValidated(maxLength = 20, required = true)
    private String classShift;

    @ApiModelProperty(value = "开机班次，对应数据字典class_num_three_plan", position = 50)
    @Excel(name = "ui.data.column.machine.openMachineClass", dictType = "class_num_three_plan", dictTypeToExcelEnable = false)
    @TableField("OPEN_MACHINE_CLASS")
    @ImportValidated(maxLength = 20)
    private String openMachineClass;

    @ApiModelProperty(value = "机台状态", position = 60)
    @Excel(name = "ui.data.column.machine.status", dictType = "STATUS")
    @TableField("STATUS")
    @ImportValidated(maxLength = 1, required = true)
    private String status;

    @ApiModelProperty(value = "备注", position = 70)
    @Excel(name = "ui.common.column.remark")
    @TableField("REMARK")
    @ImportValidated(maxLength = 900)
    private String remark;

    @ApiModelProperty(value = "定额：该机台单班标准产量", position = 80)
    @Excel(name = "ui.data.column.machine.quata")
    @TableField("QUOTA")
    private Double quota;

    @ApiModelProperty(value = "分厂编码", position = 90)
    @Excel(name = "ui.data.column.factoryCode", dictType = "biz_factory_name")
    @TableField("FACTORY_CODE")
    @ImportValidated(maxLength = 20)
    private String factoryCode;
}
