package com.zlt.aps.cd90.api.domain.dto;

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
public class Cd90LossSettingDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_LOSS_SETTING
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 帘布代码
     */
    @Excel(name = "ui.data.column.loss.clothCode", sort = 10)
    @ApiModelProperty(value = "帘布代码")
    @ImportValidated(isCode = true, maxLength = 20)
    private String clothCode;

    /**
     * 机台id（对应T_CD90_MACHINE_INFO表id）
     */
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    /**
     * 损耗率(百分比)
     */
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%", sort = 30)
    @ImportValidated(number = true, required = true, min = 0, max = 99.99)
    @ApiModelProperty(value = "损耗率(百分比)")
    private Double lossRate;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.loss.line", importName = "ui.data.column.loss.line", sort = 20)
    @ImportValidated(maxLength = 30)
    @ApiModelProperty(value = "机台名称")
    private String machineName;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(maxLength = 300)
    private String remark;
}
