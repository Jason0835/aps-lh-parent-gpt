package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.StructureMinMachineRetentionService;
import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.IProductionStrategy;
import com.zlt.aps.lh.engine.strategy.impl.NewSpecProductionStrategy;
import com.zlt.aps.lh.engine.strategy.impl.TypeBlockProductionStrategy;
import com.zlt.aps.lh.engine.strategy.support.TimedMachineOffLoadPreview;
import com.zlt.aps.lh.engine.strategy.support.TimedMachineOffRequest;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 时间下机前一次性有界预演；只返回负荷，所有模拟结果和账本均回滚。 */
@Slf4j
@Service
public class TimedMachineOffLoadPreviewService {
    @Resource
    private ScheduleStrategyFactory strategyFactory;
    @Resource
    private TimedMachineOffShiftService timedMachineOffShiftService;
    @Resource
    private PreviousAlternatePlanReuseService previousAlternatePlanReuseService;
    @Resource
    private TypeBlockProductionStrategy typeBlockProductionStrategy;
    @Resource
    private NewSpecProductionStrategy newSpecProductionStrategy;
    @Resource
    private StructureMinMachineRetentionService structureMinMachineRetentionService;

    /**
     * 每批只模拟一次相关释放机台，候选池和正式时间轴均复用原链路。
     * @param context 余量收尾完成、正式续作尚未扣账的上下文
     * @return 独立负荷快照；无可调整请求时不创建资源快照、不扫描候选
     */
    public TimedMachineOffLoadPreview preview(LhScheduleContext context) {
        List<TimedMachineOffRequest> requests = context.getTimedMachineOffRequests().stream()
                .filter(request -> !timedMachineOffShiftService.hasFixedRelease(context, request)).collect(Collectors.toList());
        if (requests.isEmpty()) {
            return null;
        }
        long started = System.nanoTime();
        LocalDate lastDate = requests.stream().map(TimedMachineOffRequest::getStartDate).max(LocalDate::compareTo).get();
        Date lastMorning = context.getScheduleWindowShifts().stream()
                .filter(shift -> shift.isMorningShift() && lastDate.equals(shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate()))
                .map(shift -> shift.getShiftStartDateTime()).findFirst()
                .orElseThrow(() -> new IllegalStateException("时间下机负荷预演缺少最后需求日早班"));
        Set<String> sourceMachineCodes = context.getScheduleResultList().stream()
                .map(LhScheduleResult::getLhMachineCode).collect(Collectors.toCollection(LinkedHashSet::new));
        sourceMachineCodes.addAll(context.getPreviousAlternateReleaseEventMap().keySet());
        Set<String> requestPhysicalCodes = requests.stream().flatMap(request -> request.getProfile().getOriginals().stream())
                .map(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<SkuScheduleDTO> sources = new ArrayList<>(context.getContinuousSkuList());
        sources.addAll(context.getNewSpecSkuList());
        sources.addAll(context.getPreviousAlternateNewSpecCandidates());
        sources.addAll(context.getScheduleResultSourceSkuMap().values());
        TimedMachineOffPreviewSnapshot snapshot = new TimedMachineOffPreviewSnapshot(context, sources);
        Map<String, int[]> counts = new LinkedHashMap<>();
        List<TimedMachineOffLoadPreview.ChangeoverEvent> events = new ArrayList<>();
        Map<String, Map<LocalDate, String>> structureHandoffs = new LinkedHashMap<>();
        int scopeSize = 0;
        int proposalCount = 0;
        try {
            context.setTimedMachineOffLoadPreview(true);
            context.setStructureShiftInMachineIndex(null);
            // 先在模拟账本发布现有释放事实，正式上下文仍保留清零前画像和未消费日计划。
            timedMachineOffShiftService.resolveAndApply(context);
            IProductionStrategy continuous = strategyFactory.getProductionStrategy(ScheduleTypeEnum.CONTINUOUS.getCode());
            continuous.finalizeContinuousProduction(context);
            Set<String> scope = sourceMachineCodes.stream().filter(code -> {
                MachineScheduleDTO machine = context.getMachineScheduleMap().get(code);
                return Objects.nonNull(machine) && Objects.nonNull(machine.getEstimatedEndTime())
                        && !machine.getEstimatedEndTime().after(lastMorning);
            }).collect(Collectors.toCollection(LinkedHashSet::new));
            scopeSize = (int) scope.stream().map(LhSingleControlMachineUtil::resolvePhysicalMachineCode).distinct().count();
            if (!scope.isEmpty()) {
                context.setNewSpecMachineResourceScopeCodeSet(scope);
                previousAlternatePlanReuseService.reuse(context);
                typeBlockProductionStrategy.scheduleTypeBlockChange(context);
                // 普通候选只预测其他释放机台的竞争负荷；本请求无后料时不虚构一次换模。
                Set<String> ordinaryScope = scope.stream().filter(code -> !requestPhysicalCodes.contains(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(code)))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (!ordinaryScope.isEmpty()) {
                    context.setNewSpecMachineResourceScopeCodeSet(ordinaryScope);
                    context.setCurrentStep(ScheduleStepEnum.S4_5_NEW_PRODUCTION.getCode());
                    newSpecProductionStrategy.previewTimedMachineOffLoad(context, lastDate,
                            strategyFactory.getMachineMatchStrategy(), strategyFactory.getMouldChangeBalanceStrategy(),
                            strategyFactory.getFirstInspectionBalanceStrategy(), strategyFactory.getCapacityCalculateStrategy());
                }
            }
            context.getDailyMouldChangeCountMap().forEach((date, count) -> counts.put(date, count.clone()));
            events = this.collectChangeoverEvents(context, requestPhysicalCodes);
            proposalCount = events.size();
            structureHandoffs = this.collectStructureHandoffs(context, requests, events);
        } finally {
            snapshot.restore(context);
        }
        String detail = String.format("批次=%s, 请求数=%s, 相关物理机台=%s, 有量交替预演数=%s, "
                        + "负荷=%s, 耗时毫秒=%s, 正式数量和次数账本=已恢复",
                context.getBatchNo(), requests.size(), scopeSize, proposalCount,
                counts.entrySet().stream().map(entry -> entry.getKey() + "=" + Arrays.toString(entry.getValue()))
                        .collect(Collectors.joining(";")), (System.nanoTime() - started) / 1_000_000L);
        log.info("按时间下机负荷预演完成, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "按时间下机负荷预演完成", detail);
        return new TimedMachineOffLoadPreview(counts, events, structureHandoffs);
    }

    /** @param context 模拟结果 @param requestedMachines 可调整物理机台 @return 去重后的成功事件及首次可移动交接 */
    private List<TimedMachineOffLoadPreview.ChangeoverEvent> collectChangeoverEvents(
            LhScheduleContext context, Set<String> requestedMachines) {
        Set<String> eventKeys = new LinkedHashSet<>();
        Set<String> firstHandoffMachines = new LinkedHashSet<>();
        List<TimedMachineOffLoadPreview.ChangeoverEvent> events = new ArrayList<>();
        List<LhScheduleResult> changedResults = context.getScheduleResultList().stream()
                .filter(result -> Objects.nonNull(result.getMouldChangeStartTime()) && ShiftFieldUtil.resolveScheduledQty(result) > 0)
                .sorted(Comparator.comparing(LhScheduleResult::getMouldChangeStartTime)).collect(Collectors.toList());
        for (LhScheduleResult result : changedResults) {
            Date changeTime = result.getMouldChangeStartTime();
            String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode());
            if (!eventKeys.add(physical + "|" + changeTime.getTime() + "|" + result.getMaterialCode() + "|" + result.getProductStatus())) {
                continue;
            }
            int slot = LhScheduleTimeUtil.isMorningShift(context, changeTime) ? 0 : 1;
            if (!LhScheduleTimeUtil.isMorningShift(context, changeTime) && !LhScheduleTimeUtil.isAfternoonShift(context, changeTime)) {
                continue;
            }
            LocalDate date = changeTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            boolean movable = requestedMachines.contains(physical) && firstHandoffMachines.add(physical);
            events.add(new TimedMachineOffLoadPreview.ChangeoverEvent(physical, result.getMaterialCode(),
                    result.getProductStatus(), changeTime, date, slot, movable));
        }
        return events;
    }

    /**
     * 只从完整内核已经排出正量的后料提取接替证据。早班已到结构上限且释放机台不在其中时，
     * 恢复该机早班生产会阻断已验证后料；此时才允许结构接替优先于中位均衡偏好。
     * @param context 完整预演上下文 @param requests 可调整请求 @param events 成功换模事件
     * @return 请求物理机台到各日结构接替证据
     */
    private Map<String, Map<LocalDate, String>> collectStructureHandoffs(LhScheduleContext context,
            List<TimedMachineOffRequest> requests, List<TimedMachineOffLoadPreview.ChangeoverEvent> events) {
        Map<String, Map<LocalDate, String>> handoffs = new LinkedHashMap<>();
        Set<String> changedMachines = events.stream().map(TimedMachineOffLoadPreview.ChangeoverEvent::getPhysicalMachineCode)
                .collect(Collectors.toSet());
        for (TimedMachineOffRequest request : requests) {
            String structure = request.getSourceSku().getStructureName();
            if (Objects.isNull(structure) || structure.isEmpty()) {
                continue;
            }
            String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                    request.getProfile().getOriginals().get(0).getLhMachineCode());
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                LocalDate date = shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (!shift.isMorningShift() || date.isBefore(request.getStartDate())) {
                    continue;
                }
                Set<String> successors = context.getScheduleResultList().stream()
                        .filter(result -> structure.equals(result.getStructureName())
                                && Objects.nonNull(result.getMouldChangeStartTime())
                                && Objects.nonNull(ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()))
                                && ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()) > 0)
                        .map(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                        .filter(machine -> !physical.equals(machine) && changedMachines.contains(machine))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (successors.isEmpty()) {
                    continue;
                }
                Set<String> scheduled = structureMinMachineRetentionService
                        .resolveEffectiveStructureMachineStatistics(context, structure, shift).getScheduledPhysicalMachineCodes();
                int limit = context.getStructurePlanMachineCount(date, structure);
                if (limit > 0 && scheduled.size() == limit && !scheduled.contains(physical)) {
                    handoffs.computeIfAbsent(physical, key -> new LinkedHashMap<>()).put(date,
                            "结构=" + structure + ", 班次=" + shift.getShiftIndex() + ", 已排=" + scheduled.size()
                                    + ", 上限=" + limit + ", 接替机台=" + String.join(",", successors));
                }
            }
        }
        return handoffs;
    }
}
