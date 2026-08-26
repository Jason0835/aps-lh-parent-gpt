package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.enums.NewSpecFailReasonEnum;
import com.zlt.aps.lh.component.StructureEarlyProductionAdmission;
import org.apache.commons.lang3.StringUtils;

/**
 * 提前生产判断日志原因编码。
 *
 * <p>该枚举只负责过程日志分类，不参与提前生产准入、候选排序、选机或排产结果计算。
 * 既有业务原因仍保留在日志 {@code detail} 字段中，避免分类转换丢失原始上下文。</p>
 *
 * @author APS
 */
public enum EarlyProductionLogReason {

    /** 已形成有效提前生产结果。 */
    SUCCESS("SUCCESS"),
    /** 已通过提前生产准入，正在继续执行候选机台和资源校验。 */
    PENDING("PENDING"),
    /** 结构班次已排机台数达到或超过计划机台数。 */
    STRUCTURE_MACHINE_LIMIT("STRUCTURE_MACHINE_LIMIT"),
    /** 可用模具不足或无可用模具。 */
    MOLD_INSUFFICIENT("MOLD_INSUFFICIENT"),
    /** 没有满足当前阶段要求的候选机台。 */
    NO_AVAILABLE_MACHINE("NO_AVAILABLE_MACHINE"),
    /** 当前日期或窗口没有剩余产能。 */
    NO_REMAINING_CAPACITY("NO_REMAINING_CAPACITY"),
    /** 受最早胎胚可供硫化时间限制。 */
    EARLIEST_EMBRYO_TIME_LIMIT("EARLIEST_EMBRYO_TIME_LIMIT"),
    /** 不满足 SKU 或结构提前生产条件。 */
    NOT_MEET_ADVANCE_CONDITION("NOT_MEET_ADVANCE_CONDITION"),
    /** 机台硬性约束过滤后没有可用机台。 */
    MACHINE_CONSTRAINT("MACHINE_CONSTRAINT"),
    /** 换模、首检或其他班次窗口约束导致无法排产。 */
    SHIFT_LIMIT("SHIFT_LIMIT"),
    /** 未匹配到已定义分类的其他原因。 */
    OTHER("OTHER");

    /** 日志输出编码。 */
    private final String code;

    EarlyProductionLogReason(String code) {
        this.code = code;
    }

    /**
     * 获取日志原因编码。
     *
     * @return 原因编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 按既有提前生产准入结果解析日志原因。
     *
     * @param decision 既有准入判断结果
     * @param admission 结构当天资格快照
     * @return 日志原因编码
     */
    public static EarlyProductionLogReason fromDecision(
            EarlyProductionDecision decision,
            StructureEarlyProductionAdmission admission) {
        if (java.util.Objects.isNull(decision)) {
            return OTHER;
        }
        if (!decision.isEarlyProduction()) {
            return NOT_MEET_ADVANCE_CONDITION;
        }
        if (admission != null && !admission.isAllowed()
                && admission.getCurrentPlanMachineCount() > 0
                && admission.getScheduledStructureCount()
                >= admission.getCurrentPlanMachineCount()) {
            return STRUCTURE_MACHINE_LIMIT;
        }
        if (!decision.isAllowed()) {
            String reason = decision.getReason();
            if (containsEmbryoTimeReason(reason)) {
                return EARLIEST_EMBRYO_TIME_LIMIT;
            }
            return NOT_MEET_ADVANCE_CONDITION;
        }
        return PENDING;
    }

    /**
     * 按既有候选失败原因解析日志原因。
     *
     * @param detail 既有失败原因明细
     * @param failReason 既有新增排产失败原因
     * @return 日志原因编码
     */
    public static EarlyProductionLogReason fromFailure(
            String detail,
            NewSpecFailReasonEnum failReason) {
        if (containsStructureLimitReason(detail)) {
            return STRUCTURE_MACHINE_LIMIT;
        }
        if (containsMouldReason(detail)) {
            return MOLD_INSUFFICIENT;
        }
        if (containsEmbryoTimeReason(detail)) {
            return EARLIEST_EMBRYO_TIME_LIMIT;
        }
        if (containsShiftReason(detail)
                || failReason == NewSpecFailReasonEnum.MOULD_CHANGE_SHIFT_ALLOCATE_FAILED
                || failReason == NewSpecFailReasonEnum.FIRST_INSPECTION_SHIFT_ALLOCATE_FAILED) {
            return SHIFT_LIMIT;
        }
        if (failReason == NewSpecFailReasonEnum.NO_CAPACITY_IN_SCHEDULE_WINDOW
                || containsCapacityReason(detail)) {
            return NO_REMAINING_CAPACITY;
        }
        if (containsMachineConstraintReason(detail)) {
            return MACHINE_CONSTRAINT;
        }
        if (failReason == NewSpecFailReasonEnum.MACHINE_SELECTION_FAILED) {
            return containsNoMachineReason(detail)
                    ? NO_AVAILABLE_MACHINE : MACHINE_CONSTRAINT;
        }
        return OTHER;
    }

    /** 判断是否为结构机台数限制原因。 */
    private static boolean containsStructureLimitReason(String detail) {
        return StringUtils.contains(detail, "结构")
                && (StringUtils.contains(detail, "计划机台数")
                || StringUtils.contains(detail, "达到")
                || StringUtils.contains(detail, "超过"));
    }

    /** 判断是否为模具资源不足原因。 */
    private static boolean containsMouldReason(String detail) {
        return StringUtils.contains(detail, "模具")
                && !StringUtils.contains(detail, "模具台账");
    }

    /** 判断是否为最早胎胚时间原因。 */
    private static boolean containsEmbryoTimeReason(String detail) {
        return StringUtils.contains(detail, "胎胚")
                && (StringUtils.contains(detail, "可供")
                || StringUtils.contains(detail, "最早")
                || StringUtils.contains(detail, "时间下限"));
    }

    /** 判断是否为班次约束原因。 */
    private static boolean containsShiftReason(String detail) {
        return StringUtils.contains(detail, "班次")
                || StringUtils.contains(detail, "换模窗口")
                || StringUtils.contains(detail, "首检");
    }

    /** 判断是否为产能不足原因。 */
    private static boolean containsCapacityReason(String detail) {
        return StringUtils.contains(detail, "产能")
                || StringUtils.contains(detail, "可开产")
                || StringUtils.contains(detail, "有效班次计划量")
                || StringUtils.contains(detail, "排程窗口");
    }

    /** 判断是否为机台硬约束原因。 */
    private static boolean containsMachineConstraintReason(String detail) {
        return StringUtils.contains(detail, "硬约束")
                || StringUtils.contains(detail, "单控")
                || StringUtils.contains(detail, "特殊材料")
                || StringUtils.contains(detail, "结构收尾对齐")
                || StringUtils.contains(detail, "指定机台");
    }

    /** 判断是否为没有候选机台原因。 */
    private static boolean containsNoMachineReason(String detail) {
        return StringUtils.contains(detail, "无可用硫化机台")
                || StringUtils.contains(detail, "无候选机台")
                || StringUtils.contains(detail, "候选机台数为0")
                || StringUtils.contains(detail, "候选机台");
    }
}
