package com.zlt.aps.template.cd15;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "15度裁断定额设定导入模板", description = "15度裁断定额设定导入模板")
public class Cd15QuotaSettingTemp extends BaseEntity {

    @Excel(name = "ui.data.column.cd15.setting.steelStripCode", sort = 10)
    @ApiModelProperty(value = "钢带代码", position = 10)
    private String steelStripCode;

    @Excel(name = "ui.data.column.machine.machineCode", sort = 30)
    @ApiModelProperty(value = "机台编号", position = 40)
    private String machineCode;

    @Excel(name = "ui.data.column.quota.quota", sort = 40)
    @ApiModelProperty(value = "定额", position = 50)
    private BigDecimal quota;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
