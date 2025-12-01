package com.zlt.aps.template.gsq;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "钢丝圈损耗率设定导入模板", description = "钢丝圈损耗率设定导入模板 ")
public class GsqLossSettingTemp extends BaseEntity {

    @Excel(name = "ui.data.column.loss.steelRingCode",sort = 10)
    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    @Excel(name = "ui.data.column.loss.line", sort = 20)
    @ApiModelProperty(value = "生产线")
    private String machineCode;

    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%",sort = 30)
    @ApiModelProperty(value = "损耗率(百分比)")
    private Double lossRate;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
