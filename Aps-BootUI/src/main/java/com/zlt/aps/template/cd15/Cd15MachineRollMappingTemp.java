package com.zlt.aps.template.cd15;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "15度裁断钢压大卷与机台的映射导入模板", description = "15度裁断钢压大卷与机台的映射导入模板")
public class Cd15MachineRollMappingTemp extends BaseEntity {

    @ApiModelProperty(value = "工厂编码")
    @Excel(name = "ui.data.column.cd15MachineRollMapping.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, maxLength = 50)
    private String factoryCode;

    @ApiModelProperty(value = "钢压大卷编号")
    @Excel(name = "ui.data.column.cd15MachineRollMapping.bigRollCode")
    @ImportValidated(required = true, isCode = true, maxLength = 30)
    private String bigRollCode;

    @ApiModelProperty(value = "机台编码")
    @Excel(name = "ui.data.column.cd15MachineRollMapping.machineCode")
    @ImportValidated(required = true, maxLength = 30)
    private String machineCode;

    @ApiModelProperty(value = "班次")
    @Excel(name = "ui.data.column.cd15MachineRollMapping.shiftCode", dictType = "class_num")
    @ImportValidated(required = true, maxLength = 50)
    private String shiftCode;
    @ApiModelProperty(value = "备注", position = 50)
    @Excel(name = "ui.common.column.remark")
    private String remark;
}
