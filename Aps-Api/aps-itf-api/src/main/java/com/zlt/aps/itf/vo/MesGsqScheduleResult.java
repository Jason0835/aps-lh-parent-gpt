package com.zlt.aps.itf.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * MES钢丝圈排程结果中间表实体
 * 对应表：MES_GSQ_SCHEDULE_RESULT
 *
 * @author APS
 */
@Data
@TableName(value = "MES_GSQ_SCHEDULE_RESULT")
@ApiModel(value = "MES钢丝圈排程结果中间表实体", description = "MES钢丝圈排程结果中间表")
public class MesGsqScheduleResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排程日期 */
    @ApiModelProperty(value = "排程日期", name = "scheduleDate")
    @TableField(value = "SCHEDULE_DATE")
    private LocalDate scheduleDate;

    /** 胎圈批次号 */
    @ApiModelProperty(value = "胎圈批次号", name = "tqBatchNo")
    @TableField(value = "TQ_BATCH_NO")
    private String tqBatchNo;

    /** 钢丝圈批次号 */
    @ApiModelProperty(value = "钢丝圈批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 工单号 */
    @ApiModelProperty(value = "工单号", name = "orderNo")
    @TableField(value = "ORDER_NO")
    private String orderNo;

    /** 钢丝圈代码 */
    @ApiModelProperty(value = "钢丝圈代码", name = "steelRingCode")
    @TableField(value = "STEEL_RING_CODE")
    private String steelRingCode;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码", name = "materialCode")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 钢丝圈类型 */
    @ApiModelProperty(value = "钢丝圈类型", name = "steelType")
    @TableField(value = "STEEL_TYPE")
    private String steelType;

    /** 胎胚描述 */
    @ApiModelProperty(value = "胎胚描述", name = "embryoSpecDesc")
    @TableField(value = "EMBRYO_SPEC_DESC")
    private String embryoSpecDesc;

    /** 单耗 */
    @ApiModelProperty(value = "单耗", name = "unitConsume")
    @TableField(value = "UNIT_CONSUME")
    private Double unitConsume;

    /** 钢丝缠绕盘代码 */
    @ApiModelProperty(value = "钢丝缠绕盘代码", name = "twiningDiscCode")
    @TableField(value = "TWINING_DISC_CODE")
    private String twiningDiscCode;

    /** 英寸 */
    @ApiModelProperty(value = "英寸", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private String proSize;

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /** 库存数量 */
    @ApiModelProperty(value = "库存数量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private Double stockQty;

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

    // ========== 对应胎圈1~6班消耗量 ==========
    /** 对应胎圈1班消耗量 */
    @ApiModelProperty(value = "对应胎圈1班消耗量", name = "tqClass1Plan")
    @TableField(value = "TQ_CLASS1_PLAN")
    private Integer tqClass1Plan;

    /** 对应胎圈2班消耗量 */
    @ApiModelProperty(value = "对应胎圈2班消耗量", name = "tqClass2Plan")
    @TableField(value = "TQ_CLASS2_PLAN")
    private Integer tqClass2Plan;

    /** 对应胎圈3班消耗量 */
    @ApiModelProperty(value = "对应胎圈3班消耗量", name = "tqClass3Plan")
    @TableField(value = "TQ_CLASS3_PLAN")
    private Integer tqClass3Plan;

    /** 对应胎圈4班消耗量 */
    @ApiModelProperty(value = "对应胎圈4班消耗量", name = "tqClass4Plan")
    @TableField(value = "TQ_CLASS4_PLAN")
    private Integer tqClass4Plan;

    /** 对应胎圈5班消耗量 */
    @ApiModelProperty(value = "对应胎圈5班消耗量", name = "tqClass5Plan")
    @TableField(value = "TQ_CLASS5_PLAN")
    private Integer tqClass5Plan;

    /** 对应胎圈6班消耗量 */
    @ApiModelProperty(value = "对应胎圈6班消耗量", name = "tqClass6Plan")
    @TableField(value = "TQ_CLASS6_PLAN")
    private Integer tqClass6Plan;

    // ========== 状态与公共字段 ==========
    /** 是否发布 */
    @ApiModelProperty(value = "是否发布", name = "isRelease")
    @TableField(value = "IS_RELEASE")
    private String isRelease;

    /** 收尾规格标记(0：收尾，1：非收尾) */
    @ApiModelProperty(value = "收尾规格标记", name = "closeOutSpecFlag")
    @TableField(value = "CLOSE_OUT_SPEC_FLAG")
    private String closeOutSpecFlag;

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
