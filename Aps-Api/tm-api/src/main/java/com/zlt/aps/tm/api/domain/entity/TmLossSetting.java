package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@ApiModel(value = "胎面损耗率设定对象", description = "胎面损耗率设定对象")
@Data
@TableName(value = "T_TM_LOSS_SETTING")
public class TmLossSetting extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tm.LossSetting.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tm.LossSetting.treadCode")
    @ApiModelProperty(value = "胎面编码", name = "treadCode")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    @Excel(name = "ui.data.column.tm.LossSetting.machineCode")
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.tm.LossSetting.lossRate")
    @ApiModelProperty(value = "损耗率", name = "lossRate")
    @TableField(value = "LOSS_RATE")
    private BigDecimal lossRate;

    @Excel(name = "ui.data.column.tm.LossSetting.settingLevel")
    @ApiModelProperty(value = "配置层级", name = "settingLevel")
    @TableField(value = "SETTING_LEVEL")
    private String settingLevel;

    @Excel(name = "ui.data.column.tm.LossSetting.priority")
    @ApiModelProperty(value = "优先级", name = "priority")
    @TableField(value = "PRIORITY")
    private Integer priority;

    @Excel(name = "ui.data.column.tm.LossSetting.enableStatus", dictType = "biz_yes_no")
    @ApiModelProperty(value = "是否启用", name = "enableStatus")
    @TableField(value = "ENABLE_STATUS")
    private String enableStatus;

}
