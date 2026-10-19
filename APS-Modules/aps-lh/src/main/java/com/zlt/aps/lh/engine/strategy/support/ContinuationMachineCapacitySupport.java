package com.zlt.aps.lh.engine.strategy.support;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.ShiftProductionControlDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.component.CapsuleReplacementRuleService;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.service.impl.LhMaintenanceScheduleService;
import com.zlt.aps.lh.util.CleaningDispositionResolver;
import com.zlt.aps.lh.util.CleaningScheduleRuleUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.util.ShiftProductionControlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 续作共用的真实产能画像及物理窗口支持，不承担下机排序或节点决策。
 *
 * @author APS
 * @date 2026/10/19
 */
@Slf4j
@Component
public class ContinuationMachineCapacitySupport {

    /**
     * 统一目标及数量单位解析入口。
     */
    private final TargetScheduleQtyResolver targetScheduleQtyResolver;

    /**
     * 原维护硬截止解析入口。
     */
    private final LhMaintenanceScheduleService maintenanceScheduleService;

    /**
     * 原胶囊数量及时间约束入口。
     */
    private final CapsuleReplacementRuleService capsuleReplacementRuleService;

    /**
     * 兼容既有直接构造调用，使用与原策略相同的无状态组件组合。
     */
    public ContinuationMachineCapacitySupport() {
        this(new TargetScheduleQtyResolver(), new LhMaintenanceScheduleService(), new CapsuleReplacementRuleService());
    }

    /**
     * 由容器注入共享产能依赖，不持有批次资源状态。
     *
     * @param targetScheduleQtyResolver 统一数量解析器
     * @param maintenanceScheduleService 维护硬截止服务
     * @param capsuleReplacementRuleService 胶囊容量服务
     */
    @Autowired
    public ContinuationMachineCapacitySupport(TargetScheduleQtyResolver targetScheduleQtyResolver,
            LhMaintenanceScheduleService maintenanceScheduleService, CapsuleReplacementRuleService capsuleReplacementRuleService) {
        this.targetScheduleQtyResolver = targetScheduleQtyResolver;
        this.maintenanceScheduleService = maintenanceScheduleService;
        this.capsuleReplacementRuleService = capsuleReplacementRuleService;
    }

    /**
     * 解析班次业务日期。
     *
     * @param shift 班次配置
     * @return 业务日期；班次为空时返回null
     */
    public LocalDate resolveShiftWorkDate(LhShiftConfigVO shift) {
        if (shift == null || shift.getWorkDate() == null) {
            return null;
        }
        return shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 构造一台物理机台的缓存容量，单控按两侧共同合法容量同步生产。
     *
     * @param context 上下文
     * @param sides 物理机台结果（L/R作为整组）
     * @param shifts 班次
     * @param cutoffs 已提交后续资源的最早占用时间
     * @param pendingStopHoldResults 尚未提交的停产保机结果
     * @param remainderFinish 是否为余量收尾；按时间下机保留原模数归整口径
     * @return 只读容量时间轴，缺少必要数据返回null而不是虚构0产能
     */
    public ContinuationEndingMachineProfile buildMachineProfile(
            LhScheduleContext context, List<LhScheduleResult> sides, List<LhShiftConfigVO> shifts,
            Map<String, Date> cutoffs, Set<LhScheduleResult> pendingStopHoldResults, boolean remainderFinish) {
        return this.buildMachineProfile(context, sides, shifts, cutoffs,
                pendingStopHoldResults, remainderFinish, null);
    }

    /**
     * 构造原机真实容量画像，指定收尾日跳过日标准量修正，其余班次保持既有口径。
     *
     * @param context 排程上下文
     * @param sides 普通机台结果或同一单控整机左右侧
     * @param shifts 完整窗口班次
     * @param cutoffs 实际占用及强制退出截止
     * @param pendingStopHoldResults 已确认的停产保机结果
     * @param remainderFinish 是否执行余量收尾数量归整
     * @param finishDateOffset 使用标准班产的收尾日偏移，空值表示沿用日标准量
     * @return 原机容量与时间轴，输入不完整时返回空
     */
    public ContinuationEndingMachineProfile buildMachineProfile(
            LhScheduleContext context, List<LhScheduleResult> sides, List<LhShiftConfigVO> shifts,
            Map<String, Date> cutoffs, Set<LhScheduleResult> pendingStopHoldResults,
            boolean remainderFinish, Integer finishDateOffset) {
        List<LhScheduleResult> rawProfiles = new ArrayList<>(sides.size());
        List<List<MachineCleaningWindowDTO>> cleaningWindows = new ArrayList<>(sides.size());
        List<List<MachineMaintenanceWindowDTO>> maintenanceWindows = new ArrayList<>(sides.size());
        int[] capacities = new int[shifts.size()];
        java.util.Arrays.fill(capacities, Integer.MAX_VALUE);
        int sideMultiple = Objects.nonNull(sides.get(0).getMouldQty()) ? sides.get(0).getMouldQty() : 0;
        // 普通双模余量收尾必须按整模分配；L/R整机仍按两侧共同单位同量同步。
        int quantityMultiple = remainderFinish
                ? targetScheduleQtyResolver.resolveAllocationMultiple(context, sides.get(0), sideMultiple)
                : sideMultiple;
        for (LhScheduleResult result : sides) {
            if (Objects.isNull(result.getLhTime()) || result.getLhTime() <= 0
                    || Objects.isNull(result.getSingleMouldShiftQty()) || result.getSingleMouldShiftQty() <= 0
                    || sideMultiple <= 0 || sideMultiple > Integer.MAX_VALUE / sides.size()
                    || !Objects.equals(result.getMouldQty(), sideMultiple)) {
                return null;
            }
            Date start = this.resolveRedistributeStartTime(result, shifts);
            if (Objects.isNull(start)) {
                return null;
            }
            List<MachineCleaningWindowDTO> cleaning = this.resolveEffectiveCleaningWindowList(context, result, start);
            List<MachineMaintenanceWindowDTO> maintenance = this.resolveMachineMaintenanceWindowList(context, result.getLhMachineCode());
            Map<Integer, Integer> rawCapacities = this.resolveEndingWindowShiftCapacities(
                    context, result, shifts, finishDateOffset);
            LhScheduleResult raw = BeanUtil.copyProperties(result, LhScheduleResult.class);
            Date cutoff = cutoffs.get(result.getLhMachineCode());
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
            Date forceDown = Objects.nonNull(machine) ? maintenanceScheduleService.resolveForceDownCutoffTime(machine) : null;
            if (Objects.nonNull(forceDown) && (Objects.isNull(cutoff) || forceDown.before(cutoff))) {
                cutoff = forceDown;
            }
            for (int position = 0; position < shifts.size(); position++) {
                LhShiftConfigVO shift = shifts.get(position);
                ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(context, shift, start);
                int capacity = rawCapacities.getOrDefault(shift.getShiftIndex(), 0);
                if (Objects.isNull(control) || !control.isCanSchedule() || capacity <= 0) {
                    ShiftFieldUtil.setShiftPlanQty(raw, shift.getShiftIndex(), 0, null, null);
                    capacities[position] = 0;
                    continue;
                }
                ShiftFieldUtil.setShiftPlanQty(raw, shift.getShiftIndex(), capacity,
                        control.getEffectiveStartTime(), control.getEffectiveEndTime());
                int allowed = this.limitSurplusEndingCapacity(context, result, shift, control, capacity,
                        cutoff, cleaning, maintenance, quantityMultiple,
                        pendingStopHoldResults.contains(result), shifts);
                // 时间内核保留原始班产比例；可分配容量按整模向下截取，尾量由组内其他机台承接。
                if (remainderFinish) {
                    allowed = allowed / quantityMultiple * quantityMultiple;
                }
                capacities[position] = Math.min(capacities[position], allowed);
            }
            rawProfiles.add(raw);
            cleaningWindows.add(cleaning);
            maintenanceWindows.add(maintenance);
        }
        for (int position = 0; position < capacities.length; position++) {
            // 两侧必须同量同班，不把一侧多余产能借给另一侧；整数溢出属于无效产能数据。
            if (capacities[position] > Integer.MAX_VALUE / sides.size()) {
                return null;
            }
            capacities[position] *= sides.size();
        }
        return new ContinuationEndingMachineProfile(context, shifts, sides, rawProfiles,
                cleaningWindows, maintenanceWindows, capacities, quantityMultiple * sides.size());
    }

    /**
     * 在不改变真实时间比例的前提下反算占用截止前可排量。
     *
     * @param context 上下文
     * @param result 原结果
     * @param shift 班次
     * @param control 生产控制窗口
     * @param capacity 原有效班产
     * @param cutoff 后续占用或强制下机截止
     * @param cleaning 清洗窗口
     * @param maintenance 维修窗口
     * @param multiple 数量归整单位，普通双模为2，单模、单控和胎胚收尾按业务例外解析
     * @param pendingStopHold 当前结果是否为尚未提交的停产保机候选
     * @param shifts 整窗班次，用于确定首个保机早班
     * @return 实际允许容量
     */
    public int limitSurplusEndingCapacity(LhScheduleContext context, LhScheduleResult result,
            LhShiftConfigVO shift, ShiftProductionControlDTO control, int capacity,
            Date cutoff, List<MachineCleaningWindowDTO> cleaning,
            List<MachineMaintenanceWindowDTO> maintenance, int multiple,
            boolean pendingStopHold, List<LhShiftConfigVO> shifts) {
        Integer release = context.getContinuousReducedMachineReleaseBoundaryShiftIndex(result.getLhMachineCode());
        if (context.isContinuousStopHoldShift(result.getLhMachineCode(), this.resolveShiftWorkDate(shift),
                shift.getShiftIndex())
                || (pendingStopHold && this.isPendingContinuousStopHoldShift(context, result, shift, shifts))
                || (Objects.nonNull(release) && shift.getShiftIndex() > release)) {
            return 0;
        }
        if (Objects.isNull(cutoff) || !cutoff.before(control.getEffectiveEndTime())) {
            return capacity;
        }
        int low = 0;
        int high = capacity;
        while (low < high) {
            int middle = low + (int) (((long) high - low + 1) / 2);
            Date end = ShiftCapacityResolverUtil.resolveShiftPlanEndTime(context.getDevicePlanShutList(), cleaning,
                    maintenance, result.getLhMachineCode(), control.getEffectiveStartTime(), control.getEffectiveEndTime(),
                    middle, capacity, context, result, shift);
            if (Objects.nonNull(end) && !end.after(cutoff)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low == capacity ? low : low / multiple * multiple;
    }

    /**
     * 判断尚未提交的停产保机候选在目标班次是否应停产。
     * <p>首个连续保机业务日从早班开始，首日前置夜班仍保留原续作产能；后续业务日整日停产。</p>
     *
     * @param context 排程上下文
     * @param result 停产保机候选结果
     * @param shift 目标班次
     * @param shifts 本次试算窗口班次
     * @return true-目标班次应停产；false-目标班次仍可按原续作规则生产
     */
    public boolean isPendingContinuousStopHoldShift(LhScheduleContext context,
            LhScheduleResult result,
            LhShiftConfigVO shift,
            List<LhShiftConfigVO> shifts) {
        if (Objects.isNull(context) || Objects.isNull(result) || Objects.isNull(shift)
                || CollectionUtils.isEmpty(shifts)) {
            return false;
        }
        LocalDate firstDate = shifts.stream()
                .map(this::resolveShiftWorkDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate shiftDate = this.resolveShiftWorkDate(shift);
        if (Objects.isNull(firstDate) || Objects.isNull(shiftDate) || shiftDate.isBefore(firstDate)) {
            return false;
        }
        boolean continuingFromPreviousDate = context.isContinuousStopHoldDate(
                result.getLhMachineCode(), firstDate.minusDays(1));
        return shiftDate.isAfter(firstDate) || continuingFromPreviousDate || !shift.isNightShift();
    }

    /**
     * 合并一份既有机台占用视图，重复记录只取最早时间。
     *
     * @param assigned 已提交结果
     * @param own 当前组
     * @param codes 当前组机台编码
     * @param shifts 班次
     * @param cutoffs 返回最早占用
     * @param nextActions 返回已有交替开始时间
     * @return 已有正量是否具备真实时间，缺失时不能推断为空闲
     */
    public boolean collectOccupiedCutoffs(List<LhScheduleResult> assigned, Set<LhScheduleResult> own,
            Set<String> codes, List<LhShiftConfigVO> shifts, Map<String, Date> cutoffs, Map<String, Date> nextActions) {
        if (CollectionUtils.isEmpty(assigned)) {
            return true;
        }
        for (LhScheduleResult other : assigned) {
            if (Objects.isNull(other) || own.contains(other) || !codes.contains(other.getLhMachineCode())) {
                continue;
            }
            String code = other.getLhMachineCode();
            Date action = other.getMouldChangeStartTime();
            if (Objects.nonNull(action)) {
                cutoffs.merge(code, action, (first, second) -> first.before(second) ? first : second);
                nextActions.merge(code, action, (first, second) -> first.before(second) ? first : second);
            }
            for (LhShiftConfigVO shift : shifts) {
                Date start = ShiftFieldUtil.getShiftStartTime(other, shift.getShiftIndex());
                Integer quantity = ShiftFieldUtil.getShiftPlanQty(other, shift.getShiftIndex());
                Date end = ShiftFieldUtil.getShiftEndTime(other, shift.getShiftIndex());
                if (Objects.nonNull(quantity) && quantity > 0
                        && (Objects.isNull(start) || Objects.isNull(end) || end.before(start))) {
                    return false;
                }
                if (Objects.nonNull(start) && Objects.nonNull(quantity) && quantity > 0) {
                    cutoffs.merge(code, start, (first, second) -> first.before(second) ? first : second);
                }
            }
        }
        return true;
    }

    /**
     * 解析余量收尾使用的真实班次产能，统一纳入日标准、停机清洗和换胶囊上限。
     *
     * @param context 上下文
     * @param result 续作结果
     * @param shifts 待计算班次
     * @return 班次有效产能
     */
    public Map<Integer, Integer> resolveEndingWindowShiftCapacities(
            LhScheduleContext context, LhScheduleResult result, List<LhShiftConfigVO> shifts) {
        return this.resolveEndingWindowShiftCapacities(context, result, shifts, null);
    }

    /**
     * 保留正常生产前缀，仅将冻结收尾日替换为标准班产并统一应用胶囊容量上限。
     *
     * @param context 排程上下文
     * @param result 续作结果
     * @param shifts 完整窗口班次
     * @param finishDateOffset 收尾日偏移，空值表示整窗沿用日标准量
     * @return 各班次最终可分配容量
     */
    public Map<Integer, Integer> resolveEndingWindowShiftCapacities(LhScheduleContext context,
            LhScheduleResult result, List<LhShiftConfigVO> shifts, Integer finishDateOffset) {
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                Objects.isNull(result.getMouldQty()) ? 0 : result.getMouldQty());
        Date startTime = this.resolveRedistributeStartTime(result, shifts);
        int shiftCapacity = Objects.isNull(result.getSingleMouldShiftQty()) ? 0 : result.getSingleMouldShiftQty();
        int lhTimeSeconds = Objects.isNull(result.getLhTime()) ? 0 : result.getLhTime();
        List<MachineCleaningWindowDTO> cleaningWindows = this.resolveEffectiveCleaningWindowList(
                context, result, this.resolveFirstPlannedShiftStartTime(result));
        List<MachineMaintenanceWindowDTO> maintenanceWindows = this.resolveMachineMaintenanceWindowList(
                context, result.getLhMachineCode());
        Map<Integer, Integer> capacities = this.calculateDailyStandardShiftCapacityMap(context, result, shifts,
                startTime, shiftCapacity, lhTimeSeconds, mouldQty, cleaningWindows, maintenanceWindows,
                "续作余量收尾窗口分配");
        if (Objects.nonNull(finishDateOffset)) {
            List<LhShiftConfigVO> finishDayShifts = shifts.stream()
                    .filter(shift -> Objects.equals(shift.getDateOffset(), finishDateOffset))
                    .collect(Collectors.toList());
            Map<Integer, Integer> finishDayCapacities = this.calculateContinuousShiftCapacityMap(context,
                    result, finishDayShifts, startTime, shiftCapacity, lhTimeSeconds, mouldQty,
                    cleaningWindows, maintenanceWindows, "续作收尾日标准班产", false);
            // 同时替换可分配量及随后画像的时间基数，不能用日标准量14条反算班产16条的十九点节点。
            capacities.putAll(finishDayCapacities);
        }
        for (LhShiftConfigVO shift : shifts) {
            int capacity = capacities.getOrDefault(shift.getShiftIndex(), 0);
            capacities.put(shift.getShiftIndex(), capsuleReplacementRuleService
                    .resolveReplacementShiftCapacityUpperLimit(context, result, shift, capacity));
        }
        return capacities;
    }

    /**
     * 取两个时间中的较早值。
     *
     * @param left 左侧时间
     * @param right 右侧时间
     * @return 较早时间
     */
    public Date earlier(Date left, Date right) {
        if (Objects.isNull(left)) {
            return right;
        }
        if (Objects.isNull(right)) {
            return left;
        }
        return left.before(right) ? left : right;
    }

    /**
     * 按SKU日标准产量修正续作班次最大计划量。
     *
     * @param context 排程上下文
     * @param result 续作结果
     * @param shifts 班次列表
     * @param startTime 首个可排开始时间
     * @param shiftCapacity 运行态班产
     * @param lhTimeSeconds 硫化时长
     * @param mouldQty 模台数
     * @param cleaningWindowList 清洗窗口
     * @param maintenanceWindowList 保养窗口
     * @param processName 当前流程
     * @return 修正后的班次最大计划量
     */
    public Map<Integer, Integer> calculateDailyStandardShiftCapacityMap(LhScheduleContext context,
            LhScheduleResult result,
            List<LhShiftConfigVO> shifts,
            Date startTime,
            int shiftCapacity,
            int lhTimeSeconds,
            int mouldQty,
            List<MachineCleaningWindowDTO> cleaningWindowList,
            List<MachineMaintenanceWindowDTO> maintenanceWindowList,
            String processName) {
        return this.calculateContinuousShiftCapacityMap(context, result, shifts, startTime, shiftCapacity,
                lhTimeSeconds, mouldQty, cleaningWindowList, maintenanceWindowList, processName, true);
    }

    /**
     * 计算续作真实可用班产，并按调用场景决定是否叠加日标准量修正。
     *
     * @param context 排程上下文
     * @param result 续作结果及物料、机台身份
     * @param shifts 待计算班次
     * @param startTime 首个可排开始时间
     * @param shiftCapacity 标准班产
     * @param lhTimeSeconds 硫化时长秒数
     * @param mouldQty 实际模数
     * @param cleaningWindowList 清洗占用窗口
     * @param maintenanceWindowList 维修占用窗口
     * @param processName 日志中的业务场景
     * @param applyDailyStandard 是否按日标准量修正，余量收尾当日传入否
     * @return 扣除真实约束后的班次产能
     */
    public Map<Integer, Integer> calculateContinuousShiftCapacityMap(LhScheduleContext context,
            LhScheduleResult result, List<LhShiftConfigVO> shifts, Date startTime, int shiftCapacity,
            int lhTimeSeconds, int mouldQty, List<MachineCleaningWindowDTO> cleaningWindowList,
            List<MachineMaintenanceWindowDTO> maintenanceWindowList, String processName,
            boolean applyDailyStandard) {
        Map<Integer, Integer> rawShiftCapacityMap = new LinkedHashMap<Integer, Integer>(
                CollectionUtils.isEmpty(shifts) ? 0 : shifts.size());
        if (context == null || result == null || CollectionUtils.isEmpty(shifts)
                || shiftCapacity <= 0 || lhTimeSeconds <= 0 || mouldQty <= 0) {
            return rawShiftCapacityMap;
        }
        int dryIceLossQty = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_LOSS_QTY, LhScheduleConstant.DRY_ICE_LOSS_QTY);
        int dryIceDurationHours = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_DURATION_HOURS, LhScheduleConstant.DRY_ICE_DURATION_HOURS);
        int plannedRepairFixedQty = context.getParamIntValue(
                LhScheduleParamConstant.PLANNED_REPAIR_FIXED_QTY, LhScheduleConstant.PLANNED_REPAIR_FIXED_QTY);
        String configPlusShiftType = ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context);
        String remainShiftType = ShiftCapacityResolverUtil.resolveDailyStandardCapacityRemainShiftType(context);
        boolean dailyStandardStructureMatched = applyDailyStandard
                && ShiftCapacityResolverUtil.isDailyStandardCapacityStructureMatched(
                        context, result.getStructureName());
        int remainShiftCapacityUpperLimit =
                ShiftCapacityResolverUtil.resolveDailyStandardRemainShiftCapacityUpperLimit(
                        context, result.getMaterialCode(), shiftCapacity);
        boolean singleControlMachine = LhSingleControlMachineUtil.isConfiguredSingleControlMachine(
                context, result.getLhMachineCode());
        boolean started = false;
        for (LhShiftConfigVO shift : shifts) {
            if (!started) {
                if (startTime != null && !startTime.before(shift.getShiftEndDateTime())
                        && shift != shifts.get(shifts.size() - 1)) {
                    continue;
                }
                started = true;
            }
            ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(context, shift, startTime);
            if (control == null || !control.isCanSchedule()) {
                continue;
            }
            // 仅参数清单内结构允许剩余班次使用独立理论上限；未命中结构始终从原始班产开始扣减。
            int currentShiftCapacity = dailyStandardStructureMatched && !singleControlMachine
                    && ShiftCapacityResolverUtil.isDailyStandardRemainShift(shift, remainShiftType)
                    ? remainShiftCapacityUpperLimit : shiftCapacity;
            int shiftMaxQty = ShiftCapacityResolverUtil.resolveShiftCapacityWithDowntime(
                    context.getDevicePlanShutList(),
                    cleaningWindowList,
                    maintenanceWindowList,
                    result.getLhMachineCode(),
                    control.getEffectiveStartTime(),
                    control.getEffectiveEndTime(),
                    currentShiftCapacity,
                    lhTimeSeconds,
                    mouldQty,
                    ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift),
                    dryIceLossQty,
                    dryIceDurationHours,
                    shift,
                    configPlusShiftType,
                    ScheduleTypeEnum.CONTINUOUS.getCode(),
                    plannedRepairFixedQty);
            shiftMaxQty = ShiftProductionControlUtil.deductCapacityByControl(control, shiftMaxQty, mouldQty);
            rawShiftCapacityMap.put(shift.getShiftIndex(), Math.max(0, shiftMaxQty));
        }
        int dailyStandardQty = ShiftCapacityResolverUtil.resolveDailyStandardQty(context, result.getMaterialCode());
        Map<Integer, Integer> adjustedMap = dailyStandardStructureMatched
                ? ShiftCapacityResolverUtil.adjustShiftPlanQtyMapByDailyStandard(
                        shifts, rawShiftCapacityMap, rawShiftCapacityMap, dailyStandardQty, shiftCapacity,
                        remainShiftCapacityUpperLimit, remainShiftType,
                        singleControlMachine, ScheduleTypeEnum.CONTINUOUS.getCode())
                : rawShiftCapacityMap;
        if (!Objects.equals(rawShiftCapacityMap, adjustedMap)) {
            log.info("日标准产量班次计划量修正, 当前流程: {}, materialCode: {}, structureName: {}, "
                            + "结构是否命中参数: {}, machineCode: {}, 是否单控机台: {}, "
                            + "SKU日标准产量: {}, 班产: {}, 剩余班次理论上限: {}, "
                            + "日标准产量剩余班次参数值: {}, "
                            + "修正前班次计划量: {}, 修正后班次计划量: {}",
                    processName, result.getMaterialCode(), result.getStructureName(),
                    dailyStandardStructureMatched, result.getLhMachineCode(), singleControlMachine,
                    dailyStandardQty, shiftCapacity, remainShiftCapacityUpperLimit,
                    remainShiftType, rawShiftCapacityMap, adjustedMap);
        }
        return adjustedMap;
    }

    /**
     * 获取结果当前的首个开产时间，供续作班次重分配时保留残班起点。
     *
     * @param result 排程结果
     * @param shifts 班次列表
     * @return 首个有效开产时间
     */
    public Date resolveRedistributeStartTime(LhScheduleResult result, List<LhShiftConfigVO> shifts) {
        for (LhShiftConfigVO shift : shifts) {
            Date shiftStartTime = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
            if (shiftStartTime != null) {
                return shiftStartTime;
            }
        }
        return shifts.get(0).getShiftStartDateTime();
    }

    /**
     * 获取首个有排产量的班次索引。
     *
     * @param result 排程结果
     * @return 班次索引；未找到返回 -1
     */
    public int resolveFirstPlannedShiftIndex(LhScheduleResult result) {
        if (result == null) {
            return -1;
        }
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (shiftPlanQty != null && shiftPlanQty > 0) {
                return shiftIndex;
            }
        }
        return -1;
    }

    /**
     * 获取首个有排产量班次的开始时间。
     *
     * @param result 排程结果
     * @return 班次开始时间
     */
    public Date resolveFirstPlannedShiftStartTime(LhScheduleResult result) {
        int firstPlannedShiftIndex = this.resolveFirstPlannedShiftIndex(result);
        return firstPlannedShiftIndex > 0
                ? ShiftFieldUtil.getShiftStartTime(result, firstPlannedShiftIndex) : null;
    }

    /**
     * 解析尚须执行的机台清洗窗口，共用收尾和已提交正规换模取消结论。
     *
     * @param context 当前批次上下文
     * @param machineCode 当前运行侧机台
     * @return 仍参与产能和摘要计算的清洗窗口
     */
    public List<MachineCleaningWindowDTO> resolveMachineCleaningWindowList(LhScheduleContext context, String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        if (machine == null || CollectionUtils.isEmpty(machine.getCleaningWindowList())) {
            return new ArrayList<>();
        }
        return CleaningDispositionResolver.resolveEffectiveWindows(
                context, machineCode, machine.getCleaningWindowList());
    }

    /**
     * 按原续作约定解析机台有效维修窗口。
     *
     * @param context 排程上下文及已有资源状态
     * @param machineCode 运行机台编码
     * @return 按当前物料及资源口径解析的结果
     */
    public List<MachineMaintenanceWindowDTO> resolveMachineMaintenanceWindowList(LhScheduleContext context, String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        List<MachineMaintenanceWindowDTO> maintenanceWindowList = machine == null
                ? new ArrayList<>() : machine.getMaintenanceWindowList();
        // 续作的所有产能预演、实际分配和结束时间推进必须共用同一维修时间轴。
        // 临时维修窗口不会回写 machine，因而不会被误识别为精度保养或占用保养额度。
        return ShiftCapacityResolverUtil.resolveCapacityMaintenanceWindowList(
                context, context.getDevicePlanShutList(), machineCode, maintenanceWindowList);
    }

    /**
     * 解析续作/换活字块结果在排产阶段需要生效的清洗窗口。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param firstProductionStartTime 首个有排产量班次开始时间
     * @return 有效清洗窗口列表
     */
    public List<MachineCleaningWindowDTO> resolveEffectiveCleaningWindowList(LhScheduleContext context,
            LhScheduleResult result,
            Date firstProductionStartTime) {
        if (result == null) {
            return new ArrayList<>(0);
        }
        List<MachineCleaningWindowDTO> cleaningWindowList = this.resolveMachineCleaningWindowList(
                context, result.getLhMachineCode());
        if (CleaningScheduleRuleUtil.shouldSkipCleaningByResultEnding(result)) {
            return new ArrayList<>(0);
        }
        Date switchEndTime = this.resolveCleaningSwitchEndTime(context, result, firstProductionStartTime);
        return new ArrayList<>(MachineCleaningOverlapUtil.excludeOverlapWindows(
                cleaningWindowList, result.getMouldChangeStartTime(), switchEndTime));
    }

    /**
     * 解析清洗与切换重叠过滤使用的切换结束时间。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param firstProductionStartTime 首个有计划量班次开始时间
     * @return 清洗重叠过滤使用的切换结束时间
     */
    public Date resolveCleaningSwitchEndTime(LhScheduleContext context,
            LhScheduleResult result,
            Date firstProductionStartTime) {
        if (Objects.nonNull(result) && Objects.nonNull(result.getMouldChangeStartTime())) {
            return LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(),
                    LhScheduleTimeUtil.getMouldChangeTotalHours(context));
        }
        return firstProductionStartTime;
    }
}
