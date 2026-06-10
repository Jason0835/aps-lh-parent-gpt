package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "胎面定点与禁排机台规则对象", description = "胎面定点与禁排机台规则对象")
@Data
@TableName(value = "T_TM_SPECIFY_MACHINE")
public class TmSpecifyMachine extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tm.SpecifyMachine.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tm.SpecifyMachine.treadCode")
    @ApiModelProperty(value = "胎面编码", name = "treadCode")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    @Excel(name = "ui.data.column.tm.SpecifyMachine.machineCode")
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.tm.SpecifyMachine.jobType")
    @ApiModelProperty(value = "作业类型", name = "jobType")
    @TableField(value = "JOB_TYPE")
    private String jobType;

    @Excel(name = "ui.data.column.tm.SpecifyMachine.priority")
    @ApiModelProperty(value = "优先级", name = "priority")
    @TableField(value = "PRIORITY")
    private Integer priority;

    @Excel(name = "ui.data.column.tm.SpecifyMachine.enableStatus", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否启用", name = "enableStatus")
    @TableField(value = "ENABLE_STATUS")
    private String enableStatus;
}
