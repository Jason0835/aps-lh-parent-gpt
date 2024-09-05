package com.zlt.aps.template.tm;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 胎面定点机台表
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Data
@ApiModel(value = "TmSpecifyMachine对象", description = "胎面定点机台表")
public class TmSpecifyMachineTemp extends ApsBaseDto implements Serializable {

    private static final long serialVersionUID = 1L;


    @ApiModelProperty(value = "胎面代码")
    @Excel(name = "ui.tm.specifyMachine.column.treadCode")
    @ImportValidated(name = "ui.tm.specifyMachine.column.treadCode", required = true, isCode = true, maxLength = 30)
    private String treadCode;

    @Excel(name="ui.specifyMachine.column.machineName")
    @ApiModelProperty(value = "生产线")
    private String machineName;


    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线")
    @Excel(name = "ui.specifyMachine.column.lineType", dictType = "LINE_TYPE")
    @ImportValidated(name = "ui.specifyMachine.column.lineType", required = true, maxLength = 9)
    private String lineType;

    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业")
    @Excel(name = "ui.specifyMachine.column.jobType", dictType = "JOB_TYPE")
    @ImportValidated(name = "ui.specifyMachine.column.jobType", required = true, maxLength = 12)
    private String jobType;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
