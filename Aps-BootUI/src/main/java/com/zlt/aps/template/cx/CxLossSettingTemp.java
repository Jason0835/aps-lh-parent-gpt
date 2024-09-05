package com.zlt.aps.template.cx;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 成型损耗率设定对象 t_cx_loss_setting
 *
 * @author chen
 * @date 2021-07-19
 */
@Data
@ApiModel(value = "成型损耗率设定对象", description = "成型损耗率设定对象 ")
public class CxLossSettingTemp extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * 胎胚代码
     */
    @Excel(name = "ui.data.column.loss.embryoCode", sort = 1)
    @ImportValidated(isCode = true, maxLength = 20)
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    @ApiModelProperty(value = "机台名称")
    @Excel(name = "ui.data.column.machine.machineName", sort = 2)
    private String machineName;

    /**
     * 损耗率
     */
    @ImportValidated(number = true, required = true, min = 0, max = 99)
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%", sort = 3)
    @ApiModelProperty(value = "损耗率")
    private Double lossRate;


    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark", sort = 4)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

}
