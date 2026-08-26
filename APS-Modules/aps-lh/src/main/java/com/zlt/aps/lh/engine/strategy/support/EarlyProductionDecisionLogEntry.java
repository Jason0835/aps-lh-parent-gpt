package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 单个 SKU 的提前生产判断日志明细。
 *
 * <p>对象只保存日志所需的标量快照，不持有 SKU、机台、模具或排程上下文，避免过程日志
 * 延长大对象生命周期。字段由现有准入、选机和结果提交链逐步回填，日志采集本身不参与业务判断。</p>
 *
 * @author APS
 */
@Data
public class EarlyProductionDecisionLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前业务日采集器内的顺序。 */
    private Integer sortOrder;
    /** SKU真实进入新增主循环时的顺序；前置拒绝时为空。 */
    private Integer actualSelectionOrder;
    /** 当前业务日期。 */
    private LocalDate businessDate;
    /** 原计划/提前生产来源日期。 */
    private LocalDate sourcePlanDate;
    /** 实际形成结果的业务日期。 */
    private LocalDate actualProductionDate;
    /** 物料编码。 */
    private String materialCode;
    /** 产品状态。 */
    private String productStatus;
    /** 结构名称。 */
    private String structureName;
    /** 当前提前生产阶段。 */
    private String phase;
    /** 当前 SKU 来源编码或来源说明。 */
    private String source;
    /** 当前业务日原始结构计划机台数。 */
    private Integer currentDatePlanMachineCount;
    /** 现有准入实际使用的有效结构计划机台数。 */
    private Integer effectivePlanMachineCount;
    /** 排程窗口 T～T+2 的结构计划硫化机台数文本。 */
    private String structurePlanMachineCountRange;
    /** 现有结构资格判断使用的班次。 */
    private Integer admissionShiftIndex;
    /** 现有结构资格判断时已排结构机台数。 */
    private Integer scheduledStructureCount;
    /** 当前候选实际尝试班次。 */
    private Integer attemptShiftIndex;
    /** 当前候选实际尝试班次的结构已排机台数。 */
    private Integer attemptScheduledStructureCount;
    /** 判断时窗口各班次已排总计划量。 */
    private String realtimeShiftTotalPlanQty;
    /** 判断时窗口各班次换模/换活字块次数。 */
    private String realtimeShiftChangeCount;
    /** 判断时窗口各班次结构已排物理机台数。 */
    private String realtimeStructureMachineCount;
    /** 结构是否通过提前生产准入。 */
    private Boolean structureAdmission;
    /** 模具是否满足当前候选。 */
    private Boolean mouldSatisfied;
    /** 模具所需数量。 */
    private Integer requiredMouldQty;
    /** 模具可用总数量。 */
    private Integer availableMouldQty;
    /** 模具已占用数量。 */
    private Integer occupiedMouldQty;
    /** 模具分配前剩余可用数量。 */
    private Integer remainingAvailableMouldQty;
    /** 当前候选机台数量。 */
    private Integer candidateMachineCount;
    /** 当前候选可排剩余产能。 */
    private Integer remainingCapacity;
    /** 是否允许进入提前生产排产。 */
    private Boolean allowedAdvance;
    /** 是否最终实际形成提前生产结果。 */
    private Boolean actualScheduled;
    /** 实际排产机台编码。 */
    private String actualMachineCode;
    /** 实际计划量。 */
    private Integer plannedQty;
    /** 日志原因编码。 */
    private String reasonCode;
    /** 原有业务原因明细。 */
    private String detail;

    /**
     * 构建单条可落库文本。
     *
     * @return 中文键值明细
     */
    public String buildDetail() {
        StringBuilder detailBuilder = new StringBuilder(1024);
        this.appendField(detailBuilder, "排序", this.displaySortOrder());
        this.appendField(detailBuilder, "物料", materialCode);
        this.appendOptionalField(detailBuilder, "产品状态", productStatus);
        this.appendField(detailBuilder, "允许提前", allowedAdvance);
        this.appendField(detailBuilder, "实际排产", actualScheduled);
        this.appendOptionalField(detailBuilder, "结构", structureName);
        this.appendOptionalField(detailBuilder, "原计划日期", sourcePlanDate);
        this.appendField(detailBuilder, "提前日期", this.resolveAdvanceDate());
        this.appendOptionalField(detailBuilder, "实时班次总计划量", realtimeShiftTotalPlanQty);
        this.appendOptionalField(detailBuilder, "实时班次已排硫化机台数",
                realtimeStructureMachineCount);
        this.appendOptionalField(detailBuilder, "计划硫化机台数", structurePlanMachineCountRange);
        this.appendOptionalField(detailBuilder, "实时班次换模/换活字块次数",
                realtimeShiftChangeCount);
        this.appendField(detailBuilder, "原因", reasonCode);
        this.appendOptionalField(detailBuilder, "明细", detail);
        return detailBuilder.toString();
    }

    /**
     * 追加必填日志字段。
     *
     * @param detailBuilder 日志构建器
     * @param fieldName 字段名
     * @param value 字段值
     */
    private void appendField(StringBuilder detailBuilder, String fieldName, Object value) {
        if (detailBuilder.length() > 0) {
            detailBuilder.append('|');
        }
        detailBuilder.append(fieldName).append('=').append(this.display(value));
    }

    /**
     * 仅在字段有实际值时追加日志字段，避免输出无业务意义的占位符。
     *
     * @param detailBuilder 日志构建器
     * @param fieldName 字段名
     * @param value 字段值
     */
    private void appendOptionalField(
            StringBuilder detailBuilder, String fieldName, Object value) {
        if (Objects.isNull(value)
                || (value instanceof String && StringUtils.isEmpty((String) value))) {
            return;
        }
        this.appendField(detailBuilder, fieldName, value);
    }

    /**
     * 解析日志展示的提前日期。
     *
     * <p>失败明细没有实际结果日期时，仍需展示当前尝试的业务日期，便于按 T/T+1/T+2
     * 还原判断过程。</p>
     *
     * @return 实际落地日期或当前判断业务日期
     */
    private LocalDate resolveAdvanceDate() {
        return Objects.nonNull(actualProductionDate) ? actualProductionDate : businessDate;
    }

    /**
     * 复制一份新的业务尝试明细。
     *
     * <p>当同一 SKU 在同一业务日进入不同实际班次时，保留原准入快照并创建新的尝试对象，
     * 避免后一个班次覆盖前一个班次的失败原因。</p>
     *
     * @return 新的尝试明细
     */
    public EarlyProductionDecisionLogEntry copyForAttempt() {
        EarlyProductionDecisionLogEntry copy = new EarlyProductionDecisionLogEntry();
        copy.setBusinessDate(businessDate);
        copy.setSourcePlanDate(sourcePlanDate);
        copy.setMaterialCode(materialCode);
        copy.setProductStatus(productStatus);
        copy.setStructureName(structureName);
        copy.setPhase(phase);
        copy.setSource(source);
        copy.setCurrentDatePlanMachineCount(currentDatePlanMachineCount);
        copy.setEffectivePlanMachineCount(effectivePlanMachineCount);
        copy.setStructurePlanMachineCountRange(structurePlanMachineCountRange);
        copy.setRealtimeShiftTotalPlanQty(realtimeShiftTotalPlanQty);
        copy.setRealtimeShiftChangeCount(realtimeShiftChangeCount);
        copy.setRealtimeStructureMachineCount(realtimeStructureMachineCount);
        copy.setAdmissionShiftIndex(admissionShiftIndex);
        copy.setScheduledStructureCount(scheduledStructureCount);
        copy.setStructureAdmission(structureAdmission);
        copy.setAllowedAdvance(allowedAdvance);
        copy.setActualScheduled(false);
        copy.setReasonCode(EarlyProductionLogReason.PENDING.getCode());
        copy.setDetail(detail);
        return copy;
    }

    /**
     * 获取日志展示顺序。
     *
     * @return 实际选机顺序优先，否则使用提前生产判断采集顺序
     */
    private String displaySortOrder() {
        return actualSelectionOrder != null && actualSelectionOrder > 0
                ? String.valueOf(actualSelectionOrder) : this.display(sortOrder);
    }

    /**
     * 将空值转换为日志安全文本。
     *
     * @param value 原始值
     * @return 日志文本
     */
    private String display(Object value) {
        if (Objects.isNull(value)) {
            return "-";
        }
        if (value instanceof String && StringUtils.isEmpty((String) value)) {
            return "-";
        }
        return String.valueOf(value);
    }
}
