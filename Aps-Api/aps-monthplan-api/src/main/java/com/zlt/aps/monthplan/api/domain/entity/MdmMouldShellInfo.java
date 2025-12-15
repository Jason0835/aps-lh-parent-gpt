package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：mdmMouldShellInfo.java
 * 描    述：模壳台账对象 t_mdm_mould_shell_info
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-05
 */
@ApiModel(value = "模壳台账对象", description = "模壳台账对象")
@Data
@TableName(value = "T_MDM_MOULD_SHELL_INFO")
public class MdmMouldShellInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmMouldShellInfo.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 模套型号
     */
    @ImportExcelValidated(required = true, maxLength = 30)
    @Excel(name = "ui.data.column.mdmMouldShellInfo.moldModelCode")
    @ApiModelProperty(value = "模套型号", name = "moldModelCode")
    @TableField(value = "MOLD_MODEL_CODE")
    private String moldModelCode;

    /**
     * 总数量
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 999999)
    @Excel(name = "ui.data.column.mdmMouldShellInfo.qty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "总数量", name = "qty")
    @TableField(value = "QTY")
    private Integer qty;

    /**
     * 机台数量
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 999999)
    @Excel(name = "ui.data.column.mdmMouldShellInfo.machineQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "机台数量", name = "machineQty")
    @TableField(value = "MACHINE_QTY")
    private Integer machineQty;

    /**
     * 在库数量
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 999999)
    @Excel(name = "ui.data.column.mdmMouldShellInfo.onHandQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "在库数量", name = "onHandQty")
    @TableField(value = "ON_HAND_QTY")
    private Integer onHandQty;

    /**
     * 出库数量
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 999999)
    @Excel(name = "ui.data.column.mdmMouldShellInfo.outBoundQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "出库数量", name = "outBoundQty")
    @TableField(value = "OUT_BOUND_QTY")
    private Integer outBoundQty;

    /**
     * 计划出库数量
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 999999)
    @Excel(name = "ui.data.column.mdmMouldShellInfo.outBoundPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "计划出库数量", name = "outBoundPlanQty")
    @TableField(value = "OUT_BOUND_PLAN_QTY")
    private Integer outBoundPlanQty;

    /**
     * 下机数量
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 999999)
    @Excel(name = "ui.data.column.mdmMouldShellInfo.deplaneQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "下机数量", name = "deplaneQty")
    @TableField(value = "DEPLANE_QTY")
    private Integer deplaneQty;

    /**
     * 强制出库数量
     */
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 999999)
    @Excel(name = "ui.data.column.mdmMouldShellInfo.forceOutBoundQty", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "强制出库数量", name = "forceOutBoundQty")
    @TableField(value = "FORCE_OUT_BOUND_QTY")
    private Integer forceOutBoundQty;


}
