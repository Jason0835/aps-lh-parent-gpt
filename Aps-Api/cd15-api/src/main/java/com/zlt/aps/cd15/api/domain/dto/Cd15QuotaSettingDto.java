package com.zlt.aps.cd15.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 15度裁断定额设定对象 t_cd15_quota_setting
 * 
 * @author chen
 * @date 2021-06-28
 */
@Data
@ApiModel(value = "15度裁断定额设定对象", description = "15度裁断定额设定对象 ")
public class Cd15QuotaSettingDto extends ApsBaseDto
{
    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_QUOTA_SETTING */
    @ApiModelProperty(value = "主键ID")
    private Long id;

    /** 钢带代码 */
    @Excel(name = "ui.data.column.cd15.setting.steelStripCode", sort = 10)
    @ApiModelProperty(value = "钢带代码", position = 10)
    @ImportValidated(isCode = true, maxLength = 20)
    private String steelStripCode;

    /** 机台id */
    @ApiModelProperty(value = "机台id", position = 30)
    private Long machineId;

    @Excel(name = "ui.data.column.machine.machineName", importName = "ui.data.column.machine.machineCode", sort = 30)
    @ApiModelProperty(value = "机台名称", position = 40)
    @ImportValidated(maxLength = 30)
    private String machineName;

    /** 定额 */
    @Excel(name = "ui.data.column.quota.quota", sort = 40)
    @ApiModelProperty(value = "定额", position = 50)
    @ImportValidated(required = true, number = true, min = 0, max = 9999999.999)
    private BigDecimal quota;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(maxLength = 300)
    private String remark;
}
