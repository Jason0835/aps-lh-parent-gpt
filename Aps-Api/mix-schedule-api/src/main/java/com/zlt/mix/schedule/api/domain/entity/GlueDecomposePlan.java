package com.zlt.mix.schedule.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 分解胶料需求量对象 t_glue_decompose_plan
 *
 * @author chen
 * @date 2022-05-04
 */
@ApiModel(value = "分解胶料需求量对象", description = "分解胶料需求量对象 ")
@TableName("t_glue_decompose_plan")
@KeySequence(value = "seq_t_glue_decompose_plan", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueDecomposePlan extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_GLUE_DECOMPOSE_PLAN
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_DECOMPOSE_PLAN", position = 10)
    private Long id;
    /**
     * 对应汇总胶料需求计划的批次号
     */
    @ApiModelProperty(value = "对应汇总胶料需求计划的批次号", position = 20)
    private String collectBatchNo;
    /**
     * 批次号，每次分解计划批次号重新生成。规则：DECOMPOSE+密炼区+年月日+3位定长自增序号
     */
    @ApiModelProperty(value = "批次号，每次分解计划批次号重新生成。规则：DECOMPOSE+密炼区+年月日+3位定长自增序号", position = 30)
    private String batchNo;
    /**
     * 计划日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "schedule.glueDecomposePlan.planDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期", position = 50)
    private Date planDate;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @Excel(name = "schedule.glueDecomposePlan.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "schedule.glueDecomposePlan.mixArea", dictType = "MIX_AREA", maxLength = 10)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 60)
    private String mixArea;
    /**
     * 胶料名称
     */
    @Excel(name = "schedule.glueDecomposePlan.glue")
    @ImportValidated(name = "schedule.glueDecomposePlan.glue", maxLength = 30)
    @ApiModelProperty(value = "胶料名称", position = 70)
    private String glue;
    /**
     * 计划量(车)
     */
    @Excel(name = "schedule.glueDecomposePlan.planQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueDecomposePlan.planQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "计划量(车)", position = 80)
    private Double planQty;
    /**
     * 库存(车)
     */
    @Excel(name = "schedule.glueDecomposePlan.stockQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueDecomposePlan.stockQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "库存(车)", position = 90)
    private Double stockQty;
    /**
     * 安全库存(车)
     */
    @Excel(name = "schedule.glueDecomposePlan.safeStockQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueDecomposePlan.safeStockQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "安全库存(车)", position = 100)
    private Double safeStockQty;

    /**
     * 预生产库存倍数
     */
    @Excel(name = "setting.safeStock.reserveStockRate", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "setting.safeStock.reserveStockRate", number = true, min = 0, max = 100)
    @ApiModelProperty(value = "预生产库存倍数", position = 105)
    private Double reserveStockRate;

    /**
     * 生产量(车)
     */
    @Excel(name = "schedule.glueDecomposePlan.produceQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueDecomposePlan.produceQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "生产量(车)", position = 110)
    private Double produceQty;
    /**
     * 密炼机台编号（多个逗号分隔）
     */
    @ApiModelProperty(value = "密炼机台编号（多个逗号分隔）", position = 120)
    private String machineCode;
    /**
     * 密炼机台名称（多个逗号分隔）
     */
    @Excel(name = "setting.machine.machineName")
    @ImportValidated(name = "setting.machine.machineName", maxLength = 300)
    @ApiModelProperty(value = "密炼机台名称（多个逗号分隔）", position = 130)
    @TableField(exist = false)
    private String machineName;
    /**
     * 数据来源：0&gt;分解计划；1&gt;新增；2&gt;导入
     */
    @ApiModelProperty(value = "数据来源：0&gt;分解计划；1&gt;新增；2&gt;导入", position = 140)
    private String dataSource;

    @ApiModelProperty(value = "收尾计划标识(1-是，准备收尾；0--否)", position = 150)
    private String isFinishing;

    @ApiModelProperty(value = "对应终炼胶名称", position = 160)
    private String finalGlueMachine;

    @ApiModelProperty(value = "上级胶名称", position = 170)
    private String upGlue;

    @ApiModelProperty(value = "上级胶对应机台", position = 180)
    private String upMachineCode;

    // 排产日标记，1：第一天，2：第二天
    @ApiModelProperty(value = "排产日标记", position = 230)
    private String dayFlag;

    /**
     * 备注
     */
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 150)
    private String remark;

    @ApiModelProperty(value = "原始库存量", position = 200)
    @TableField(exist = false)
    private Double originStockQty;

    @ApiModelProperty(value = "昨日计划量", position = 210)
    @TableField(exist = false)
    private Double lastDayPlan;

    @ApiModelProperty(value = "昨日白班消耗量", position = 220)
    @TableField(exist = false)
    private Double lastDayConsume;

    @ApiModelProperty(value = "生产量差值", position = 230)
    @TableField(exist = false)
    private Double produceQtyDiff;

    @ApiModelProperty(value = "机台标识", position = 999)
    @TableField(exist = false)
    private Boolean fixedMachineFlag;
}
