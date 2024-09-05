package com.zlt.aps.template.cd90;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 90度裁断损耗率设定对象 t_cd90_loss_setting
 *
 * @author chen
 * @date 2021-07-19
 */
@Data
@ApiModel(value = "90度裁断损耗率设定对象", description = "90度裁断损耗率设定对象 ")
public class Cd90LossSettingTemp extends ApsBaseDto {

    @Excel(name = "ui.data.column.loss.clothCode", sort = 10)
    @ApiModelProperty(value = "帘布代码")
    private String clothCode;

    @Excel(name = "ui.data.column.loss.line",sort = 20)
    @ApiModelProperty(value = "生产线")
    private String machineCode;

    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%", sort = 30)
    @ApiModelProperty(value = "损耗率(百分比)")
    private Double lossRate;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
