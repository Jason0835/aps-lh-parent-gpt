package com.zlt.aps.template.cx;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 定点机台对象 t_specify_machine
 *
 * @author zlt
 * @date 2021-06-11
 */
@ApiModel(value = "定点机台对象", description = "定点机台对象")
public class CxMatchingSpecifyMachineTemp extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 胎胚代码(不允许为空)
     */
    @Excel(name = "ui.data.column.cx.machine.embryoCode",sort = 1)
    @ApiModelProperty(value = "胎胚代码", position = 10)
    private String embryoCode;

    /**
     * SAP品号(不需要外SAP+胎胚确定唯一，SAP品号可以为空)
     */
    @Excel(name = "ui.data.column.cx.machine.sap",sort = 2)
    @ApiModelProperty(value = "SAP品号", position = 20)
    private String sap;


    /**
     * 线路，数据维护在数据字典：0-生产线、1-备用线
     */
    @Excel(name = "ui.data.column.cx.machine.lineType", dictType = "LINE_TYPE",sort = 4)
    @ApiModelProperty(value = "线路", position = 50)
    private String lineType;

    /**
     * 作业类型，数据维护在数据字典：0-限制作业；1-不可作业
     */
    @Excel(name = "ui.data.column.cx.machine.jobType", dictType = "JOB_TYPE",sort = 5)
    @ApiModelProperty(value = "作业类型", position = 60)
    private String jobType;

    /**
     * 项目描述
     */
    @Excel(name = "ui.data.column.cx.machine.projectDesc",sort = 6)
    @ApiModelProperty(value = "项目描述", position = 40)
    private String projectDesc;


    @Excel(name = "ui.common.column.remark",sort = 7)
    @ApiModelProperty(value = "备注", position = 70)
    private String remark;
}
