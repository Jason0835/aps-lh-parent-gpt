package com.zlt.aps.cx.api.domain.dto;

import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 成型定额设定对象 t_cx_quota_setting
 *
 * @author chen
 * @date 2021-06-16
 */
@Data
@ApiModel(value = "CxQuotaSettingDto对象", description = "成型定额设定信息")
public class CxQuotaSettingDto extends ApsBaseDto {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 成型机台机型类型，数据来源数据字典。如一次法：1；二次法：2；
     */
    @Excel(name = "ui.data.column.machine.machineType", dictType = "CX_MACHINE_TYPE", width = 32)
    @ImportValidated(required = true, maxLength = 30)
    @ApiModelProperty(value = "成型机台机型类型", position = 10)
    private String machineType;

    /**
     * 外胎规格尺寸信息
     */
    @ImportValidated(required = true, min = 0, max = 9999999,number = true)
    @Excel(name = "ui.data.column.cx.limit.specDimension")
    @ApiModelProperty(value = "外胎规格尺寸信息", position = 20)
    private BigDecimal specDimension;

    /**
     * 胎体布层数
     */
    @ImportValidated(required = true, min = 0, max = 999, digits = true)
    @Excel(name = "ui.data.column.cx.setting.carcassBothLayer")
    @ApiModelProperty(value = "胎体布层数", position = 30)
    private Integer carcassBothLayer;

    /**
     * 是否补强
     */
    @Excel(name = "ui.data.column.cx.setting.reinforce", dictType = "ISORNOT")
    @ImportValidated(required = true)
    @ApiModelProperty(value = "是否补强", position = 40)
    private String reinforce;

    /**
     * 轮胎类型，数据字典维护
     */
    @Excel(name = "ui.data.column.cx.setting.tireType", dictType = "TIRE_TYPE")
    @ImportValidated(required = true)
    @ApiModelProperty(value = "轮胎类型", position = 50)
    private String tireType;

    /**
     * 断面宽(下限)
     */
    @ImportValidated(required = true, min = 0, max = 99999999, digits = true)
    @Excel(name = "ui.data.column.cx.setting.sectionWidthMinimum")
    @ApiModelProperty(value = "断面宽(下限)", position = 60)
    private Integer sectionWidthMinimum;

    /**
     * 断面宽(上限)
     */
    @ImportValidated(required = true, min = 0, max = 99999999, digits = true)
    @Excel(name = "ui.data.column.cx.setting.sectionWidthMaximum")
    @ApiModelProperty(value = "断面宽(上限)", position = 70)
    private Integer sectionWidthMaximum;

    /**
     * 2人定额
     */
    @ImportValidated(min = 0, max = 99999999, digits = true)
    @Excel(name = "ui.data.column.cx.setting.twoPersonQuota")
    @ApiModelProperty(value = "两人定额", position = 80)
    private Integer twoPersonQuota;

    /**
     * 单人折合定额
     */
    @ImportValidated(min = 0, max = 99999999, digits = true)
    @Excel(name = "ui.data.column.cx.setting.onePersonQuota")
    @ApiModelProperty(value = "单人折合定额", position = 90)
    private Integer onePersonQuota;

    @ImportValidated(maxLength = 300)
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
