package com.zlt.aps.xwyy.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 纤维压延损耗率设定对象 t_xwyy_loss_setting
 *
 * @author chen
 * @date 2021-07-19
 */
@Data
@ApiModel(value = "纤维压延损耗率设定对象", description = "纤维压延损耗率设定对象 ")
public class XwyyLossSettingDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_LOSS_SETTING
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 帘布大卷编号
     */
    @ImportValidated(isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.loss.xwyy.bigRollCode", sort = 10)
    @ApiModelProperty(value = "帘布大卷编号")
    private String bigRollCode;

    /**
     * 机台id（对应T_XWYY_MACHINE_INFO表id）
     */
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    /**
     * 损耗率(百分比)
     */
    @ImportValidated(number = true, required = true, min = 0, max = 99.99)
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%", sort = 30)
    @ApiModelProperty(value = "损耗率(百分比)")
    private Double lossRate;

    /**
     * 机台名称
     */
    @ImportValidated(maxLength = 30)
    @Excel(name = "ui.data.column.loss.line", sort = 20, importName = "ui.data.column.loss.line")
    @ApiModelProperty(value = "机台名称")
    private String machineName;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
