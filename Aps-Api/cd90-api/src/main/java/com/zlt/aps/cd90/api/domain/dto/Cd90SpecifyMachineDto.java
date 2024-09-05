package com.zlt.aps.cd90.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 90度裁断定点机台表
 * </p>
 *
 * @author zhangbinglin
 * @since 2021-06-04
 */
@Data
@ApiModel(value = "Cd90SpecifyMachine对象", description = "90度裁断定点机台表")
public class Cd90SpecifyMachineDto extends ApsBaseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "帘布代码")
    @Excel(name = "ui.common.column.lb.clothCode")
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    private String clothCode;

    @ApiModelProperty(value = "机台id（对应T_cd90_MACHINE_INFO表id）")
    private Long machineId;

    @ApiModelProperty(value = "机台code")
    private String machineCode;

    @ApiModelProperty(value = "生产线")
    @ImportValidated(required = true, maxLength = 30)
    @Excel(name = "ui.specifyMachine.column.machineName")
    private String machineName;

    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线")
    @Excel(name = "ui.specifyMachine.column.lineType", dictType = "LINE_TYPE")
    @ImportValidated(required = true,maxLength = 9)
    private String lineType;

    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业")
    @Excel(name = "ui.specifyMachine.column.jobType", dictType = "JOB_TYPE")
    @ImportValidated(required = true,maxLength = 12)
    private String jobType;

    @ApiModelProperty(value = "备注", position = 500)
    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(maxLength = 300)
    private String remark;
}
