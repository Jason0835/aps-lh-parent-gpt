package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 胎面排程结果解释表实体类。
 *
 * <p>用于保存一次排程中单个结果的计划量分量、规则命中、候选机台、未排证据和系统分析。
 * 该实体仅映射数据库表，不承载排程算法。</p>
 */
@Data
@ApiModel(value = "胎面排程结果解释表对象", description = "胎面排程结果解释表对象")
@TableName(value = "T_TM_SCHEDULE_RESULT_EXPLAIN")
public class TmScheduleResultExplain extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编号 */
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /** 结果ID */
    @ApiModelProperty(value = "结果ID", name = "resultId")
    @TableField(value = "RESULT_ID")
    private Long resultId;

    /** 批次号 */
    @ApiModelProperty(value = "批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 追踪标识 */
    @ApiModelProperty(value = "追踪标识", name = "traceId")
    @TableField(value = "TRACE_ID")
    private String traceId;

    /** 任务业务键，由胎面|胶料|口型|班次组成，用于唯一定位同一结果下的任务解释 */
    @ApiModelProperty(value = "任务业务键", name = "taskBusinessKey")
    @TableField(value = "TASK_BUSINESS_KEY")
    private String taskBusinessKey;

    /** 任务工单号 */
    @ApiModelProperty(value = "任务工单号", name = "taskOrderNo")
    @TableField(value = "TASK_ORDER_NO")
    private String taskOrderNo;

    /** 聚合前来源成型工单号集合 */
    @ApiModelProperty(value = "来源成型工单号集合", name = "sourceOrderNos")
    @TableField(value = "SOURCE_ORDER_NOS")
    private String sourceOrderNos;

    /** 班次顺序 */
    @ApiModelProperty(value = "班次顺序", name = "shiftOrder")
    @TableField(value = "SHIFT_ORDER")
    private Integer shiftOrder;

    /** 同胎面同班次计划量汇总组业务键 */
    @ApiModelProperty(value = "计划量汇总组业务键", name = "planGroupKey")
    @TableField(value = "PLAN_GROUP_KEY")
    private String planGroupKey;

    /** 汇总组来源任务数量 */
    @ApiModelProperty(value = "汇总组来源任务数量", name = "groupSourceCount")
    @TableField(value = "GROUP_SOURCE_COUNT")
    private Integer groupSourceCount;

    /** 当前来源参与汇总的需求量 */
    @ApiModelProperty(value = "来源参与汇总需求量", name = "sourceRequiredQty")
    @TableField(value = "SOURCE_REQUIRED_QTY")
    private BigDecimal sourceRequiredQty;

    /** 汇总组库存抵扣前需求量 */
    @ApiModelProperty(value = "汇总组需求量", name = "groupRequiredQty")
    @TableField(value = "GROUP_REQUIRED_QTY")
    private BigDecimal groupRequiredQty;

    /** 汇总组库存抵扣后基础需求量 */
    @ApiModelProperty(value = "汇总组基础需求量", name = "groupBaseDemandQty")
    @TableField(value = "GROUP_BASE_DEMAND_QTY")
    private BigDecimal groupBaseDemandQty;

    /** 汇总组最小起排调整量 */
    @ApiModelProperty(value = "汇总组最小起排调整量", name = "groupMinStartAdjustQty")
    @TableField(value = "GROUP_MIN_START_ADJUST_QTY")
    private BigDecimal groupMinStartAdjustQty;

    /** 汇总组收尾或卷曲取整调整量 */
    @ApiModelProperty(value = "汇总组收尾或卷曲取整调整量", name = "groupRoundAdjustQty")
    @TableField(value = "GROUP_ROUND_ADJUST_QTY")
    private BigDecimal groupRoundAdjustQty;

    /** 汇总组最终计划量 */
    @ApiModelProperty(value = "汇总组最终计划量", name = "groupFinalPlanQty")
    @TableField(value = "GROUP_FINAL_PLAN_QTY")
    private BigDecimal groupFinalPlanQty;

    /** 来源解释关联的已排或未排目标片段 */
    @ApiModelProperty(value = "来源解释目标关联列表", name = "targetRelationList")
    @TableField(exist = false)
    private List<TmScheduleExplainTargetRel> targetRelationList;

    /** 基础需求量 */
    @ApiModelProperty(value = "基础需求量", name = "baseDemandQty")
    @TableField(value = "BASE_DEMAND_QTY")
    private BigDecimal baseDemandQty;

    /** 损耗补偿量 */
    @ApiModelProperty(value = "损耗补偿量", name = "lossAddQty")
    @TableField(value = "LOSS_ADD_QTY")
    private BigDecimal lossAddQty;

    /** 库存抵扣量 */
    @ApiModelProperty(value = "库存抵扣量", name = "stockDeductQty")
    @TableField(value = "STOCK_DEDUCT_QTY")
    private BigDecimal stockDeductQty;

    /** 上班次供应量 */
    @ApiModelProperty(value = "上班次供应量", name = "lastShiftSupplyQty")
    @TableField(value = "LAST_SHIFT_SUPPLY_QTY")
    private BigDecimal lastShiftSupplyQty;

    /** 月结余抵扣量 */
    @ApiModelProperty(value = "月结余抵扣量", name = "monthSurplusDeductQty")
    @TableField(value = "MONTH_SURPLUS_DEDUCT_QTY")
    private BigDecimal monthSurplusDeductQty;

    /** 工装限制调整量 */
    @ApiModelProperty(value = "工装限制调整量", name = "toolLimitAdjustQty")
    @TableField(value = "TOOL_LIMIT_ADJUST_QTY")
    private BigDecimal toolLimitAdjustQty;

    /** 最小开机调整量 */
    @ApiModelProperty(value = "最小开机调整量", name = "minStartAdjustQty")
    @TableField(value = "MIN_START_ADJUST_QTY")
    private BigDecimal minStartAdjustQty;

    /** 尾数取整调整量 */
    @ApiModelProperty(value = "尾数取整调整量", name = "tailRoundAdjustQty")
    @TableField(value = "TAIL_ROUND_ADJUST_QTY")
    private BigDecimal tailRoundAdjustQty;

    /** 产能调整量 */
    @ApiModelProperty(value = "产能调整量", name = "capacityAdjustQty")
    @TableField(value = "CAPACITY_ADJUST_QTY")
    private BigDecimal capacityAdjustQty;

    /** 最终计划量 */
    @ApiModelProperty(value = "最终计划量", name = "finalPlanQty")
    @TableField(value = "FINAL_PLAN_QTY")
    private BigDecimal finalPlanQty;

    /** 计算公式说明 */
    @ApiModelProperty(value = "计算公式说明", name = "calcFormulaDesc")
    @TableField(value = "CALC_FORMULA_DESC")
    private String calcFormulaDesc;

    /** 当前库存量 */
    @ApiModelProperty(value = "当前库存量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private BigDecimal stockQty;

    /** 计划库存量 */
    @ApiModelProperty(value = "计划库存量", name = "planStockQty")
    @TableField(value = "PLAN_STOCK_QTY")
    private BigDecimal planStockQty;

    /** 供应小时数 */
    @ApiModelProperty(value = "供应小时数", name = "supplyHours")
    @TableField(value = "SUPPLY_HOURS")
    private BigDecimal supplyHours;

    /** 覆盖班次数 */
    @ApiModelProperty(value = "覆盖班次数", name = "coverageShiftCount")
    @TableField(value = "COVERAGE_SHIFT_COUNT")
    private Integer coverageShiftCount;

    /** 上班次计划量 */
    @ApiModelProperty(value = "上班次计划量", name = "lastShiftPlanQty")
    @TableField(value = "LAST_SHIFT_PLAN_QTY")
    private BigDecimal lastShiftPlanQty;

    /** 月结余量 */
    @ApiModelProperty(value = "月结余量", name = "monthSurplusQty")
    @TableField(value = "MONTH_SURPLUS_QTY")
    private BigDecimal monthSurplusQty;

    /** 库存抵扣前当前班成型胎面需求量 */
    @ApiModelProperty(value = "库存抵扣前当前班成型胎面需求量", name = "requiredQty")
    @TableField(value = "REQUIRED_QTY")
    private BigDecimal requiredQty;

    /** 规则命中JSON */
    @ApiModelProperty(value = "规则命中JSON", name = "ruleHitJson")
    @TableField(value = "RULE_HIT_JSON")
    private String ruleHitJson;

    /** 规则摘要说明 */
    @ApiModelProperty(value = "规则摘要说明", name = "ruleSummaryDesc")
    @TableField(value = "RULE_SUMMARY_DESC")
    private String ruleSummaryDesc;

    /** 候选机台JSON */
    @ApiModelProperty(value = "候选机台JSON", name = "candidateMachineJson")
    @TableField(value = "CANDIDATE_MACHINE_JSON")
    private String candidateMachineJson;

    /** 选中机台评分 */
    @ApiModelProperty(value = "选中机台评分", name = "selectedMachineScore")
    @TableField(value = "SELECTED_MACHINE_SCORE")
    private BigDecimal selectedMachineScore;

    /** 最终选机说明 */
    @ApiModelProperty(value = "最终选机说明", name = "machineSelectReason")
    @TableField(value = "MACHINE_SELECT_REASON")
    private String machineSelectReason;

    /** 分配状态 */
    @ApiModelProperty(value = "分配状态", name = "assignStatus")
    @TableField(value = "ASSIGN_STATUS")
    private String assignStatus;

    /** 未排原因编码 */
    @ApiModelProperty(value = "未排原因编码", name = "unplannedReasonCode")
    @TableField(value = "UNPLANNED_REASON_CODE")
    private String unplannedReasonCode;

    /** 未排原因描述 */
    @ApiModelProperty(value = "未排原因描述", name = "unplannedReasonDesc")
    @TableField(value = "UNPLANNED_REASON_DESC")
    private String unplannedReasonDesc;

    /** 未排证据JSON */
    @ApiModelProperty(value = "未排证据JSON", name = "unplannedEvidenceJson")
    @TableField(value = "UNPLANNED_EVIDENCE_JSON")
    private String unplannedEvidenceJson;

    /** 任务状态 */
    @ApiModelProperty(value = "任务状态", name = "taskStatus")
    @TableField(value = "TASK_STATUS")
    private String taskStatus;

    /** 人工锁定标识 */
    @ApiModelProperty(value = "人工锁定标识", name = "manualLockedFlag")
    @TableField(value = "MANUAL_LOCKED_FLAG")
    private String manualLockedFlag;

    /** 顺序锁定标识 */
    @ApiModelProperty(value = "顺序锁定标识", name = "sequenceLockFlag")
    @TableField(value = "SEQUENCE_LOCK_FLAG")
    private String sequenceLockFlag;

    /** 强制变更标识 */
    @ApiModelProperty(value = "强制变更标识", name = "forceChangeFlag")
    @TableField(value = "FORCE_CHANGE_FLAG")
    private String forceChangeFlag;

    /** 生成模式 */
    @ApiModelProperty(value = "生成模式", name = "generateMode")
    @TableField(value = "GENERATE_MODE")
    private String generateMode;

    /** 当前步骤编码 */
    @ApiModelProperty(value = "当前步骤编码", name = "currentStepCode")
    @TableField(value = "CURRENT_STEP_CODE")
    private String currentStepCode;

    /** 系统分析 */
    @ApiModelProperty(value = "系统分析", name = "sysAnalysis")
    @TableField(value = "SYS_ANALYSIS")
    private String sysAnalysis;

    /** 告警信息 */
    @ApiModelProperty(value = "告警信息", name = "warningMsg")
    @TableField(value = "WARNING_MSG")
    private String warningMsg;

    /** 异常信息 */
    @ApiModelProperty(value = "异常信息", name = "errorMsg")
    @TableField(value = "ERROR_MSG")
    private String errorMsg;

    /** 胎面编码 */
    @ApiModelProperty(value = "胎面编码", name = "treadCode")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    /** 主胶料编码 */
    @ApiModelProperty(value = "主胶料编码", name = "glueCode")
    @TableField(value = "GLUE_CODE")
    private String glueCode;

    /** 基部胶编码 */
    @ApiModelProperty(value = "基部胶编码", name = "baseGlueCode")
    @TableField(value = "BASE_GLUE_CODE")
    private String baseGlueCode;

    /** 口型板编码 */
    @ApiModelProperty(value = "口型板编码", name = "mouthPlateCode")
    @TableField(value = "MOUTH_PLATE_CODE")
    private String mouthPlateCode;
}
