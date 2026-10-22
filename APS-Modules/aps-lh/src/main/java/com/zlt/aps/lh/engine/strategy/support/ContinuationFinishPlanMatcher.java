package com.zlt.aps.lh.engine.strategy.support;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 在均有可靠历史关系的机台之间匹配既定收尾方案，使交接时间贴近历史班次。
 * 无历史机台及固定角色保持原方案；只调整计算计划，实际后料资源由正式承接链隔离复核。
 *
 * @author Codex
 * @date 2026/10/13
 */
@Slf4j
public final class ContinuationFinishPlanMatcher {
    /**
     * 在历史机台内部按命中班次、距离、等待及对调数量择优，一次更新完整映射。
     *
     * @param context 已冻结历史资格与物理资源的上下文
     * @param sku 当前前物料及产品状态
     * @param profiles 与原计算计划对应的真实机台画像
     * @param shifts 完整窗口班次
     * @param stopHoldResults 不得调整的保机结果
     * @param calculated 已生成的合法逐机计划，方法只替换其中的计划列表
     */
    public void match(LhScheduleContext context, SkuScheduleDTO sku,
            List<ContinuationEndingMachineProfile> profiles, List<LhShiftConfigVO> shifts,
            List<LhScheduleResult> stopHoldResults, ContinuationFinishScheduleResult calculated) {
        int count = profiles.size();
        if (count < 2 || calculated.getRemainingQty() > 0) {
            return;
        }
        List<LhShiftConfigVO> targets = profiles.stream()
                .map(profile -> this.resolveHistoricalShift(context, sku, profile, shifts))
                .collect(Collectors.toList());
        if (targets.stream().filter(Objects::nonNull).count() < 2) {
            return;
        }
        List<ContinuationMachineFinishPlan> baseline = calculated.getMachinePlans();
        boolean[] fixed = new boolean[count];
        for (int position = 0; position < count; position++) {
            fixed[position] = Objects.isNull(targets.get(position))
                    || this.isFixed(context, profiles.get(position), baseline.get(position),
                            stopHoldResults, shifts.get(shifts.size() - 1).getShiftEndDateTime());
        }
        ContinuationMachineFinishPlan[][] candidates = new ContinuationMachineFinishPlan[count][count];
        long[][][] scores = new long[count][count][];
        List<String> rejected = new ArrayList<>();
        for (int machine = 0; machine < count; machine++) {
            for (int slot = 0; slot < count; slot++) {
                String rejection = machine == slot ? null : this.rejectCandidate(profiles.get(machine),
                        profiles.get(slot), baseline.get(machine), baseline.get(slot), fixed[machine] || fixed[slot]);
                if (Objects.nonNull(rejection)) {
                    if (Objects.nonNull(targets.get(machine))) {
                        rejected.add(this.code(profiles.get(machine)) + "<-" + this.code(profiles.get(slot))
                                + ":" + rejection);
                    }
                    continue;
                }
                ContinuationMachineFinishPlan candidate = machine == slot ? baseline.get(machine)
                        : this.transferPlan(profiles.get(machine), baseline.get(slot));
                Date changeover = profiles.get(machine).earliestChangeoverTime(candidate.getPlanQty());
                if (Objects.isNull(changeover)) {
                    rejected.add(this.code(profiles.get(machine)) + "<-" + this.code(profiles.get(slot))
                            + ":缺少可交替时间");
                    continue;
                }
                candidates[machine][slot] = candidate;
                scores[machine][slot] = this.score(targets.get(machine), changeover, candidate.getOfflineTime(),
                        machine == slot);
            }
        }
        int[] assignment = this.minimumAssignment(this.encodeCosts(scores));
        if (Objects.isNull(assignment)) {
            this.record(context, sku, "无完整合法映射，保留原计划", rejected.toString());
            return;
        }
        List<ContinuationMachineFinishPlan> selected = IntStream.range(0, count)
                .mapToObj(machine -> candidates[machine][assignment[machine]]).collect(Collectors.toList());
        String detail = IntStream.range(0, count).mapToObj(machine -> {
            LhShiftConfigVO target = targets.get(machine);
            Date previous = profiles.get(machine).earliestChangeoverTime(baseline.get(machine).getPlanQty());
            Date current = profiles.get(machine).earliestChangeoverTime(selected.get(machine).getPlanQty());
            return this.code(profiles.get(machine)) + "<-" + this.code(profiles.get(assignment[machine]))
                    + ",历史班次=" + (Objects.isNull(target) ? "无" : target.getShiftIndex())
                    + ",历史区间=" + (Objects.isNull(target) ? "无" : this.time(target.getShiftStartDateTime())
                    + "~" + this.time(target.getShiftEndDateTime()))
                    + ",原/新数量=" + baseline.get(machine).getPlanQty() + "/" + selected.get(machine).getPlanQty()
                    + ",原/新可交替=" + this.time(previous) + "/" + this.time(current)
                    + ",距离毫秒=" + scores[machine][assignment[machine]][1];
        }).collect(Collectors.joining(";"));
        // 原对象、来源SKU、模具及各类机台索引保持不变，统一提交入口按目标机台时间轴写回。
        calculated.setMachinePlans(selected);
        this.record(context, sku, "整组候选匹配完成，待承接资源复核", detail + ";排除=" + rejected);
    }

    /**
     * 以运行态机台和前物料解析唯一历史班次；历史产品状态属于后料，不用于前料状态比较。
     *
     * @param context 冻结历史关系
     * @param sku 前料需求
     * @param profile 物理机台及其左右侧
     * @param shifts 窗口班次
     * @return 各侧一致的历史班次，无可靠关系时为空
     */
    private LhShiftConfigVO resolveHistoricalShift(LhScheduleContext context, SkuScheduleDTO sku,
            ContinuationEndingMachineProfile profile, List<LhShiftConfigVO> shifts) {
        LhShiftConfigVO target = null;
        for (LhScheduleResult result : profile.getOriginals()) {
            SkuScheduleDTO source = context.getScheduleResultSourceSkuMap().get(result);
            LhMouldChangePlan history = context.getPreviousAlternateMachineOrder()
                    .uniquePlan(result.getLhMachineCode(), sku.getMaterialCode());
            if (Objects.isNull(history) || Objects.isNull(history.getPlanDate()) || Objects.isNull(source)
                    || !context.getMachineScheduleMap().containsKey(result.getLhMachineCode())
                    || !StringUtils.equals(result.getMaterialCode(), sku.getMaterialCode())
                    || !StringUtils.equals(result.getProductStatus(), sku.getProductStatus())
                    || !StringUtils.equals(source.getMaterialCode(), sku.getMaterialCode())
                    || !StringUtils.equals(source.getProductStatus(), sku.getProductStatus())) {
                return null;
            }
            LhShiftConfigVO resolved = shifts.stream().filter(shift ->
                    Objects.equals(LhScheduleTimeUtil.clearTime(shift.getWorkDate()),
                            LhScheduleTimeUtil.clearTime(history.getPlanDate()))
                            && StringUtils.equals(shift.getShiftType(), history.getClassIndex()))
                    .findFirst().orElse(null);
            if (Objects.isNull(resolved) || (Objects.nonNull(target) && target != resolved)) {
                return null;
            }
            target = resolved;
        }
        return target;
    }

    /**
     * 保留已确认的物理硬截止及指定释放角色，不将未完成占用或保机计划作为可交换槽位。
     *
     * @param context 当前上下文
     * @param profile 真实机台画像
     * @param plan 原计划
     * @param held 停产保机结果
     * @param windowEnd 窗口末端
     * @return 是否仅允许保留原计划
     */
    private boolean isFixed(LhScheduleContext context, ContinuationEndingMachineProfile profile,
            ContinuationMachineFinishPlan plan, List<LhScheduleResult> held, Date windowEnd) {
        return plan.getFinishState() != ContinuationMachineFinishPlan.FinishState.SURPLUS_COMPLETED
                || Objects.nonNull(plan.getHistoricalFinishPlanId()) || Objects.nonNull(plan.getTDayFinishConstraint())
                || Objects.nonNull(plan.getNightFinishConstraint())
                || StringUtils.isNotEmpty(plan.getFinishRoleMachineCode())
                || (Objects.nonNull(profile.getReleaseDeadline()) && profile.getReleaseDeadline().before(windowEnd))
                || profile.getOriginals().stream().anyMatch(result -> held.contains(result)
                || context.isContinuousStopHoldMachine(result.getLhMachineCode())
                || context.getOnlySandBlastContinuationReleaseWindowMap().containsKey(result.getLhMachineCode())
                || context.getContinuationTemporaryFaultTransferEventMap().containsKey(result.getLhMachineCode())
                || context.hasTimedMachineOffRequest(result)
                || context.getTimedMachineOffDecisionMap().containsKey(result));
    }

    /**
     * 核对目标机台承接后逐班量、侧别、归整单位及节点均保持合法，不以总量相等代替班次守恒。
     *
     * @param target 目标机台
     * @param source 槽位原机台
     * @param original 目标机台原节点
     * @param slot 待承接计划
     * @param fixed 是否涉及固定角色
     * @return 不可承接原因；合法时为空
     */
    private String rejectCandidate(ContinuationEndingMachineProfile target, ContinuationEndingMachineProfile source,
            ContinuationMachineFinishPlan original, ContinuationMachineFinishPlan slot, boolean fixed) {
        if (fixed) {
            return "固定释放或保机角色";
        }
        if (target.getMultiple() != source.getMultiple() || !this.sides(target).equals(this.sides(source))) {
            return "模数或左右侧不兼容";
        }
        int quantity = slot.getPlanQty();
        if (!target.isLegalQuantity(quantity) || !Arrays.equals(target.shiftQuantities(quantity),
                source.shiftQuantities(quantity))) {
            return "目标机真实容量不能保持逐班量";
        }
        Date finish = target.completionTime(quantity);
        Date release = target.switchReadyTime(quantity);
        if (Objects.isNull(release) || (quantity > 0 && Objects.isNull(finish))) {
            return "目标机真实时间不完整";
        }
        if ((Objects.nonNull(original.getFinishDeadline()) && release.after(original.getFinishDeadline()))
                || (Objects.nonNull(slot.getFinishDeadline()) && release.after(slot.getFinishDeadline()))) {
            return "越过既定收尾节点";
        }
        return null;
    }

    /**
     * 提取侧别及实际模数签名，单控L/R必须作为整台参与匹配。
     *
     * @param profile 物理机台画像
     * @return 与机台编码无关的有序侧别签名
     */
    private List<String> sides(ContinuationEndingMachineProfile profile) {
        return profile.getOriginals().stream().map(result ->
                StringUtils.defaultString(result.getLeftRightMould()) + ":" + result.getMouldQty())
                .sorted().collect(Collectors.toList());
    }

    /**
     * 保留槽位数量及约束，并以目标机真实节拍重算完成、释放和收尾班次。
     *
     * @param target 目标机台
     * @param slot 原槽位计划
     * @return 独立的目标机台计算计划
     */
    private ContinuationMachineFinishPlan transferPlan(ContinuationEndingMachineProfile target,
            ContinuationMachineFinishPlan slot) {
        ContinuationMachineFinishPlan transferred = BeanUtil.copyProperties(slot, ContinuationMachineFinishPlan.class);
        transferred.setFinishTime(target.completionTime(slot.getPlanQty()));
        transferred.setOfflineTime(target.switchReadyTime(slot.getPlanQty()));
        transferred.setFinishShiftIndex(slot.getPlanQty() > 0 ? target.productionEndingShift(slot.getPlanQty()) : null);
        return transferred;
    }

    /**
     * 计算字典序代价，不通过人为等待制造班次命中；班末是下一班的起点。
     *
     * @param target 历史交替目标班次
     * @param changeover 最早物理可交替时间
     * @param release 真实释放时间
     * @param unchanged 是否保持原绑定
     * @return 未命中数、区间距离、交接等待毫秒及对调数
     */
    private long[] score(LhShiftConfigVO target, Date changeover, Date release, boolean unchanged) {
        long distance = 0;
        long missed = 0;
        long wait = 0;
        if (Objects.nonNull(target)) {
            long start = target.getShiftStartDateTime().getTime();
            long end = target.getShiftEndDateTime().getTime();
            long actual = changeover.getTime();
            missed = actual >= start && actual < end ? 0 : 1;
            distance = actual < start ? start - actual : Math.max(0, actual - end);
            wait = Math.max(0, Math.max(start, actual) - release.getTime());
        }
        return new long[]{missed, distance, wait, unchanged ? 0 : 1};
    }

    /**
     * 用整组代价上界编码字典序，任意低优先级总和均不能覆盖高优先级的一次改善。
     *
     * @param scores 合法候选的四级非负代价，空值表示不允许匹配
     * @return 无溢出的标量代价矩阵
     */
    private BigInteger[][] encodeCosts(long[][][] scores) {
        BigInteger[] bases = new BigInteger[4];
        for (int level = 0; level < bases.length; level++) {
            long maximum = 0;
            for (long[][] row : scores) {
                for (long[] score : row) {
                    if (Objects.nonNull(score)) {
                        maximum = Math.max(maximum, score[level]);
                    }
                }
            }
            bases[level] = BigInteger.valueOf(maximum).multiply(BigInteger.valueOf(scores.length)).add(BigInteger.ONE);
        }
        BigInteger[][] costs = new BigInteger[scores.length][scores.length];
        for (int machine = 0; machine < scores.length; machine++) {
            for (int slot = 0; slot < scores.length; slot++) {
                if (Objects.nonNull(scores[machine][slot])) {
                    BigInteger cost = BigInteger.ZERO;
                    for (int level = 0; level < bases.length; level++) {
                        cost = cost.multiply(bases[level]).add(BigInteger.valueOf(scores[machine][slot][level]));
                    }
                    costs[machine][slot] = cost;
                }
            }
        }
        return costs;
    }

    /**
     * 使用最小代价完整二分匹配处理两机交换及多机循环，避免逐台贪心占用其他历史机台的唯一槽位。
     * 势能与增广路采用匈牙利算法；空边始终不可选，复杂度为机台数的三次方。
     *
     * @param costs 机台到收尾槽位的代价矩阵
     * @return 每台机对应的唯一槽位；不存在完整映射时为空
     */
    private int[] minimumAssignment(BigInteger[][] costs) {
        int count = costs.length;
        BigInteger[] machinePotential = new BigInteger[count + 1];
        BigInteger[] slotPotential = new BigInteger[count + 1];
        Arrays.fill(machinePotential, BigInteger.ZERO);
        Arrays.fill(slotPotential, BigInteger.ZERO);
        int[] owner = new int[count + 1];
        int[] predecessor = new int[count + 1];
        for (int machine = 1; machine <= count; machine++) {
            owner[0] = machine;
            int currentSlot = 0;
            BigInteger[] slack = new BigInteger[count + 1];
            boolean[] visited = new boolean[count + 1];
            do {
                visited[currentSlot] = true;
                int currentMachine = owner[currentSlot];
                BigInteger delta = null;
                int nextSlot = -1;
                for (int slot = 1; slot <= count; slot++) {
                    if (visited[slot]) {
                        continue;
                    }
                    BigInteger cost = costs[currentMachine - 1][slot - 1];
                    if (Objects.nonNull(cost)) {
                        BigInteger reduced = cost.subtract(machinePotential[currentMachine])
                                .subtract(slotPotential[slot]);
                        if (Objects.isNull(slack[slot]) || reduced.compareTo(slack[slot]) < 0) {
                            slack[slot] = reduced;
                            predecessor[slot] = currentSlot;
                        }
                    }
                    if (Objects.nonNull(slack[slot]) && (Objects.isNull(delta) || slack[slot].compareTo(delta) < 0)) {
                        delta = slack[slot];
                        nextSlot = slot;
                    }
                }
                if (nextSlot < 0) {
                    return null;
                }
                for (int slot = 0; slot <= count; slot++) {
                    if (visited[slot]) {
                        machinePotential[owner[slot]] = machinePotential[owner[slot]].add(delta);
                        slotPotential[slot] = slotPotential[slot].subtract(delta);
                    } else if (Objects.nonNull(slack[slot])) {
                        slack[slot] = slack[slot].subtract(delta);
                    }
                }
                currentSlot = nextSlot;
            } while (owner[currentSlot] != 0);
            do {
                int previousSlot = predecessor[currentSlot];
                owner[currentSlot] = owner[previousSlot];
                currentSlot = previousSlot;
            } while (currentSlot != 0);
        }
        int[] assignment = new int[count];
        for (int slot = 1; slot <= count; slot++) {
            assignment[owner[slot] - 1] = slot - 1;
        }
        return assignment;
    }

    /**
     * 读取物理画像的首个运行态机台编码，仅用于日志。
     *
     * @param profile 物理机台画像
     * @return 首侧编码
     */
    private String code(ContinuationEndingMachineProfile profile) {
        return profile.getOriginals().get(0).getLhMachineCode();
    }

    /**
     * 格式化业务时间，不输出依赖环境语言的日期文本。
     *
     * @param value 可为空的时间
     * @return 可对账时间文本
     */
    private String time(Date value) {
        return LhScheduleTimeUtil.formatDateTime(value);
    }

    /**
     * 同时记录应用日志和过程日志，保留整组匹配及具体硬边界原因。
     *
     * @param context 本轮批次
     * @param sku 前料需求
     * @param decision 决策结论
     * @param detail 机台映射、目标班次与时间距离
     */
    private void record(LhScheduleContext context, SkuScheduleDTO sku, String decision, String detail) {
        String message = String.format("工厂=%s, 批次=%s, 物料=%s, 状态=%s, 决定=%s, %s",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(),
                sku.getProductStatus(), decision, detail);
        PriorityTraceLogHelper.appendProcessLog(context, "余量收尾历史班次匹配", message);
        log.info("余量收尾历史班次匹配, {}", message);
    }
}
