package com.zlt.aps.gsq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 钢丝圈损耗率管理对象 T_GSQ_LOSS_SETTING
 * 用于维护"钢丝圈编码+机台编码"维度的损耗率（百分比）
 * 唯一约束：STEEL_RING_CODE + MACHINE_CODE（两者至少一个有值）
 *
 * @author zlt
 * @date 2026-07-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_GSQ_LOSS_SETTING")
@ApiModel(value = "钢丝圈损耗率管理对象", description = "钢丝圈损耗率管理对象")
public class GsqLossRate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 钢丝圈编码 */
    @Excel(name = "ui.data.column.gsq.lossRate.steelRingCode")
    @ApiModelProperty(value = "钢丝圈编码", position = 20)
    @TableField("STEEL_RING_CODE")
    @ImportValidated(maxLength = 50)
    private String steelRingCode;

    /** 机台编码 */
    @Excel(name = "ui.data.column.gsq.lossRate.machineName")
    @ApiModelProperty(value = "机台编码", position = 30)
    @TableField("MACHINE_CODE")
    @ImportValidated(maxLength = 50)
    private String machineCode;

    /** 机台名称（反显字段，非数据库字段，仅供列表/导出显示，不出现在导入模板） */
    @ApiModelProperty(value = "机台名称", position = 35)
    @TableField(exist = false)
    private String machineName;

    /** 损耗率(百分比) */
    @Excel(name = "ui.data.column.gsq.lossRate.lossRate", suffix = "%")
    @ApiModelProperty(value = "损耗率(百分比)", position = 40)
    @TableField("LOSS_RATE")
    @ImportValidated(required = true, number = true, min = 0, max = 99.99)
    private BigDecimal lossRate;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 900)
    private String remark;
}
