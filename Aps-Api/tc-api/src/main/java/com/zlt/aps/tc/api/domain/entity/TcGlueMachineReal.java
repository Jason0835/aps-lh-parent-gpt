package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "胎侧胶料与机台关系对象", description = "胎侧胶料与机台关系对象")
@Data
@TableName(value = "T_TC_GLUE_MACHINE_REAL")
public class TcGlueMachineReal extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tc.glueMachineReal.factoryCode", dictType = "biz_factory_name", sort = 10)
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tc.glueMachineReal.glueCode", sort = 20)
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "胶料代号", name = "glueCode")
    @TableField(value = "GLUE_CODE")
    private String glueCode;

    @ApiModelProperty(value = "基部胶编码", name = "baseGlueCode")
    @TableField(value = "BASE_GLUE_CODE")
    private String baseGlueCode;

    @Excel(name = "ui.data.column.tc.glueMachineReal.machineCode", sort = 40)
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.tc.glueMachineReal.priority", sort = 50)
    @ImportValidated(digits = true, min = 0, max = 999)
    @ApiModelProperty(value = "优先级", name = "priority")
    @TableField(value = "PRIORITY")
    private Integer priority;

    @Excel(name = "ui.data.column.tc.glueMachineReal.allowFlag", dictType = "biz_yes_no", sort = 60)
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否允许", name = "allowFlag")
    @TableField(value = "ALLOW_FLAG")
    private String allowFlag;

    @Excel(name = "ui.data.column.tc.glueMachineReal.enableStatus", dictType = "biz_yes_no", sort = 70)
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否启用", name = "enableStatus")
    @TableField(value = "ENABLE_STATUS")
    private String enableStatus;

    @Excel(name = "ui.common.column.remark", sort = 80)
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;

    @Excel(name = "ui.data.column.tcGlueMachineReal.machineName", sort = 30)
    @ApiModelProperty(value = "机台名称", name = "machineName")
    @TableField(exist = false)
    private String machineName;
}