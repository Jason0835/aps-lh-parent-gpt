package com.zlt.aps.dj.api.domain.entity;

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
 * <p>
 * 垫胶损耗率设定表
 * </p>
 *
 * @author zlt
 * @since 2026-06-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_DJ_LOSS_SETTING")
@ApiModel(value = "DjLossSetting对象", description = "垫胶损耗率设定表")
public class DjLossSetting extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "垫胶代码")
    @Excel(name="ui.dj.lossSetting.column.paddingCode")
    @ImportValidated(name = "ui.dj.specifyMachine.column.paddingCode", required = true, isCode = true, maxLength = 20)
    @TableField("PADDING_CODE")
    private String paddingCode;

    @ApiModelProperty(value = "机台编码")
    @Excel(name="ui.specifyMachine.column.machineName")
    @TableField("MACHINE_CODE")
    private String machineCode;

    @ApiModelProperty(value = "损耗率(百分比)")
    @Excel(name="ui.data.column.loss.lossRate")
    @ImportValidated(name = "ui.data.column.loss.lossRate", required = true, isCode = true, maxLength = 20)
    @TableField("LOSS_RATE")
    private BigDecimal lossRate;

}
