package com.zlt.aps.template.tq;

import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "胎圈定点机台导入模板", description = "胎圈定点机台导入模板")
public class TqSpecifyMachineTemp {

    @ApiModelProperty(value = "胎圈代码")
    @Excel(name = "ui.tq.specifyMachine.column.beadCode")
    private String beadCode;

    @Excel(name="ui.specifyMachine.column.machineName")
    @ApiModelProperty(value = "生产线")
    private String machineName;

    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线")
    @Excel(name = "ui.specifyMachine.column.lineType", dictType = "LINE_TYPE")
    private String lineType;

    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业")
    @Excel(name = "ui.specifyMachine.column.jobType", dictType = "JOB_TYPE")
    private String jobType;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.data.column.stock.remark")
    private String remark;
}
