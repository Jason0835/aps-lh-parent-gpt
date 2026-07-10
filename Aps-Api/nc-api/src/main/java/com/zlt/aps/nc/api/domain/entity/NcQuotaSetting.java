package com.zlt.aps.nc.api.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 内衬定额设定对象 t_nc_quota_setting
 *
 * @author zlt
 * @date 2026-06-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_NC_QUOTA_SETTING")
@ApiModel(value = "内衬定额设定对象", description = "内衬定额设定对象 ")
public class NcQuotaSetting extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 内衬代码
     */
    @Excel(name = "ui.data.column.quota.liningCode")
    @ApiModelProperty(value = "内衬代码")
    @ImportValidated(name = "ui.data.column.quota.liningCode", isCode = true, maxLength = 20)
    @TableField("LINING_CODE")
    private String liningCode;

    /**
     * 机台id（对应T_NC_MACHINE_INFO表id）
     */
    @ApiModelProperty(value = "机台id")
    @TableField("MACHINE_ID")
    private Long machineId;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.machine.machineName", importName = "ui.data.column.machine.machineCode")
    @ApiModelProperty(value = "机台名称")
    @ImportValidated(name = "ui.data.column.machine.machineCode", isCode = true, maxLength = 30)
    @TableField("MACHINE_NAME")
    private String machineName;

    /**
     * 定额
     */
    @Excel(name = "ui.data.column.quota.quota")
    @ApiModelProperty(value = "定额")
    @ImportValidated(name = "ui.data.column.quota.quota", required = true, number = true, min = 0, max = 9999999)
    @TableField("QUOTA")
    private BigDecimal quota;

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    @TableField("REMARK")
    private String remark;

}
