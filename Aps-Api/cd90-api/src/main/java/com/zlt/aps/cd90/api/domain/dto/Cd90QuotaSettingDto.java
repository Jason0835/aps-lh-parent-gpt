package com.zlt.aps.cd90.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 90度裁断定额设定对象 t_cd90_quota_setting
 *
 * @author chen
 * @date 2021-06-29
 */
@Data
@ApiModel(value = "90度裁断定额设定对象", description = "90度裁断定额设定对象 ")
public class Cd90QuotaSettingDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_QUOTA_SETTING
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 帘布代码
     */
    @Excel(name = "ui.data.column.cd90.quota.clothCode", sort = 10)
    @ImportValidated(isCode = true, maxLength = 20)
    @ApiModelProperty(value = "帘布代码")
    private String clothCode;

    /**
     * 机台id（对应T_CD90_MACHINE_INFO表id）
     */
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    @Excel(name = "ui.data.column.machine.machineName", sort = 20, importName = "ui.data.column.machine.machineCode")
    @ImportValidated(isCode = true, maxLength = 30)
    @ApiModelProperty(value = "机台名称", position = 40)
    private String machineName;

    /**
     * 定额
     */
    @Excel(name = "ui.data.column.quota.quota", sort = 30)
    @ImportValidated(required = true, number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "定额")
    private BigDecimal quota;

    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    @ImportValidated(maxLength = 300)
    private String remark;
}
