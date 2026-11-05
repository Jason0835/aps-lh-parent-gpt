package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;

import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;

/**
 * 原机回补单次资源复核索引，按运行侧、物理机台及模具缩小占用查询范围。
 * 仅在一个SKU的完整试算与提交中使用，资源变化后的下一组重新建立。
 *
 * @author Codex
 * @date 2026/10/29
 */
public final class StructureEndingResourceIndex {
    /**
     * 当前仍有效的结果身份，零量清理后的原对象不会误认为有量前缀。
     */
    private final Set<LhScheduleResult> live = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * 同运行侧的正式占用。
     */
    private final Map<String, List<LhScheduleResult>> runtime = new LinkedHashMap<>();
    /**
     * 整机模式下的物理占用。
     */
    private final Map<String, List<LhScheduleResult>> physical = new LinkedHashMap<>();
    /**
     * 每副实际模具的正式占用。
     */
    private final Map<String, List<LhScheduleResult>> moulds = new LinkedHashMap<>();
    /**
     * 该资源版本下的最早准备或生产时刻，按结果身份去重。
     */
    private final Map<LhScheduleResult, Date> starts = new IdentityHashMap<>();

    /**
     * 一次扫描正式结果和机台分配，冻结本组需要的资源索引及占用起点。
     *
     * @param context 当前实时结果、分配和正式班次
     */
    public StructureEndingResourceIndex(LhScheduleContext context) {
        live.addAll(context.getScheduleResultList());
        Set<LhScheduleResult> assignments = Collections.newSetFromMap(new IdentityHashMap<>());
        assignments.addAll(live);
        context.getMachineAssignmentMap().values().stream().filter(Objects::nonNull).forEach(assignments::addAll);
        for (LhScheduleResult result : assignments) {
            Date start = result.getMouldChangeStartTime();
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                if (quantity != null && quantity > 0) {
                    Date productionStart = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
                    if (productionStart == null) {
                        start = context.getScheduleWindowShifts().get(0).getShiftStartDateTime();
                        break;
                    }
                    if (start == null || productionStart.before(start)) {
                        start = productionStart;
                    }
                }
            }
            if (start == null) {
                continue;
            }
            starts.put(result, start);
            runtime.computeIfAbsent(result.getLhMachineCode(), ignored -> new ArrayList<>()).add(result);
            physical.computeIfAbsent(LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()),
                    ignored -> new ArrayList<>()).add(result);
            LhMouldCodeUtil.splitMouldCode(result.getMouldCode()).forEach(mould ->
                    moulds.computeIfAbsent(mould, ignored -> new ArrayList<>()).add(result));
        }
    }

    /**
     * 按身份查询原结果是否仍在正式列表中。
     *
     * @param result 原结果
     * @return 当前资源版本中仍存活时为真
     */
    public boolean isLive(LhScheduleResult result) {
        return live.contains(result);
    }

    /**
     * 查询与原机或原模具相关的最早其他占用，独立单控侧不误用整机排他语义。
     *
     * @param original 原机结果及真实模具
     * @param own 本次允许追加的原结果单位
     * @param whole 是否按物理整机约束
     * @return 最早其他占用；没有相关已提交动作时为空
     */
    public Date nextOccupation(LhScheduleResult original, List<LhScheduleResult> own, boolean whole) {
        Set<LhScheduleResult> related = Collections.newSetFromMap(new IdentityHashMap<>());
        String key = whole ? LhSingleControlMachineUtil.resolvePhysicalMachineCode(original.getLhMachineCode())
                : original.getLhMachineCode();
        related.addAll((whole ? physical : runtime).getOrDefault(key, Collections.emptyList()));
        LhMouldCodeUtil.splitMouldCode(original.getMouldCode()).forEach(mould ->
                related.addAll(moulds.getOrDefault(mould, Collections.emptyList())));
        own.forEach(related::remove);
        return related.stream().map(starts::get).min(Date::compareTo).orElse(null);
    }
}
