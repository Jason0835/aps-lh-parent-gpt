package com.zlt.mix.schedule.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 分厂胶料需求计划（初始表）对象 t_glue_demand_plan_init
 *
 * @author Gim
 * @date 2022-04-05
 */
@ApiModel(value = "分厂胶料需求计划（初始表）对象", description = "分厂胶料需求计划（初始表）对象 ")
@TableName("t_glue_demand_plan_init")
@KeySequence(value = "seq_t_glue_demand_plan_init", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueDemandPlanInit extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_GLUE_DEMAND_PLAN
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_DEMAND_PLAN", position = 10)
    private Long id;
    /**
     * 批次号，每次导入批次号重新生成。规则：DEMAND+分厂+年月日+3位定长自增序号
     */
    @ApiModelProperty(value = "批次号，每次导入批次号重新生成。规则：DEMAND+分厂+年月日+3位定长自增序号", position = 20)
    private String batchNo;
    /**
     * 计划日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "schedule.factoryGluePlanStatistics.planDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期", position = 30)
    private Date planDate;
    /**
     * 分厂（对应数据字典：FACTORY）
     */
    @Excel(name = "schedule.factoryGluePlanStatistics.factory")
    @ImportValidated(name = "schedule.factoryGluePlanStatistics.factory", maxLength = 10, dictType = "FACTORY")
    @ApiModelProperty(value = "分厂（对应数据字典：FACTORY）", position = 40)
    private String factory;

    /**
     * 胶料名称
     */
    @Excel(name = "schedule.factoryGluePlanStatistics.glue")
    @ImportValidated(name = "schedule.factoryGluePlanStatistics.glue", required = true, maxLength = 30)
    @ApiModelProperty(value = "胶料名称", position = 50)
    private String glue;

    /**
     * 密炼区(多个的时候，逗号分割；对应数据字典code：MIX_AREA)
     */
    @Excel(name = "schedule.glueDemandPlan.mixArea")
    @ImportValidated(name = "schedule.glueDemandPlan.mixArea", maxLength = 100, dictType = "MIX_AREA")
    @ApiModelProperty(value = "密炼区(多个的时候，逗号分割；对应数据字典code：MIX_AREA)", position = 60)
    private String mixArea;
    /**
     * 日计划(车)
     */
    @Excel(name = "schedule.factoryGluePlanStatistics.totalPlanQty")
    @ImportValidated(name = "schedule.factoryGluePlanStatistics.totalPlanQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "日计划(车)", position = 70)
    private BigDecimal totalPlanQty;
    /**
     * 中班计划量（16::00-0:00，单位：车）
     */
    @Excel(name = "ui.plan")
    @ImportValidated(name = "ui.plan", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "中班计划量（16::00-0:00，单位：车）", position = 80)
    private BigDecimal midPlanQty;
    /**
     * 中班备注
     */
    @Excel(name = "ui.remark")
    @ImportValidated(name = "ui.remark", maxLength = 300)
    @ApiModelProperty(value = "中班备注", position = 90)
    private String midRemark;
    /**
     * 夜班计划量（0:00-8:00，单位：车）
     */
    @Excel(name = "ui.plan")
    @ImportValidated(name = "ui.plan", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班计划量（0:00-8:00，单位：车）", position = 100)
    private BigDecimal nightPlanQty;
    /**
     * 夜班备注
     */
    @Excel(name = "ui.remark")
    @ImportValidated(name = "ui.remark", maxLength = 300)
    @ApiModelProperty(value = "夜班备注", position = 110)
    private String nightRemark;
    /**
     * 白班计划量（8::00-16:00，单位：车）
     */
    @Excel(name = "ui.plan")
    @ImportValidated(name = "ui.plan", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "白班计划量（8::00-16:00，单位：车）", position = 120)
    private BigDecimal dayPlanQty;
    /**
     * 白班备注
     */
    @Excel(name = "ui.remark")
    @ImportValidated(name = "ui.remark", maxLength = 300)
    @ApiModelProperty(value = "白班备注", position = 130)
    private String dayRemark;
    /**
     * 数据来源：1&gt;新增;2:导入
     */
    @ApiModelProperty(value = "数据来源：1&gt;新增;2:导入", position = 140)
    private String dataSource;
    /**
     * 备注
     */
    @Excel(name = "ui.remark")
    @ImportValidated(name = "ui.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 150)
    private String remark;

    private transient String startTime;

    private transient String endTime;

    @ApiModelProperty(value = "导入日期", position = 50)
    private String importDate;

    @ApiModelProperty(value = "导入分厂", position = 50)
    private String importFactory;

    @ApiModelProperty(value = "胶料名称", position = 50)
    private String glueName;

    @ApiModelProperty(value = "品号", position = 50)
    private String sapCode;

    @ApiModelProperty(value = "库存量", position = 20)
    private String stockQty;

    @ApiModelProperty(value = "白班剩余计划", position = 20)
    private String dayPlanSurplus;

    @ApiModelProperty(value = "实际完成", position = 20)
    private String actualFinish;

    @ApiModelProperty(value = "单车重量", position = 20)
    private String vehicleWeight;

    @ApiModelProperty(value = "车/桌", position = 20)
    private String vehicleDesk;

    @ApiModelProperty(value = "中班支领", position = 20)
    private String midCollar;

    @ApiModelProperty(value = "夜班支领", position = 20)
    private String nightCollar;

    @ApiModelProperty(value = "白班支领", position = 20)
    private String dayCollar;

    @ApiModelProperty(value = "次日中班", position = 20)
    private String nextMidPlanQty;

    @ApiModelProperty(value = "未支领", position = 20)
    private String notCollar;

}
