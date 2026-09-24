package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import lombok.Getter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 单台物理机台的只读选择快照，结果引用由后续原流程提交。 */
@Getter
public final class MachineReductionDecision {
    /** 物理机台编码，L/R整机只计一个名额。 */
    private final String machineCode;
    /** 选择原因。 */
    private final ReductionReason reason;
    /** 历史命中证据或正常排序比较值。 */
    private final String reasonDetail;
    /** 本轮最终选择顺序，从1开始。 */
    private final Integer priority;
    /** 同SKU原始结果，正规单控包含L/R；选机时不修改。 */
    private final List<LhScheduleResult> results;

    /**
     * 构造选择快照。
     * @param machineCode 物理机台编码
     * @param reason 选择原因
     * @param reasonDetail 原因明细
     * @param priority 最终选择顺序
     * @param results 该机台原结果
     */
    public MachineReductionDecision(String machineCode, ReductionReason reason, String reasonDetail,
                                    Integer priority, List<LhScheduleResult> results) {
        this.machineCode = machineCode;
        this.reason = reason;
        this.reasonDetail = reasonDetail;
        this.priority = priority;
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
    }
}
