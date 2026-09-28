package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import org.apache.commons.lang3.StringUtils;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingMachineProfile;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingQuantityReachability;
import com.zlt.aps.lh.engine.strategy.support.ContinuationFinishScheduleResult;
import com.zlt.aps.lh.engine.strategy.support.ContinuationMachineFinishPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;
import javax.annotation.Resource;

/**
 * 续作余量收尾的唯一分配器。输入顺序由调用方按场景角色确定，本服务不选机、不扣账、不预占后料。
 * 数量及时间均复用机台真实容量画像，06:00和19:00只限定数量上限，不用于虚增计划。
 */
@Slf4j
@Service
public class ContinuationRemainderFinishScheduleService {
    /** 复用前日最新有效批次来源，只为本次收尾纠偏读取唯一关系。 */
    @Resource
    private PreviousAlternatePlanEligibilityService previousAlternatePlanEligibilityService;
    /** 早班资源交接节点。 */
    private static final int MORNING_RELEASE_HOUR = 6;
    /** 中班生产硬截止，允许恰好19:00结束。 */
    private static final int AFTERNOON_RELEASE_HOUR = 19;
    /** 两机阈值采用整数交叉比较，避免浮点误差。 */
    private static final int NIGHT_THRESHOLD_NUMERATOR = 3;
    /** 两机阈值分母。 */
    private static final int NIGHT_THRESHOLD_DENOMINATOR = 2;

    /**
     * 计算完成后的最后一步：生成槽位到机台的完整置换，不修改数量、时间、历史事件或真实结果。
     * @param context 排程上下文
     * @param sku 当前续作余量收尾SKU
     * @param profiles 已计算的固定物理机台画像
     * @param calculated 已确定的收尾槽位
     * @param proposed 已完成计算及校验的结果副本
     * @param stopHoldResults 不能交换的停产保机结果
     * @return 原槽位对象到目标机台原对象的映射；空映射表示保留当前绑定
     */
    public Map<LhScheduleResult, LhScheduleResult> adjustContinuationRemainingFinishMachineOrder(
            LhScheduleContext context, SkuScheduleDTO sku, List<ContinuationEndingMachineProfile> profiles,
            ContinuationFinishScheduleResult calculated, Map<LhScheduleResult, LhScheduleResult> proposed,
            List<LhScheduleResult> stopHoldResults) {
        if (profiles.size() < 2 || CollectionUtils.isEmpty(context.getHistoricalReverseMouldChangePlanList())) {
            return Collections.emptyMap();
        }
        List<String> codes = profiles.stream().flatMap(profile -> profile.getOriginals().stream())
                .map(LhScheduleResult::getLhMachineCode).collect(Collectors.toList());
        Map<String, LhMouldChangePlan> plans = previousAlternatePlanEligibilityService
                .resolveRemainingFinishPlans(context, sku.getMaterialCode(), codes);
        Map<Integer, LhMouldChangePlan> eligible = new LinkedHashMap<>(profiles.size());
        for (int position = 0; position < profiles.size(); position++) {
            LhMouldChangePlan plan = this.resolveFinishProfilePlan(context, sku, profiles.get(position), plans, stopHoldResults);
            ContinuationMachineFinishPlan finish = calculated.getMachinePlans().get(position);
            if (Objects.nonNull(plan) && Objects.nonNull(finish.getOfflineTime())) {
                eligible.put(position, plan);
            }
        }
        if (eligible.size() < 2) {
            this.logFinishMachineOrder(context, sku, "保持原结果：有效且唯一的历史机台不足两台", codes.toString());
            return Collections.emptyMap();
        }
        // 槽位按真实完成时刻排序；零量仅使用既有释放事实，不伪造生产班次。
        List<Integer> slots = new ArrayList<>(eligible.keySet());
        slots.sort(Comparator.comparing(position -> this.finishSlotTime(calculated.getMachinePlans().get(position))));
        if (slots.stream().map(position -> this.finishSlotTime(calculated.getMachinePlans().get(position)))
                .distinct().count() < 2) {
            return Collections.emptyMap();
        }
        List<Integer> machines = new ArrayList<>(slots);
        // 从槽位顺序做稳定排序，同历史日期同班次时不制造ID优先级。
        machines.sort(Comparator.comparing((Integer position) -> this.planDay(eligible.get(position)))
                .thenComparing(position -> eligible.get(position).getClassIndex()));
        Map<LhScheduleResult, LhScheduleResult> bindings = new IdentityHashMap<>(codes.size());
        for (int position = 0; position < slots.size(); position++) {
            int slotIndex = slots.get(position);
            int machineIndex = machines.get(position);
            if (slotIndex == machineIndex) {
                continue;
            }
            ContinuationEndingMachineProfile source = profiles.get(slotIndex);
            ContinuationEndingMachineProfile target = profiles.get(machineIndex);
            if (!target.canAcceptFinishSlot(source, proposed, calculated.getMachinePlans().get(slotIndex))) {
                this.logFinishMachineOrder(context, sku, "保持原结果：目标机台产能、侧别或占用窗口不兼容", codes.toString());
                return Collections.emptyMap();
            }
            for (LhScheduleResult original : source.getOriginals()) {
                LhScheduleResult targetOriginal = target.getOriginals().stream()
                        .filter(result -> Objects.equals(LhSingleControlMachineUtil.resolveSplitSide(result.getLhMachineCode()),
                                LhSingleControlMachineUtil.resolveSplitSide(original.getLhMachineCode())))
                        .findFirst().orElseThrow(() -> new IllegalStateException("已校验的收尾机台侧别映射丢失"));
                bindings.put(original, targetOriginal);
            }
        }
        if (!CollectionUtils.isEmpty(bindings)) {
            String detail = slots.stream().map(position -> {
                LhMouldChangePlan plan = eligible.get(position);
                return plan.getLhMachineCode() + ":" + this.planDay(plan) + "/" + plan.getClassIndex();
            }).collect(Collectors.joining(","));
            this.logFinishMachineOrder(context, sku, "历史顺序与槽位映射校验通过，等待整组提交", detail);
        }
        return bindings;
    }

    /**
     * 校验同SKU物理机台及L/R计划能否可靠对应；设备专属强制处置不能随槽位移动。
     * @param context 上下文 @param sku 来源SKU @param profile 物理机台画像
     * @param plans 有效唯一历史计划 @param stopHoldResults 前置保机结果
     * @return 物理机台统一的历史顺序计划；无法对应时返回null
     */
    private LhMouldChangePlan resolveFinishProfilePlan(LhScheduleContext context, SkuScheduleDTO sku,
            ContinuationEndingMachineProfile profile, Map<String, LhMouldChangePlan> plans,
            List<LhScheduleResult> stopHoldResults) {
        LhMouldChangePlan first = null;
        for (LhScheduleResult result : profile.getOriginals()) {
            String code = result.getLhMachineCode();
            LhMouldChangePlan plan = plans.get(code);
            SkuScheduleDTO source = context.getScheduleResultSourceSkuMap().get(result);
            if (Objects.isNull(plan) || Objects.isNull(source) || !context.getMachineScheduleMap().containsKey(code)
                    || !StringUtils.equals(ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType())
                    || !StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                    || !StringUtils.equals(sku.getProductStatus(), result.getProductStatus())
                    || !StringUtils.equals(sku.getMaterialCode(), source.getMaterialCode())
                    || !StringUtils.equals(sku.getProductStatus(), source.getProductStatus())
                    || stopHoldResults.contains(result) || context.isContinuousStopHoldMachine(code)
                    || context.getOnlySandBlastContinuationReleaseWindowMap().containsKey(code)
                    || context.getContinuationTemporaryFaultTransferEventMap().containsKey(code)
                    || context.getTimedMachineOffDecisionMap().containsKey(result)) {
                return null;
            }
            PreviousAlternatePlanReleaseEvent event = context.getPreviousAlternateReleaseEventMap().get(code);
            if (Objects.nonNull(event) && (event.getPlan() != plan
                    || !StringUtils.equals(event.getMaterialCode(), sku.getMaterialCode())
                    || !StringUtils.equals(event.getProductStatus(), sku.getProductStatus()))) {
                return null;
            }
            if (Objects.nonNull(first) && (!Objects.equals(this.planDay(first), this.planDay(plan))
                    || !StringUtils.equals(first.getClassIndex(), plan.getClassIndex()))) {
                return null;
            }
            first = plan;
        }
        return first;
    }

    /** @param plan 前日交替计划 @return 业务日期；同日时分秒不得覆盖早中班顺序 */
    private LocalDate planDay(LhMouldChangePlan plan) {
        return plan.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** @param finish 已计算槽位 @return 有量取生产结束，无量取既有物理释放时间 */
    private Date finishSlotTime(ContinuationMachineFinishPlan finish) {
        return Objects.nonNull(finish.getFinishTime()) ? finish.getFinishTime() : finish.getOfflineTime();
    }

    /** @param context 上下文 @param sku 收尾SKU @param reason 纠偏决定 @param detail 机台对应详情 */
    private void logFinishMachineOrder(LhScheduleContext context, SkuScheduleDTO sku, String reason, String detail) {
        String message = String.format("工厂=%s, 批次=%s, 目标日=%s, 物料=%s, 状态=%s, 决定=%s, 机台=%s",
                context.getFactoryCode(), context.getBatchNo(), context.getScheduleTargetDate(),
                sku.getMaterialCode(), sku.getProductStatus(), reason, detail);
        PriorityTraceLogHelper.appendProcessLog(context, "续作余量收尾机台顺序纠正", message);
        log.info("续作余量收尾机台顺序纠正, {}", message);
    }

    /**
     * 计算整窗实际排量，先保留目标日前的连续生产前缀，再对目标日剩余量执行节点规则。
     * 前日19:00释放会撤回该台前缀的尾量，并归还本次统一余量，不能重复计量。
     * @param machines 已按场景角色排序的物理机台画像：T日大余量及多机阶段式收尾按保留顺序
     * @param shifts 真实排程班次
     * @param remainQty 本组整窗尚可消费的余量，不含其他固定结果
     * @param shiftQty SKU标准单台班产，用于业务阈值；实际分配采用逐机真实容量
     * @param dateOffset 前置容量判定的T/T+1/T+2日期偏移
     * @return 数量、收尾和下机时间，以及真实未消化余量
     */
    public ContinuationFinishScheduleResult calculate(List<ContinuationEndingMachineProfile> machines,
            List<LhShiftConfigVO> shifts, int remainQty, int shiftQty, int dateOffset) {
        if (CollectionUtils.isEmpty(machines) || CollectionUtils.isEmpty(shifts) || remainQty < 0 || shiftQty <= 0 || dateOffset < 0) {
            throw new IllegalArgumentException("续作余量收尾计算输入不完整");
        }
        Date windowEnd = shifts.get(shifts.size() - 1).getShiftEndDateTime();
        LhShiftConfigVO first = shifts.stream().filter(shift -> Objects.equals(shift.getDateOffset(), dateOffset))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("续作收尾目标日不在排程窗口内"));
        LocalDate day = first.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int[] quantities = new int[machines.size()];
        int[] minimumQuantities = new int[machines.size()];
        int[] limits = machines.stream().mapToInt(machine -> machine.quantityUntil(windowEnd)).toArray();
        Date[] deadlines = new Date[machines.size()];
        Arrays.fill(deadlines, windowEnd);
        // 分配状态只由实际累计量计算，不能同时维护一份可漂移的局部余量。
        if (dateOffset > 0) {
            this.allocateStage(machines, quantities, remainQty, 0, machines.size(), first.getShiftStartDateTime());
        }
        int dayRemaining = remainQty - Arrays.stream(quantities).sum();
        if (machines.size() == 1) {
            this.allocate(machines, quantities, remainQty, 0, limits[0]);
        } else if (dateOffset == 0) {
            this.scheduleOnT(machines, quantities, limits, deadlines, remainQty, shiftQty, day);
        } else if (machines.size() == 2) {
            this.scheduleTwoMachinesWithNightShift(machines, quantities, limits, deadlines,
                    remainQty, shiftQty, day, 0);
        } else if ((long) dayRemaining < (long) machines.size() * shiftQty) {
            this.scheduleSmallNightGroup(machines, quantities, limits, deadlines, remainQty, shiftQty, day);
        } else {
            // 先完成全部机台的当前夜班，再分配中班节点和后续夜班，后置归整不能回撤已分配阶段。
            this.scheduleNightStages(machines, shifts, quantities, limits, deadlines, remainQty, day, dateOffset);
            minimumQuantities = quantities.clone();
        }
        return this.buildResult(machines, shifts, quantities, minimumQuantities, limits, deadlines, remainQty);
    }

    /**
     * 多机大余量按06:00、19:00、次日06:00顺序消化；中班截止之后仅使用后续夜班。
     * @param machines 按保留优先级排序的机台画像
     * @param shifts 真实排程班次
     * @param quantities 当前累计量，包含目标日前缀
     * @param limits 各台最终合法上限
     * @param deadlines 各台最终截止
     * @param total 本组真实余量预算
     * @param day 收尾目标业务日
     * @param dateOffset 收尾目标日期偏移
     */
    private void scheduleNightStages(List<ContinuationEndingMachineProfile> machines, List<LhShiftConfigVO> shifts,
            int[] quantities, int[] limits, Date[] deadlines, int total, LocalDate day, int dateOffset) {
        Date morning = this.node(day, MORNING_RELEASE_HOUR);
        Date afternoon = this.node(day, AFTERNOON_RELEASE_HOUR);
        Date nextMorning = this.node(day.plusDays(1), MORNING_RELEASE_HOUR);
        // 只截短本日中班，后续班次仍保留已校验的真实容量，不改变原始产量与时间比例。
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.equals(shift.getDateOffset(), dateOffset) && shift.isAfternoonShift()) {
                for (ContinuationEndingMachineProfile profile : machines) {
                    profile.restrictShiftToDeadline(shift.getShiftIndex(), afternoon);
                }
            }
        }
        Date finalDeadline = nextMorning;
        for (Date node : Arrays.asList(morning, afternoon, nextMorning)) {
            this.allocateStage(machines, quantities, total, 0, machines.size(), node);
            log.info("续作余量分阶段分配, 批次={}, 物料={}, 状态={}, 节点={}, 总预算={}, 累计分配={}, 剩余={}",
                    machines.get(0).getOriginals().get(0).getBatchNo(),
                    machines.get(0).getOriginals().get(0).getMaterialCode(),
                    machines.get(0).getOriginals().get(0).getProductStatus(), node, total,
                    Arrays.toString(quantities), total - Arrays.stream(quantities).sum());
            if (Arrays.stream(quantities).sum() == total) {
                // 当前阶段已耗尽，后置合法量补齐也不得再进入下一阶段。
                finalDeadline = node;
                break;
            }
        }
        for (int machine = 0; machine < machines.size(); machine++) {
            this.restrict(machines, quantities, limits, deadlines, machine, finalDeadline);
        }
    }

    /**
     * T日按已确定顺序释放前组，后组顺排实际余量。
     * @param machines 机台画像
     * @param quantities 当前累计量
     * @param limits 各台允许的总量上限
     * @param deadlines 各台收尾截止
     * @param total 原始预算
     * @param shiftQty 标准班产
     * @param day T日
     */
    private void scheduleOnT(List<ContinuationEndingMachineProfile> machines, int[] quantities,
            int[] limits, Date[] deadlines, int total, int shiftQty, LocalDate day) {
        int earlyCount = total <= shiftQty ? machines.size() - 1 : machines.size() / 2;
        Date cutoff = this.node(day, total <= shiftQty ? MORNING_RELEASE_HOUR : AFTERNOON_RELEASE_HOUR);
        for (int machine = 0; machine < earlyCount; machine++) {
            this.restrict(machines, quantities, limits, deadlines, machine, cutoff);
            this.allocate(machines, quantities, total, machine, limits[machine]);
        }
        this.allocateRemaining(machines, quantities, limits, total, earlyCount);
    }

    /**
     * 含夜班的小余量组先释放前半组；剩余两台必须复用统一两机规则。
     * @param machines 机台画像
     * @param quantities 累计量
     * @param limits 总量上限
     * @param deadlines 收尾截止
     * @param total 原始预算
     * @param shiftQty 标准班产
     * @param day 目标业务日
     */
    private void scheduleSmallNightGroup(List<ContinuationEndingMachineProfile> machines, int[] quantities,
            int[] limits, Date[] deadlines, int total, int shiftQty, LocalDate day) {
        int half = machines.size() / 2;
        Date previousAfternoon = this.node(day.minusDays(1), AFTERNOON_RELEASE_HOUR);
        for (int machine = 0; machine < half; machine++) {
            this.restrict(machines, quantities, limits, deadlines, machine, previousAfternoon);
            this.allocate(machines, quantities, total, machine, limits[machine]);
        }
        int remainingMachines = machines.size() - half;
        if (remainingMachines == 2) {
            this.scheduleTwoMachinesWithNightShift(machines, quantities, limits, deadlines, total, shiftQty, day, half);
        } else if (remainingMachines == 1) {
            this.allocateRemaining(machines, quantities, limits, total, half);
        } else {
            for (int machine = half; machine < machines.size() - 1; machine++) {
                this.restrict(machines, quantities, limits, deadlines, machine, this.node(day, MORNING_RELEASE_HOUR));
                this.allocate(machines, quantities, total, machine, limits[machine]);
            }
            // 最后一台承接实际尾量；06:00为优先节点，不能因此丢弃有真实容量可消化的余量。
            this.allocate(machines, quantities, total, machines.size() - 1, limits[machines.size() - 1]);
        }
    }

    /**
     * 两机公共规则：第一台优先承接夜班及后续产能，第二台本轮最多一个班产。
     * @param machines 机台画像
     * @param quantities 累计量
     * @param limits 总量上限
     * @param deadlines 收尾截止
     * @param total 原始预算
     * @param shiftQty 标准班产
     * @param day 目标日
     * @param first 两机子组的起始位置
     */
    private void scheduleTwoMachinesWithNightShift(List<ContinuationEndingMachineProfile> machines,
            int[] quantities, int[] limits, Date[] deadlines, int total, int shiftQty, LocalDate day, int first) {
        int remaining = total - Arrays.stream(quantities).sum();
        int second = first + 1;
        if ((long) remaining * NIGHT_THRESHOLD_DENOMINATOR < (long) shiftQty * NIGHT_THRESHOLD_NUMERATOR) {
            this.restrict(machines, quantities, limits, deadlines, first,
                    this.node(day.minusDays(1), AFTERNOON_RELEASE_HOUR));
            this.allocate(machines, quantities, total, first, limits[first]);
            this.allocate(machines, quantities, total, second, limits[second]);
            return;
        }
        Date afternoon = this.node(day, AFTERNOON_RELEASE_HOUR);
        int firstRound = Math.min(machines.get(first).quantityUntil(afternoon),
                quantities[first] + remaining - shiftQty);
        this.allocate(machines, quantities, total, first, firstRound);
        // 截止限制和一个班产限制同时生效，不能把越界部分丢弃。
        this.restrict(machines, quantities, limits, deadlines, second, afternoon);
        limits[second] = Math.min(limits[second], quantities[second] + shiftQty);
        this.allocate(machines, quantities, total, second, limits[second]);
        this.allocate(machines, quantities, total, first, limits[first]);
    }

    /** @param machines 画像 @param quantities 累计量 @param total 预算 @param from 起始机台 @param to 结束位置 @param deadline 阶段节点 */
    private void allocateStage(List<ContinuationEndingMachineProfile> machines, int[] quantities,
            int total, int from, int to, Date deadline) {
        for (int machine = from; machine < to && Arrays.stream(quantities).sum() < total; machine++) {
            this.allocate(machines, quantities, total, machine, machines.get(machine).quantityUntil(deadline));
        }
    }

    /** @param machines 画像 @param quantities 累计量 @param limits 上限 @param total 预算 @param from 起始机台 */
    private void allocateRemaining(List<ContinuationEndingMachineProfile> machines, int[] quantities,
            int[] limits, int total, int from) {
        for (int machine = from; machine < machines.size() && Arrays.stream(quantities).sum() < total; machine++) {
            this.allocate(machines, quantities, total, machine, limits[machine]);
        }
    }

    /** @param machines 画像 @param quantities 累计量 @param total 预算 @param machine 机台位置 @param maximum 该阶段累计上限 */
    private void allocate(List<ContinuationEndingMachineProfile> machines, int[] quantities,
            int total, int machine, int maximum) {
        int remainingQty = total - Arrays.stream(quantities).sum();
        if (remainingQty <= 0 || maximum <= quantities[machine]) {
            return;
        }
        int upper = quantities[machine] + Math.min(remainingQty, maximum - quantities[machine]);
        quantities[machine] = machines.get(machine).floorQuantity(upper);
    }

    /** @param machines 画像 @param quantities 累计量 @param limits 上限 @param deadlines 截止 @param machine 机台位置 @param deadline 目标节点 */
    private void restrict(List<ContinuationEndingMachineProfile> machines, int[] quantities,
            int[] limits, Date[] deadlines, int machine, Date deadline) {
        deadlines[machine] = deadline;
        limits[machine] = Math.min(limits[machine], machines.get(machine).quantityUntil(deadline));
        quantities[machine] = Math.min(quantities[machine], limits[machine]);
    }

    /** @param day 生产日期 @param hour 固定交接小时 @return 带真实日期的节点 */
    private Date node(LocalDate day, int hour) {
        return Date.from(day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant());
    }

    /** @param machines 画像 @param shifts 班次 @param quantities 计划量 @param minimumQuantities 已冻结阶段下限 @param limits 上限 @param deadlines 截止 @param total 预算 @return 完整计算输出 */
    private ContinuationFinishScheduleResult buildResult(List<ContinuationEndingMachineProfile> machines,
            List<LhShiftConfigVO> shifts, int[] quantities, int[] minimumQuantities, int[] limits, Date[] deadlines, int total) {
        ContinuationFinishScheduleResult result = new ContinuationFinishScheduleResult();
        if (Arrays.stream(quantities).sum() < total) {
            // 只补合法归整组合，不复用旧错峰评分；各场景截止和两机班产上限始终保留。
            ContinuationEndingQuantityReachability reachability = new ContinuationEndingQuantityReachability(
                    machines.stream().map(ContinuationEndingMachineProfile::getCapacities).toArray(int[][]::new),
                    machines.stream().mapToInt(ContinuationEndingMachineProfile::getMultiple).toArray());
            int[] exact = reachability.allocate(total, minimumQuantities, limits, quantities);
            if (Objects.nonNull(exact)) {
                quantities = exact;
            }
            result.setDiagnostic(reachability.getDiagnostic());
        }
        List<ContinuationMachineFinishPlan> plans = new ArrayList<>(machines.size());
        for (int machine = 0; machine < machines.size(); machine++) {
            ContinuationEndingMachineProfile profile = machines.get(machine);
            ContinuationMachineFinishPlan plan = new ContinuationMachineFinishPlan();
            plan.setPlanQty(quantities[machine]);
            plan.setFinishDeadline(deadlines[machine]);
            if (quantities[machine] > 0) {
                plan.setFinishTime(profile.completionTime(quantities[machine]));
                plan.setFinishShiftIndex(profile.productionEndingShift(quantities[machine]));
                plan.setOfflineTime(profile.switchReadyTime(quantities[machine]));
                if (Objects.isNull(plan.getFinishTime()) || plan.getFinishTime().after(deadlines[machine])) {
                    throw new IllegalStateException("续作余量收尾计算越过指定节点");
                }
            } else {
                plan.setOfflineTime(profile.switchReadyTime(0));
            }
            plans.add(plan);
        }
        result.setMachinePlans(plans);
        result.setRemainingQty(total - Arrays.stream(quantities).sum());
        return result;
    }
}
