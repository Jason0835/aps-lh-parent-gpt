package com.zlt.aps.template.cd15;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value="15度裁断钢压大卷与机台的映射导入模板", description="15度裁断钢压大卷与机台的映射导入模板")
public class Cd15MachineRollMappingTemp {

    @ApiModelProperty(value = "钢压大卷编号")
    @Excel(name="ui.common.column.gy.bigRollCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String bigRollCode;

    @ApiModelProperty(value = "机台编号")
    @Excel(name="ui.data.column.loss.line")
    private String machineCode;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
