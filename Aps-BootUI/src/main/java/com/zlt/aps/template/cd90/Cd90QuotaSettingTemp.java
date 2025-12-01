package com.zlt.aps.template.cd90;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "90度裁断定额设定对象", description = "90度裁断定额设定对象 ")
public class Cd90QuotaSettingTemp extends BaseEntity {

    @Excel(name = "ui.data.column.cd90.quota.clothCode", sort = 10)
    @ApiModelProperty(value = "帘布代码")
    private String clothCode;

    @Excel(name = "ui.data.column.machine.machineCode", sort = 20)
    @ApiModelProperty(value = "机台编号", position = 40)
    private String machineCode;

    @Excel(name = "ui.data.column.quota.quota", sort = 30)
    @ApiModelProperty(value = "定额")
    private BigDecimal quota;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
