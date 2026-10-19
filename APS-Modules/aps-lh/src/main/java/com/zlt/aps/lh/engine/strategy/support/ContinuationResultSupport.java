package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ResultDowntimeSummaryUtil;
import com.zlt.aps.lh.util.ResultShiftTimeUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 续作结果的来源、摘要、整组校验及资源登记支持，不决定下机时机。
 *
 * @author APS
 * @date 2026/10/19
 */
@Slf4j
@Component
public class ContinuationResultSupport {

    /**
     * 全续作共用的数量与合法单位入口。
     */
    private final TargetScheduleQtyResolver targetScheduleQtyResolver;

    /**
     * 共用基础容量和窗口解析，不反向依赖续作策略。
     */
    private final ContinuationMachineCapacitySupport capacitySupport;

    /**
     * 沿用原续作的标识常量。
     */
    private static final String CONTINUOUS_SCHEDULE_TYPE = "01";

    /**
     * 兼容既有直接构造的纯上下文计算入口。
     */
    public ContinuationResultSupport() {
        this(new TargetScheduleQtyResolver(), new ContinuationMachineCapacitySupport());
    }

    /**
     * 注入同一数量和产能支持，所有实际状态仍保存在上下文。
     *
     * @param targetScheduleQtyResolver 统一数量解析器
     * @param capacitySupport 基础产能与窗口支持
     */
    @Autowired
    public ContinuationResultSupport(TargetScheduleQtyResolver targetScheduleQtyResolver,
            ContinuationMachineCapacitySupport capacitySupport) {
        this.targetScheduleQtyResolver = targetScheduleQtyResolver;
        this.capacitySupport = capacitySupport;
    }

    /**
     * 只验证结果副本的守恒、摘要及真实时间，校验失败不触碰任何真实结果或消费账本。
     *
     * @param context 上下文
     * @param sku 来源SKU
     * @param proposed 整组结果副本
     * @param quantities 各物理机台分配量
     * @param shifts 真实班次窗口
     * @return 是否通过整体硬校验
     */
    public boolean validatePlan(LhScheduleContext context, SkuScheduleDTO sku,
            Map<LhScheduleResult, LhScheduleResult> proposed, int[] quantities, List<LhShiftConfigVO> shifts) {
        long planned = proposed.values().stream().mapToLong(ShiftFieldUtil::resolveScheduledQty).sum();
        if (planned != java.util.Arrays.stream(quantities).asLongStream().sum() || planned > sku.resolveTargetScheduleQty()) {
            log.warn("续作多机台余量收尾候选未提交，数量不守恒, materialCode: {}, targetQty: {}, plannedQty: {}",
                    sku.getMaterialCode(), sku.resolveTargetScheduleQty(), planned);
            return false;
        }
        for (LhScheduleResult copy : proposed.values()) {
            // 总量守恒不能替代逐班偶数校验，必须在扣账和资源释放前拦截非法分配。
            if (!targetScheduleQtyResolver.isAllocatedShiftQtyValid(context, copy)) {
                return false;
            }
            // 摘要也先在副本上计算，避免提交一半后才发现时间数据不足。
            this.refreshResultSummary(context, copy, shifts);
            Date previousEnd = null;
            for (LhShiftConfigVO shift : shifts) {
                Integer quantity = ShiftFieldUtil.getShiftPlanQty(copy, shift.getShiftIndex());
                if (Objects.isNull(quantity) || quantity <= 0) {
                    continue;
                }
                Date start = ShiftFieldUtil.getShiftStartTime(copy, shift.getShiftIndex());
                Date end = ShiftFieldUtil.getShiftEndTime(copy, shift.getShiftIndex());
                if (Objects.isNull(start) || Objects.isNull(end) || !end.after(start)
                        || end.after(shift.getShiftEndDateTime())
                        || (Objects.nonNull(previousEnd) && start.before(previousEnd))) {
                    log.warn("续作多机台余量收尾候选未提交，生产时间无效, materialCode: {}, machineCode: {}, shiftIndex: {}",
                            sku.getMaterialCode(), copy.getLhMachineCode(), shift.getShiftIndex());
                    return false;
                }
                previousEnd = end;
            }
        }
        return true;
    }

    /**
     * 解析来源SKU对应的续作机台结果列表。
     * <p>用于在不直接持有skuResults的场景（如增机台判断）中，
     * 从排程上下文获取当前续作机台，以判断是否为单控机台。</p>
     *
     * @param context 排程上下文
     * @param sourceSku 来源SKU
     * @return 续作机台结果列表
     */
    public List<LhScheduleResult> resolveContinuousMachineResults(LhScheduleContext context,
            SkuScheduleDTO sourceSku) {
        if (context == null || sourceSku == null
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return new ArrayList<LhScheduleResult>(0);
        }
        List<LhScheduleResult> results = new ArrayList<LhScheduleResult>(4);
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!this.isPureContinuousResult(result)) {
                continue;
            }
            SkuScheduleDTO resultSourceSku = this.resolveResultSourceSku(context, result);
            if (resultSourceSku == null
                    || resultSourceSku.getDailyPlanQuotaMap() != sourceSku.getDailyPlanQuotaMap()) {
                continue;
            }
            results.add(result);
        }
        return results;
    }

    /**
     * 按业务日聚合排程窗口班次。
     *
     * @param shifts 全窗口班次
     * @return 业务日到班次的映射
     */
    public Map<LocalDate, List<LhShiftConfigVO>> groupShiftsByWorkDate(List<LhShiftConfigVO> shifts) {
        Map<LocalDate, List<LhShiftConfigVO>> shiftMapByDate = new LinkedHashMap<LocalDate, List<LhShiftConfigVO>>(4);
        if (CollectionUtils.isEmpty(shifts)) {
            return shiftMapByDate;
        }
        for (LhShiftConfigVO shift : shifts) {
            if (shift == null || shift.getWorkDate() == null) {
                continue;
            }
            LocalDate workDate = shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            shiftMapByDate.computeIfAbsent(workDate, key -> new ArrayList<LhShiftConfigVO>(4)).add(shift);
        }
        return shiftMapByDate;
    }

    /**
     * 识别续作结果列表中属于双模SKU单控整机的机台编码集合。
     * <p>双模SKU降模时L/R必须同步保留或释放；该方法按结果来源SKU的冻结模式识别整机组。</p>
     *
     * @param context 排程上下文
     * @param skuResults 同组续作结果列表
     * @return 双模SKU单控整机机台编码集合
     */
    public Set<String> resolveWholeSingleControlMachineCodes(LhScheduleContext context,
            List<LhScheduleResult> skuResults) {
        Set<String> machineCodes = new HashSet<String>(4);
        if (context == null || CollectionUtils.isEmpty(skuResults)) {
            return machineCodes;
        }
        for (LhScheduleResult result : skuResults) {
            if (result == null || StringUtils.isEmpty(result.getLhMachineCode())) {
                continue;
            }
            if (!LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, result.getLhMachineCode())) {
                continue;
            }
            // 通过结果反查sourceSku,判断是否为正规SKU整机粒度
            SkuScheduleDTO sourceSku = this.resolveResultSourceSku(context, result);
            if (sourceSku != null && LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sourceSku)) {
                machineCodes.add(result.getLhMachineCode());
            }
        }
        return machineCodes;
    }

    /**
     * 判断指定机台在该业务日是否仍存在正计划班次。
     * <p>用于停产保机/释放边界置零后判断是否回滚已排机台统计，
     * 同机台同结构的其他结果仍有量时不允许回滚，避免机台数统计漏计。</p>
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param zeroedResult 被置零的续作结果
     * @param workDate 业务日期
     * @return true-该机台该业务日仍有正计划班次；false-已无正计划班次
     */
    public boolean isMachineStillScheduledOnWorkDate(LhScheduleContext context,
            List<LhShiftConfigVO> shifts,
            LhScheduleResult zeroedResult,
            LocalDate workDate) {
        if (Objects.isNull(context) || Objects.isNull(zeroedResult)
                || StringUtils.isEmpty(zeroedResult.getLhMachineCode())
                || Objects.isNull(workDate)
                || CollectionUtils.isEmpty(context.getScheduleResultList())
                || CollectionUtils.isEmpty(shifts)) {
            return false;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.isNull(result)
                    || !StringUtils.equals(zeroedResult.getLhMachineCode(), result.getLhMachineCode())
                    || !StringUtils.equals(StringUtils.trimToEmpty(zeroedResult.getStructureName()),
                    StringUtils.trimToEmpty(result.getStructureName()))) {
                continue;
            }
            for (LhShiftConfigVO shift : shifts) {
                if (Objects.isNull(shift) || !workDate.equals(capacitySupport.resolveShiftWorkDate(shift))) {
                    continue;
                }
                Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                if (Objects.nonNull(planQty) && planQty > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 将当前仍在停产保机的机台资源占用延续到窗口末班。
     * <p>有历史产量的结果保留原班次量，只延长机台/模具占用结束时间；整窗零量结果同时补齐零量续作状态。</p>
     *
     * @param context 排程上下文
     * @param result 停产保机结果
     * @param shifts 排程窗口班次
     */
    public void extendContinuousStopHoldOccupancyToWindowEnd(LhScheduleContext context,
            LhScheduleResult result,
            List<LhShiftConfigVO> shifts) {
        if (Objects.isNull(context) || Objects.isNull(result)) {
            return;
        }
        if (Objects.isNull(result.getDailyPlanQty()) || result.getDailyPlanQty() <= 0) {
            this.retainContinuousStopHoldZeroResult(context, result, shifts);
            return;
        }
        Date occupiedEndTime = CollectionUtils.isEmpty(shifts)
                ? context.getWindowEndDate() : shifts.get(shifts.size() - 1).getShiftEndDateTime();
        if (Objects.isNull(occupiedEndTime)) {
            return;
        }
        result.setSpecEndTime(occupiedEndTime);
        result.setTdaySpecEndTime(occupiedEndTime);
    }

    /**
     * 保留整个窗口均为零产量的停产保机续作结果。
     *
     * @param context 排程上下文
     * @param result 停产保机结果
     * @param shifts 排程窗口班次
     */
    public void retainContinuousStopHoldZeroResult(LhScheduleContext context,
            LhScheduleResult result,
            List<LhShiftConfigVO> shifts) {
        Date occupiedEndTime = CollectionUtils.isEmpty(shifts)
                ? context.getWindowEndDate() : shifts.get(shifts.size() - 1).getShiftEndDateTime();
        result.setDailyPlanQty(0);
        result.setProductionStatus("0");
        result.setIsEnd("0");
        result.setSpecEndTime(occupiedEndTime);
        result.setTdaySpecEndTime(occupiedEndTime);
        ResultDowntimeSummaryUtil.clearDowntimeSummary(result);
    }

    /**
     * 计算规格收尾时间（最后一个有计划量班次中，完成剩余量所需的时间点）
     *
     * @param context 排程上下文及已有资源状态
     * @param result 当前排程结果
     * @param shifts 本次完整班次窗口
     * @param lhTimeSeconds 硫化周期秒数
     * @param mouldQty 实际模具数量
     * @param isEnding 是否使用当前收尾上下文
     * @return 按当前物料及资源口径解析的结果
     */
    public Date calcSpecEndTime(LhScheduleContext context,
            LhScheduleResult result,
            List<LhShiftConfigVO> shifts,
            int lhTimeSeconds,
            int mouldQty,
            boolean isEnding) {
        if (!isEnding) {
            return null;
        }
        // 找到最后一个有计划量的班次，按真实产量推导完工时刻，避免被班次结束时刻放大。
        for (int i = shifts.size() - 1; i >= 0; i--) {
            LhShiftConfigVO shift = shifts.get(i);
            Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            Date shiftStartTime = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
            Date shiftEndTime = ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex());
            if (shiftPlanQty == null || shiftPlanQty <= 0 || shiftStartTime == null) {
                continue;
            }
            // 正式落班或数量修改入口已经计算完成时间，不再使用另一套周期口径二次推导。
            if (Objects.nonNull(shiftEndTime)) {
                return shiftEndTime;
            }
            return this.resolveResultShiftEndTime(context, result, shift, shiftStartTime, shiftPlanQty);
        }
        return null;
    }

    /**
     * 刷新结果行的汇总计划量和收尾时间。
     * <p>停产保机结果的结束时间表示机台和模具物理占用边界，不等同于最后正计划班次的生产完成时间。
     * 因此每次汇总后都必须恢复保机占用，避免最终日计划账本同步覆盖窗口末班边界。</p>
     *
     * @param result 排程结果
     * @param shifts 班次列表
     *
     * @param context 排程上下文及已有资源状态
     */
    public void refreshResultSummary(LhScheduleContext context, LhScheduleResult result, List<LhShiftConfigVO> shifts) {
        if (result == null) {
            return;
        }
        ShiftFieldUtil.syncDailyPlanQty(result);
        if (result.getDailyPlanQty() == null || result.getDailyPlanQty() <= 0) {
            if (Objects.nonNull(context)
                    && context.isContinuousStopHoldMachine(result.getLhMachineCode())) {
                this.retainContinuousStopHoldZeroResult(context, result, shifts);
                return;
            }
            // 零计划结果不参与完工时刻语义。
            result.setSpecEndTime(null);
            result.setTdaySpecEndTime(null);
            ResultDowntimeSummaryUtil.clearDowntimeSummary(result);
            return;
        }
        int lhTimeSeconds = result.getLhTime() != null ? result.getLhTime() : 0;
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                result.getMouldQty() != null ? result.getMouldQty() : 0);
        this.synchronizeMissingShiftEndTimes(context, result, shifts, lhTimeSeconds, mouldQty);
        Date specEndTime = this.calcSpecEndTime(context, result, shifts, lhTimeSeconds, mouldQty, "1".equals(result.getIsEnd()));
        if (specEndTime == null) {
            // 非收尾结果也要保留可推导完工时刻，避免后续校验出现 specEndTime 缺失。
            specEndTime = this.resolveActualCompletionTime(context, result);
        }
        OffMachineDecision timedDecision = context.getTimedMachineOffDecisionMap().get(result);
        if (Objects.nonNull(timedDecision)) {
            specEndTime = timedDecision.getOccupancyEndTime();
        }
        result.setSpecEndTime(specEndTime);
        result.setTdaySpecEndTime(specEndTime);
        this.syncResultDowntimeSummary(context, result);
        if (Objects.nonNull(context)
                && context.isContinuousStopHoldMachine(result.getLhMachineCode())) {
            this.extendContinuousStopHoldOccupancyToWindowEnd(context, result, shifts);
        }
    }

    /**
     * 补齐续作后置裁剪后缺失的班次结束时间。
     *
     * <p>账本回裁、日标准收敛和目标量收口都会写班次计划量。只要留下正量与开始时间，
     * 结束时间必须同步保留；这里按当前最终数量重新推导，避免有量无完工边界的数据进入S4.6。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 排程窗口班次
     * @param lhTimeSeconds 硫化周期秒数
     * @param mouldQty 机台模台数
     */
    public void synchronizeMissingShiftEndTimes(LhScheduleContext context,
            LhScheduleResult result,
            List<LhShiftConfigVO> shifts,
            int lhTimeSeconds,
            int mouldQty) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || CollectionUtils.isEmpty(shifts)
                || lhTimeSeconds <= 0 || mouldQty <= 0) {
            return;
        }
        for (LhShiftConfigVO shift : shifts) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            Date shiftStartTime = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
            Date shiftEndTime = ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex());
            if (Objects.isNull(planQty) || planQty <= 0
                    || Objects.isNull(shiftStartTime) || Objects.nonNull(shiftEndTime)) {
                continue;
            }
            // 后置补量与正向排量共用班产、有效窗口和模数，禁止改用月计划默认硫化周期。
            Date recalculatedEndTime = this.resolveResultShiftEndTime(
                    context, result, shift, shiftStartTime, planQty);
            ShiftFieldUtil.setShiftPlanQty(
                    result, shift.getShiftIndex(), planQty, shiftStartTime, recalculatedEndTime);
            log.info("续作排产班次结束时间补齐, batchNo: {}, materialCode: {}, machineCode: {}, "
                            + "classNo: class{}, planQty: {}, shiftStartTime: {}, shiftEndTime: {}",
                    context.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(),
                    shift.getShiftIndex(), planQty,
                    LhScheduleTimeUtil.formatDateTime(shiftStartTime),
                    LhScheduleTimeUtil.formatDateTime(recalculatedEndTime));
        }
    }

    /**
     * 记录本轮历史前料释放决定，保持既有应用及过程日志口径。
     *
     * @param context 本批上下文
     * @param event 历史关系
     * @param decision 本轮实际释放决定
     */
    public void logPreviousAlternateReleaseDecision(LhScheduleContext context,
            PreviousAlternatePlanReleaseEvent event, String decision) {
        String detail = String.format("工厂=%s, 批次=%s, 计划ID=%s, 机台=%s, 前物料=%s, 状态=%s, 后物料=%s, 历史下机=%s, 决定=%s",
                context.getFactoryCode(), context.getBatchNo(), event.getPlan().getId(), event.getMachineCode(),
                event.getMaterialCode(), event.getProductStatus(), event.getPlan().getAfterMaterialCode(),
                LhScheduleTimeUtil.formatDateTime(event.getOfflineTime()), decision);
        PriorityTraceLogHelper.appendProcessLog(context, "历史交替本轮释放资格", detail);
        log.info("历史交替本轮释放资格, {}", detail);
    }

    /**
     * 从最后正量班次读取真实生产结束，不将历史交替截止冒充生产时间。
     *
     * @param result 原续作结果
     * @return 最后正量班次结束；无正量时为空
     */
    public Date resolveLastPositiveProductionEndTime(LhScheduleResult result) {
        int shiftIndex = ShiftFieldUtil.resolveLastPlannedShiftIndex(result);
        return shiftIndex > 0 ? ShiftFieldUtil.getShiftEndTime(result, shiftIndex) : null;
    }

    /**
     * 判断结果是否为纯续作结果。
     * <p>续作多机台降模只处理原在机SKU，不能把换活字块结果混入同SKU多机台降模。</p>
     *
     * @param result 排程结果
     * @return true-纯续作结果
     */
    public boolean isPureContinuousResult(LhScheduleResult result) {
        return result != null
                && CONTINUOUS_SCHEDULE_TYPE.equals(result.getScheduleType())
                && !"1".equals(result.getIsTypeBlock());
    }

    /**
     * 按原续作约定解析当前资源与结果状态。
     *
     * @param context 排程上下文及已有资源状态
     * @param result 当前排程结果
     * @return 按当前物料及资源口径解析的结果
     */
    public Date resolveActualCompletionTime(LhScheduleContext context, LhScheduleResult result) {
        if (result == null) {
            return null;
        }
        int lhTimeSeconds = result.getLhTime() != null ? result.getLhTime() : 0;
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                result.getMouldQty() != null ? result.getMouldQty() : 0);
        if (lhTimeSeconds > 0 && mouldQty > 0) {
            Date actualCompletionTime = null;
            for (int shiftIndex = 1; shiftIndex <= 8; shiftIndex++) {
                Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
                Date shiftStartTime = ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
                if (shiftPlanQty == null || shiftPlanQty <= 0 || shiftStartTime == null) {
                    continue;
                }
                Date shiftEndTime = ShiftFieldUtil.getShiftEndTime(result, shiftIndex);
                final int currentShiftIndex = shiftIndex;
                Date shiftCompletionTime = shiftEndTime;
                if (Objects.isNull(shiftCompletionTime)) {
                    LhShiftConfigVO shift = context.getScheduleWindowShifts().stream()
                            .filter(currentShift -> Objects.equals(currentShift.getShiftIndex(), currentShiftIndex))
                            .findFirst().orElse(null);
                    shiftCompletionTime = this.resolveResultShiftEndTime(
                            context, result, shift, shiftStartTime, shiftPlanQty);
                }
                if (Objects.isNull(shiftCompletionTime)) {
                    continue;
                }
                if (actualCompletionTime == null || shiftCompletionTime.after(actualCompletionTime)) {
                    actualCompletionTime = shiftCompletionTime;
                }
            }
            if (actualCompletionTime != null) {
                return actualCompletionTime;
            }
        }
        return result.getSpecEndTime();
    }

    /**
     * 获取机台真实精度保养窗口，仅供停机摘要使用。
     *
     * @param context 排程上下文
     * @param machineCode 机台编号
     * @return 真实精度保养窗口，不包含容量计算专用的计划性维修窗口
     */
    public List<MachineMaintenanceWindowDTO> resolveActualMachineMaintenanceWindowList(
            LhScheduleContext context, String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        return machine == null || CollectionUtils.isEmpty(machine.getMaintenanceWindowList())
                ? new ArrayList<>() : machine.getMaintenanceWindowList();
    }

    /**
     * 按原续作约定解析当前资源与结果状态。
     *
     * @param context 排程上下文及已有资源状态
     * @param machineCode 运行机台编码
     * @return 按当前物料及资源口径解析的结果
     */
    public List<MdmDevicePlanShut> resolveMachineShutdownWindowList(LhScheduleContext context, String machineCode) {
        if (context == null || CollectionUtils.isEmpty(context.getDevicePlanShutList())
                || StringUtils.isEmpty(machineCode)) {
            return new ArrayList<>();
        }
        List<MdmDevicePlanShut> shutdownWindowList = new ArrayList<>(4);
        for (MdmDevicePlanShut planShut : context.getDevicePlanShutList()) {
            if (planShut != null && StringUtils.equals(machineCode, planShut.getMachineCode())) {
                shutdownWindowList.add(planShut);
            }
        }
        return shutdownWindowList;
    }

    /**
     * 按原续作约定解析当前资源与结果状态。
     *
     * @param context 排程上下文及已有资源状态
     * @param result 当前排程结果
     */
    public void syncResultDowntimeSummary(LhScheduleContext context, LhScheduleResult result) {
        if (context == null || result == null) {
            return;
        }
        Date firstPlannedShiftStartTime = capacitySupport.resolveFirstPlannedShiftStartTime(result);
        if (firstPlannedShiftStartTime == null || result.getSpecEndTime() == null) {
            ResultDowntimeSummaryUtil.clearDowntimeSummary(result);
            return;
        }
        List<LhShiftConfigVO> scheduleWindowShifts = context.getScheduleWindowShifts();
        ResultDowntimeSummaryUtil.fillDowntimeSummary(
                result,
                this.resolveActualMachineMaintenanceWindowList(context, result.getLhMachineCode()),
                capacitySupport.resolveEffectiveCleaningWindowList(context, result, firstPlannedShiftStartTime),
                this.resolveMachineShutdownWindowList(context, result.getLhMachineCode()),
                scheduleWindowShifts);
        // 清洗与普通换模重叠时只执行换模，有效清洗窗口已剔除该清洗；这里用原始全量清洗窗口
        // 按真实换模8h窗口补写“清洗+换模”备注，与新增排产口径保持一致。
        Date mouldChangeCompleteTime = Objects.nonNull(result.getMouldChangeStartTime())
                ? LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(),
                LhScheduleTimeUtil.getMouldChangeTotalHours(context)) : firstPlannedShiftStartTime;
        ResultDowntimeSummaryUtil.appendCleaningMouldChangeAnalysis(
                result,
                capacitySupport.resolveMachineCleaningWindowList(context, result.getLhMachineCode()),
                result.getMouldChangeStartTime(),
                mouldChangeCompleteTime,
                scheduleWindowShifts);
    }

    /**
     * 登记续作阶段释放的机台，供S4.4换活字块识别和S4.5新增选机降低优先级使用。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @param materialCode 续作SKU物料编码
     * @param reason 释放原因
     */
    public void registerReleasedContinuousMachine(LhScheduleContext context, String machineCode,
            String materialCode, String reason) {
        if (context == null || StringUtils.isEmpty(machineCode)) {
            return;
        }
        boolean added = context.getReleasedContinuousMachineCodeSet().add(machineCode);
        if (added) {
            log.info("登记续作释放机台, materialCode: {}, machineCode: {}, reason: {}, "
                            + "effect: S4.4换活字块可识别，S4.5新增选机仅降优先级",
                    materialCode, machineCode, reason);
        }
        // 双模SKU释放时配对侧必须同步释放，不允许只释放单边给其他SKU使用。
        this.registerWholeSingleControlPairReleaseIfNeeded(context, machineCode, materialCode, reason);
    }

    /**
     * 双模SKU单控整机释放时同步释放配对侧。
     * <p>是否同步只读取冻结模式；单模独立释放，双模确保L/R同进同出。</p>
     *
     * @param context 排程上下文
     * @param machineCode 当前释放机台编码
     * @param materialCode 续作SKU物料编码
     * @param reason 释放原因
     */
    public void registerWholeSingleControlPairReleaseIfNeeded(LhScheduleContext context,
            String machineCode,
            String materialCode,
            String reason) {
        if (context == null || StringUtils.isEmpty(machineCode) || StringUtils.isEmpty(materialCode)) {
            return;
        }
        if (!LhSingleControlMachineUtil.isConfiguredSingleControlMachine(context, machineCode)) {
            return;
        }
        // 通过续作SKU列表反查物料对应的SKU类型,判断是否为正规SKU整机粒度
        if (!this.isMaterialWholeSingleControlSku(context, materialCode)) {
            return;
        }
        String pairMachineCode = LhSingleControlMachineUtil.resolvePairMachineCode(machineCode);
        if (StringUtils.isEmpty(pairMachineCode)) {
            return;
        }
        boolean pairAdded = context.getReleasedContinuousMachineCodeSet().add(pairMachineCode);
        if (pairAdded) {
            log.info("双模SKU单控整机同步释放配对侧, materialCode: {}, machineCode: {}, pairMachineCode: {}, reason: {}",
                    materialCode, machineCode, pairMachineCode, reason);
        }
    }

    /**
     * 判断指定物料是否冻结为双模整机粒度。
     * <p>从续作和新增列表查找对应SKU并读取本次排程快照，不再按SKU类型判断。</p>
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @return true-双模整机粒度
     */
    public boolean isMaterialWholeSingleControlSku(LhScheduleContext context, String materialCode) {
        if (context == null || StringUtils.isEmpty(materialCode)) {
            return false;
        }
        if (!CollectionUtils.isEmpty(context.getContinuousSkuList())) {
            for (SkuScheduleDTO sku : context.getContinuousSkuList()) {
                if (sku != null && StringUtils.equals(materialCode, sku.getMaterialCode())
                        && LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)) {
                    return true;
                }
            }
        }
        if (!CollectionUtils.isEmpty(context.getNewSpecSkuList())) {
            for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
                if (sku != null && StringUtils.equals(materialCode, sku.getMaterialCode())
                        && LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 解析排程结果对应的来源SKU。
     * <p>优先命中运行态映射；未注册时回退到物料编码查找，兼容旧测试夹具。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @return 来源SKU
     */
    public SkuScheduleDTO resolveResultSourceSku(LhScheduleContext context, LhScheduleResult result) {
        if (context == null || result == null) {
            return null;
        }
        SkuScheduleDTO sourceSku = context.getScheduleResultSourceSkuMap().get(result);
        if (sourceSku != null) {
            return sourceSku;
        }
        return this.findSkuDto(context, result.getMaterialCode(), result.getProductStatus());
    }

    /**
     * 在所有SKU列表中查找指定materialCode的SKU
     *
     * @param context 排程上下文及已有资源状态
     * @param materialCode 物料编码
     * @return 按当前物料及资源口径解析的结果
     */
    public SkuScheduleDTO findSkuDto(LhScheduleContext context, String materialCode) {
        if (context == null || StringUtils.isEmpty(materialCode)) {
            return null;
        }
        for (SkuScheduleDTO sku : context.getContinuousSkuList()) {
            if (materialCode.equals(sku.getMaterialCode())) {
                return sku;
            }
        }
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            if (materialCode.equals(sku.getMaterialCode())) {
                return sku;
            }
        }
        if (!CollectionUtils.isEmpty(context.getStructureSkuMap())) {
            for (List<SkuScheduleDTO> skuList : context.getStructureSkuMap().values()) {
                if (CollectionUtils.isEmpty(skuList)) {
                    continue;
                }
                for (SkuScheduleDTO sku : skuList) {
                    if (materialCode.equals(sku.getMaterialCode())) {
                        return sku;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 按物料编码与产品状态精确查找续作来源SKU。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @return 精确匹配的SKU，未找到返回null
     */
    public SkuScheduleDTO findSkuDto(LhScheduleContext context,
            String materialCode,
            String productStatus) {
        if (context == null || StringUtils.isEmpty(materialCode)) {
            return null;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(materialCode, productStatus);
        SkuScheduleDTO indexedSku = context.getAllSkuScheduleDtoMap().get(skuKey);
        if (indexedSku != null) {
            return indexedSku;
        }
        for (SkuScheduleDTO sku : context.getContinuousSkuList()) {
            if (this.isSameSku(materialCode, productStatus, sku)) {
                return sku;
            }
        }
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            if (this.isSameSku(materialCode, productStatus, sku)) {
                return sku;
            }
        }
        return null;
    }

    /**
     * 判断物料和产品状态是否同时一致。
     *
     * @param materialCode 物料编码
     * @param productStatus 产品状态
     * @param sku 待比较SKU
     * @return true-同一业务SKU
     */
    public boolean isSameSku(String materialCode, String productStatus, SkuScheduleDTO sku) {
        return sku != null
                && StringUtils.equals(materialCode, sku.getMaterialCode())
                && StringUtils.equals(StringUtils.trimToEmpty(productStatus),
                StringUtils.trimToEmpty(sku.getProductStatus()));
    }

    /**
     * 数量修改后的完成时间统一沿用本策略的有效清洗、维修和胶囊窗口。
     *
     * @param context 排程上下文
     * @param result 当前结果
     * @param shift 真实班次
     * @param startTime 实际开始时间
     * @param planQty 当前最终数量
     * @return 相同班产口径的完成时间
     */
    public Date resolveResultShiftEndTime(LhScheduleContext context, LhScheduleResult result,
            LhShiftConfigVO shift, Date startTime, int planQty) {
        // 保留本策略已有清洗豁免语义，复用公共入口计算容量和完成时间。
        return ResultShiftTimeUtil.resolveEndTime(context, result, shift, startTime, planQty,
                capacitySupport.resolveEffectiveCleaningWindowList(context, result, capacitySupport.resolveFirstPlannedShiftStartTime(result)),
                capacitySupport.resolveMachineMaintenanceWindowList(context, result.getLhMachineCode()));
    }
}
