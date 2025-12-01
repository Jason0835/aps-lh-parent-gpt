package com.zlt.aps.template.cd90;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "90度裁断帘布大卷与机台的映射导入模板", description = "90度裁断帘布大卷与机台的映射导入模板")
public class Cd90MachineRollMappingTemp extends BaseEntity {

    @ApiModelProperty(value = "帘布大卷代号")
    @Excel(name = "ui.bigRollColor.column.bigRollCode")
    private String bigRollCode;

    @ApiModelProperty(value = "生产线")
    @Excel(name = "ui.data.column.loss.line")
    private String machineName;

    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
