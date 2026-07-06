package com.zlt.aps.itf.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * MES胎面排程结果中间表实体
 * 对应表：MES_TM_SCHEDULE_RESULT
 *
 * @author APS
 */
@Data
@TableName(value = "MES_TM_SCHEDULE_RESULT")
@ApiModel(value = "MES胎面排程结果中间表实体", description = "MES胎面排程结果中间表")
public class MesTmScheduleResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期 */
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private LocalDate scheduleDate;

    /** 胎面批次号 */
    @ApiModelProperty(value = "胎面批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 工单号 */
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 胎面代码 */
    @ApiModelProperty(value = "胎面代码", name = "treadCode")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    /** SAP物料编码 */
    @ApiModelProperty(value = "SAP物料编码", name = "sapMaterialCode")
    @TableField(value = "SAP_MATERIAL_CODE")
    private String sapMaterialCode;

    /** 主胶料编码 */
    @ApiModelProperty(value = "主胶料编码", name = "glueCode")
    @TableField(value = "GLUE_CODE")
    private String glueCode;

    /** 基部胶编码 */
    @ApiModelProperty(value = "基部胶编码", name = "baseGlueCode")
    @TableField(value = "BASE_GLUE_CODE")
    private String baseGlueCode;

    /** 整条胶料组合编码 */
    @ApiModelProperty(value = "整条胶料组合编码", name = "wholeGlueCode")
    @TableField(value = "WHOLE_GLUE_CODE")
    private String wholeGlueCode;

    /** 胶料顺序 */
    @ApiModelProperty(value = "胶料顺序", name = "glueSeq")
    @TableField(value = "GLUE_SEQ")
    private String glueSeq;

    /** 口型板代码 */
    @ApiModelProperty(value = "口型板代码", name = "mouthPlateCode")
    @TableField(value = "MOUTH_PLATE_CODE")
    private String mouthPlateCode;

    /** 尺寸 */
    @ApiModelProperty(value = "尺寸", name = "specSize")
    @TableField(value = "SPEC_SIZE")
    private String specSize;

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /** 单耗 */
    @ApiModelProperty(value = "单耗", name = "unitConsume")
    @TableField(value = "UNIT_CONSUME")
    private Double unitConsume;

    /** 库存数量 */
    @ApiModelProperty(value = "库存数量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Double stockQty;

    /** 库存供应成型时长（小时） */
    @ApiModelProperty(value = "库存供应成型时长", name = "supplyTime")
    @TableField(value = "SUPPLY_TIME")
    private Double supplyTime;

    // ========== 中班(14:00-22:00) ==========
    /** 中班计划量 */
    @ApiModelProperty(value = "中班计划量", name = "midPlanQty")
    @TableField(value = "MID_PLAN_QTY")
    private Double midPlanQty;

    /** 中班生产顺序 */
    @ApiModelProperty(value = "中班生产顺序", name = "midProduceOrder")
    @TableField(value = "MID_PRODUCE_ORDER")
    private Integer midProduceOrder;

    /** 中班系统原因分析 */
    @ApiModelProperty(value = "中班系统原因分析", name = "midSysAnalysis")
    @TableField(value = "MID_SYS_ANALYSIS")
    private String midSysAnalysis;

    /** 中班手工输入原因分析 */
    @ApiModelProperty(value = "中班手工输入原因分析", name = "midHandAnalysis")
    @TableField(value = "MID_HAND_ANALYSIS")
    private String midHandAnalysis;

    // ========== 夜班(22:00-06:00) ==========
    /** 夜班计划量 */
    @ApiModelProperty(value = "夜班计划量", name = "nightPlanQty")
    @TableField(value = "NIGHT_PLAN_QTY")
    private Double nightPlanQty;

    /** 夜班生产顺序 */
    @ApiModelProperty(value = "夜班生产顺序", name = "nightProduceOrder")
    @TableField(value = "NIGHT_PRODUCE_ORDER")
    private Integer nightProduceOrder;

    /** 夜班系统原因分析 */
    @ApiModelProperty(value = "夜班系统原因分析", name = "nightSysAnalysis")
    @TableField(value = "NIGHT_SYS_ANALYSIS")
    private String nightSysAnalysis;

    /** 夜班手工输入原因分析 */
    @ApiModelProperty(value = "夜班手工输入原因分析", name = "nightHandAnalysis")
    @TableField(value = "NIGHT_HAND_ANALYSIS")
    private String nightHandAnalysis;

    // ========== 早班(06:00-14:00) ==========
    /** 早班计划量 */
    @ApiModelProperty(value = "早班计划量", name = "dayPlanQty")
    @TableField(value = "DAY_PLAN_QTY")
    private Double dayPlanQty;

    /** 早班生产顺序 */
    @ApiModelProperty(value = "早班生产顺序", name = "dayProduceOrder")
    @TableField(value = "DAY_PRODUCE_ORDER")
    private Integer dayProduceOrder;

    /** 早班系统原因分析 */
    @ApiModelProperty(value = "早班系统原因分析", name = "daySysAnalysis")
    @TableField(value = "DAY_SYS_ANALYSIS")
    private String daySysAnalysis;

    /** 早班手工输入原因分析 */
    @ApiModelProperty(value = "早班手工输入原因分析", name = "dayHandAnalysis")
    @TableField(value = "DAY_HAND_ANALYSIS")
    private String dayHandAnalysis;

    // ========== 次日中班(次日14:00-22:00) ==========
    /** 次日中班计划量 */
    @ApiModelProperty(value = "次日中班计划量", name = "nextMidPlanQty")
    @TableField(value = "NEXT_MID_PLAN_QTY")
    private Double nextMidPlanQty;

    /** 次日中班生产顺序 */
    @ApiModelProperty(value = "次日中班生产顺序", name = "nextMidProduceOrder")
    @TableField(value = "NEXT_MID_PRODUCE_ORDER")
    private Integer nextMidProduceOrder;

    /** 次日中班系统原因分析 */
    @ApiModelProperty(value = "次日中班系统原因分析", name = "nextMidSysAnalysis")
    @TableField(value = "NEXT_MID_SYS_ANALYSIS")
    private String nextMidSysAnalysis;

    /** 次日中班手工输入原因分析 */
    @ApiModelProperty(value = "次日中班手工输入原因分析", name = "nextMidHandAnalysis")
    @TableField(value = "NEXT_MID_HAND_ANALYSIS")
    private String nextMidHandAnalysis;

    // ========== 状态与公共字段 ==========
    /** 是否发布 */
    @ApiModelProperty(value = "是否发布", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /** 收尾提示标识(0:提示收尾；1:不需要提示) */
    @ApiModelProperty(value = "收尾提示标识", name = "markCloseOutTip")
    @TableField(value = "MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

    /** 是否收尾任务 */
    @ApiModelProperty(value = "是否收尾任务", name = "tailFlag")
    @TableField(value = "TAIL_FLAG")
    private String tailFlag;

    /** 生产状态 */
    @ApiModelProperty(value = "生产状态", name = "productionStatus")
    @TableField(value = "PRODUCTION_STATUS")
    private String productionStatus;

    /** 备注 */
    @ApiModelProperty(value = "备注", name = "remark")
    @TableField(value = "REMARK")
    private String remark;

    /** 版本号 */
    @ApiModelProperty(value = "版本号", name = "dataVersion")
    @TableField(value = "DATA_VERSION")
    private String dataVersion;

    /** 分公司编码 */
    @ApiModelProperty(value = "分公司编码", name = "companyCode")
    @TableField(value = "COMPANY_CODE")
    private String companyCode;

    /** 厂别 */
    @ApiModelProperty(value = "厂别", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;
}
