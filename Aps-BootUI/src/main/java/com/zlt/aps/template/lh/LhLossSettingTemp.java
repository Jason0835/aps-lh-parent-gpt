package com.zlt.aps.template.lh;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 硫化损耗率设定对象 t_lh_loss_setting
 *
 * @author chen
 * @date 2021-07-19
 */
@Data
@ApiModel(value = "硫化损耗率设定对象", description = "硫化损耗率设定对象 ")
public class LhLossSettingTemp extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * SAP品号信息
     */
    @ImportValidated(isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.loss.sapCode", sort = 10)
    @ApiModelProperty(value = "SAP品号信息")
    private String sapCode;

    /**
     * 机台编号（对应T_LH_MACHINE_INFO表编号）
     */
    @ApiModelProperty(value = "机台名称")
    @Excel(name = "ui.data.column.machine.machineName", sort = 20)
    private String machineName;

    /**
     * 损耗率
     */
    @ImportValidated(number = true, required = true, min = 0, max = 99)
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%", sort = 30)
    @ApiModelProperty(value = "损耗率")
    private Double lossRate;


    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

}
