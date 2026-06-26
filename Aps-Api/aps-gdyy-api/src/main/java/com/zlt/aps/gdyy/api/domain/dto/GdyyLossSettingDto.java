package com.zlt.aps.gdyy.api.domain.dto;

import com.zlt.aps.common.core.annotation.ImportValidated;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 钢带压延损耗率设定对象 t_gdyy_loss_setting
 * 
 * @author chen
 * @date 2021-07-19
 */
@Data
@ApiModel(value = "钢带压延损耗率设定对象", description = "钢带压延损耗率设定对象 ")
public class GdyyLossSettingDto extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_LOSS_SETTING */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 机台id（对应T_GDYY_MACHINE_INFO表id）
     */
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    /** 钢带大卷编号 */
    @Excel(name = "ui.data.column.loss.gdyy.bigRollCode", sort = 10)
    @ApiModelProperty(value = "钢带大卷编号")
    @ImportValidated(name = "ui.data.column.loss.gdyy.bigRollCode", required = true, isCode = true, maxLength = 20)
    private String bigRollCode;

    /** 损耗率(百分比) */
    @Excel(name = "ui.data.column.loss.lossRate", suffix = "%",sort = 20)
    @ApiModelProperty(value = "损耗率(百分比)")
    @ImportValidated(name = "ui.data.column.loss.lossRate", number = true, required = true, min = 0,  max = 99.99)
    private Double lossRate;

    /**
     * 机台名称
     */
    @ImportValidated(maxLength = 30)
    @Excel(name = "ui.data.column.loss.line", sort = 20, importName = "ui.data.column.loss.line")
    @ApiModelProperty(value = "机台名称")
    private String machineName;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;

}
