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
 * 汇总胶料需求计划对象 t_glue_collect_plan
 * 
 * @author chen
 * @date 2022-04-25
 */
@ApiModel(value = "汇总胶料需求计划对象", description = "汇总胶料需求计划对象 ")
@TableName("t_glue_collect_plan")
@KeySequence(value = "seq_t_glue_collect_plan", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueCollectPlan extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_T_GLUE_COLLECT_PLAN */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_COLLECT_PLAN", position = 10)
    private Long id;
    /** 对应胶料需求计划批次号，多个逗号分割 */
    @ApiModelProperty(value = "对应胶料需求计划批次号，多个逗号分割", position = 20)
    private String demandBatchNo;
    /** 批次号，每次汇总计划批次号重新生成。规则：COLLECT+年月日+3位定长自增序号 */
    @ApiModelProperty(value = "批次号，每次汇总计划批次号重新生成。规则：COLLECT+年月日+3位定长自增序号", position = 30)
    private String batchNo;
    /** 计划日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "schedule.glueCollectPlan.planDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期", position = 40)
    private Date planDate;
    /** 分厂（多个的时候，逗号分割；对应数据字典：FACTORY） */
    @Excel(name = "schedule.glueCollectPlan.factory", dictType = "FACTORY")
    @ImportValidated(name = "schedule.glueCollectPlan.factory", dictType = "FACTORY", maxLength=100)
    @ApiModelProperty(value = "分厂（多个的时候，逗号分割；对应数据字典：FACTORY）", position = 50)
    private String factory;
    /** 胶料名称 */
    @Excel(name = "schedule.glueCollectPlan.glue")
    @ImportValidated(name = "schedule.glueCollectPlan.glue", maxLength=30)
    @ApiModelProperty(value = "胶料名称", position = 60)
    private String glue;
    /** 密炼区(对应数据字典code：MIX_AREA) */
    @Excel(name = "schedule.glueCollectPlan.mixArea", dictType = "MIX_AREA")
    @ImportValidated(name = "schedule.glueCollectPlan.mixArea", dictType = "MIX_AREA", maxLength=10)
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 70)
    private String mixArea;
    /**
     * 密炼机台编号（多个逗号分隔）
     */
    @ApiModelProperty(value = "密炼机台编号（多个逗号分隔）", position = 80)
    private String machineCode;
    /**
     * 密炼机台名称（多个逗号分隔）
     */
    @Excel(name = "setting.machine.machineName")
    @ImportValidated(name = "setting.machine.machineName", maxLength = 300)
    @ApiModelProperty(value = "密炼机台名称（多个逗号分隔）", position = 80)
    @TableField(exist = false)
    private String machineName;
    /**
     * 日计划(车)
     */
    @Excel(name = "schedule.glueCollectPlan.totalPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueCollectPlan.totalPlanQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "日计划(车)", position = 90)
    private Double totalPlanQty;
    /**
     * 昨日剩余(车)
     */
    @Excel(name = "schedule.glueCollectPlan.lastSurplus", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueCollectPlan.lastSurplus", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "昨日剩余(车)", position = 94)
    private Double lastSurplus;
    /**
     * 生产量(车)
     */
    @Excel(name = "schedule.glueCollectPlan.produceQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueCollectPlan.produceQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "生产量(车)", position = 96)
    private Double produceQty;
    /**
     * 中班计划量（16::00-0:00，单位：车）
     */
    @Excel(name = "schedule.glueCollectPlan.midPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueCollectPlan.midPlanQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "中班计划量（16::00-0:00，单位：车）", position = 100)
    private Double midPlanQty;
    /**
     * 中班备注
     */
    @Excel(name = "schedule.glueCollectPlan.midRemark")
    @ImportValidated(name = "schedule.glueCollectPlan.midRemark", maxLength = 300)
    @ApiModelProperty(value = "中班备注", position = 110)
    private String midRemark;
    /**
     * 夜班计划量（0:00-8:00，单位：车）
     */
    @Excel(name = "schedule.glueCollectPlan.nightPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueCollectPlan.nightPlanQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班计划量（0:00-8:00，单位：车）", position = 120)
    private Double nightPlanQty;
    /**
     * 夜班备注
     */
    @Excel(name = "schedule.glueCollectPlan.nightRemark")
    @ImportValidated(name = "schedule.glueCollectPlan.nightRemark", maxLength=300)
    @ApiModelProperty(value = "夜班备注", position = 130)
    private String nightRemark;
    /** 白班计划量（8::00-16:00，单位：车） */
    @Excel(name = "schedule.glueCollectPlan.dayPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueCollectPlan.dayPlanQty", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "白班计划量（8::00-16:00，单位：车）", position = 140)
    private Double dayPlanQty;
    /** 白班备注 */
    @Excel(name = "schedule.glueCollectPlan.dayRemark")
    @ImportValidated(name = "schedule.glueCollectPlan.dayRemark", maxLength=300)
    @ApiModelProperty(value = "白班备注", position = 150)
    private String dayRemark;
    /** 数据来源：0-汇总计划;1&gt;新增;2:导入 */
    @ApiModelProperty(value = "数据来源：0-汇总计划;1&gt;新增;2:导入", position = 160)
    private String dataSource;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength=300)
    @ApiModelProperty(value = "备注", position = 170)
    private String remark;

    /**
     * 收尾计划标识(1-是，准备收尾；0--否)
     */
    @Excel(name = "schedule.glueCollectPlan.isFinishing")
    @ImportValidated(name = "schedule.glueCollectPlan.isFinishing", maxLength = 1)
    @ApiModelProperty(value = "收尾计划标识(1-是，准备收尾；0--否)", position = 180)
    private String isFinishing;
}
