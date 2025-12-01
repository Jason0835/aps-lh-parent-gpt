package com.zlt.aps.template.gsq;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value="钢丝圈定点机台导入模板", description="钢丝圈定点机台导入模板")
public class GsqSpecifyMachineTemp extends BaseEntity {

    @ApiModelProperty(value = "钢丝圈代码")
    @Excel(name="ui.gsq.specifyMachine.column.steelRingCode")
    private String steelRingCode;

    @ApiModelProperty(value = "生产线")
    @Excel(name="ui.specifyMachine.column.machineName")
    private String machineName;

    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线")
    @Excel(name="ui.specifyMachine.column.lineType" ,dictType="LINE_TYPE")
    private String lineType;

    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业")
    @Excel(name="ui.specifyMachine.column.jobType",dictType="JOB_TYPE")
    private String jobType;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.data.column.stock.remark")
    private String remark;
}
