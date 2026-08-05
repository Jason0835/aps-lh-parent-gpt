package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "胎面胶料与机台关系对象", description = "胎面胶料与机台关系对象")
@Data
@TableName(value = "T_TM_GLUE_MACHINE_REAL")
public class TmGlueMachineReal extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tm.glueMachineReal.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tm.glueMachineReal.glueCode")
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "胶料代号", name = "glueCode")
    @TableField(value = "GLUE_CODE")
    private String glueCode;

    @Excel(name = "ui.data.column.tm.glueMachineReal.baseGlueCode")
    @ImportValidated(isCode = true, maxLength = 60)
    @ApiModelProperty(value = "基部胶编码", name = "baseGlueCode")
    @TableField(value = "BASE_GLUE_CODE")
    private String baseGlueCode;

    @Excel(name = "ui.data.column.tm.glueMachineReal.machineCode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.tm.glueMachineReal.priority")
    @ImportValidated(digits = true, min = 0, max = 999)
    @ApiModelProperty(value = "优先级", name = "priority")
    @TableField(value = "PRIORITY")
    private Integer priority;

    @Excel(name = "ui.data.column.tm.glueMachineReal.allowFlag", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否允许", name = "allowFlag")
    @TableField(value = "ALLOW_FLAG")
    private String allowFlag;

    @Excel(name = "ui.data.column.tm.glueMachineReal.enableStatus", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否启用", name = "enableStatus")
    @TableField(value = "ENABLE_STATUS")
    private String enableStatus;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    @ApiModelProperty(value = "机台名称", name = "machineName")
    @TableField(exist = false)
    private String machineName;
}
