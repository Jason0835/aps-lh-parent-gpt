package com.zlt.aps.template.tc;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧定额设定对象 tc_quota_setting
 *
 * @author zlt
 * @date 2021-06-28
 */
@Data
@ApiModel(value = "胎侧定额设定对象", description = "胎侧定额设定对象 ")
public class TcQuotaSettingTemp extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 胎侧代码
     */
    @Excel(name = "ui.data.column.quota.sidewallCode")
    @ApiModelProperty(value = "胎侧代码")
    @ImportValidated(name = "ui.data.column.quota.sidewallCode", isCode = true, maxLength = 20)
    private String sidewallCode;

    /**
     * 机台id（对应T_TC_MACHINE_INFO表id）
     */
    @Excel(name = "ui.data.column.machine.machineCode")
    @ApiModelProperty(value = "机台id")
    private Long machineId;

    /**
     * 定额
     */
    @Excel(name = "ui.data.column.quota.quota")
    @ApiModelProperty(value = "定额")
    @ImportValidated(name = "ui.data.column.quota.quota", required = true, number = true, min = 0, max = 9999999)
    private BigDecimal quota;

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;
}
