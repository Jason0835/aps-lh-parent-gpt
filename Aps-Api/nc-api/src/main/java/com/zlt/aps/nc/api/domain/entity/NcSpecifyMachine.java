package com.zlt.aps.nc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 定点机台表
 * </p>
 *
 * @author zlt
 * @since 2026-06-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_SPECIFY_MACHINE")
@ApiModel(value = "NcSpecifyMachine对象", description = "定点机台表")
public class NcSpecifyMachine extends ApsBaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name="ui.nc.specifyMachine.column.liningCode")
    @ImportExcelValidated(name = "ui.nc.specifyMachine.column.liningCode", required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "内衬代码")
    @TableField("LINING_CODE")
    private String liningCode;

    @ApiModelProperty(value = "机台Code（对应T_NC_MACHINE_INFO表machineCode）")
    @TableField("MACHINE_CODE")
    private String machineCode;

    @Excel(name="ui.specifyMachine.column.machineName")
    @ImportExcelValidated(name = "ui.specifyMachine.column.machineName", required = true, maxLength = 30)
    @ApiModelProperty(value = "机台")
    @TableField(exist = false)
    private String machineName;

    @Excel(name="ui.specifyMachine.column.lineType", dictType="LINE_TYPE")
    @ImportExcelValidated(name = "ui.specifyMachine.column.lineType", required = true, maxLength = 9)
    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线")
    @TableField("LINE_TYPE")
    private String lineType;

    @Excel(name="ui.specifyMachine.column.jobType", dictType="JOB_TYPE")
    @ImportExcelValidated(name = "ui.specifyMachine.column.jobType", required = true, maxLength = 12)
    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业")
    @TableField("JOB_TYPE")
    private String jobType;

    @Excel(name = "ui.data.column.info.remark")
    @ImportExcelValidated(name = "ui.data.column.info.remark", maxLength = 100)
    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;
}
