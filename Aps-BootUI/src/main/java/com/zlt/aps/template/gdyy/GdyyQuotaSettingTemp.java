package com.zlt.aps.template.gdyy;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "钢带压延定额设定对象", description = "钢带压延定额设定对象 ")
public class GdyyQuotaSettingTemp extends BaseEntity {

    @Excel(name = "ui.data.column.gdyy.quota.bigRollCode", sort = 10)
    @ApiModelProperty(value = "钢带大卷编号")
    private String bigRollCode;

    @Excel(name = "ui.data.column.quota.quota", sort = 20)
    @ApiModelProperty(value = "定额")
    private BigDecimal quota;

    @Excel(name = "ui.common.column.remark", sort = 30)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
