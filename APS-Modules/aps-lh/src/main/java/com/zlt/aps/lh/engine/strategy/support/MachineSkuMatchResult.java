package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 指定机台反向匹配 SKU 的只读结果。
 *
 * <p>本对象只承载硬匹配结论、单控物理机台声明范围和既有软指标快照，
 * 不分配模具、不登记机台、不修改 SKU 或排程上下文。</p>
 *
 * @author APS
 */
public final class MachineSkuMatchResult {

    /** 是否通过全部静态硬约束 */
    private final boolean matched;
    /** 交给正式新增主链的代表机台，正规单控整机固定为左侧 */
    private final MachineScheduleDTO machine;
    /** 当前待排 SKU，仅用于日志与编排映射 */
    private final SkuScheduleDTO sku;
    /** 单次提交需要声明的运行态机台编码，普通机台一个、单控整机两个 */
    private final List<String> declaredMachineCodes;
    /** 通用六层匹配结果 */
    private final MachineSkuMatchLevel matchLevel;
    /** 正向选机已使用的软指标快照 */
    private final MachinePriorityMetricSnapshot priorityMetricSnapshot;
    /** 未通过时的明确原因 */
    private final String failureReason;

    private MachineSkuMatchResult(boolean matched,
                                  MachineScheduleDTO machine,
                                  SkuScheduleDTO sku,
                                  List<String> declaredMachineCodes,
                                  MachineSkuMatchLevel matchLevel,
                                  MachinePriorityMetricSnapshot priorityMetricSnapshot,
                                  String failureReason) {
        this.matched = matched;
        this.machine = machine;
        this.sku = sku;
        this.declaredMachineCodes = declaredMachineCodes == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(declaredMachineCodes));
        this.matchLevel = matchLevel;
        this.priorityMetricSnapshot = priorityMetricSnapshot;
        this.failureReason = failureReason;
    }

    /**
     * 构造成功结果。
     *
     * @param machine 代表机台
     * @param sku SKU
     * @param declaredMachineCodes 声明机台编码
     * @param matchLevel 匹配层级
     * @param priorityMetricSnapshot 软指标快照
     * @return 成功结果
     */
    public static MachineSkuMatchResult matched(
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            List<String> declaredMachineCodes,
            MachineSkuMatchLevel matchLevel,
            MachinePriorityMetricSnapshot priorityMetricSnapshot) {
        return new MachineSkuMatchResult(true, machine, sku, declaredMachineCodes,
                matchLevel, priorityMetricSnapshot, null);
    }

    /**
     * 构造失败结果。
     *
     * @param machine 指定机台
     * @param sku SKU
     * @param failureReason 失败原因
     * @return 失败结果
     */
    public static MachineSkuMatchResult failed(
            MachineScheduleDTO machine,
            SkuScheduleDTO sku,
            String failureReason) {
        return new MachineSkuMatchResult(false, machine, sku,
                Collections.<String>emptyList(), null, null, failureReason);
    }

    public boolean isMatched() {
        return matched;
    }

    public MachineScheduleDTO getMachine() {
        return machine;
    }

    public SkuScheduleDTO getSku() {
        return sku;
    }

    public List<String> getDeclaredMachineCodes() {
        return declaredMachineCodes;
    }

    public MachineSkuMatchLevel getMatchLevel() {
        return matchLevel;
    }

    public MachinePriorityMetricSnapshot getPriorityMetricSnapshot() {
        return priorityMetricSnapshot;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
