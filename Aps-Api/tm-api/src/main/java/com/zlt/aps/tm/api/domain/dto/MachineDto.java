package com.zlt.aps.tm.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
public class MachineDto {

    @ApiModelProperty(value = "机台id")
    private Long id;

    @ApiModelProperty(value = "机台id")
    private String machineCode;

    @ApiModelProperty(value = "机台名称")
    private String machineName;

    @ApiModelProperty(value = "工序code：0-硫化、1-成型、2-胎面、3-胎侧、4-内衬、5-胎圈、6-钢丝圈、7-15度裁断、8-90裁断、9-钢带压延、10-纤维压延（对应数据字典：PROCEDURE_CODE）")
    private String procedureCode;

}
