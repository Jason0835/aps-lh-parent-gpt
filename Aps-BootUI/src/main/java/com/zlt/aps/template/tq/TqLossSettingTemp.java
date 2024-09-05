package com.zlt.aps.template.tq;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "胎圈损耗率设定导入模板", description = "胎圈损耗率设定导入模板 ")
public class TqLossSettingTemp {

    @Excel(name = "ui.data.column.loss.beadCode",sort = 10)
    @ApiModelProperty(value = "胎圈代码")
    private String beadCode;

    @Excel(name = "ui.data.column.loss.line", sort = 20)
    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%",sort = 30)
    @ApiModelProperty(value = "损耗率(百分比)")
    private Double lossRate;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
