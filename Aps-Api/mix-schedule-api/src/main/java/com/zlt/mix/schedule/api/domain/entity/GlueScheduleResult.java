package com.zlt.mix.schedule.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.mix.common.core.domain.ZltBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.zlt.mix.common.core.annotation.ImportValidated;
import org.apache.ibatis.type.JdbcType;

/**
 * 终炼/母炼日计划排程对象 t_glue_schedule_result
 *
 * @author chen
 * @date 2022-05-16
 */
@ApiModel(value = "终炼/母炼日计划排程对象", description = "终炼/母炼日计划排程对象 ")
@TableName("t_glue_schedule_result")
@KeySequence(value = "seq_t_glue_schedule_result", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class GlueScheduleResult extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_T_GLUE_SCHEDULE_RESULT */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_GLUE_SCHEDULE_RESULT", position = 10)
    private Long id;
    /** 对应分解胶料需求批次号 */
    @ApiModelProperty(value = "对应分解胶料需求批次号", position = 20)
    private String decomposeBatchNo;
    /** 批次号，每次分解计划批次号重新生成。规则：SCHEDULE+密炼区+年月日+3位定长自增序号 */
    @ApiModelProperty(value = "批次号，每次分解计划批次号重新生成。规则：SCHEDULE+密炼区+年月日+3位定长自增序号", position = 30)
    private String batchNo;
    /** 工单号，自动生成（批次号+4位定长自增序号） */
    @ApiModelProperty(value = "工单号，自动生成（批次号+4位定长自增序号）", position = 40)
    private String orderNo;
    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期", position = 60)
    private Date scheduleDate;
    /** 密炼区(对应数据字典code：MIX_AREA) */
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 70)
    private String mixArea;
    /** 胶料名称 */
    @Excel(name = "schedule.glueScheduleResult.glue", sort = 150)
    @ImportValidated(name = "schedule.glueScheduleResult.glue", maxLength=30, required = true)
    @ApiModelProperty(value = "胶料名称", position = 80)
    private String glue;
    /** 品号 */
    @ApiModelProperty(value = "品号", position = 80)
    private String sapCode;
    /** 密炼机台编号 */
    @ApiModelProperty(value = "密炼机台编号", position = 90)
    private String machineCode;
    /**
     * 配方类型
     */
    @Excel(name = "schedule.glueScheduleResult.recipeType", sort = 200)
    @ImportValidated(name = "schedule.glueScheduleResult.recipeType", maxLength = 3)
    @ApiModelProperty(value = "配方类型", position = 110)
    private String recipeType;
    /**
     * 配方版本号
     */
    @Excel(name = "schedule.glueScheduleResult.recipeVersionId", sort = 200)
    @ImportValidated(name = "schedule.glueScheduleResult.recipeVersionId", maxLength = 30)
    @ApiModelProperty(value = "配方版本号", position = 100)
    private String recipeVersionId;
    /**
     * 配方阶段(对应数据字典：PRODUCT_STAGE)
     */
    @Excel(name = "schedule.glueScheduleResult.recipeStage", sort = 200)
    @ImportValidated(name = "schedule.glueScheduleResult.recipeStage", maxLength = 10)
    @ApiModelProperty(value = "配方阶段(对应数据字典：RECIPE_STAGE)", position = 110)
    private String recipeStage;
    /** 库存(车) */
    @Excel(name = "schedule.glueScheduleResult.stockQty", sort = 200, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.stockQty", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "库存(车)", position = 100)
    private Double stockQty;
    /** 安全库存(车) */
    @Excel(name = "schedule.glueScheduleResult.safeStockQty", sort = 250, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.safeStockQty", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "安全库存(车)", position = 110)
    private Double safeStockQty;
    /** 配方重量(KG) */
    @Excel(name = "schedule.glueScheduleResult.formulaWeight", sort = 300, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.formulaWeight", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "配方重量(KG)", position = 120)
    private Double formulaWeight;
    /** 配方时间 */
    @Excel(name = "schedule.glueScheduleResult.formulaTime", sort = 350, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.formulaTime", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "配方时间", position = 130)
    private Double formulaTime;
    /** 总计划 */
    @Excel(name = "schedule.glueScheduleResult.totalPlanQty", sort = 400, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.totalPlanQty", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "总计划", position = 140)
    private Double totalPlanQty;
    /** 总剩余 */
    @Excel(name = "schedule.glueScheduleResult.totalSurplus", sort = 450, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.totalSurplus", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "总剩余", position = 150)
    private Double totalSurplus;
    /** 中班计划量（16::00-0:00，单位：车） */
    @Excel(name = "schedule.glueScheduleResult.midPlanQty", sort = 600, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.midPlanQty", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "中班计划量（16::00-0:00，单位：车）", position = 160)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private Double midPlanQty;
    /** 中班完成量（16::00-0:00，单位：车） */
    @Excel(name = "schedule.glueScheduleResult.midFinishQty", sort = 650, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.midFinishQty", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "中班完成量（16::00-0:00，单位：车）", position = 160)
    @TableField(exist = false)
    private Double midFinishQty;

    /**
     * 中班完成率
     */
    @Excel(name = "schedule.glueScheduleResult.midFinishRate", sort = 550, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.midFinishRate", min = 0, max = 100)
    @ApiModelProperty(value = "中班完成率", position = 180)
    @TableField(exist = false)
    private Double midFinishRate;

    /** 中班生产顺序 */
    @Excel(name = "schedule.glueScheduleResult.midProduceOrder", sort = 550, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.midProduceOrder", number=true, min=0, max=999999)
    @ApiModelProperty(value = "中班生产顺序", position = 170)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.INTEGER)
    private Integer midProduceOrder;
    /** 中班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "中班预计开始时间", position = 180)
    private Date midExpectStartTime;
    /** 中班预计完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.glueScheduleResult.midExpectFinishTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 700)
    @ApiModelProperty(value = "中班预计完成时间", position = 190)
    private Date midExpectFinishTime;
    /** 中班备注 */
    @Excel(name = "schedule.glueScheduleResult.midRemark", sort = 750)
    @ImportValidated(name = "schedule.glueScheduleResult.midRemark", maxLength=300)
    @ApiModelProperty(value = "中班备注", position = 200)
    private String midRemark;
    /** 夜班计划量（0:00-8:00，单位：车） */
    @Excel(name = "schedule.glueScheduleResult.nightPlanQty", sort = 850, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.nightPlanQty", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "夜班计划量（0:00-8:00，单位：车）", position = 210)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private Double nightPlanQty;
    /** 夜班完成量（0:00-8:00，单位：车） */
    @Excel(name = "schedule.glueScheduleResult.nightFinishQty", sort = 900, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.nightFinishQty", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "夜班完成量（0:00-8:00，单位：车）", position = 210)
    @TableField(exist = false)
    private Double nightFinishQty;

    /**
     * 夜班完成率
     */
    @Excel(name = "schedule.glueScheduleResult.nightFinishRate", sort = 550, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.nightFinishRate", min = 0, max = 100)
    @ApiModelProperty(value = "夜班完成率", position = 180)
    @TableField(exist = false)
    private Double nightFinishRate;

    /** 夜班生产顺序 */
    @Excel(name = "schedule.glueScheduleResult.nightProduceOrder", sort = 800, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.nightProduceOrder", number=true, min=0, max=999999)
    @ApiModelProperty(value = "夜班生产顺序", position = 220)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.INTEGER)
    private Integer nightProduceOrder;
    /** 夜班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "夜班预计开始时间", position = 230)
    private Date nightExpectStartTime;
    /** 夜班预计完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.glueScheduleResult.nightExpectFinishTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 950)
    @ApiModelProperty(value = "夜班预计完成时间", position = 240)
    private Date nightExpectFinishTime;
    /** 夜班备注 */
    @Excel(name = "schedule.glueScheduleResult.nightRemark", sort = 1000)
    @ImportValidated(name = "schedule.glueScheduleResult.nightRemark", maxLength=300)
    @ApiModelProperty(value = "夜班备注", position = 250)
    private String nightRemark;
    /** 白班计划量（8::00-16:00，单位：车） */
    @Excel(name = "schedule.glueScheduleResult.dayPlanQty", sort = 1100, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.dayPlanQty", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "白班计划量（8::00-16:00，单位：车）", position = 260)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private Double dayPlanQty;
    /** 白班完成量（8::00-16:00，单位：车） */
    @Excel(name = "schedule.glueScheduleResult.dayFinishQty", sort = 1150, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.dayFinishQty", number=true, min=0, max=9999999)
    @ApiModelProperty(value = "白班完成量（8::00-16:00，单位：车）", position = 260)
    @TableField(exist = false)
    private Double dayFinishQty;

    /**
     * 白班完成率
     */
    @Excel(name = "schedule.glueScheduleResult.dayFinishRate", sort = 550, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.dayFinishRate", min = 0, max = 100)
    @ApiModelProperty(value = "白班完成率", position = 180)
    @TableField(exist = false)
    private Double dayFinishRate;

    /** 白班生产顺序 */
    @Excel(name = "schedule.glueScheduleResult.dayProduceOrder", sort = 1050, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.dayProduceOrder", number=true, min=0, max=999999)
    @ApiModelProperty(value = "白班生产顺序", position = 270)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.INTEGER)
    private Integer dayProduceOrder;
    /** 白班预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "白班预计开始时间", position = 280)
    private Date dayExpectStartTime;
    /** 白班预计完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.glueScheduleResult.dayExpectFinishTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 1200)
    @ApiModelProperty(value = "白班预计完成时间", position = 290)
    private Date dayExpectFinishTime;
    /** 白班备注 */
    @Excel(name = "schedule.glueScheduleResult.dayRemark", sort = 1250)
    @ImportValidated(name = "schedule.glueScheduleResult.dayRemark", maxLength=300)
    @ApiModelProperty(value = "白班备注", position = 300)
    private String dayRemark;
    /** 发布状态，0--未发布，1--已发布，2-发布失败，3-发布中，4-超时失败，5-待发布。对应数据字典为：RELEASE_STATUS */
    @Excel(name = "schedule.glueScheduleResult.releaseStatus", dictType = "MIX_RELEASE_STATUS", sort = 50)
    @ImportValidated(name = "schedule.glueScheduleResult.releaseStatus", maxLength=1)
    @ApiModelProperty(value = "发布状态，0--未发布，1--已发布，2-发布失败，3-发布中，4-超时失败，5-待发布。对应数据字典为：RELEASE_STATUS", position = 310)
    private String releaseStatus;
    /** 发布成功计数器，每次发布成功进行累加。如果大于1发，发布状态只能到待发布 */
    @ApiModelProperty(value = "发布成功计数器，每次发布成功进行累加。如果大于1发，发布状态只能到待发布", position = 320)
    private Integer publishSuccessCount;
    /** 保留最新的一次发布成功时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "保留最新的一次发布成功时间", position = 330)
    private Date newestPublishTime;
    /** 数据来源：0&gt;自动排程；1&gt;插单；2&gt;导入 */
    @ApiModelProperty(value = "数据来源：0&gt;自动排程；1&gt;插单；2&gt;导入", position = 340)
    private String dataSource;
    /** 备注 */
    @Excel(name = "ui.remark")
    @ImportValidated(name = "ui.remark", maxLength=300)
    @ApiModelProperty(value = "备注", position = 350)
    private String remark;

    @Excel(name = "schedule.glueScheduleResult.machineName", sort = 100)
    @ImportValidated(name = "schedule.glueScheduleResult.machineName")
    @ApiModelProperty(value = "机台名称", position = 360)
    @TableField(exist = false)
    private String machineName;

    @Excel(name = "schedule.glueScheduleResult.totalFinish", sort = 500, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.glueScheduleResult.totalFinish", isCode = true, maxLength=90)
    @ApiModelProperty(value = "总完成", position = 360)
    @TableField(exist = false)
    private Double totalFinish;

    @ApiModelProperty(value = "页面选中多条记录后的id", position = 370)
    @TableField(exist = false)
    private String ids;

    /**
     * 配方类型名称
     */
    @Excel(name = "schedule.glueScheduleResult.recipeTypeName", sort = 510)
    @ImportValidated(name = "schedule.glueScheduleResult.recipeTypeName")
    @ApiModelProperty(value = "配方类型名称", position = 370)
    @TableField(exist = false)
    private String recipeTypeName;

    /**
     * 是否过滤分厂未提报胶料，true过滤，false不过滤
     */
    @ApiModelProperty(value = "是否过滤分厂未提报胶料，true过滤，false不过滤", position = 400)
    @TableField(exist = false)
    private Boolean isFilterNoMachine;

    /**
     * 配方物料编号
     */
    @ApiModelProperty(value = "配方物料编号", position = 410)
    private String recipeMaterialCode;

    /**
     * 收尾计划标识(1-是，准备收尾；0--否)
     */
    @ApiModelProperty(value = "收尾计划标识(1-是，准备收尾；0--否)", position = 420)
    private String isFinishing;

    /**
     * 是否级联修改母炼胶标识
     */
    @ApiModelProperty(value = "是否级联修改母炼胶标识", position = 430)
    @TableField(exist = false)
    private Boolean isChangeMasterbatch;

    /**
     * 是否新增记录标识
     */
    @ApiModelProperty(value = "是否新增记录标识", position = 440)
    @TableField(exist = false)
    private Boolean isAddNew;
    
	/**
	 * 中班发布状态，1=发布成功；2=发布失败
	 */
	private String midPublishStatus;
    
	/**
	 * 夜班发布状态，1=发布成功；2=发布失败
	 */
	private String nightPublishStatus;
    
	/**
	 * 白班发布状态，1=发布成功；2=发布失败
	 */
	private String dayPublishStatus;
    
    /** 各班发布状态提示信息 */
    @TableField(exist = false)
    private String releaseStatusTip;
    
	/**
	 * 来源工单号，目前用于转机台保存来源的工单号
	 */
    @TableField(exist = false)
	private String sourceOrderNo;
    
    /**
     * 工厂
     */
    @TableField(exist = false)
    private String factoryCode;
    
    /**
     * 公司
     */
    @TableField(exist = false)
    private String companyCode;
    
    /**
     * 版本号
     */
    @TableField(exist = false)
    private String dataVersion;
    
    /**
     * 操作IP
     */
    @TableField(exist = false)
    private String operIp;
}
