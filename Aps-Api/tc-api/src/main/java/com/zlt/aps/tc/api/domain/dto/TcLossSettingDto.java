package com.zlt.aps.tc.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 胎侧损耗率设定对象 t_tc_loss_setting
 * 
 * @author chen
 * @date 2021-07-13
 */
@Data
@ApiModel(value = "胎侧损耗率设定对象", description = "胎侧损耗率设定对象 ")
public class TcLossSettingDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_LOSS_SETTING */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 胎侧代码 */
    @Excel(name = "ui.data.column.loss.sidewallCode",sort = 10)
    @ApiModelProperty(value = "胎侧代码")
    @ImportValidated(name = "ui.data.column.loss.sidewallCode", isCode = true, maxLength = 20)
    private String sidewallCode;

    /** 机台id（对应T_TC_MACHINE_INFO表id） */
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    /** 损耗率(百分比) */
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%",sort = 30)
    @ApiModelProperty(value = "损耗率(百分比)")
    @ImportValidated(name = "ui.data.column.loss.lossRate", number = true, required = true, min = 0,  max = 99.99)
    private Double lossRate;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.loss.line", importName = "ui.data.column.loss.line", sort = 20)
    @ApiModelProperty(value = "机台名称")
    @ImportValidated( maxLength = 30)
    private String machineName;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
