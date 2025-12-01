package com.zlt.aps.template.xwyy;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "纤维压延损耗率设定对象", description = "纤维压延损耗率设定对象 ")
public class XwyyLossSettingTemp extends ApsBaseEntity {

    /**
     * 帘布大卷编号
     */
    @Excel(name = "ui.data.column.loss.xwyy.bigRollCode", sort = 10)
    @ApiModelProperty(value = "帘布大卷编号")
    private String bigRollCode;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.loss.line", sort = 20)
    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    /**
     * 损耗率(百分比)
     */
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%", sort = 30)
    @ApiModelProperty(value = "损耗率(百分比)")
    private Double lossRate;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
