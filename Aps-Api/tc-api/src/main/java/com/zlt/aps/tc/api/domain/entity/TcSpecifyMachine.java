package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "胎侧定点与禁排机台规则对象", description = "胎侧定点与禁排机台规则对象")
@Data
@TableName(value = "T_TC_SPECIFY_MACHINE")
public class TcSpecifyMachine extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tc.specifyMachine.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tc.specifyMachine.sidewallCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "胎侧编码", name = "sidewallCode")
    @TableField(value = "SIDEWALL_CODE")
    private String sidewallCode;

    @Excel(name = "ui.data.column.tc.specifyMachine.machineCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /**
     * 作业类型，字典：JOB_TYPE，0-限制作业、1-不可作业
     */
    @Excel(name = "ui.data.column.tc.specifyMachine.jobType", dictType = "JOB_TYPE")
    @ImportExcelValidated(required = true, maxLength = 10)
    @ApiModelProperty(value = "作业类型", name = "jobType")
    @TableField(value = "JOB_TYPE")
    private String jobType;

    @Excel(name = "ui.data.column.tc.specifyMachine.priority")
    @ImportExcelValidated(digits = true, min = 0, max = 999)
    @ApiModelProperty(value = "优先级", name = "priority")
    @TableField(value = "PRIORITY")
    private Integer priority;

    @Excel(name = "ui.data.column.tc.specifyMachine.enableStatus", dictType = "biz_yes_no")
    @ImportExcelValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否启用", name = "enableStatus")
    @TableField(value = "ENABLE_STATUS")
    private String enableStatus;

    @Excel(name = "ui.common.column.remark")
    @ImportExcelValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}