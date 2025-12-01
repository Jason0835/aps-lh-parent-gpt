package com.zlt.aps.cxlh.cx.api.domain.dto;

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
public class CxLossSettingDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_LOSS_SETTING
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 胎胚代码
     */
    @Excel(name = "ui.data.column.loss.embryoCode", sort = 10)
    @ImportValidated(isCode = true, maxLength = 20)
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /**
     * 成型机台编号
     */
    @ApiModelProperty(value = "成型机台编号")
    private String machineCode;

    /**
     * 损耗率
     */
    @ImportValidated(number = true, required = true, min = 0, max = 99.99)
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%", sort = 30)
    @ApiModelProperty(value = "损耗率")
    private Double lossRate;

    /**
     * 机台名称
     */
    @ImportValidated(maxLength = 20)
    @Excel(name = "ui.data.column.machine.machineName", sort = 20)
    @ApiModelProperty(value = "机台名称")
    private String machineName;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

}
