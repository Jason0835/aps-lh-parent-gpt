package com.zlt.aps.template.xwyy;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "XwyyMachineRollMappingDto对象", description = "纤维压延帘布大卷与机台的映射表")
public class XwyyMachineRollMappingTemp extends ApsBaseEntity {

    @ApiModelProperty(value = "帘布大卷编号")
    @Excel(name = "ui.bigRollColor.column.bigRollCode")
    private String bigRollCode;

    @ApiModelProperty(value = "机台编号")
    @Excel(name = "ui.data.column.machine.machineCode")
    private String machineCode;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
