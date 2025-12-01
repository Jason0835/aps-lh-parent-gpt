package com.zlt.mix.schedule.api.domain.entity;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.ibatis.type.JdbcType;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.domain.ZltBaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 终炼/母炼补量计划
 *
 */
@ApiModel(value = "终炼/母炼补量计划", description = "终炼/母炼补量计划 ")
@TableName("t_glue_schedule_supplement")
@KeySequence(value = "seq_t_glue_schedule_supplement", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueScheduleSupplement extends ZltBaseEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = -256784530000884317L;

	/** 主键ID，对应自增序列为：SEQ_T_GLUE_SCHEDULE_SUPPLEMENT */
	private Long id;
	/** 生成的排程ID */
	private Long scheduleResultId;

	/** 排程日期 */
	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
	private Date scheduleDate;

	/** 密炼区(对应数据字典code：MIX_AREA) */
	@Excel(name = "schedule.scheduleReport.mixArea", dictType = "MIX_AREA", sort = 20)
	private String mixArea;

	/** 胶料名称 */
	@Excel(name = "schedule.glueScheduleResult.glue", sort = 40)
	private String glue;

	/** 品号 */
	private String sapCode;

	/** 密炼机台编号 */
	private String machineCode;
	/**
	 * 配方类型
	 */
	private String recipeType;
	/**
	 * 配方版本号
	 */
	@Excel(name = "schedule.glueScheduleResult.recipeVersionId", sort = 60)
	private String recipeVersionId;

	/**
	 * 配方阶段(对应数据字典：PRODUCT_STAGE)
	 */
	@Excel(name = "schedule.glueScheduleResult.recipeStage", dictType = "PRODUCT_STAGE", sort = 70)
	private String recipeStage;

	/** 配方物料编号 */
	private String recipeMaterialCode;

	/** 配方重量(KG) */
	private Double formulaWeight;

	/** 配方时间 */
	private Long formulaTime;

	/** 总计划 */
	@Excel(name = "schedule.glueScheduleResult.supplement.total", sort = 80, cellType = Excel.ColumnType.NUMERIC)
	@TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
	private BigDecimal totalPlanQty;

	/** 中班计剩余产能（分钟） */
	@Excel(name = "schedule.glueScheduleResult.supplement.capacity.mid", sort = 90, cellType = Excel.ColumnType.NUMERIC, width = 20)
	@TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
	private BigDecimal midCapacity;

	/** 中班补量 */
	@Excel(name = "schedule.glueScheduleResult.supplement.plan.mid", sort = 100, cellType = Excel.ColumnType.NUMERIC)
	@TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
	private BigDecimal midPlanQty;

	/** 中班生产顺序 */
	@TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
	private Integer midProduceOrder;

	/** 夜班计剩余产能（分钟） */
	@Excel(name = "schedule.glueScheduleResult.supplement.capacity.night", sort = 110, cellType = Excel.ColumnType.NUMERIC, width = 20)
	@TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
	private BigDecimal nightCapacity;

	/** 夜班计划量 */
	@Excel(name = "schedule.glueScheduleResult.supplement.plan.night", sort = 120, cellType = Excel.ColumnType.NUMERIC)
	@TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
	private BigDecimal nightPlanQty;

	/** 夜班生产顺序 */
	@TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
	private Integer nightProduceOrder;

	/** 白班计剩余产能（分钟） */
	@Excel(name = "schedule.glueScheduleResult.supplement.capacity.day", sort = 600, cellType = Excel.ColumnType.NUMERIC, width = 20)
	@TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
	private BigDecimal dayCapacity;

	/** 白班计划量（0:00-08:00，单位：车） */
	@Excel(name = "schedule.glueScheduleResult.supplement.plan.day", sort = 600, cellType = Excel.ColumnType.NUMERIC)
	@TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
	private BigDecimal dayPlanQty;

	/** 白班生产顺序 */
	@TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.INTEGER)
	private Integer dayProduceOrder;

	private Double stockQty;

	private Double safeStockQty;

	/**
	 * 排程日期格式化，用于展示
	 */
	@Excel(name = "schedule.glueScheduleResult.scheduleDate", sort = 10)
	private String scheduleDateChar;

	/**
	 * 配方类型名称
	 */
	@TableField(exist = false)
	@Excel(name = "schedule.glueScheduleResult.recipeType", sort = 50)
	private String recipeTypeName;
	/**
	 * 机台名称
	 */
	@Excel(name = "schedule.glueScheduleResult.machineName", sort = 30)
	@TableField(exist = false)
	private String machineName;

	@TableField(exist = false)
	private BigDecimal oldMidCapacity;

	@TableField(exist = false)
	private BigDecimal oldNightCapacity;

	@TableField(exist = false)
	private BigDecimal oldDayCapacity;

	@TableField(exist = false)
	private BigDecimal mixOntervalTime;

	@TableField(exist = false)
	private BigDecimal scheduleSwitchTime;
}
