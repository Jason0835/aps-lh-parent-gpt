package com.zlt.aps.template.cx;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 定点机台对象 t_cx_specify_machine
 *
 * @author zlt
 * @date 2021-07-21
 */
@ApiModel(value = "定点机台对象", description = "定点机台对象 ")
public class CxSpecifyMachineTemp extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * SAP品号
     */
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.specifyMachine.sapCode")
    @ApiModelProperty(value = "SAP品号")
    private String sapCode;

    /**
     * 胎胚代码
     */
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.specifyMachine.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    @ApiModelProperty(value = "机台名称")
    @Excel(name = "ui.data.column.machine.machineName")
    private String machineName;

    /**
     * 线路
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.specifyMachine.lineType", dictType = "LINE_TYPE")
    @ApiModelProperty(value = "线路")
    private String lineType;

    /**
     * 作业类型
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.specifyMachine.jobType", dictType = "JOB_TYPE")
    @ApiModelProperty(value = "作业类型")
    private String jobType;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.data.column.remark")
    private String remark;


}
