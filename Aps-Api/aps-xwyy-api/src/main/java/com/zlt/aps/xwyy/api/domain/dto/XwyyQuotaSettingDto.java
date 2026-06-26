package com.zlt.aps.xwyy.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 纤维压延定额设定对象 t_xwyy_quota_setting
 *
 * @author chen
 * @date 2021-06-29
 */
@Data
@ApiModel(value = "纤维压延定额设定对象", description = "纤维压延定额设定对象 ")
public class XwyyQuotaSettingDto extends ApsBaseDto {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_QUOTA_SETTING
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 帘布大卷编号
     */
    @ImportValidated(isCode = true, maxLength = 20)
    @Excel(name = "ui.data.column.xwyy.quota.bigRollCode", sort = 10)
    @ApiModelProperty(value = "帘布大卷编号")
    private String bigRollCode;

    /**
     * 机台id（对应T_XWYY_MACHINE_INFO表id）
     */
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    @ImportValidated(isCode = true, maxLength = 30)
    @Excel(name = "ui.data.column.machine.machineName", sort = 20, importName = "ui.data.column.machine.machineCode")
    @ApiModelProperty(value = "机台名称", position = 40)
    private String machineName;

    /**
     * 定额
     */
    @ImportValidated(min = 0, max = 9999999, required = true, number = true)
    @Excel(name = "ui.data.column.quota.quota", sort = 30)
    @ApiModelProperty(value = "定额")
    private BigDecimal quota;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark", sort = 40)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
