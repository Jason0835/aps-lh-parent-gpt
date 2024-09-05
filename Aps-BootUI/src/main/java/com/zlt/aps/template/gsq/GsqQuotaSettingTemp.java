package com.zlt.aps.template.gsq;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "钢丝圈定额设定导入模板", description = "钢丝圈定额设定导入模板 ")
public class GsqQuotaSettingTemp {

    @Excel(name = "ui.data.column.quota.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    @Excel(name = "ui.data.column.machine.machineCode")
    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    @Excel(name = "ui.data.column.quota.quota")
    @ApiModelProperty(value = "定额")
    private BigDecimal quota;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    private String remark;
}
