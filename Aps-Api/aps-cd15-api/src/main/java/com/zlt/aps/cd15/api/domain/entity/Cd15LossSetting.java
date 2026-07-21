package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 斜裁损耗率设定。
 */
@Data
@ApiModel(value = "斜裁损耗率设定", description = "斜裁损耗率设定")
@TableName("t_cd15_loss_setting")
public class Cd15LossSetting extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15LossSetting.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 钢带代码 */
    @ApiModelProperty(value = "钢带代码", name = "steelStripCode")
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("STEEL_STRIP_CODE")
    @Excel(name = "ui.data.column.cd15LossSetting.steelStripCode")
    private String steelStripCode;

    /** 机台编码 */
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @ImportExcelValidated(maxLength = 30)
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd15LossSetting.machineCode")
    private String machineCode;

    /** 损耗率(百分比) */
    @ApiModelProperty(value = "损耗率(百分比)", name = "lossRate")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("LOSS_RATE")
    @Excel(name = "ui.data.column.cd15LossSetting.lossRate")
    private Double lossRate;
}
