package com.zlt.mix.schedule.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
import org.apache.ibatis.type.JdbcType;

import java.util.Date;

/**
 * 硫化辅料日计划排程对象 t_material_schedule_result
 *
 * @author chen
 * @date 2022-05-24
 */
@ApiModel(value = "硫化辅料日计划排程对象", description = "硫化辅料日计划排程对象 ")
@TableName("t_material_schedule_result")
@KeySequence(value = "seq_t_material_schedule_result", dbType = DbType.ORACLE)
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialScheduleResult extends ZltBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_T_MATERIAL_SCHEDULE_RESULT
     */
    @ApiModelProperty(value = "主键ID，对应自增序列为：SEQ_T_MATERIAL_SCHEDULE_RESULT", position = 10)
    private Long id;
    /**
     * 终炼母炼排程批次号
     */
    @ApiModelProperty(value = "终炼母炼排程批次号", position = 20)
    private String glueScheduleBatchNo;
    /**
     * 批次号，每次分解计划批次号重新生成。规则：MATERIAL+密炼区+年月日+3位定长自增序号
     */
    @ApiModelProperty(value = "批次号，每次分解计划批次号重新生成。规则：MATERIAL+密炼区+年月日+3位定长自增序号", position = 30)
    private String batchNo;

    @ApiModelProperty(value = "终炼母炼排程工单号", position = 31)
    private String glueScheduleOrderNo;
    /**
     * 工单号，自动生成（批次号+4位定长自增序号）
     */
    @ApiModelProperty(value = "工单号，自动生成（批次号+4位定长自增序号）", position = 40)
    private String orderNo;
    /**
     * 排程日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "排程日期", position = 50)
    private Date scheduleDate;
    /**
     * 小料机台编号
     */
    @ApiModelProperty(value = "小料机台编号", position = 60)
    private String machineCode;
    /**
     * 密炼区(对应数据字典code：MIX_AREA)
     */
    @ApiModelProperty(value = "密炼区(对应数据字典code：MIX_AREA)", position = 70)
    private String mixArea;
    /**
     * 物料名称
     */
    @Excel(name = "schedule.materialScheduleResult.materialName", sort = 150)
    @ImportValidated(name = "schedule.materialScheduleResult.materialName", required = true, maxLength = 30)
    @ApiModelProperty(value = "物料名称", position = 80)
    private String materialName;
    /**
     * 物理编码
     */
    @ApiModelProperty(value = "物料编码", position = 90)
    private String materialCode;
    /**
     * 配方版本号
     */
    @Excel(name = "schedule.materialScheduleResult.recipeVersionId", sort = 200)
    @ImportValidated(name = "schedule.materialScheduleResult.recipeVersionId", required = true, maxLength = 30)
    @ApiModelProperty(value = "配方版本号", position = 100)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String recipeVersionId;
    /**
     * 配方类型
     */
    @Excel(name = "schedule.materialScheduleResult.recipeType", sort = 200)
    @ImportValidated(name = "schedule.materialScheduleResult.recipeType", maxLength = 3)
    @ApiModelProperty(value = "配方类型", position = 110)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String recipeType;
    /**
     * 配方阶段(对应数据字典：PRODUCT_STAGE)
     */
    @Excel(name = "schedule.materialScheduleResult.recipeStage", sort = 200)
    @ImportValidated(name = "schedule.materialScheduleResult.recipeStage", maxLength = 10)
    @ApiModelProperty(value = "配方阶段(对应数据字典：PRODUCT_STAGE)", position = 110)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.VARCHAR)
    private String recipeStage;
    /**
     * 配方物料编号
     */
    @ApiModelProperty(value = "配方物料编号", position = 120)
    private String recipeMaterialCode;
    /**
     * 常用（0--否，1-是）
     */
    @Excel(name = "schedule.materialScheduleResult.commonlyUsed", sort = 250)
    @ImportValidated(name = "schedule.materialScheduleResult.commonlyUsed", number = true, min = 0, max = 1)
    @ApiModelProperty(value = "常用（0--否，1-是）", position = 130)
    private Integer commonlyUsed;
    /**
     * 库存(车)
     */
    @Excel(name = "schedule.materialScheduleResult.stockQty", sort = 300, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.stockQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "库存(车)", position = 140)
    private Double stockQty;
    /**
     * 需求量
     */
    @Excel(name = "schedule.materialScheduleResult.demandQty", sort = 350, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.demandQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "需求量", position = 150)
    private Double demandQty;
    /**
     * 需求计划
     */
    @ApiModelProperty(value = "需求计划", position = 150)
    private String demandPlanning;

    @ApiModelProperty(value = "常用规格的安全库存量", position = 151)
    private Double safeStockQty;

    /**
     * 总计划
     */
    @Excel(name = "schedule.materialScheduleResult.totalPlanQty", sort = 400, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.totalPlanQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "总计划", position = 160)
    private Double totalPlanQty;
    /**
     * 总剩余
     */
    @Excel(name = "schedule.materialScheduleResult.totalSurplus", sort = 450, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.totalSurplus", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "总剩余", position = 170)
    private Double totalSurplus;
    /**
     * 中班计划量（单位：车）
     */
    @Excel(name = "schedule.materialScheduleResult.midPlanQty", sort = 550, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.midPlanQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "中班计划量（单位：车）", position = 180)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private Double midPlanQty;
    /**
     * 中班完成量（单位：车）
     */
    @Excel(name = "schedule.materialScheduleResult.midFinishQty", sort = 550, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.midFinishQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "中班完成量（单位：车）", position = 180)
    @TableField(exist = false)
    private Double midFinishQty;

    /**
     * 中班完成率
     */
    @Excel(name = "schedule.materialScheduleResult.midFinishRate", sort = 550, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.midFinishRate", min = 0, max = 100)
    @ApiModelProperty(value = "中班完成率", position = 180)
    @TableField(exist = false)
    private Double midFinishRate;

    /**
     * 中班生产顺序
     */
    @Excel(name = "schedule.materialScheduleResult.midProduceOrder", sort = 500, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.midProduceOrder", digits = true, min = 0, max = 999999)
    @ApiModelProperty(value = "中班生产顺序", position = 190)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.INTEGER)
    private Integer midProduceOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "中班预计开始时间", position = 190)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DATE)
    private Date midExpectStartTime;

    /**
     * 中班预计完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.materialScheduleResult.midExpectFinishTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 600)
    @ApiModelProperty(value = "中班预计完成时间", position = 200)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DATE)
    private Date midExpectFinishTime;
    /**
     * 中班备注
     */
    @Excel(name = "schedule.materialScheduleResult.midRemark", sort = 650)
    @ImportValidated(name = "schedule.materialScheduleResult.midRemark", maxLength = 300)
    @ApiModelProperty(value = "中班备注", position = 210)
    private String midRemark;
    /**
     * 夜班计划量（单位：车）
     */
    @Excel(name = "schedule.materialScheduleResult.nightPlanQty", sort = 750, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.nightPlanQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班计划量（单位：车）", position = 220)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private Double nightPlanQty;
    /**
     * 夜班完成量（0:00-8:00，单位：车）
     */
    @Excel(name = "schedule.materialScheduleResult.nightFinishQty", sort = 750, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.nightFinishQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "夜班完成量（0:00-8:00，单位：车）", position = 210)
    @TableField(exist = false)
    private Double nightFinishQty;

    /**
     * 夜班完成率
     */
    @Excel(name = "schedule.materialScheduleResult.nightFinishRate", sort = 550, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.nightFinishRate", min = 0, max = 100)
    @ApiModelProperty(value = "夜班完成率", position = 180)
    @TableField(exist = false)
    private Double nightFinishRate;

    /**
     * 夜班生产顺序
     */
    @Excel(name = "schedule.materialScheduleResult.nightProduceOrder", sort = 700, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.nightProduceOrder", digits = true, min = 0, max = 999999)
    @ApiModelProperty(value = "夜班生产顺序", position = 230)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.INTEGER)
    private Integer nightProduceOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "中班预计开始时间", position = 230)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DATE)
    private Date nightExpectStartTime;

    /**
     * 夜班预计完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.materialScheduleResult.nightExpectFinishTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 800)
    @ApiModelProperty(value = "夜班预计完成时间", position = 240)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DATE)
    private Date nightExpectFinishTime;
    /**
     * 夜班备注
     */
    @Excel(name = "schedule.materialScheduleResult.nightRemark", sort = 850)
    @ImportValidated(name = "schedule.materialScheduleResult.nightRemark", maxLength = 300)
    @ApiModelProperty(value = "夜班备注", position = 250)
    private String nightRemark;
    /**
     * 白班计划量（单位：车）
     */
    @Excel(name = "schedule.materialScheduleResult.dayPlanQty", sort = 950, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.dayPlanQty", digits = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "白班计划量（单位：车）", position = 260)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DOUBLE)
    private Double dayPlanQty;
    /**
     * 白班完成量（8::00-16:00，单位：车）
     */
    @Excel(name = "schedule.materialScheduleResult.dayFinishQty", sort = 950, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.dayFinishQty", number = true, min = 0, max = 9999999)
    @ApiModelProperty(value = "白班完成量（8::00-16:00，单位：车）", position = 260)
    @TableField(exist = false)
    private Double dayFinishQty;

    /**
     * 白班完成率
     */
    @Excel(name = "schedule.materialScheduleResult.dayFinishRate", sort = 550, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.dayFinishRate", min = 0, max = 100)
    @ApiModelProperty(value = "白班完成率", position = 180)
    @TableField(exist = false)
    private Double dayFinishRate;

    /**
     * 白班生产顺序
     */
    @Excel(name = "schedule.materialScheduleResult.dayProduceOrder", sort = 900, cellType = Excel.ColumnType.NUMERIC)
    @ImportValidated(name = "schedule.materialScheduleResult.dayProduceOrder", digits = true, min = 0, max = 999999)
    @ApiModelProperty(value = "白班生产顺序", position = 270)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.INTEGER)
    private Integer dayProduceOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "白班预计开始时间", position = 270)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DATE)
    private Date dayExpectStartTime;

    /**
     * 白班预计完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "schedule.materialScheduleResult.dayExpectFinishTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", sort = 1000)
    @ApiModelProperty(value = "白班预计完成时间", position = 280)
    @TableField(updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.DATE)
    private Date dayExpectFinishTime;
    /**
     * 白班备注
     */
    @Excel(name = "schedule.materialScheduleResult.dayRemark", sort = 1050)
    @ImportValidated(name = "schedule.materialScheduleResult.dayRemark", maxLength = 300)
    @ApiModelProperty(value = "白班备注", position = 290)
    private String dayRemark;
    /**
     * 发布状态，0--未发布，1--已发布，2-发布失败，3-发布中，4-超时失败，5-待发布。对应数据字典为：RELEASE_STATUS
     */
    @Excel(name = "schedule.materialScheduleResult.releaseStatus", sort = 50)
    @ImportValidated(name = "schedule.materialScheduleResult.releaseStatus", maxLength = 300)
    @ApiModelProperty(value = "发布状态，0--未发布，1--已发布，2-发布失败，3-发布中，4-超时失败，5-待发布。对应数据字典为：RELEASE_STATUS", position = 300)
    private String releaseStatus;
    /**
     * 发布成功计数器，每次发布成功进行累加。如果大于1发，发布状态只能到待发布
     */
    @ApiModelProperty(value = "发布成功计数器，每次发布成功进行累加。如果大于1发，发布状态只能到待发布", position = 310)
    private Integer publishSuccessCount;
    /**
     * 保留最新的一次发布成功时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "保留最新的一次发布成功时间", position = 320)
    private Date newestPublishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "胶料中班计划开始生产时间", position = 330)
    private Date glueMidExpectStartTime;

    @ApiModelProperty(value = "胶料中班计划量", position = 335)
    private Double glueMidPlanQty;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "胶料夜班计划开始生产时间", position = 340)
    private Date glueNightExpectStartTime;

    @ApiModelProperty(value = "胶料夜班计划量", position = 345)
    private Double glueNightPlanQty;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "胶料白班计划开始生产时间", position = 345)
    private Date glueDayExpectStartTime;

    @ApiModelProperty(value = "胶料白班计划量", position = 347)
    private Double glueDayPlanQty;

    /**
     * 数据来源：0&gt;自动排程；1&gt;插单；2&gt;导入；3&gt;跨区新增
     */
    @ApiModelProperty(value = "数据来源：0&gt;自动排程；1&gt;插单；2&gt;导入；3&gt;跨区新增", position = 350)
    private String dataSource;
    /**
     * 备注
     */
    @Excel(name = "ui.remark")
    @ImportValidated(name = "ui.remark", maxLength = 300)
    @ApiModelProperty(value = "备注", position = 340)
    private String remark;

    @Excel(name = "schedule.materialScheduleResult.machineName", sort = 100)
    @ImportValidated(name = "schedule.materialScheduleResult.machineName")
    @ApiModelProperty(value = "机台名称", position = 360)
    @TableField(exist = false)
    private String machineName;

    @ApiModelProperty(value = "页面选中多条记录后的id", position = 370)
    @TableField(exist = false)
    private String ids;

    /**
     * 配方类型名称
     */
    @Excel(name = "schedule.materialScheduleResult.recipeTypeName", sort = 510)
    @ImportValidated(name = "schedule.materialScheduleResult.recipeTypeName", required = true)
    @ApiModelProperty(value = "配方类型名称", position = 370)
    @TableField(exist = false)
    private String recipeTypeName;

    /**
     * 班制(如1--长白班，2--两班制，3--三班制；对应数据字典LH_CLASS_SHIFT)
     */
    @Excel(name = "schedule.materialScheduleResult.classShift", dictType = "LH_CLASS_SHIFT")
    @ApiModelProperty(value = "班制(如1--长白班，2--两班制，3--三班制；对应数据字典LH_CLASS_SHIFT)", position = 50)
    private Integer classShift;

    @ApiModelProperty(value = "机台产能", position = 50)
    private Integer capacity;

    /**
     * 小料机台编号
     */
    @ApiModelProperty(value = "旧机台编号")
    @TableField(exist = false)
    private String oldMachineCode;
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
     * 操作IP
     */
    @TableField(exist = false)
    private String operIp;
}
