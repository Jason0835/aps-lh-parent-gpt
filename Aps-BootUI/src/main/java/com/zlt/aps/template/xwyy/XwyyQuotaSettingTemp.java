package com.zlt.aps.template.xwyy;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "纤维压延定额设定对象", description = "纤维压延定额设定对象 ")
public class XwyyQuotaSettingTemp {

    @Excel(name = "ui.data.column.xwyy.quota.bigRollCode", sort = 10)
    private String bigRollCode;

    @ImportValidated(isCode = true, maxLength = 30)
    @Excel(name = "ui.data.column.machine.machineCode", sort = 20)
    private String machineName;

    @Excel(name = "ui.data.column.quota.quota", sort = 30)
    @ApiModelProperty(value = "定额")
    private BigDecimal quota;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
