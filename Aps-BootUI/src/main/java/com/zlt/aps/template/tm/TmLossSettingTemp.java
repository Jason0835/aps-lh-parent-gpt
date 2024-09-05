package com.zlt.aps.template.tm;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 胎面损耗率设定对象 t_tm_loss_setting
 *
 * @author chen
 * @date 2021-07-12
 */
@Data
@ApiModel(value = "胎面损耗率设定对象", description = "胎面损耗率设定对象 ")
public class TmLossSettingTemp extends ApsBaseDto {

    private static final long serialVersionUID = 1L;


    /**
     * 胎面代码
     */
    @Excel(name = "ui.data.column.quota.treadCode", sort = 10)
    @ApiModelProperty(value = "胎面代码")
    @ImportValidated(name = "ui.data.column.quota.treadCode", isCode = true, maxLength = 20)
    private String treadCode;

    /**
     * 机台id（对应T_TM_MACHINE_INFO表id）
     */
    @Excel(name = "ui.data.column.loss.line", sort = 20)
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    /**
     * 损耗率
     */
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%", sort = 30)
    @ApiModelProperty(value = "损耗率")
    @ImportValidated(name = "ui.data.column.loss.lossRate", number = true, required = true, max = 99)
    private Double lossRate;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
