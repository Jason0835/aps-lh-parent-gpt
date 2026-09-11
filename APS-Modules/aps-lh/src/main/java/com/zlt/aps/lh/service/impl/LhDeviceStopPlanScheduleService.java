package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.CleaningScheduleDateFillItem;
import com.zlt.aps.lh.api.domain.dto.MachineFaultWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.api.enums.CleaningTypeEnum;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.mapper.MdmDevicePlanShutMapper;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeParseException;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 设备停机计划排程公共服务。
 *
 * <p>业务定位：统一识别设备停机计划中的清洗、维修、精度等停机类型，并提供清洗场景需要的
 * 班次归属、喷砂顺延、每日上限日期归属和普通停机列表剥离能力。清洗计划从设备停机表读取后，
 * 后续产能扣减必须只使用已纳入排程的清洗窗口，不能让未纳入的清洗停机记录继续作为普通停机扣产能。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class LhDeviceStopPlanScheduleService {

    /** 启用配置值 */
    private static final int ENABLED = 1;
    /** 多值参数分隔符 */
    private static final String VALUE_SEPARATOR = ",";
    /** 05独立时刻使用严格HH:mm校验，不将非法值替换成隐式默认值。 */
    private static final DateTimeFormatter REPAIR_START_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    /** 设备停机计划 Mapper：用于回填清洗实际排程日期到 T_MDM_DEVICE_PLAN_SHUT.SCHEDULE_DATE */
    @Resource
    private MdmDevicePlanShutMapper mdmDevicePlanShutMapper;

    /** 故障独立登记，禁止混入清洗、精度或切换调度。 */
    @Resource
    private LhTemporaryFaultService temporaryFaultService;

    /**
     * 按来源日期准备05维修及06故障，其他类型保留原有对象和处理规则。
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
                        plan.getId(), plan.getMachineCode(), plan.getMachineStopType(), plan.getBeginDate(), plan.getEndDate());
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
                context.getPlannedRepairSourcePlanMap().putIfAbsent(plan.getId(), plan);
                MdmDevicePlanShut actual = this.buildActualRepairPlan(context,
                        context.getPlannedRepairSourcePlanMap().get(plan.getId()));
                if (Objects.nonNull(actual)) {
                    businessPlans.add(actual);
                }
            }
        }
        return businessPlans;
    }

    /**
     * 按维修独立开始时刻平移原始时长；不覆盖设备计划原始起止字段。
     * @param context 排程上下文
     * @param source 原始05计划
     * @return 仅用于本轮计算的实际维修计划
     */
    private MdmDevicePlanShut buildActualRepairPlan(LhScheduleContext context, MdmDevicePlanShut source) {
        String configuredTime = context.getParamValue(
                LhScheduleParamConstant.PLANNED_REPAIR_START_TIME, LhScheduleConstant.PLANNED_REPAIR_START_TIME);
        LocalTime startTime;
        if (StringUtils.isEmpty(configuredTime)) {
            log.warn("计划性维修开始时刻为空，本条不登记，不阻断排程, 计划ID: {}", source.getId());
            return null;
        }
        try {
            startTime = LocalTime.parse(configuredTime, REPAIR_START_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            log.warn("计划性维修开始时刻配置非法，本条不登记，不阻断排程, 计划ID: {}, 参数: {}, 配置值: {}",
                    source.getId(), LhScheduleParamConstant.PLANNED_REPAIR_START_TIME, configuredTime);
            return null;
        }
        Date actualStart = Date.from(source.getBeginDate().toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate().atTime(startTime).atZone(ZoneId.systemDefault()).toInstant());
        long durationMillis = source.getEndDate().getTime() - source.getBeginDate().getTime();
        MdmDevicePlanShut actual = new MdmDevicePlanShut();
        BeanUtil.copyProperties(source, actual);
        actual.setBeginDate(actualStart);
        actual.setEndDate(new Date(actualStart.getTime() + durationMillis));
        return actual;
    }

    /**
     * 在结果保存事务中回填实际纳入排程的05/06；窗口外计划和不存在的机台不回填。
     * @param context 已完成排程的上下文
     * @return 实际更新条数
     */
    public int batchFillStopScheduleDates(LhScheduleContext context) {
        return this.batchFillStopScheduleDates(this.resolveScheduledStopDates(context));
    }

    /**
     * 回填已确认的来源日期，也供班次9复用其独立保存事务。
     * @param scheduleDates 来源计划主键到实际执行日期
     * @return 实际更新条数
     */
    public int batchFillStopScheduleDates(Map<Long, Date> scheduleDates) {
        int updatedCount = 0;
        for (Map.Entry<Long, Date> entry : scheduleDates.entrySet()) {
            MdmDevicePlanShut update = new MdmDevicePlanShut();
            update.setId(entry.getKey());
            update.setScheduleDate(entry.getValue());
            // 仅更新来源日期，更新条数异常只记录应用日志，不把校验升级为排程中断。
            int affectedRows = mdmDevicePlanShutMapper.updateById(update);
            if (affectedRows != 1) {
                log.warn("设备停机排程日期回填条数异常，不阻断排程, 计划ID: {}, 排程日期: {}, 更新条数: {}",
                        entry.getKey(), entry.getValue(), affectedRows);
            }
            updatedCount += affectedRows;
        }
        return updatedCount;
    }

    /**
     * 计算本轮已锁定设备窗口的回填日期。无SKU产出也可实际安排维修或发生故障。
     * @param context 完成排程的上下文
     * @return 来源计划ID到实际开始日期，不使用本次请求日期替代
     */
    public Map<Long, Date> resolveScheduledStopDates(LhScheduleContext context) {
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
        for (MdmDevicePlanShut source : sources.values()) {
            boolean repair = StringUtils.equals(MachineStopTypeEnum.PLANNED_REPAIR.getCode(), source.getMachineStopType());
            MdmDevicePlanShut actual = repair ? this.buildActualRepairPlan(context, source) : source;
            if (Objects.isNull(actual)) {
                continue;
            }
            boolean machineExists = context.getMachineScheduleMap().keySet().stream()
                    .anyMatch(code -> this.isSamePhysicalMachine(code, source.getMachineCode()));
            boolean inWindow = LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate()).stream()
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
        orderedOccupations.sort(Comparator.comparing(entry -> entry.getValue()[0], Comparator.nullsLast(Date::compareTo)));
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
                finalReadyTime, StringUtils.equals(MachineStopTypeEnum.TEMPORARY_FAULT.getCode(), source.getMachineStopType()));
    }

    /**
     * 读取已安排业务窗口用于日志；故障自身不进入普通业务窗口。
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

    /** @param first 第一机台 @param second 第二机台 @return 是否同一物理机台 */
    private boolean isSamePhysicalMachine(String first, String second) {
        return StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(first),
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(second));
    }

    /**
     * 从设备停机计划中过滤干冰/喷砂清洗候选，并按计划开始时间、机台编码升序返回。
     * <p>清洗计划加载口径与普通设备停机不同：只加载计划开始时间不早于 T 日的候选，
     * 但不要求计划开始时间落在 T～T+2 排程窗口内；T 日及之后的候选按计划时间排序后，
     * 实际清洗开始/结束时间由清洗排程服务按 T～T+2 窗口班次重新安排。</p>
     *
     * @param context 排程上下文
     * @return T 日及之后的清洗类设备停机候选（按计划开始时间、机台编码升序）
     */
    public List<MdmDevicePlanShut> queryCleaningStopPlans(LhScheduleContext context) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getDevicePlanShutList())) {
            return Collections.emptyList();
        }
        List<MdmDevicePlanShut> cleaningStopPlans = new ArrayList<>(context.getDevicePlanShutList().size());
        for (MdmDevicePlanShut planShut : context.getDevicePlanShutList()) {
            // 清洗类停机计划的实际时长由配置参数决定（干冰3h/喷砂含首检12h），不依赖计划 begin/end 时长，
            // 因此允许 begin=end 的清洗计划纳入候选。
            if (Objects.isNull(planShut) || !isCleaningStopType(planShut.getMachineStopType())) {
                continue;
            }
            if (!isValidCleaningStopPlan(planShut)) {
                continue;
            }
            // 服务层再次校验计划日期必须落在本次排程窗口内，防止其他初始化入口误把窗口外清洗计划放入上下文。
            if (!isPlanBeginWithinScheduleWindow(context, planShut.getBeginDate())) {
                log.info("清洗计划日期不在排程窗口内，本次不纳入候选, T日: {}, 机台: {}, 停机类型: {}, 计划开始: {}",
                        LhScheduleTimeUtil.formatDate(context.getScheduleDate()),
                        planShut.getMachineCode(), planShut.getMachineStopType(),
                        LhScheduleTimeUtil.formatDateTime(planShut.getBeginDate()));
                continue;
            }
            cleaningStopPlans.add(planShut);
        }
        cleaningStopPlans.sort(Comparator
                .comparing(MdmDevicePlanShut::getBeginDate, Comparator.nullsLast(Date::compareTo))
                .thenComparing(MdmDevicePlanShut::getMachineCode, Comparator.nullsLast(String::compareTo)));
        return cleaningStopPlans;
    }

    /**
     * 过滤出非清洗类设备停机计划。
     *
     * <p>清洗类停机计划会先被转换成运行态清洗窗口；转换完成后必须从普通停机列表中剥离，
     * 否则超过每日上限或 3 天内收尾被跳过的清洗仍会按普通停机扣减产能。</p>
     *
     * <p>非清洗类停机（含精度校验、润滑、巡检点检、预见性维护、预防性维护、计划性维修、
     * 临时性故障、盘点等）保留在普通停机列表中，后续统一由 {@link ShiftCapacityResolverUtil}
     * 按时间重叠折算扣减班次可生产秒数。其中盘点（{@link MachineStopTypeEnum#TAKE_STOCK}）
     * 只扣时间产能，不触发换模、换活字块、预热等逻辑。</p>
     *
     * @param devicePlanShutList 原始设备停机计划
     * @return 非清洗类设备停机计划
     */
    public List<MdmDevicePlanShut> filterNonCleaningStopPlans(List<MdmDevicePlanShut> devicePlanShutList) {
        if (CollectionUtils.isEmpty(devicePlanShutList)) {
            return Collections.emptyList();
        }
        List<MdmDevicePlanShut> normalStopPlans = new ArrayList<>(devicePlanShutList.size());
        for (MdmDevicePlanShut planShut : devicePlanShutList) {
            if (Objects.isNull(planShut) || isCleaningStopType(planShut.getMachineStopType())) {
                continue;
            }
            normalStopPlans.add(planShut);
        }
        return normalStopPlans;
    }

    /**
     * 判断停机类型是否为干冰/喷砂清洗。
     *
     * @param machineStopType 停机类型编码
     * @return true-清洗类停机；false-普通设备停机
     */
    public boolean isCleaningStopType(String machineStopType) {
        return isDryIceCleaning(machineStopType) || isSandBlastCleaning(machineStopType);
    }

    /**
     * 判断停机类型是否为干冰清洗。
     *
     * @param machineStopType 停机类型编码
     * @return true-干冰清洗
     */
    public boolean isDryIceCleaning(String machineStopType) {
        return StringUtils.equals(MachineStopTypeEnum.DRY_ICE_CLEANING.getCode(), machineStopType);
    }

    /**
     * 判断停机类型是否为喷砂清洗。
     *
     * @param machineStopType 停机类型编码
     * @return true-喷砂清洗
     */
    public boolean isSandBlastCleaning(String machineStopType) {
        return StringUtils.equals(MachineStopTypeEnum.SANDBLASTING_CLEANING.getCode(), machineStopType);
    }

    /**
     * 判断停机类型是否为盘点。
     * <p>盘点停机只扣除对应时间段的可生产计划量，不触发换模、换活字块、首检、预热等逻辑；
     * 盘点期间机台视为已完成预热，盘点结束后可直接进入生产排产。</p>
     *
     * @param machineStopType 停机类型编码
     * @return true-盘点停机；false-非盘点停机
     */
    public boolean isInventoryStopType(String machineStopType) {
        return StringUtils.equals(MachineStopTypeEnum.TAKE_STOCK.getCode(), machineStopType);
    }

    /**
     * 将设备停机类型转换为清洗类型。
     *
     * @param planShut 设备停机计划
     * @return 清洗类型编码；非清洗类型返回 null
     */
    public String resolveCleaningType(MdmDevicePlanShut planShut) {
        if (Objects.isNull(planShut)) {
            return null;
        }
        if (isDryIceCleaning(planShut.getMachineStopType())) {
            return CleaningTypeEnum.DRY_ICE.getCode();
        }
        if (isSandBlastCleaning(planShut.getMachineStopType())) {
            return CleaningTypeEnum.SAND_BLAST.getCode();
        }
        return null;
    }

    /**
     * 解析设备停机计划自身维护的持续时长。
     *
     * @param planShut 设备停机计划
     * @return 持续时长毫秒；计划非法时返回 0
     */
    public long resolvePlanDurationMillis(MdmDevicePlanShut planShut) {
        if (!isValidStopPlan(planShut)) {
            return 0L;
        }
        return planShut.getEndDate().getTime() - planShut.getBeginDate().getTime();
    }

    /**
     * 解析喷砂清洗实际执行开始时间。
     *
     * <p>喷砂固定中班执行：计划不在中班时向后归一到后续第一个中班；若该中班命中喷砂机维保日或周日，
     * 继续向后顺延到下一个满足条件的中班。该方法只处理喷砂自身日期/班次约束，每日 1 台上限由清洗纳入服务判断。</p>
     *
     * @param context 排程上下文
     * @param planBeginTime 计划开始时间
     * @return 实际喷砂中班开始时间
     */
    public Date resolveSandBlastExecutableStartTime(LhScheduleContext context, Date planBeginTime) {
        if (Objects.isNull(planBeginTime)) {
            return null;
        }
        Date candidateStartTime = normalizeToAfternoonShift(context, planBeginTime);
        for (int attempt = 0; attempt < LhScheduleConstant.MAX_SHIFT_SLOT_COUNT * 30; attempt++) {
            if (!isSandBlastForbiddenDate(context, candidateStartTime)) {
                return candidateStartTime;
            }
            log.info("喷砂清洗日期不满足约束，顺延到下一天中班, 原日期: {}, 原因: {}",
                    LhScheduleTimeUtil.formatDate(candidateStartTime),
                    resolveSandBlastForbiddenReason(context, candidateStartTime));
            candidateStartTime = LhScheduleTimeUtil.getAfternoonShiftStart(
                    context, LhScheduleTimeUtil.addDays(candidateStartTime, 1));
        }
        return candidateStartTime;
    }

    /**
     * 解析干冰清洗实际执行开始时间。
     *
     * <p>干冰只能落在早班或中班。计划在早班/中班内时保持原计划时间；计划在早班前归一到当天早班；
     * 计划在夜班或中班后归一到次日早班。</p>
     *
     * @param context 排程上下文
     * @param planBeginTime 计划开始时间
     * @return 实际干冰清洗开始时间
     */
    public Date resolveDryIceExecutableStartTime(LhScheduleContext context, Date planBeginTime) {
        if (Objects.isNull(planBeginTime)) {
            return null;
        }
        if (LhScheduleTimeUtil.isMorningShift(context, planBeginTime)
                || LhScheduleTimeUtil.isAfternoonShift(context, planBeginTime)) {
            return planBeginTime;
        }
        Date morningStart = LhScheduleTimeUtil.getMorningShiftStart(context, planBeginTime);
        if (planBeginTime.before(morningStart)) {
            return morningStart;
        }
        return LhScheduleTimeUtil.getMorningShiftStart(context, LhScheduleTimeUtil.addDays(planBeginTime, 1));
    }

    /**
     * 判断清洗实际开始时间是否仍在排程窗口内。
     *
     * @param context 排程上下文
     * @param cleanStartTime 清洗实际开始时间
     * @return true-在窗口内；false-窗口外
     */
    public boolean isInScheduleWindow(LhScheduleContext context, Date cleanStartTime) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleDate()) || Objects.isNull(cleanStartTime)) {
            return true;
        }
        Date windowStart = LhScheduleTimeUtil.clearTime(context.getScheduleDate());
        Date windowEnd = LhScheduleTimeUtil.addDays(windowStart, LhScheduleTimeUtil.getScheduleDays(context));
        return !cleanStartTime.before(windowStart) && cleanStartTime.before(windowEnd);
    }

    /**
     * 判断指定时间是否归属于早班。
     *
     * @param context 排程上下文
     * @param startTime 开始时间
     * @return true-早班
     */
    public boolean isMorningShift(LhScheduleContext context, Date startTime) {
        return Objects.nonNull(startTime) && LhScheduleTimeUtil.isMorningShift(context, startTime);
    }

    /**
     * 判断指定时间是否归属于中班。
     *
     * @param context 排程上下文
     * @param startTime 开始时间
     * @return true-中班
     */
    public boolean isAfternoonShift(LhScheduleContext context, Date startTime) {
        return Objects.nonNull(startTime) && LhScheduleTimeUtil.isAfternoonShift(context, startTime);
    }

    /**
     * 判断设备停机计划开始时间是否属于本次清洗候选范围。
     *
     * @param context 排程上下文
     * @param planBeginTime 设备停机计划开始时间
     * @return true-计划开始时间落在排程窗口内
     */
    private boolean isPlanBeginWithinScheduleWindow(LhScheduleContext context, Date planBeginTime) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleDate()) || Objects.isNull(planBeginTime)) {
            return true;
        }
        Date windowStartTime = LhScheduleTimeUtil.clearTime(context.getScheduleDate());
        Date windowEndTime = LhScheduleTimeUtil.addDays(windowStartTime, LhScheduleTimeUtil.getScheduleDays(context));
        return !planBeginTime.before(windowStartTime) && planBeginTime.before(windowEndTime);
    }

    /**
     * 批量回填清洗计划排程日期到设备停机计划。
     *
     * <p>在排程结果落库事务内调用：将清洗成功排程的实际清洗日期、或因 SKU 收尾未安排清洗时的收尾日期，
     * 回填到对应设备停机计划（{@code T_MDM_DEVICE_PLAN_SHUT}）的 {@code SCHEDULE_DATE} 字段。
     * 按设备停机计划主键 id 去重（同一计划只回填一次，保留首条），避免重复更新；仅更新非空字段。</p>
     *
     * @param fillList 清洗排程日期回填项列表
     * @return 实际更新记录数（去重后条数）
     */
    public int batchFillCleaningScheduleDate(List<CleaningScheduleDateFillItem> fillList) {
        if (CollectionUtils.isEmpty(fillList)) {
            log.info("清洗排程日期回填跳过，待回填列表为空");
            return 0;
        }
        // 按设备停机计划主键 id 去重，同一计划只回填一次（保留首次出现的回填项）
        Map<Long, CleaningScheduleDateFillItem> dedupMap = new LinkedHashMap<>(fillList.size());
        for (CleaningScheduleDateFillItem fillItem : fillList) {
            if (Objects.isNull(fillItem) || Objects.isNull(fillItem.getPlanId())
                    || Objects.isNull(fillItem.getScheduleDate())) {
                continue;
            }
            dedupMap.putIfAbsent(fillItem.getPlanId(), fillItem);
        }
        if (dedupMap.isEmpty()) {
            log.info("清洗排程日期回填跳过，去重后无可回填记录，原始条数: {}", fillList.size());
            return 0;
        }
        log.info("清洗排程日期回填开始，原始条数: {}, 去重后待回填条数: {}", fillList.size(), dedupMap.size());
        int updatedCount = 0;
        for (CleaningScheduleDateFillItem fillItem : dedupMap.values()) {
            // 设备停机计划排程日期只保留业务自然日，所有清洗处置场景统一归零时分秒。
            Date scheduleDate = LhScheduleTimeUtil.clearTime(fillItem.getScheduleDate());
            // 仅按主键更新 SCHEDULE_DATE，MyBatis-Plus updateById 默认不覆盖 null 字段
            MdmDevicePlanShut updateEntity = new MdmDevicePlanShut();
            updateEntity.setId(fillItem.getPlanId());
            updateEntity.setScheduleDate(scheduleDate);
            mdmDevicePlanShutMapper.updateById(updateEntity);
            updatedCount++;
            log.info("清洗排程日期回填, 设备停机计划ID: {}, 机台: {}, 清洗类型: {}, 回填日期: {}, 原因: {}",
                    fillItem.getPlanId(), fillItem.getMachineCode(), fillItem.getCleanType(),
                    LhScheduleTimeUtil.formatDateTime(scheduleDate), fillItem.getFillReason());
        }
        log.info("清洗排程日期回填完成，实际更新记录数: {}", updatedCount);
        return updatedCount;
    }

    /**
     * 校验清洗类设备停机计划是否有效。
     * <p>清洗类停机计划的实际清洗时长由配置参数决定（干冰3h/喷砂含首检12h），
     * 不依赖计划 begin/end 时长，因此允许 begin=end（零时长计划）纳入候选。</p>
     *
     * @param planShut 设备停机计划
     * @return true-有效清洗候选；false-无效
     */
    private boolean isValidCleaningStopPlan(MdmDevicePlanShut planShut) {
        return Objects.nonNull(planShut)
                && StringUtils.isNotEmpty(planShut.getMachineCode())
                && Objects.nonNull(planShut.getBeginDate())
                && Objects.nonNull(planShut.getEndDate());
    }

    private boolean isValidStopPlan(MdmDevicePlanShut planShut) {
        return Objects.nonNull(planShut)
                && StringUtils.isNotEmpty(planShut.getMachineCode())
                && Objects.nonNull(planShut.getBeginDate())
                && Objects.nonNull(planShut.getEndDate())
                && planShut.getBeginDate().before(planShut.getEndDate());
    }

    private Date normalizeToAfternoonShift(LhScheduleContext context, Date planBeginTime) {
        Date afternoonStart = LhScheduleTimeUtil.getAfternoonShiftStart(context, planBeginTime);
        Date nightStart = LhScheduleTimeUtil.getNightShiftStart(context, planBeginTime);
        if (LhScheduleTimeUtil.isAfternoonShift(context, planBeginTime)) {
            return planBeginTime;
        }
        if (planBeginTime.before(afternoonStart)) {
            return afternoonStart;
        }
        if (!planBeginTime.before(nightStart)) {
            return LhScheduleTimeUtil.getAfternoonShiftStart(context, LhScheduleTimeUtil.addDays(planBeginTime, 1));
        }
        return afternoonStart;
    }

    private boolean isSandBlastForbiddenDate(LhScheduleContext context, Date cleanTime) {
        return isSandBlastMaintenanceDate(context, cleanTime) || isSunday(cleanTime);
    }

    private String resolveSandBlastForbiddenReason(LhScheduleContext context, Date cleanTime) {
        if (isSandBlastMaintenanceDate(context, cleanTime)) {
            return "喷砂机维保日";
        }
        if (isSunday(cleanTime)) {
            return "周日";
        }
        return "未知原因";
    }

    private boolean isSandBlastMaintenanceDate(LhScheduleContext context, Date cleanTime) {
        if (Objects.isNull(context) || Objects.isNull(cleanTime)) {
            return false;
        }
        String maintenanceDates = context.getParamValue(LhScheduleParamConstant.SAND_BLAST_MAINTENANCE_DATES,
                LhScheduleConstant.SAND_BLAST_MAINTENANCE_DATES);
        if (StringUtils.isEmpty(maintenanceDates)) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(cleanTime);
        String dayOfMonth = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        for (String maintenanceDate : maintenanceDates.split(VALUE_SEPARATOR)) {
            if (StringUtils.equals(dayOfMonth, StringUtils.trim(maintenanceDate))) {
                return true;
            }
        }
        return false;
    }

    private boolean isSunday(Date cleanTime) {
        if (Objects.isNull(cleanTime)) {
            return false;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(cleanTime);
        return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
    }
}
