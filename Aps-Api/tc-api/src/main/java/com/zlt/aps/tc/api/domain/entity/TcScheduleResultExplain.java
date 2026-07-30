package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 胎侧排程任务级解释实体。
 *
 * <p>高频过滤字段独立存列，稀疏计算分量、候选机台、规则命中、未排证据和异常明细
 * 使用带 schemaVersion 的 JSON 保存。</p>
 */
@Data
@ApiModel(value = "胎侧排程任务解释对象", description = "胎侧排程任务解释对象")
@TableName("T_TC_SCHEDULE_RESULT_EXPLAIN")
public class TcScheduleResultExplain extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /** 批次号 */
    @TableField("BATCH_NO")
    private String batchNo;

    /** 已排结果主键，未排和无需生产任务为空 */
    @TableField("RESULT_ID")
    private Long resultId;

    /** 稳定任务业务键 */
    @TableField("TASK_BUSINESS_KEY")
    private String taskBusinessKey;

    /** 班次顺序 */
    @TableField("SHIFT_ORDER")
    private Integer shiftOrder;

    /** 同胎侧同班次计划量汇总组业务键 */
    @TableField("PLAN_GROUP_KEY")
    private String planGroupKey;

    /** 汇总组来源任务数量 */
    @TableField("GROUP_SOURCE_COUNT")
    private Integer groupSourceCount;

    /** 当前来源参与汇总的需求量 */
    @TableField("SOURCE_REQUIRED_QTY")
    private BigDecimal sourceRequiredQty;

    /** 汇总组库存抵扣前需求量 */
    @TableField("GROUP_REQUIRED_QTY")
    private BigDecimal groupRequiredQty;

    /** 汇总组库存抵扣后基础需求量 */
    @TableField("GROUP_BASE_DEMAND_QTY")
    private BigDecimal groupBaseDemandQty;

    /** 汇总组最小起排调整量 */
    @TableField("GROUP_MIN_START_ADJUST_QTY")
    private BigDecimal groupMinStartAdjustQty;

    /** 汇总组收尾或卷曲取整调整量 */
    @TableField("GROUP_ROUND_ADJUST_QTY")
    private BigDecimal groupRoundAdjustQty;

    /** 汇总组最终计划量 */
    @TableField("GROUP_FINAL_PLAN_QTY")
    private BigDecimal groupFinalPlanQty;

    /** 来源解释关联的已排或未排目标片段 */
    @ApiModelProperty(value = "来源解释目标关联列表", name = "targetRelationList")
    @TableField(exist = false)
    private List<TcScheduleExplainTargetRel> targetRelationList;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    /** 追踪标识 */
    @TableField("TRACE_ID")
    private String traceId;

    /** 胎侧编码 */
    @TableField("SIDEWALL_CODE")
    private String sidewallCode;

    /** 主胶料编码 */
    @TableField("GLUE_CODE")
    private String glueCode;

    /** 基部胶编码 */
    @TableField("BASE_GLUE_CODE")
    private String baseGlueCode;

    /** 口型板编码 */
    @TableField("MOUTH_PLATE_CODE")
    private String mouthPlateCode;

    /** 分配状态 */
    @TableField("ASSIGN_STATUS")
    private String assignStatus;

    /** 任务状态 */
    @TableField("TASK_STATUS")
    private String taskStatus;

    /** 未排原因编码 */
    @TableField("UNPLANNED_REASON_CODE")
    private String unplannedReasonCode;

    /** 库存抵扣后的基础应排需求量 */
    @TableField("BASE_DEMAND_QTY")
    private BigDecimal baseDemandQty;

    /** 库存抵扣量 */
    @TableField("STOCK_DEDUCT_QTY")
    private BigDecimal stockDeductQty;

    /** 最终计划量 */
    @TableField("FINAL_PLAN_QTY")
    private BigDecimal finalPlanQty;

    /** 全局工装账本结算顺序 */
    @TableField("TOOL_LEDGER_ORDER")
    private Integer toolLedgerOrder;

    /** 工装账本结算前可用数量 */
    @TableField("AVAILABLE_TOOL_QTY")
    private BigDecimal availableToolQty;

    /** 当前任务工装净占用数量 */
    @TableField("TOOL_USED_QTY")
    private BigDecimal toolUsedQty;

    /** 工装账本结算后剩余数量 */
    @TableField("REMAINING_TOOL_QTY")
    private BigDecimal remainingToolQty;

    /** 计划量计算分解 JSON */
    @TableField("PLAN_QTY_BREAKDOWN_JSON")
    private String planQtyBreakdownJson;

    /** 六点库存净值 */
    @TableField("STOCK_QTY")
    private BigDecimal stockQty;

    /** 交接班预计库存 */
    @TableField("PLAN_STOCK_QTY")
    private BigDecimal planStockQty;

    /** 库存供应时长 */
    @TableField("SUPPLY_HOURS")
    private BigDecimal supplyHours;

    /** 最低库存保证班数 */
    @TableField("COVERAGE_SHIFT_COUNT")
    private Integer coverageShiftCount;

    /** 选中机台编码 */
    @TableField("SELECTED_MACHINE_CODE")
    private String selectedMachineCode;

    /** 选中机台评分 */
    @TableField("SELECTED_MACHINE_SCORE")
    private BigDecimal selectedMachineScore;

    /** 候选机台 JSON */
    @TableField("CANDIDATE_MACHINE_JSON")
    private String candidateMachineJson;

    /** 规则命中 JSON */
    @TableField("RULE_HIT_JSON")
    private String ruleHitJson;

    /** 未排证据 JSON */
    @TableField("UNPLANNED_EVIDENCE_JSON")
    private String unplannedEvidenceJson;

    /** 任务最高异常级别 */
    @TableField("ISSUE_LEVEL")
    private String issueLevel;

    /** 任务结构化异常 JSON */
    @TableField("ISSUE_JSON")
    private String issueJson;

    /** 人工锁定标识 */
    @TableField("MANUAL_LOCKED_FLAG")
    private String manualLockedFlag;

    /** 顺序锁定标识 */
    @TableField("SEQUENCE_LOCK_FLAG")
    private String sequenceLockFlag;

    /** 强制变更标识 */
    @TableField("FORCE_CHANGE_FLAG")
    private String forceChangeFlag;
}
