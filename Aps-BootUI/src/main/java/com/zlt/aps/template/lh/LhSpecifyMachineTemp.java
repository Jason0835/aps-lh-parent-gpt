package com.zlt.aps.template.lh;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 硫化定点机台信息对象 t_lh_specify_machine
 *
 * @author zlt
 * @date 2021-07-21
 */
@Data
@ApiModel(value = "硫化定点机台信息对象", description = "硫化定点机台信息对象 ")
public class LhSpecifyMachineTemp extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * SAP品号信息
     */
    @ImportValidated(maxLength = 20, isCode = true, required = true)
    @Excel(name = "ui.data.column.specifyMachine.sapCode")
    @ApiModelProperty(value = "SAP品号信息")
    private String sapCode;

    /**
     * 机台名称
     */
    @ApiModelProperty(value = "生产线")
    @Excel(name = "ui.specifyMachine.column.machineName")
    private String machineName;

    /**
     * 线路类型
     */
    @ImportValidated(required = true)
    @Excel(name = "ui.data.column.specifyMachine.lineType", dictType = "LINE_TYPE")
    @ApiModelProperty(value = "线路类型")
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
