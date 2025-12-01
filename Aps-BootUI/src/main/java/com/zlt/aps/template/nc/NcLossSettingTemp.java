package com.zlt.aps.template.nc;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 内衬损耗率设定对象 t_nc_loss_setting
 * 
 * @author chen
 * @date 2021-07-13
 */
@Data
@ApiModel(value = "内衬损耗率设定对象", description = "内衬损耗率设定对象 ")
public class NcLossSettingTemp extends BaseEntity {

    private static final long serialVersionUID = 1L;


    /** 内衬代码 */
    @Excel(name = "ui.data.column.loss.liningCode",sort = 10)
    @ApiModelProperty(value = "内衬代码")
    @ImportValidated(name = "ui.data.column.loss.liningCode", isCode = true, maxLength = 20)
    private String liningCode;

    /** 机台id（对应T_NC_MACHINE_INFO表id） */
    @Excel(name = "ui.data.column.loss.line", sort = 20)
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    /** 损耗率(百分比) */
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%",sort = 30)
    @ApiModelProperty(value = "损耗率(百分比)")
    @ImportValidated(name = "ui.data.column.loss.lossRate", number = true, required = true, min = 0, max = 99)
    private Double lossRate;


    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
