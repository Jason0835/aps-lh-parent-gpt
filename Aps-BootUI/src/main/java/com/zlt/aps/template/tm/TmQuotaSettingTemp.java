package com.zlt.aps.template.tm;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;

/**
 * 胎面定额设定对象 tm_quota_setting
 *
 * @author zlt
 * @date 2021-06-28
 */
@Data
@ApiModel(value = "胎面定额设定对象", description = "胎面定额设定对象 ")
public class TmQuotaSettingTemp extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;


    /**
     * 胎面代码
     */
    @Excel(name = "ui.data.column.quota.treadCode")
    @ApiModelProperty(value = "胎面代码")
    @ImportValidated(name = "ui.data.column.quota.treadCode", isCode = true, maxLength = 20)
    private String treadCode;

    /**
     * 机台id（对应T_TM_MACHINE_INFO表id）
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
