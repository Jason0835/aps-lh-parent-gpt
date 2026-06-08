package com.zlt.aps.dj.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 垫胶损耗率设定对象 t_nc_loss_setting
 * 
 * @author zlt
 * @date 20216-06-13
 */
@Data
@ApiModel(value = "垫胶损耗率设定对象", description = "垫胶损耗率设定对象 ")
public class DjLossSettingDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /** 垫胶代码 */
    @Excel(name = "ui.data.column.loss.liningCode",sort = 10)
    @ApiModelProperty(value = "垫胶代码")
    @ImportValidated(name = "ui.data.column.loss.liningCode", isCode = true, maxLength = 20)
    private String liningCode;

    /** 机台id（对应T_NC_MACHINE_INFO表id） */
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    /** 损耗率(百分比) */
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%",sort = 30)
    @ApiModelProperty(value = "损耗率(百分比)")
    @ImportValidated(name = "ui.data.column.loss.lossRate", number = true, required = true, min = 0, max = 99.99)
    private Double lossRate;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.loss.line", importName = "ui.data.column.loss.line", sort = 20)
    @ApiModelProperty(value = "机台名称")
    @ImportValidated(maxLength = 30)
    private String machineName;
}
