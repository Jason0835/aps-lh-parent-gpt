package com.zlt.aps.template.cd15;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "15度裁断损耗率设定导入模板", description = "15度裁断损耗率设定导入模板")
public class Cd15LossSettingTemp {

    @Excel(name = "ui.data.column.loss.steelStripCode",sort = 10)
    @ApiModelProperty(value = "钢带代码")
    private String steelStripCode;

    @Excel(name = "ui.data.column.loss.line", sort = 20)
    @ApiModelProperty(value = "生产线")
    private String machineName;

    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%",sort = 30)
    @ApiModelProperty(value = "损耗率(百分比)")
    private Double lossRate;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
