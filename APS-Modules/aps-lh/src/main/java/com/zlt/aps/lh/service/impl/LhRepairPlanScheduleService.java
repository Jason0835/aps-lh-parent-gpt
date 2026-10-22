package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineFaultWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.RepairPlanRequest;
import com.zlt.aps.lh.engine.strategy.support.RepairPlanResult;
import com.zlt.aps.lh.engine.strategy.support.RepairPlanRuntimeState;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;

/**
 * 维修公共编排入口，按原执行阶段准备来源、解析候选、识别故障和汇总保存事实。
 * 不分配生产资源，不扣业务账本，不直接写数据库。
 *
 * @author APS
 * @date 2026/10/21
 */
@Slf4j
@Component
public class LhRepairPlanScheduleService {

    /**
     * 原始故障窗口登记服务。
     */
    @Resource
    private LhTemporaryFaultService temporaryFaultService;
    /**
     * 续作故障迁移判定服务，保持在续作计算前识别。
     */
    @Resource
    private ContinuationTemporaryFaultTransferService continuationTemporaryFaultTransferService;

    /**
     * 在原续作事件准备位置识别故障，不提前执行数量截断或资源释放。
     *
     * @param context 原续作阶段上下文
     */
    public void prepareContinuationEvents(LhScheduleContext context) {
        continuationTemporaryFaultTransferService.prepare(context);
    }

    /**
     * 解析当前候选维修约束，候选组合不缓存，不修改维修来源或生产资源。
     *
     * @param context 当前批次
     * @param request 当前场景及完整切换时间输入
     * @return 独立维修业务结果
     */
    public static RepairPlanResult resolveForScheduling(LhScheduleContext context, RepairPlanRequest request) {
        RepairPlanResult result = LhRepairOverlapResolver.resolveSwitch(context, context.getDevicePlanShutList(),
                request.getMachineCode(), request.getPreviousProductionEndTime(),
                request.getSwitchStartTime(), request.getSwitchEndTime());
        result.setRequest(request.copy());
        result.setFixedMorningQtyLimit(context.getParamIntValue(
                LhScheduleParamConstant.PLANNED_REPAIR_FIXED_QTY, LhScheduleConstant.PLANNED_REPAIR_FIXED_QTY));
        String physicalCode = result.getPhysicalMachineCode();
        List<MdmDevicePlanShut> physicalPlans = context.getRepairPlanRuntimeState().getPhysicalRepairPlanMap()
                .getOrDefault(physicalCode, Collections.emptyList());
        physicalPlans.stream().map(MdmDevicePlanShut::getId).distinct()
                .map(context.getPlannedRepairSourcePlanMap()::get).filter(Objects::nonNull)
                .map(RepairPlanRuntimeState::copySource).forEach(result.getSourcePlans()::add);
        for (MachineMaintenanceWindowDTO window : result.getPlannedRepairItems()) {
            result.getPlannedRepairSourceIds().add(physicalPlans.stream()
                    .filter(plan -> Objects.nonNull(plan.getBeginDate()) && Objects.nonNull(plan.getEndDate()))
                    .filter(plan -> plan.getBeginDate().before(window.getMaintenanceEndTime())
                            && plan.getEndDate().after(window.getMaintenanceStartTime()))
                    .map(MdmDevicePlanShut::getId).filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        }
        context.getRepairPlanRuntimeState().getPhysicalFaultWindowMap()
                .getOrDefault(physicalCode, Collections.emptyList()).stream()
                .map(RepairPlanRuntimeState::copyFault).forEach(result.getFaultWindows()::add);
        result.getCapacityWindows().addAll(LhTemporaryFaultService.projectCapacityWindows(
                result.getFaultWindows(), request.getMachineCode()));
        context.getContinuationTemporaryFaultTransferEventMap().values().stream()
                .filter(event -> StringUtils.equals(physicalCode, event.getOriginalPhysicalMachineCode()))
                .map(RepairPlanRuntimeState::copyEvent).forEach(result.getFaultTransferEvents()::add);
        return result;
    }

    /**
     * 兼容只需要时间约束的消费入口，命中标志与恢复点在一次解析中同时生成。
     *
     * @param context 当前批次
     * @param machineCode 运行侧机台
     * @param previousProductionEndTime 前序生产结束时间
     * @param switchStartTime 切换开始时间
     * @param switchEndTime 切换自身完成时间
     * @return 当前输入对应的独立维修结果
     */
    public static RepairPlanResult resolveForScheduling(LhScheduleContext context, String machineCode,
            Date previousProductionEndTime, Date switchStartTime, Date switchEndTime) {
        return resolveForScheduling(context, machineCode, null, "切换时间约束",
                previousProductionEndTime, switchStartTime, switchEndTime);
    }

    /**
     * 关联候选身份生成维修输入，预演和正式排产使用相同解析。
     *
     * @param context 当前批次
     * @param machineCode 运行侧机台
     * @param sku 候选物料，仅时间约束入口可为空
     * @param scenario 当前消费场景
     * @param previousProductionEndTime 前序生产结束时间
     * @param switchStartTime 切换开始时间
     * @param switchEndTime 切换自身完成时间
     * @return 本次完整输入对应的独立结果
     */
    public static RepairPlanResult resolveForScheduling(LhScheduleContext context, String machineCode,
            SkuScheduleDTO sku, String scenario, Date previousProductionEndTime,
            Date switchStartTime, Date switchEndTime) {
        RepairPlanRequest request = new RepairPlanRequest();
        request.setMachineCode(machineCode);
        request.setScenario(scenario);
        if (sku != null) {
            request.setMaterialCode(sku.getMaterialCode());
            request.setProductStatus(sku.getProductStatus());
        }
        request.setPreviousProductionEndTime(previousProductionEndTime);
        request.setSwitchStartTime(switchStartTime);
        request.setSwitchEndTime(switchEndTime);
        return resolveForScheduling(context, request);
    }

    /**
     * 最终只读汇总设备事件的回填事实，仍由原保存事务执行更新。
     *
     * @param context 已完成排产的上下文
     * @return 不依赖正量生产结果的来源日期清单
     */
    public RepairPlanResult finalizeResults(LhScheduleContext context) {
        RepairPlanResult result = new RepairPlanResult();
        result.setInputVersion(context.getRepairPlanRuntimeState().getConstraintVersion());
        result.setScheduleDateFillMap(this.resolveScheduledStopDates(context));
        return result;
    }

    /**
     * 按来源日期准备05维修及06故障，其他类型保留原有对象和处理规则。
     *
     * @param context 排程上下文
     * @param sourcePlans 本次加载或扩展的设备停机计划
     * @return 普通业务列表，05为实际维修时间，06已剥离为独立禁产窗口
     */
    public List<MdmDevicePlanShut> prepareStopPlans(LhScheduleContext context,
                                                   List<MdmDevicePlanShut> sourcePlans) {
        List<MdmDevicePlanShut> businessPlans = new ArrayList<>(sourcePlans.size());
        Date startDate = LhScheduleTimeUtil.clearTime(context.getScheduleDate());
        for (MdmDevicePlanShut plan : sourcePlans) {
            if (Objects.isNull(plan)) {
                continue;
            }
            boolean repair = StringUtils.equals(MachineStopTypeEnum.PLANNED_REPAIR.getCode(),
                    plan.getMachineStopType());
            boolean fault = StringUtils.equals(MachineStopTypeEnum.TEMPORARY_FAULT.getCode(),
                    plan.getMachineStopType());
            if (!repair && !fault) {
                businessPlans.add(plan);
                continue;
            }
            if (Objects.isNull(plan.getId()) || StringUtils.isEmpty(plan.getMachineCode())
                    || Objects.isNull(plan.getBeginDate()) || Objects.isNull(plan.getEndDate())
                    || !plan.getBeginDate().before(plan.getEndDate())) {
                log.warn("设备停机计划缺少有效主键、机台或起止时间，本条不登记，不阻断排程, 计划ID: {}, 机台: {}, 类型: {}, 开始: {}, 结束: {}",
                        plan.getId(), plan.getMachineCode(), plan.getMachineStopType(),
                        plan.getBeginDate(), plan.getEndDate());
                continue;
            }
            if (plan.getBeginDate().before(startDate)) {
                continue;
            }
            if (fault) {
                // 原始故障严格按自身起止时间登记，不设置机台全局维修标记。
                temporaryFaultService.registerFault(context, plan);
            } else {
                // 原始快照在扩展窗口时继续复用，防止已转换的时间再次作为来源时间。
                context.getPlannedRepairSourcePlanMap().computeIfAbsent(
                        plan.getId(), planId -> RepairPlanRuntimeState.copySource(plan));
                MdmDevicePlanShut actual = LhPlannedRepairResolver.buildActualRepairPlan(context,
                        context.getPlannedRepairSourcePlanMap().get(plan.getId()));
                if (Objects.nonNull(actual)) {
                    businessPlans.add(actual);
                }
            }
        }
        context.getRepairPlanRuntimeState().constraintsChanged();
        return businessPlans;
    }

    /**
     * 计算本轮已锁定设备窗口的回填日期。无SKU产出也可实际安排维修或发生故障。
     *
     * @param context 完成排程的上下文
     * @return 来源计划ID到实际开始日期，不使用本次请求日期替代
     */
    private Map<Long, Date> resolveScheduledStopDates(LhScheduleContext context) {
        Map<Long, MdmDevicePlanShut> sources = new LinkedHashMap<>(context.getPlannedRepairSourcePlanMap());
        for (MachineFaultWindowDTO fault : context.getTemporaryFaultWindowMap().values()) {
            MdmDevicePlanShut source = new MdmDevicePlanShut();
            source.setId(fault.getPlanId());
            source.setMachineCode(fault.getMachineCode());
            source.setMachineStopType(MachineStopTypeEnum.TEMPORARY_FAULT.getCode());
            source.setBeginDate(fault.getStartTime());
            source.setEndDate(fault.getEndTime());
            sources.put(source.getId(), source);
        }
        Map<Long, Date> dates = new LinkedHashMap<>(sources.size());
        Set<String> physicalMachines = context.getMachineScheduleMap().keySet().stream()
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .collect(Collectors.toSet());
        List<LhShiftConfigVO> shifts =
                LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        for (MdmDevicePlanShut source : sources.values()) {
            boolean repair = StringUtils.equals(
                    MachineStopTypeEnum.PLANNED_REPAIR.getCode(), source.getMachineStopType());
            MdmDevicePlanShut actual = repair ? LhPlannedRepairResolver.buildActualRepairPlan(context, source) : source;
            if (Objects.isNull(actual)) {
                continue;
            }
            boolean machineExists = physicalMachines.contains(
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(source.getMachineCode()));
            boolean inWindow = shifts.stream()
                    .anyMatch(shift -> actual.getBeginDate().before(shift.getShiftEndDateTime())
                            && actual.getEndDate().after(shift.getShiftStartDateTime()));
            if (!machineExists || !inWindow) {
                continue;
            }
            Date scheduleDate = LhScheduleTimeUtil.clearTime(actual.getBeginDate());
            dates.put(source.getId(), scheduleDate);
            Date readyTime = repair ? LhScheduleTimeUtil.addMinutes(actual.getEndDate(),
                    LhScheduleTimeUtil.getCapsulePreheatMinutes(context)) : actual.getEndDate();
            // 这里只记录已排业务的重叠证据，不修改任何业务计划或触发补偿。
            this.logStopExecution(context, source, actual, readyTime, scheduleDate);
        }
        return dates;
    }

    /**
     * 输出停机来源、实际窗口和与已排业务的重叠证据；06的预热明确为不适用。
     *
     * @param context 排程上下文
     * @param source 原始计划
     * @param actual 实际执行窗口
     * @param readyTime 本场景的可生产时间
     * @param scheduleDate 回填日期
     */
    private void logStopExecution(LhScheduleContext context, MdmDevicePlanShut source,
            MdmDevicePlanShut actual, Date readyTime, Date scheduleDate) {
        Map<String, Date[]> occupations = this.collectLogOccupations(context, source);
        List<String> overlaps = new ArrayList<>(occupations.size());
        Date finalReadyTime = readyTime;
        List<Map.Entry<String, Date[]>> orderedOccupations = new ArrayList<>(occupations.entrySet());
        orderedOccupations.sort(Comparator.comparing(
                entry -> entry.getValue()[0], Comparator.nullsLast(Date::compareTo)));
        for (Map.Entry<String, Date[]> entry : orderedOccupations) {
            Date[] window = entry.getValue();
            if (Objects.nonNull(window[0]) && Objects.nonNull(window[1])
                    && !window[0].after(finalReadyTime) && window[1].after(actual.getBeginDate())) {
                overlaps.add(entry.getKey());
                if (window[1].after(finalReadyTime)) {
                    finalReadyTime = window[1];
                }
            }
        }
        log.info("设备停机实际安排, 批次: {}, 计划ID: {}, 机台编码: {}, 停机类型: {}, 原计划开始时间: {}, "
                        + "原计划结束时间: {}, 计划日期: {}, 实际开始时间: {}, 实际结束时间: {}, "
                        + "预热结束/本场景可开产时间: {}, 排程日期: {}, 是否存在重叠: {}, 重叠业务类型: {}, "
                        + "最终机台可开产时间: {}, 故障仅扣时间产能: {}",
                context.getBatchNo(), source.getId(), source.getMachineCode(), source.getMachineStopType(),
                source.getBeginDate(), source.getEndDate(), LhScheduleTimeUtil.clearTime(source.getBeginDate()),
                actual.getBeginDate(), actual.getEndDate(), readyTime, scheduleDate, !overlaps.isEmpty(), overlaps,
                finalReadyTime, StringUtils.equals(
                        MachineStopTypeEnum.TEMPORARY_FAULT.getCode(), source.getMachineStopType()));
    }

    /**
     * 读取已安排业务窗口用于日志；故障自身不进入普通业务窗口。
     *
     * @param context 排程上下文
     * @param source 来源停机计划，用于排除事件自身
     * @return 业务类型及实际占用起止
     */
    private Map<String, Date[]> collectLogOccupations(LhScheduleContext context, MdmDevicePlanShut source) {
        String machineCode = source.getMachineCode();
        Map<String, Date[]> occupations = new LinkedHashMap<>(16);
        for (MdmDevicePlanShut plan : context.getDevicePlanShutList()) {
            if (Objects.equals(source.getId(), plan.getId())
                    || !this.isSamePhysicalMachine(machineCode, plan.getMachineCode())) {
                continue;
            }
            Date end = plan.getEndDate();
            if (StringUtils.equals(MachineStopTypeEnum.PLANNED_REPAIR.getCode(), plan.getMachineStopType())) {
                end = LhScheduleTimeUtil.addMinutes(end, LhScheduleTimeUtil.getCapsulePreheatMinutes(context));
            }
            occupations.put("设备停机:" + plan.getMachineStopType() + ":" + plan.getId(),
                    new Date[]{plan.getBeginDate(), end});
        }
        for (MachineFaultWindowDTO fault : context.getTemporaryFaultWindowMap().values()) {
            if (!Objects.equals(source.getId(), fault.getPlanId())
                    && this.isSamePhysicalMachine(machineCode, fault.getMachineCode())) {
                occupations.put("临时故障:" + fault.getPlanId(), new Date[]{fault.getStartTime(), fault.getEndTime()});
            }
        }
        for (MachineScheduleDTO machine : context.getMachineScheduleMap().values()) {
            if (!this.isSamePhysicalMachine(machineCode, machine.getMachineCode())) {
                continue;
            }
            for (MachineMaintenanceWindowDTO window : CollectionUtils.isEmpty(machine.getMaintenanceWindowList())
                    ? Collections.<MachineMaintenanceWindowDTO>emptyList() : machine.getMaintenanceWindowList()) {
                occupations.put("精度/维保:" + machine.getMachineCode() + ":" + occupations.size(),
                        new Date[]{window.getMaintenanceStartTime(), window.getProductionResumeTime()});
            }
            for (MachineCleaningWindowDTO window : CollectionUtils.isEmpty(machine.getCleaningWindowList())
                    ? Collections.<MachineCleaningWindowDTO>emptyList() : machine.getCleaningWindowList()) {
                occupations.put("清洗:" + window.getCleanType() + ":" + occupations.size(),
                        new Date[]{window.getCleanStartTime(), window.getReadyTime()});
            }
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!this.isSamePhysicalMachine(machineCode, result.getLhMachineCode())
                    || Objects.isNull(result.getMouldChangeStartTime())) {
                continue;
            }
            boolean typeBlock = StringUtils.equals(ScheduleTypeEnum.TYPE_BLOCK.getCode(), result.getScheduleType());
            int hours = typeBlock ? LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context)
                    : LhScheduleTimeUtil.getMouldChangeTotalHours(context);
            occupations.put((typeBlock ? "换活字块:" : "换模:") + occupations.size(),
                    new Date[]{result.getMouldChangeStartTime(),
                            LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(), hours)});
        }
        return occupations;
    }

    /**
     * 比较运行侧是否属于同一物理机台。
     *
     * @param first 第一运行侧
     * @param second 第二运行侧
     * @return 是否属于同一物理机台
     */
    private boolean isSamePhysicalMachine(String first, String second) {
        return StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(first),
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(second));
    }
}
