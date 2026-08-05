package com.zlt.aps.tq.api.domain.entity;

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
 * 胎圈备库班数配置对象 t_tq_stock_shift_config
 * <p>
 * 以「供成型机数连续区间」方式配置备库班数，与垫胶/胎侧 DepthConfig 风格一致：
 * - MIN_MACHINE_QTY：区间起始机台数（含），同一分厂第 1 条必须为 1
 * - MAX_MACHINE_QTY：区间结束机台数（含），NULL 表示无上限（仅末行允许）
 * - DEPTH_CLASS_QTY：该区间对应的备库班数
 * </p>
 *
 * @author zlt
 * @date 2026-06-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TQ_STOCK_SHIFT_CONFIG")
@ApiModel(value = "胎圈备库班数配置对象", description = "胎圈备库班数配置对象")
public class TqStockShiftConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.factoryCode", dictType = "biz_factory_name", sort = 10)
    @ApiModelProperty(value = "分厂编码", position = 20)
    @TableField("FACTORY_CODE")
    @ImportValidated(required = true, maxLength = 50)
    private String factoryCode;

    @Excel(name = "ui.tq.depthConfig.column.minMachineQty", sort = 20)
    @ApiModelProperty(value = "区间起始供成型机数（含），第1条必须为1", position = 30)
    @TableField("MIN_MACHINE_QTY")
    @ImportValidated(required = true, number = true, min = 1, max = 999)
    private Integer minMachineQty;

    @Excel(name = "ui.tq.depthConfig.column.maxMachineQty", sort = 30)
    @ApiModelProperty(value = "区间结束供成型机数（含），NULL表示无上限（仅末行允许）", position = 40)
    @TableField("MAX_MACHINE_QTY")
    @ImportValidated(number = true, min = 1, max = 999)
    private Integer maxMachineQty;

    @Excel(name = "ui.tq.depthConfig.column.depthClassQty", sort = 40)
    @ApiModelProperty(value = "备库班数", position = 50)
    @TableField("DEPTH_CLASS_QTY")
    @ImportValidated(required = true, number = true, min = 1, max = 99)
    private BigDecimal depthClassQty;
}
