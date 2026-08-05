package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@ApiModel(value = "胎侧损耗率设定对象", description = "胎侧损耗率设定对象")
@Data
@TableName(value = "T_TC_LOSS_SETTING")
public class TcLossSetting extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tc.lossSetting.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tc.lossSetting.sidewallCode")
    @ImportValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "胎侧编码", name = "sidewallCode")
    @TableField(value = "SIDEWALL_CODE")
    private String sidewallCode;

    @Excel(name = "ui.data.column.tc.lossSetting.machineCode")
    @ImportValidated(isCode = true, maxLength = 50)
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.tc.lossSetting.lossRate")
    @ImportValidated(required = true, number = true, min = 0, max = 99.99)
    @ApiModelProperty(value = "损耗率", name = "lossRate")
    @TableField(value = "LOSS_RATE")
    private BigDecimal lossRate;

//    @Excel(name = "ui.data.column.tc.lossSetting.settingLevel")
    @ImportValidated(maxLength = 20)
    @ApiModelProperty(value = "配置层级", name = "settingLevel")
    @TableField(value = "SETTING_LEVEL")
    private String settingLevel;

//    @Excel(name = "ui.data.column.tc.lossSetting.priority")
    @ImportValidated(digits = true, min = 0, max = 999)
    @ApiModelProperty(value = "优先级", name = "priority")
    @TableField(value = "PRIORITY")
    private Integer priority;

    @Excel(name = "ui.data.column.tc.lossSetting.enableStatus", dictType = "biz_yes_no")
    @ImportValidated(required = true, dictType = "biz_yes_no", maxLength = 1)
    @ApiModelProperty(value = "是否启用", name = "enableStatus")
    @TableField(value = "ENABLE_STATUS")
    private String enableStatus;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}