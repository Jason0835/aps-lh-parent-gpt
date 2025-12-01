package com.zlt.mix.schedule.api.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.annotation.ImportValidated;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 分厂胶料需求计划对象 t_glue_demand_plan
 *
 * @author chen
 * @date 2022-04-18
 */
@ApiModel(value = "分厂胶料需求计划对象", description = "分厂胶料需求计划对象 ")
@TableName("t_glue_demand_plan")
@KeySequence(value = "seq_t_glue_demand_plan", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueDemandPlan extends ZltBaseEntity {

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
    @Excel(name = "schedule.glueDemandPlan.planDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期", position = 30)
    private Date planDate;
    /**
     * 分厂（对应数据字典：FACTORY）
     */
    @Excel(name = "schedule.glueDemandPlan.factory", dictType = "FACTORY")
    @ImportValidated(name = "schedule.glueDemandPlan.factory", dictType = "FACTORY", maxLength = 10)
    @ApiModelProperty(value = "分厂（对应数据字典：FACTORY）", position = 40)
    private String factory;
    /**
     * 胶料名称
     */
    @Excel(name = "schedule.glueDemandPlan.glue")
    @ImportValidated(name = "schedule.glueDemandPlan.glue.code", required = true, maxLength = 30)
    @ApiModelProperty(value = "胶料名称", position = 50)
    private String glue;

    @ApiModelProperty(value = "胶料名称", position = 50)
    private String glueName;

    @ApiModelProperty(value = "品号", position = 50)
    private String sapCode;

    /**
     * 密炼区(多个的时候，逗号分割；对应数据字典code：MIX_AREA)
     */
    @Excel(name = "schedule.glueDemandPlan.mixArea")
    @ImportValidated(name = "schedule.glueDemandPlan.mixArea", maxLength = 100)
    @ApiModelProperty(value = "密炼区(多个的时候，逗号分割；对应数据字典code：MIX_AREA)", position = 60)
    @TableField(insertStrategy = FieldStrategy.NOT_NULL, updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String mixArea;
    /**
     * 日计划(车)
     */
    @Excel(name = "schedule.glueDemandPlan.totalPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueDemandPlan.totalPlanQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "日计划(车)", position = 70)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DECIMAL)
    private BigDecimal totalPlanQty;
    /**
     * 中班计划量（16::00-0:00，单位：车）
     */
    @Excel(name = "schedule.glueDemandPlan.nightPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueDemandPlan.nightPlanQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "中班计划量（16::00-0:00，单位：车）", position = 80)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DECIMAL)
    private BigDecimal midPlanQty;
    /**
     * 中班备注
     */
    @Excel(name = "schedule.glueDemandPlan.nightRemark")
    @ImportValidated(name = "schedule.glueDemandPlan.nightRemark", maxLength = 300)
    @ApiModelProperty(value = "中班备注", position = 90)
    private String midRemark;
    /**
     * 夜班计划量（0:00-8:00，单位：车）
     */
    @Excel(name = "schedule.glueDemandPlan.dayPlanQty", cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueDemandPlan.dayPlanQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班计划量（0:00-8:00，单位：车）", position = 100)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DECIMAL)
    private BigDecimal nightPlanQty;
    /**
     * 夜班备注
     */
    @Excel(name = "schedule.glueDemandPlan.dayRemark")
    @ImportValidated(name = "schedule.glueDemandPlan.dayRemark", maxLength = 300)
    @ApiModelProperty(value = "夜班备注", position = 110)
    private String nightRemark;
    /**
     * 白班计划量（8::00-16:00，单位：车）
     */
    // @Excel(name = "schedule.glueDemandPlan.dayPlanQty", cellType = Excel.ColumnType.NUMERIC)
    // @ImportValidated(name = "schedule.glueDemandPlan.dayPlanQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "白班计划量（8::00-16:00，单位：车）", position = 120)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DECIMAL)
    private BigDecimal dayPlanQty;
    /**
     * 白班备注
     */
    // @Excel(name = "schedule.glueDemandPlan.dayRemark")
    // @ImportValidated(name = "schedule.glueDemandPlan.dayRemark", maxLength = 300)
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
    @Excel(name = "ui.common.column.remark")
    @ImportValidated(name = "ui.common.column.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 150)
    private String remark;

    //页面传过来的id数组串
    @TableField(exist = false)
    private List<String> ids;
}
