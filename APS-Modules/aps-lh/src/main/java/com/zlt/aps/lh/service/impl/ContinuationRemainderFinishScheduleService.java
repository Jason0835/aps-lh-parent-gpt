package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternateMachineOrder;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingReleaseOrderAllocator;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import org.apache.commons.lang3.StringUtils;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingMachineProfile;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingQuantityReachability;
import com.zlt.aps.lh.engine.strategy.support.ContinuationFinishScheduleResult;
import com.zlt.aps.lh.engine.strategy.support.ContinuationMachineFinishPlan;
import com.zlt.aps.lh.engine.strategy.support.ContinuationTDayFinishConstraint;
import com.zlt.aps.lh.engine.strategy.support.ContinuationNightFinishConstraint;
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
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 续作余量收尾的唯一分配器，在固定参与机台内按前置历史顺序分配释放角色，不扣账、不预占后料。
 * 数量及时间均复用机台真实容量画像；06:00和19:00按场景限定阶段容量，不用于虚增计划。
 */
@Slf4j
@Service
public class ContinuationRemainderFinishScheduleService {
    /** 早班资源交接节点。 */
    private static final int MORNING_RELEASE_HOUR = 6;
    /** 中班优先交接节点；指定释放角色仍以此为硬截止，窗口末班阶段允许继续承接。 */
    private static final int AFTERNOON_RELEASE_HOUR = 19;

    /**
     * 先预览既有节点的释放角色，再将历史优先机台绑定到较早角色并按各自真实产能计算。
     * 预览使用独立容量数组，提交结果始终归属原机台；没有命中时完全沿用原算法。
     *
     * @param context 已初始化历史顺序的上下文
     * @param sku 当前同物料同状态需求
     * @param machines 本组真实画像，返回时与结果逐项对应
     * @param shifts 真实班次
     * @param remainQty 本组合法预算
     * @param shiftQty 标准班产
     * @param dateOffset 收尾日偏移
     * @param stopHoldResults 不参与角色调整的保机结果
     * @return 真实机台排量及物理释放事实
     */
    public ContinuationFinishScheduleResult calculateWithPreviousAlternateOrder(LhScheduleContext context,
            SkuScheduleDTO sku, List<ContinuationEndingMachineProfile> machines, List<LhShiftConfigVO> shifts,
            int remainQty, int shiftQty, int dateOffset, List<LhScheduleResult> stopHoldResults) {
        Date windowEnd = shifts.get(shifts.size() - 1).getShiftEndDateTime();
        int[] ranks = this.resolveReleaseRanks(context, machines, stopHoldResults, windowEnd);
        if (Arrays.stream(ranks).noneMatch(rank -> rank >= 0 && rank < PreviousAlternateMachineOrder.UNMATCHED)) {
            return this.calculate(machines, shifts, remainQty, shiftQty, dateOffset);
        }
        List<ContinuationEndingMachineProfile> baselineProfiles = machines.stream()
                .map(ContinuationEndingMachineProfile::copyForCalculation).collect(Collectors.toList());
        ContinuationFinishScheduleResult baseline = this.calculate(baselineProfiles, shifts, remainQty, shiftQty, dateOffset);
        List<Integer> slots = java.util.stream.IntStream.range(0, machines.size()).boxed()
                .filter(position -> ranks[position] >= 0)
                .sorted(Comparator.comparing(position -> baseline.getMachinePlans().get(position).getOfflineTime(),
                        Comparator.nullsLast(Date::compareTo))).collect(Collectors.toList());
        List<Integer> candidates = new ArrayList<>(slots);
        candidates.sort(Comparator.comparingInt(position -> ranks[position]));
        List<ContinuationEndingMachineProfile> orderedProfiles = new ArrayList<>(machines);
        for (int position = 0; position < slots.size(); position++) {
            orderedProfiles.set(slots.get(position), machines.get(candidates.get(position)));
        }
        orderedProfiles = orderedProfiles.stream().map(ContinuationEndingMachineProfile::copyForCalculation)
                .collect(Collectors.toList());
        int[] orderedRanks = this.resolveReleaseRanks(context, orderedProfiles, stopHoldResults, windowEnd);
        ContinuationFinishScheduleResult calculated = this.calculate(orderedProfiles, shifts, remainQty, shiftQty,
                dateOffset, orderedRanks);
        if (calculated.getRemainingQty() > baseline.getRemainingQty()) {
            // 排名不能牺牲原机已具备的收尾能力；原角色下仍核对历史先后，不更换身份补造产能。
            orderedProfiles = machines.stream().map(ContinuationEndingMachineProfile::copyForCalculation)
                    .collect(Collectors.toList());
            calculated = this.calculate(orderedProfiles, shifts, remainQty, shiftQty, dateOffset, ranks);
            calculated.setDiagnostic("历史优先角色的真实容量不足，保留原角色可排总量；"
                    + StringUtils.defaultString(calculated.getDiagnostic()));
        }
        machines.clear();
        machines.addAll(orderedProfiles);
        this.logReleaseOrder(context, sku, machines, calculated);
        return calculated;
    }

    /**
     * 为可调整物理机台读取统一排名，强制占用和设备专属事件不参与角色交换。
     *
     * @param context 本批上下文
     * @param machines 真实机台画像
     * @param stopHoldResults 已确定保机结果
     * @param windowEnd 窗口末端
     * @return 与画像位置对应的排名，负数表示固定机台
     */
    private int[] resolveReleaseRanks(LhScheduleContext context, List<ContinuationEndingMachineProfile> machines,
            List<LhScheduleResult> stopHoldResults, Date windowEnd) {
        return machines.stream().mapToInt(profile -> {
            boolean fixed = Objects.nonNull(profile.getReleaseDeadline()) && profile.getReleaseDeadline().before(windowEnd);
            fixed = fixed || profile.getOriginals().stream().anyMatch(result ->
                    stopHoldResults.contains(result) || context.isContinuousStopHoldMachine(result.getLhMachineCode())
                    || context.getOnlySandBlastContinuationReleaseWindowMap().containsKey(result.getLhMachineCode())
                    || context.getContinuationTemporaryFaultTransferEventMap().containsKey(result.getLhMachineCode())
                    || context.getTimedMachineOffDecisionMap().containsKey(result));
            return fixed ? -1 : context.getPreviousAlternateMachineOrder()
                    .rank(profile.getOriginals().get(0).getLhMachineCode());
        }).toArray();
    }

    /**
     * 记录原机身份下的排名、数量和最终物理释放时间，区分优先级与硬约束阻断。
     *
     * @param context 本批上下文
     * @param sku 当前需求
     * @param machines 与结果对应的真实画像
     * @param calculated 守恒计算结果
     */
    private void logReleaseOrder(LhScheduleContext context, SkuScheduleDTO sku,
            List<ContinuationEndingMachineProfile> machines, ContinuationFinishScheduleResult calculated) {
        String detail = java.util.stream.IntStream.range(0, machines.size()).mapToObj(position -> {
            String code = machines.get(position).getOriginals().get(0).getLhMachineCode();
            ContinuationMachineFinishPlan plan = calculated.getMachinePlans().get(position);
            return code + ":排名=" + context.getPreviousAlternateMachineOrder().rank(code)
                    + ",数量=" + plan.getPlanQty() + ",物理释放=" + plan.getOfflineTime();
        }).collect(Collectors.joining(";"));
        this.logFinishMachineOrder(context, sku, "前置历史角色计算完成，" + StringUtils.defaultString(calculated.getDiagnostic()), detail);
    }

    /**
     * 校验历史等待角色与同SKU物理机台及L/R计划能否可靠对应。
     * 排名只决定候选先后，不能替代唯一关系和设备专属事件校验。
     *
     * @param context 已冻结历史顺序和关系的上下文
     * @param sku 来源SKU
     * @param profile 物理机台画像
     * @param stopHoldResults 前置保机结果
     * @return 物理机台统一的历史顺序计划；无法对应时返回null
     */
    private LhMouldChangePlan resolveFinishProfilePlan(LhScheduleContext context, SkuScheduleDTO sku,
            ContinuationEndingMachineProfile profile,
            List<LhScheduleResult> stopHoldResults) {
        LhMouldChangePlan first = null;
        for (LhScheduleResult result : profile.getOriginals()) {
            String code = result.getLhMachineCode();
            LhMouldChangePlan plan = context.getPreviousAlternateMachineOrder().uniquePlan(code, sku.getMaterialCode());
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

    /**
     * 记录前置历史顺序的计算结果或硬边界阻断原因。
     *
     * @param context 上下文
     * @param sku 收尾SKU
     * @param reason 顺序决定
     * @param detail 机台对应详情
     */
    private void logFinishMachineOrder(LhScheduleContext context, SkuScheduleDTO sku, String reason, String detail) {
        String message = String.format("工厂=%s, 批次=%s, 目标日=%s, 物料=%s, 状态=%s, 决定=%s, 机台=%s",
                context.getFactoryCode(), context.getBatchNo(), context.getScheduleTargetDate(),
                sku.getMaterialCode(), sku.getProductStatus(), reason, detail);
        PriorityTraceLogHelper.appendProcessLog(context, "续作余量收尾历史机台顺序", message);
        log.info("续作余量收尾历史机台顺序, {}", message);
    }

    /**
     * 计算整窗实际排量，先保留目标日前的连续生产前缀，再对目标日剩余量执行节点规则。
     * 前日19:00释放会撤回该台前缀的尾量，并归还本次统一余量，不能重复计量。
     *
     * @param machines 已排序的物理机台画像：T日双机2.2按下机顺序，跨日3.2及多机大余量按保留顺序
     * @param shifts 真实排程班次
     * @param remainQty 本组整窗尚可消费的余量，不含其他固定结果
     * @param shiftQty SKU标准单台班产，用于业务阈值；实际分配采用逐机真实容量
     * @param dateOffset 前置容量判定的T/T+1/T+2日期偏移
     * @return 数量、收尾和下机时间，以及真实未消化余量
     */
    public ContinuationFinishScheduleResult calculate(List<ContinuationEndingMachineProfile> machines,
            List<LhShiftConfigVO> shifts, int remainQty, int shiftQty, int dateOffset) {
        return this.calculate(machines, shifts, remainQty, shiftQty, dateOffset, null);
    }

    /**
     * 复用既有节点规则，在已绑定的真实机台上一次计算数量和历史释放先后。
     *
     * @param machines 固定释放角色对应的真实画像
     * @param shifts 真实班次
     * @param remainQty 合法总预算
     * @param shiftQty 标准班产
     * @param dateOffset 收尾日偏移
     * @param ranks 对应历史排名，空值表示原有数量规则
     * @return 未消费账本的收尾计算结果
     */
    private ContinuationFinishScheduleResult calculate(List<ContinuationEndingMachineProfile> machines,
            List<LhShiftConfigVO> shifts, int remainQty, int shiftQty, int dateOffset, int[] ranks) {
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
        // 本窗原机容量不足时只有继续生产和真实强制退出事实，不能套用收尾角色提前减量。
        if (Arrays.stream(limits).asLongStream().sum() < remainQty) {
            return this.calculateWithinWindow(machines, shifts, remainQty, ranks);
        }
        // 跨日节点可能收紧画像容量；未完成时恢复独立原画像，继续消费真实剩余产能。
        List<ContinuationEndingMachineProfile> continuousProfiles = dateOffset > 0
                ? machines.stream().map(ContinuationEndingMachineProfile::copyForCalculation).collect(Collectors.toList())
                : java.util.Collections.emptyList();
        // 分配状态只由实际累计量计算，不能同时维护一份可漂移的局部余量。
        if (dateOffset > 0) {
            this.allocateStage(machines, quantities, remainQty, 0, machines.size(), first.getShiftStartDateTime());
        }
        int dayRemaining = remainQty - Arrays.stream(quantities).sum();
        ContinuationNightFinishConstraint[] nightConstraints = new ContinuationNightFinishConstraint[machines.size()];
        List<ContinuationTDayFinishConstraint> tDayConstraints = dateOffset == 0 && machines.size() > 1
                ? this.freezeTDayConstraints(machines, remainQty, shiftQty, first.getWorkDate())
                : java.util.Collections.emptyList();
        if (machines.size() == 1) {
            this.allocate(machines, quantities, remainQty, 0, limits[0]);
        } else if (dateOffset == 0) {
            this.scheduleOnT(machines, quantities, limits, deadlines, remainQty, shiftQty, day);
        } else if (machines.size() == 2) {
            this.scheduleTwoMachinesWithNightShift(machines, shifts, quantities, limits, deadlines,
                    remainQty, shiftQty, day, 0, nightConstraints);
        } else if ((long) dayRemaining < (long) machines.size() * shiftQty) {
            this.scheduleSmallNightGroup(machines, shifts, quantities, limits, deadlines,
                    remainQty, shiftQty, day, nightConstraints);
        } else {
            // 先完成全部机台的当前夜班，再分配中班节点和后续夜班，后置归整不能回撤已分配阶段。
            this.scheduleNightStages(machines, shifts, quantities, limits, deadlines, remainQty, day, dateOffset);
            minimumQuantities = quantities.clone();
        }
        // 规则分量在历史顺序调整前冻结；排序只能绑定角色，不能再次修改公式结果。
        boolean constrainedNightFinish = Arrays.stream(nightConstraints).anyMatch(Objects::nonNull);
        for (int position = 0; position < nightConstraints.length; position++) {
            if (Objects.nonNull(nightConstraints[position])) {
                minimumQuantities[position] = nightConstraints[position].getRequiredQty();
                limits[position] = nightConstraints[position].getRequiredQty();
            }
        }
        ContinuationFinishScheduleResult calculated = this.buildResult(machines, shifts, quantities,
                minimumQuantities, limits, deadlines, remainQty, ranks);
        if (calculated.getRemainingQty() > 0 && dateOffset > 0 && !constrainedNightFinish) {
            // 原有小余量及多机阶段在未完成时恢复原机容量；已冻结的3.2公式节点不进入此回退。
            for (int position = 0; position < machines.size(); position++) {
                int[] originalCapacities = continuousProfiles.get(position).getCapacities();
                System.arraycopy(originalCapacities, 0, machines.get(position).getCapacities(), 0, originalCapacities.length);
            }
            return this.calculateWithinWindow(machines, shifts, remainQty, ranks);
        }
        for (int position = 0; position < nightConstraints.length; position++) {
            ContinuationNightFinishConstraint constraint = nightConstraints[position];
            if (Objects.nonNull(constraint)) {
                ContinuationMachineFinishPlan plan = calculated.getMachinePlans().get(position);
                plan.setNightFinishConstraint(constraint);
                plan.setFinishRoleMachineCode(constraint.getMachineCode());
                plan.setFinishRule(ContinuationNightFinishConstraint.RULE_NAME);
            }
        }
        for (int position = 0; position < tDayConstraints.size(); position++) {
            ContinuationMachineFinishPlan plan = calculated.getMachinePlans().get(position);
            plan.setTDayFinishConstraint(tDayConstraints.get(position));
            plan.setFinishRoleMachineCode(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                    machines.get(position).getOriginals().get(0).getLhMachineCode()));
            plan.setFinishRule(remainQty <= shiftQty ? ContinuationTDayFinishConstraint.MORNING_RULE_NAME
                    : ContinuationTDayFinishConstraint.AFTERNOON_RULE_NAME);
        }
        this.applyFinishStates(machines, windowEnd, calculated, limits, null);
        return calculated;
    }

    /**
     * 在实际分配之前冻结T日节点容量和同数量单位下的优先承接量。
     * 异质单位仍由既有守恒内核求合法组合，审计只核对其节点及真实容量边界。
     *
     * @param machines 已绑定角色的真实物理机台
     * @param targetQty 本组唯一合法目标
     * @param shiftQty 标准班产
     * @param workDate 真实T日日期
     * @return 与前组节点角色逐项对应的只读约束
     */
    private List<ContinuationTDayFinishConstraint> freezeTDayConstraints(
            List<ContinuationEndingMachineProfile> machines, int targetQty, int shiftQty, Date workDate) {
        int roleCount = targetQty <= shiftQty ? machines.size() - 1 : machines.size() / 2;
        boolean sameMultiple = machines.stream().map(ContinuationEndingMachineProfile::getMultiple)
                .distinct().count() == 1 && targetQty % machines.get(0).getMultiple() == 0;
        List<ContinuationTDayFinishConstraint> constraints = new ArrayList<>(roleCount);
        int remainingQty = targetQty;
        for (int position = 0; position < roleCount; position++) {
            ContinuationEndingMachineProfile profile = machines.get(position);
            ContinuationTDayFinishConstraint constraint = this.freezeTDayConstraint(profile, targetQty,
                    remainingQty, shiftQty, workDate, sameMultiple);
            int requiredQty = profile.floorQuantity(Math.min(remainingQty, constraint.getNodeCapacityQty()));
            constraints.add(constraint);
            remainingQty -= requiredQty;
        }
        return constraints;
    }

    /**
     * 普通入口和历史等待入口共用分配前的节点输入，跨日承接不取消T日角色约束。
     *
     * @param profile 已绑定节点角色的真实机台
     * @param targetQty 整组合法目标
     * @param remainingQty 分配给本角色前的剩余预算
     * @param shiftQty 标准单台班产
     * @param workDate 节点所在生产日
     * @param sameMultiple 是否可独立确定必须承接的合法量
     * @return 不依赖输出计划的节点约束
     */
    private ContinuationTDayFinishConstraint freezeTDayConstraint(ContinuationEndingMachineProfile profile,
            int targetQty, int remainingQty, int shiftQty, Date workDate, boolean sameMultiple) {
        Date node = ContinuationTDayFinishConstraint.resolveNode(workDate, targetQty, shiftQty);
        int nodeCapacity = profile.quantityUntil(node);
        int requiredQty = profile.floorQuantity(Math.min(remainingQty, nodeCapacity));
        return new ContinuationTDayFinishConstraint(targetQty, shiftQty, workDate,
                nodeCapacity, sameMultiple ? requiredQty : null);
    }

    /**
     * 历史交替强制退出后的终末余量按原机真实时间轴逐班消化。
     * 已退出机台的容量已在画像中截断；剩余机台不再套用“两台中第二台最多一个班产”的角色限制。
     * 本方法同时给出原机完成能力与实际分配，补偿入口复用提交后的同一结果，不另算目标机台缺口。
     * @param machines 已应用历史交替、维护、清洗、胶囊等硬边界的原机画像
     * @param shifts 本次真实班次
     * @param remainingQty 本组尚可消费的合法余量
     * @return 原机分配及真实未消化余量，不修改日计划或生产账本
     */
    public ContinuationFinishScheduleResult calculateAfterPreviousAlternateRelease(
            List<ContinuationEndingMachineProfile> machines, List<LhShiftConfigVO> shifts,
            int remainingQty) {
        return this.calculateWithinWindow(machines, shifts, remainingQty, null);
    }

    /**
     * 在原机真实窗口内逐班消化尚未完成的余量，历史顺序只约束可调整机台。
     * 普通窗口未完成与历史强制退出复用同一数量、时间和完成状态，不扣减生产账本。
     *
     * @param machines 已冻结物理硬边界的原机真实画像
     * @param shifts 真实班次
     * @param remainingQty 本组尚可消费的合法余量
     * @param ranks 可调整机台的历史排名，空值表示无历史顺序限制
     * @return 原机真实排量、完成或退出状态及未完成缺口
     */
    private ContinuationFinishScheduleResult calculateWithinWindow(
            List<ContinuationEndingMachineProfile> machines, List<LhShiftConfigVO> shifts,
            int remainingQty, int[] ranks) {
        if (CollectionUtils.isEmpty(machines) || CollectionUtils.isEmpty(shifts)
                || remainingQty < 0) {
            throw new IllegalArgumentException("历史交替续作余量计算缺少机台、班次或合法余量");
        }
        // 月计划末日只描述需求节奏；尚未清完的原机余量使用本窗口容量，各机真实硬截止已经进入画像。
        Date windowEnd = shifts.get(shifts.size() - 1).getShiftEndDateTime();
        int[] quantities = new int[machines.size()];
        int[] limits = machines.stream().mapToInt(machine -> machine.quantityUntil(windowEnd)).toArray();
        Date[] deadlines = new Date[machines.size()];
        Arrays.fill(deadlines, windowEnd);
        for (LhShiftConfigVO shift : shifts) {
            this.allocateStage(machines, quantities, remainingQty, 0, machines.size(), shift.getShiftEndDateTime());
            if (Arrays.stream(quantities).sum() == remainingQty) {
                break;
            }
        }
        ContinuationFinishScheduleResult result = this.buildResult(machines, shifts, quantities, new int[machines.size()],
                limits, deadlines, remainingQty, ranks);
        this.applyFinishStates(machines, windowEnd, result, limits, null);
        if (log.isInfoEnabled()) {
            LhScheduleResult source = machines.get(0).getOriginals().get(0);
            log.info("续作余量窗口延续计算, 工厂={}, 批次={}, 物料={}, 状态={}, 目标={}, 真实容量={}, 分配={}, 未完成={}",
                    source.getFactoryCode(), source.getBatchNo(), source.getMaterialCode(), source.getProductStatus(),
                    remainingQty, Arrays.toString(limits),
                    result.getMachinePlans().stream().map(ContinuationMachineFinishPlan::getPlanQty).collect(Collectors.toList()),
                    result.getRemainingQty());
        }
        return result;
    }

    /**
     * 历史交替等待余量的机台组先绑定角色，再一次分配原机余量。
     * 历史角色位于T日时复用06点或19点节点，即使另一台需要跨日承接也不能绕过。
     * 后续日期命中两机大余量时共用3.2公式，历史顺序仅绑定实际释放角色；其他场景沿原节点。
     * 真实强制退出场景保留原机能力算法，不覆盖设备专属硬边界。
     *
     * @param context 包含最新有效历史关系的上下文
     * @param sku 同物料同状态的共享需求
     * @param machines 已应用真实硬截止的原物理机台画像
     * @param shifts 本次真实班次
     * @param remainingQty 本组唯一合法预算
     * @param dateOffset 未施加角色节点前按整组真实容量确定的收尾日
     * @param stopHoldResults 不参与角色调整的停产保机结果
     * @return 固定机台身份的分配及节点事实，不修改账本或历史事件
     */
    public ContinuationFinishScheduleResult calculateWaitingPreviousAlternate(
            LhScheduleContext context, SkuScheduleDTO sku, List<ContinuationEndingMachineProfile> machines,
            List<LhShiftConfigVO> shifts, int remainingQty, int dateOffset,
            List<LhScheduleResult> stopHoldResults) {
        Date windowEnd = shifts.get(shifts.size() - 1).getShiftEndDateTime();
        int[] ranks = this.resolveReleaseRanks(context, machines, stopHoldResults, windowEnd);
        boolean hardRelease = machines.stream().anyMatch(profile -> Objects.nonNull(profile.getReleaseDeadline())
                && profile.getReleaseDeadline().before(windowEnd));
        if (dateOffset == 0 && !hardRelease) {
            return this.calculateWaitingOnT(context, sku, machines, shifts, remainingQty, stopHoldResults);
        }
        if (machines.size() != 2 || remainingQty <= sku.getShiftCapacity()
                || hardRelease) {
            return this.calculateWithinWindow(machines, shifts, remainingQty, ranks);
        }
        List<String> codes = machines.stream().flatMap(profile -> profile.getOriginals().stream())
                .map(LhScheduleResult::getLhMachineCode).collect(Collectors.toList());
        Map<Integer, LhMouldChangePlan> eligible = new LinkedHashMap<>();
        for (int position = 0; position < machines.size(); position++) {
            ContinuationEndingMachineProfile profile = machines.get(position);
            LhMouldChangePlan plan = this.resolveFinishProfilePlan(context, sku, profile, stopHoldResults);
            boolean waiting = profile.getOriginals().stream()
                    .map(result -> context.getPreviousAlternateReleaseEventMap().get(result.getLhMachineCode()))
                    .allMatch(event -> Objects.nonNull(event) && event.isRemainderFinishDeferred());
            if (Objects.nonNull(plan) && waiting) {
                eligible.put(position, plan);
            }
        }
        Integer releasePosition = eligible.keySet().stream()
                .min(Comparator.comparingInt(position -> context.getPreviousAlternateMachineOrder()
                        .rank(eligible.get(position).getLhMachineCode()))).orElse(null);
        LhMouldChangePlan rolePlan = Objects.isNull(releasePosition) ? null : eligible.get(releasePosition);
        Date finishDayStart = shifts.stream().filter(shift -> Objects.equals(shift.getDateOffset(), dateOffset))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("续作收尾目标日不在排程窗口内"))
                .getShiftStartDateTime();
        int dayRemaining = remainingQty - machines.stream()
                .mapToInt(profile -> profile.quantityUntil(finishDayStart)).sum();
        if (dateOffset > 0 && ContinuationNightFinishConstraint.isLargeRemainder(dayRemaining, sku.getShiftCapacity())
                && (Objects.isNull(rolePlan) || !this.isTDayHistoricalPlan(shifts, rolePlan))) {
            return this.calculateWithPreviousAlternateOrder(context, sku, machines, shifts,
                    remainingQty, sku.getShiftCapacity(), dateOffset, stopHoldResults);
        }
        if (Objects.isNull(releasePosition)) {
            this.logFinishMachineOrder(context, sku, "历史等待组无可靠释放角色，保留原机真实能力分配", codes.toString());
            return this.calculateWithinWindow(machines, shifts, remainingQty, ranks);
        }
        LhMouldChangePlan historicalPlan = eligible.get(releasePosition);
        Date deadline = this.resolveWaitingReleaseDeadline(shifts, historicalPlan, remainingQty, sku.getShiftCapacity());
        // 窗口起点以前或没有生产容量的历史节点不能截掉原机余量，仍按真实完成能力处理。
        if (Objects.isNull(deadline) || deadline.after(windowEnd)
                || machines.get(releasePosition).quantityUntil(deadline) <= 0) {
            return this.calculateWithinWindow(machines, shifts, remainingQty, ranks);
        }
        int[] quantities = new int[machines.size()];
        int[] limits = machines.stream().mapToInt(profile -> profile.quantityUntil(windowEnd)).toArray();
        // 累计容量算出的收尾日可以跨日，历史T日角色仍必须在分配前冻结本日业务节点。
        ContinuationTDayFinishConstraint tDayConstraint = null;
        if (this.isTDayHistoricalPlan(shifts, historicalPlan)) {
            boolean sameMultiple = machines.stream().map(ContinuationEndingMachineProfile::getMultiple)
                    .distinct().count() == 1 && remainingQty % machines.get(0).getMultiple() == 0;
            tDayConstraint = this.freezeTDayConstraint(machines.get(releasePosition), remainingQty,
                    remainingQty, sku.getShiftCapacity(), shifts.get(0).getWorkDate(), sameMultiple);
        }
        Date[] deadlines = new Date[machines.size()];
        Arrays.fill(deadlines, windowEnd);
        this.restrict(machines, quantities, limits, deadlines, releasePosition, deadline);
        this.allocate(machines, quantities, remainingQty, releasePosition, limits[releasePosition]);
        int carrierPosition = 1 - releasePosition;
        this.allocate(machines, quantities, remainingQty, carrierPosition, limits[carrierPosition]);
        ContinuationFinishScheduleResult calculated = this.buildResult(machines, shifts, quantities,
                new int[machines.size()], limits, deadlines, remainingQty, ranks);
        ContinuationMachineFinishPlan releasePlan = calculated.getMachinePlans().get(releasePosition);
        releasePlan.setHistoricalFinishPlanId(historicalPlan.getId());
        releasePlan.setFinishRoleMachineCode(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                machines.get(releasePosition).getOriginals().get(0).getLhMachineCode()));
        releasePlan.setFinishRule("历史双机交替节点释放");
        releasePlan.setTDayFinishConstraint(tDayConstraint);
        this.applyFinishStates(machines, windowEnd, calculated, limits, releasePosition);
        String detail = String.format("物料=%s, 状态=%s, 历史计划=%s, 规则=%s, 释放机台=%s, 节点=%s, 承接机台=%s, 分配=%s, 未完成=%s",
                sku.getMaterialCode(), sku.getProductStatus(), historicalPlan.getId(), releasePlan.getFinishRule(),
                releasePlan.getFinishRoleMachineCode(), deadline,
                machines.get(carrierPosition).getOriginals().stream().map(LhScheduleResult::getLhMachineCode)
                        .collect(Collectors.joining(",")),
                calculated.getMachinePlans().stream().map(ContinuationMachineFinishPlan::getPlanQty).collect(Collectors.toList()),
                calculated.getRemainingQty());
        PriorityTraceLogHelper.appendProcessLog(context, "历史交替余量角色分配", detail);
        log.info("历史交替余量角色分配, {}", detail);
        return calculated;
    }

    /**
     * 使用公共T日节点完成历史等待组，保留原机身份、真实完成和未完成占用状态。
     *
     * @param context 已冻结历史顺序的上下文
     * @param sku 同物料同状态需求
     * @param machines 固定的原物理机台画像
     * @param shifts 真实窗口班次
     * @param remainingQty 合法目标量
     * @param stopHoldResults 不参与角色交换的保机结果
     * @return 公共节点下的真实机台计划
     */
    private ContinuationFinishScheduleResult calculateWaitingOnT(LhScheduleContext context, SkuScheduleDTO sku,
            List<ContinuationEndingMachineProfile> machines, List<LhShiftConfigVO> shifts, int remainingQty,
            List<LhScheduleResult> stopHoldResults) {
        Date windowEnd = shifts.get(shifts.size() - 1).getShiftEndDateTime();
        ContinuationFinishScheduleResult calculated = this.calculateWithPreviousAlternateOrder(context, sku,
                machines, shifts, remainingQty, sku.getShiftCapacity(), 0, stopHoldResults);
        for (int position = 0; position < machines.size(); position++) {
            ContinuationMachineFinishPlan plan = calculated.getMachinePlans().get(position);
            if (Objects.nonNull(plan.getTDayFinishConstraint())) {
                LhMouldChangePlan historicalPlan = this.resolveFinishProfilePlan(context, sku,
                        machines.get(position), stopHoldResults);
                if (Objects.nonNull(historicalPlan)) {
                    plan.setHistoricalFinishPlanId(historicalPlan.getId());
                }
            }
        }
        return calculated;
    }

    /**
     * 按真实历史日期确定节点，T日复用业务阈值，后续日期沿历史早中班节点。
     * 完成日偏移只表示整组容量，不能将T日历史早班起点当作无容量节点而取消收尾约束。
     *
     * @param shifts 完整窗口班次
     * @param historicalPlan 已验证的历史关系
     * @param remainingQty 整组合法余量
     * @param shiftQty 标准单台班产
     * @return T日业务节点或后续日期的历史早中班节点，日期不在窗口时返回空
     */
    private Date resolveWaitingReleaseDeadline(List<LhShiftConfigVO> shifts,
            LhMouldChangePlan historicalPlan, int remainingQty, int shiftQty) {
        if (this.isTDayHistoricalPlan(shifts, historicalPlan)) {
            return ContinuationTDayFinishConstraint.resolveNode(shifts.get(0).getWorkDate(), remainingQty, shiftQty);
        }
        LocalDate historicalDay = this.planDay(historicalPlan);
        LhShiftConfigVO releaseShift = shifts.stream()
                .filter(shift -> historicalDay.equals(shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate()))
                .filter(shift -> StringUtils.equals(historicalPlan.getClassIndex(), shift.getShiftType()))
                .findFirst().orElse(null);
        if (Objects.isNull(releaseShift)) {
            return null;
        }
        if (releaseShift.isAfternoonShift()) {
            return this.node(historicalDay, AFTERNOON_RELEASE_HOUR);
        }
        return releaseShift.getShiftStartDateTime();
    }

    /**
     * 比较历史角色生产日与窗口T日，不使用整组预计完成日替代角色日期。
     *
     * @param shifts 真实窗口班次
     * @param historicalPlan 已验证的历史角色
     * @return 历史角色是否位于本窗口T日
     */
    private boolean isTDayHistoricalPlan(List<LhShiftConfigVO> shifts, LhMouldChangePlan historicalPlan) {
        return Objects.equals(LhScheduleTimeUtil.clearTime(shifts.get(0).getWorkDate()),
                LhScheduleTimeUtil.clearTime(historicalPlan.getPlanDate()));
    }

    /**
     * 多机大余量先按06:00、19:00分配；窗口末中班承接至班末，其余日期使用后续夜班。
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
        LhShiftConfigVO lastShift = shifts.get(shifts.size() - 1);
        boolean endingAfternoon = Objects.equals(lastShift.getDateOffset(), dateOffset)
                && lastShift.isAfternoonShift();
        // 窗口最后中班保留真实容量，19:00只作为优先节点；已有交替、维护等硬截止仍在画像内。
        // 其他日期继续把19:00后的余量交给后续夜班，不改变既有阶段规则。
        for (LhShiftConfigVO shift : shifts) {
            if (!endingAfternoon && Objects.equals(shift.getDateOffset(), dateOffset) && shift.isAfternoonShift()) {
                for (ContinuationEndingMachineProfile profile : machines) {
                    profile.restrictShiftToDeadline(shift.getShiftIndex(), afternoon);
                }
            }
        }
        Date lastNode = endingAfternoon ? lastShift.getShiftEndDateTime() : nextMorning;
        Date finalDeadline = lastNode;
        for (Date node : Arrays.asList(morning, afternoon, lastNode)) {
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
     *
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
        Date cutoff = ContinuationTDayFinishConstraint.resolveNode(this.node(day, MORNING_RELEASE_HOUR), total, shiftQty);
        for (int machine = 0; machine < earlyCount; machine++) {
            this.restrict(machines, quantities, limits, deadlines, machine, cutoff);
            this.allocate(machines, quantities, total, machine, limits[machine]);
        }
        this.allocateRemaining(machines, quantities, limits, total, earlyCount);
    }

    /**
     * 含夜班的小余量组先释放前半组；剩余两台必须复用统一两机规则。
     *
     * @param machines 机台画像
     * @param shifts 真实窗口班次
     * @param quantities 累计量
     * @param limits 总量上限
     * @param deadlines 收尾截止
     * @param total 原始预算
     * @param shiftQty 标准班产
     * @param day 目标业务日
     * @param constraints 与物理机台位置对应的跨日两机约束
     */
    private void scheduleSmallNightGroup(List<ContinuationEndingMachineProfile> machines,
            List<LhShiftConfigVO> shifts, int[] quantities, int[] limits, Date[] deadlines,
            int total, int shiftQty, LocalDate day, ContinuationNightFinishConstraint[] constraints) {
        int half = machines.size() / 2;
        Date previousAfternoon = this.node(day.minusDays(1), AFTERNOON_RELEASE_HOUR);
        for (int machine = 0; machine < half; machine++) {
            this.restrict(machines, quantities, limits, deadlines, machine, previousAfternoon);
            this.allocate(machines, quantities, total, machine, limits[machine]);
        }
        int remainingMachines = machines.size() - half;
        if (remainingMachines == 2) {
            this.scheduleTwoMachinesWithNightShift(machines, shifts, quantities, limits, deadlines,
                    total, shiftQty, day, half, constraints);
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
     * 两机共用规则：公式角色在十九点前完成指定分量，另一台连续承接全部剩余余量。
     * 分配前缀及夜班优先同时冻结，真实缺口不得通过回填公式角色或取消节点掩盖。
     *
     * @param machines 真实机台画像
     * @param shifts 完整窗口班次
     * @param quantities 累计量，包含目标日前缀
     * @param limits 总量上限
     * @param deadlines 各机生产截止
     * @param total 整组合法预算
     * @param shiftQty 标准班产
     * @param day 目标业务日
     * @param first 两机子组的公式角色位置
     * @param constraints 与物理机台位置对应的分配前约束
     */
    private void scheduleTwoMachinesWithNightShift(List<ContinuationEndingMachineProfile> machines,
            List<LhShiftConfigVO> shifts, int[] quantities, int[] limits, Date[] deadlines,
            int total, int shiftQty, LocalDate day, int first, ContinuationNightFinishConstraint[] constraints) {
        int remaining = total - Arrays.stream(quantities).sum();
        int second = first + 1;
        if (!ContinuationNightFinishConstraint.isLargeRemainder(remaining, shiftQty)) {
            this.restrict(machines, quantities, limits, deadlines, first,
                    this.node(day.minusDays(1), AFTERNOON_RELEASE_HOUR));
            this.allocate(machines, quantities, total, first, limits[first]);
            this.allocate(machines, quantities, total, second, limits[second]);
            return;
        }
        int firstPrefix = quantities[first];
        int secondPrefix = quantities[second];
        Date afternoon = this.node(day, AFTERNOON_RELEASE_HOUR);
        this.restrict(machines, quantities, limits, deadlines, first, afternoon);
        int firstMaximum = Math.min(limits[first], firstPrefix + remaining - shiftQty);
        this.allocate(machines, quantities, total, first, firstMaximum);
        // 另一台承接公式分量之外的全部余量，仅受本机真实容量和窗口约束。
        this.allocate(machines, quantities, total, second, limits[second]);
        constraints[first] = this.freezeNightConstraint(machines.get(first), shifts, total, remaining,
                shiftQty, day, firstPrefix, quantities[first], true);
        constraints[second] = this.freezeNightConstraint(machines.get(second), shifts, total, remaining,
                shiftQty, day, secondPrefix, quantities[second], false);
    }

    /**
     * 在后置归整和角色匹配之前冻结单台物理机台的公式量及真实夜班优先量。
     *
     * @param profile 已绑定角色的真实机台画像
     * @param shifts 完整窗口班次
     * @param total 本组合法总量
     * @param remaining 进入两机规则时的待分配余量
     * @param shiftQty 标准班产
     * @param day 收尾业务日
     * @param prefixQty 进入本轮分配前的累计量
     * @param requiredQty 规则及真实产能共同确定的合法累计量
     * @param formulaRole 是否为十九点公式角色
     * @return 用于后续提交和独立审计的只读约束
     */
    private ContinuationNightFinishConstraint freezeNightConstraint(ContinuationEndingMachineProfile profile,
            List<LhShiftConfigVO> shifts, int total, int remaining, int shiftQty, LocalDate day,
            int prefixQty, int requiredQty, boolean formulaRole) {
        LhShiftConfigVO night = shifts.stream().filter(LhShiftConfigVO::isNightShift)
                .filter(shift -> day.equals(shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("跨日两机收尾缺少真实夜班"));
        int nightQuantity = profile.shiftQuantities(requiredQty)[shifts.indexOf(night)];
        String machineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                profile.getOriginals().get(0).getLhMachineCode());
        return new ContinuationNightFinishConstraint(total, remaining, shiftQty,
                night.getWorkDate(), machineCode, formulaRole, prefixQty,
                profile.quantityUntil(this.node(day, AFTERNOON_RELEASE_HOUR)), requiredQty,
                night.getShiftIndex(), nightQuantity);
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

    /**
     * 统一发布逐机完成事实，窗口未完成不能凭最后正量提前释放资源。
     * 已冻结的物理硬截止、T日节点及历史释放角色继续保留独立退出语义。
     *
     * @param machines 与计算槽位对应的真实物理机台画像
     * @param windowEnd 本次排程窗口末端
     * @param calculated 已通过数量和时间校验的整组计算结果
     * @param limits 本次合法数量上限，历史释放角色已包含确认节点
     * @param releasePosition 已确认历史释放角色的位置，空值表示无历史角色
     */
    private void applyFinishStates(List<ContinuationEndingMachineProfile> machines, Date windowEnd,
            ContinuationFinishScheduleResult calculated, int[] limits, Integer releasePosition) {
        for (int position = 0; position < machines.size(); position++) {
            ContinuationEndingMachineProfile profile = machines.get(position);
            ContinuationMachineFinishPlan plan = calculated.getMachinePlans().get(position);
            Date hardDeadline = profile.getReleaseDeadline();
            boolean physicalRelease = Objects.nonNull(hardDeadline) && hardDeadline.before(windowEnd)
                    && plan.getPlanQty() == profile.quantityUntil(windowEnd);
            boolean historicalRelease = Objects.equals(position, releasePosition) && plan.getPlanQty() == limits[position];
            ContinuationNightFinishConstraint nightConstraint = plan.getNightFinishConstraint();
            boolean nightNodeRelease = Objects.nonNull(nightConstraint) && nightConstraint.isFormulaRole()
                    && plan.getPlanQty() == nightConstraint.getNodeCapacityQty();
            boolean tDayRelease = Objects.nonNull(plan.getTDayFinishConstraint())
                    && plan.getPlanQty() == plan.getTDayFinishConstraint().getNodeCapacityQty();
            if (physicalRelease) {
                plan.setFinishState(ContinuationMachineFinishPlan.FinishState.FORCED_RELEASE);
            } else if (calculated.getRemainingQty() == 0) {
                plan.setFinishState(ContinuationMachineFinishPlan.FinishState.SURPLUS_COMPLETED);
            } else if (historicalRelease || tDayRelease || nightNodeRelease) {
                plan.setFinishState(ContinuationMachineFinishPlan.FinishState.FORCED_RELEASE);
            } else {
                plan.setFinishState(ContinuationMachineFinishPlan.FinishState.WINDOW_UNFINISHED);
                plan.setOfflineTime(windowEnd);
            }
        }
    }

    /**
     * 在节点数量边界内完成合法归整及历史释放顺序核对，再生成真实机台结果。
     *
     * @param machines 真实画像
     * @param shifts 真实班次
     * @param quantities 初始计划量
     * @param minimumQuantities 已冻结阶段下限
     * @param limits 各机数量上限
     * @param deadlines 各机生产截止
     * @param total 合法总预算
     * @param ranks 历史排名，空值表示原有分配
     * @return 数量、物理释放及无法满足顺序时的诊断
     */
    private ContinuationFinishScheduleResult buildResult(List<ContinuationEndingMachineProfile> machines,
            List<LhShiftConfigVO> shifts, int[] quantities, int[] minimumQuantities, int[] limits,
            Date[] deadlines, int total, int[] ranks) {
        ContinuationFinishScheduleResult result = new ContinuationFinishScheduleResult();
        if (Arrays.stream(quantities).sum() < total) {
            // 只补合法归整组合，不复用旧错峰评分；各场景截止和冻结的两机公式分量始终保留。
            ContinuationEndingQuantityReachability reachability = new ContinuationEndingQuantityReachability(
                    machines.stream().map(ContinuationEndingMachineProfile::getCapacities).toArray(int[][]::new),
                    machines.stream().mapToInt(ContinuationEndingMachineProfile::getMultiple).toArray());
            int[] exact = reachability.allocate(total, minimumQuantities, limits, quantities);
            if (Objects.nonNull(exact)) {
                quantities = exact;
            }
            result.setDiagnostic(reachability.getDiagnostic());
        }
        if (Objects.nonNull(ranks)) {
            ContinuationEndingReleaseOrderAllocator orderAllocator = new ContinuationEndingReleaseOrderAllocator();
            int[] ordered = orderAllocator.allocate(machines, ranks, minimumQuantities, limits, quantities);
            if (Objects.nonNull(ordered)) {
                quantities = ordered;
            } else {
                result.setDiagnostic(StringUtils.defaultString(result.getDiagnostic()) + orderAllocator.getDiagnostic());
            }
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
