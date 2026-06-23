package com.zlt.aps.itf.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * MES胎圈排程结果中间表实体
 * 对应表：MES_TQ_SCHEDULE_RESULT
 * 保留旧版字段命名，CX_CLASS改为3~8班
 *
 * @author APS
 */
@Data
@TableName(value = "MES_TQ_SCHEDULE_RESULT")
@ApiModel(value = "MES胎圈排程结果中间表实体", description = "MES胎圈排程结果中间表")
public class MesTqScheduleResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期 */
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private LocalDate scheduleDate;

    /** 成型批次号 */
    @ApiModelProperty(value = "成型批次号", name = "cxBatchNo")
    @TableField(value = "CX_BATCH_NO")
    private String cxBatchNo;

    /** 胎圈批次号 */
    @ApiModelProperty(value = "胎圈批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 工单号 */
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 胎圈代码 */
    @ApiModelProperty(value = "胎圈代码", name = "beadCode")
    @TableField(value = "BEAD_CODE")
    private String beadCode;

    /** SAP物料编码 */
    @ApiModelProperty(value = "SAP物料编码", name = "sapMaterialCode")
    @TableField(value = "SAP_MATERIAL_CODE")
    private String sapMaterialCode;

    /** 钢丝圈代码 */
    @ApiModelProperty(value = "钢丝圈代码", name = "steelRingCode")
    @TableField(value = "STEEL_RING_CODE")
    private String steelRingCode;

    /** 三角胶代码 */
    @ApiModelProperty(value = "三角胶代码", name = "triangleGlueCode")
    @TableField(value = "TRIANGLE_GLUE_CODE")
    private String triangleGlueCode;

    /** 胶料代码 */
    @ApiModelProperty(value = "胶料代码", name = "glueCode")
    @TableField(value = "GLUE_CODE")
    private String glueCode;

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

    // ========== 对应成型3~8班计划量 ==========
    /** 对应成型3班计划量 */
    @ApiModelProperty(value = "对应成型3班计划量", name = "cxClass3Plan")
    @TableField(value = "CX_CLASS3_PLAN")
    private Double cxClass3Plan;

    /** 对应成型4班计划量 */
    @ApiModelProperty(value = "对应成型4班计划量", name = "cxClass4Plan")
    @TableField(value = "CX_CLASS4_PLAN")
    private Double cxClass4Plan;

    /** 对应成型5班计划量 */
    @ApiModelProperty(value = "对应成型5班计划量", name = "cxClass5Plan")
    @TableField(value = "CX_CLASS5_PLAN")
    private Double cxClass5Plan;

    /** 对应成型6班计划量 */
    @ApiModelProperty(value = "对应成型6班计划量", name = "cxClass6Plan")
    @TableField(value = "CX_CLASS6_PLAN")
    private Double cxClass6Plan;

    /** 对应成型7班计划量 */
    @ApiModelProperty(value = "对应成型7班计划量", name = "cxClass7Plan")
    @TableField(value = "CX_CLASS7_PLAN")
    private Double cxClass7Plan;

    /** 对应成型8班计划量 */
    @ApiModelProperty(value = "对应成型8班计划量", name = "cxClass8Plan")
    @TableField(value = "CX_CLASS8_PLAN")
    private Double cxClass8Plan;

    // ========== 状态与公共字段 ==========
    /** 是否发布 */
    @ApiModelProperty(value = "是否发布", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /** 收尾提示标识(0:提示收尾；1:不需要提示) */
    @ApiModelProperty(value = "收尾提示标识", name = "markCloseOutTip")
    @TableField(value = "MARK_CLOSE_OUT_TIP")
    private String markCloseOutTip;

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
