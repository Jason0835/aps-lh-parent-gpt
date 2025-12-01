package com.zlt.aps.template.gdyy;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "钢带压延损耗率设定对象", description = "钢带压延损耗率设定对象 ")
public class GdyyLossSettingTemp extends BaseEntity {

    @Excel(name = "ui.data.column.loss.gdyy.bigRollCode", sort = 10)
    @ApiModelProperty(value = "钢带大卷编号")
    private String bigRollCode;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.loss.line", sort = 15)
    @ApiModelProperty(value = "机台名称")
    private String machineName;

    /** 损耗率(百分比) */
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%",sort = 20)
    @ApiModelProperty(value = "损耗率(百分比)")
    private Double lossRate;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

}
