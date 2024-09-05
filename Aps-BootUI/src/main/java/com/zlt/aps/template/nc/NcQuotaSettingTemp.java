package com.zlt.aps.template.nc;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 内衬定额设定对象 t_nc_quota_setting
 *
 * @author zlt
 * @date 2021-06-29
 */
@Data
@ApiModel(value = "内衬定额设定对象", description = "内衬定额设定对象 ")
public class NcQuotaSettingTemp extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;


    /**
     * 内衬代码
     */
    @Excel(name = "ui.data.column.quota.liningCode")
    @ApiModelProperty(value = "内衬代码")
    @ImportValidated(name = "ui.data.column.quota.liningCode", isCode = true, maxLength = 20)
    private String liningCode;

    /**
     * 机台id（对应T_NC_MACHINE_INFO表id）
     */
    @Excel(name = "ui.data.column.machine.machineCode")
    @ApiModelProperty(value = "机台id")
    private Long machineId;


    /**
     * 定额
     */
    @Excel(name = "ui.data.column.quota.quota")
    @ApiModelProperty(value = "定额")
    @ImportValidated(name = "ui.data.column.quota.quota", required = true, number = true, min = 0, maxLength = 999999)
    private BigDecimal quota;

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    private String remark;


}
