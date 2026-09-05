package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.StructureEarlyProductionAdmission;
import com.zlt.aps.lh.component.StructureMinMachineRetentionService;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 新增结构物理机台上限统一准入计算器。
 *
 * <p>普通新增、续作加机台和普通结构提前生产统一按生产占用班次所属业务日读取结构机台上限，
 * 并复用结构收尾服务的班次边界判断计算有效在机数。结构切换、结构收尾提前生产已经由
 * 提前生产中心按未来计划日完成结构规则判断，不重复套用普通结构班次门禁。计算过程只读，
 * 不修改结构索引、排程结果、候选运行态或资源账本。</p>
 *
 * <p>预演阶段仅按“结构+正式目标班次”缓存统计快照，禁止缓存Machine×SKU时间计划；
 * 正式提交使用无缓存复核，兼顾候选扫描性能和提交时点业务正确性。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class StructureMachineLimitAdmissionService {

    /** 班次边界收尾和物理机台去重统一入口 */
    @Resource
    private StructureMinMachineRetentionService structureMinMachineRetentionService;

    /**
     * 计算当前Machine×SKU生产占用班次的结构机台上限准入结果。
     *
     * @param context 排程上下文
     * @param phase 当前排程阶段
     * @param sku 当前SKU
     * @param earlyProductionDecision 当前提前生产准入结论；非提前生产阶段传null
     * @param formalTargetShift 生产占用班次；计件首检存在时为首检开始班次
     * @param machineCode 候选机台编码
     * @param originalPoolDate SKU原始候选池日期
     * @param roundCache 当前运行态版本轻量缓存；正式提交传null
     * @return 只读结构准入结果
     */
    public StructureMachineLimitDecision evaluate(
            LhScheduleContext context,
            DailySchedulePhase phase,
            SkuScheduleDTO sku,
            EarlyProductionDecision earlyProductionDecision,
            LhShiftConfigVO formalTargetShift,
            String machineCode,
            LocalDate originalPoolDate,
            NewSpecProposalRoundCache roundCache) {
        return this.evaluate(
                context, phase, sku, earlyProductionDecision,
                formalTargetShift, null, machineCode,
                originalPoolDate, roundCache);
    }

    /**
     * 按首检或正式生产实际占用时刻计算结构机台上限准入结果。
     *
     * @param context 排程上下文
     * @param phase 当前排程阶段
     * @param sku 当前SKU
     * @param earlyProductionDecision 当前提前生产准入结论
     * @param formalTargetShift 生产占用班次
     * @param productionOccupationStartTime 首检或正式生产实际占用开始时刻
     * @param machineCode 候选机台编码
     * @param originalPoolDate SKU原始候选池日期
     * @param roundCache 当前运行态版本轻量缓存
     * @return 只读结构准入结果
     */
    public StructureMachineLimitDecision evaluate(
            LhScheduleContext context,
            DailySchedulePhase phase,
            SkuScheduleDTO sku,
            EarlyProductionDecision earlyProductionDecision,
            LhShiftConfigVO formalTargetShift,
            Date productionOccupationStartTime,
            String machineCode,
            LocalDate originalPoolDate,
            NewSpecProposalRoundCache roundCache) {
        if (Objects.isNull(sku) || StringUtils.isEmpty(sku.getStructureName())) {
            return this.buildDecision(
                    false, true, phase, null, formalTargetShift, machineCode,
                    sku, originalPoolDate, null, 0, "SKU无结构信息，不执行结构机台上限约束");
        }
        if (this.shouldSkipNormalStructureShiftLimit(
                phase, earlyProductionDecision)) {
            return this.buildDecision(
                    false, true, phase, null, formalTargetShift, machineCode,
                    sku, originalPoolDate, null, 0,
                    "结构切换或结构收尾提前生产不执行普通结构班次机台数门禁");
        }
        if (Objects.isNull(context) || Objects.isNull(formalTargetShift)
                || Objects.isNull(formalTargetShift.getShiftIndex())
                || Objects.isNull(formalTargetShift.getWorkDate())
                || StringUtils.isEmpty(machineCode)) {
            return this.buildDecision(
                    true, false, phase, null, formalTargetShift, machineCode,
                    sku, originalPoolDate, null, 0,
                    "正式开产班次、班次业务日期或候选机台缺失");
        }
        LocalDate businessDate = formalTargetShift.getWorkDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        int structureMachineLimit = context.getStructurePlanMachineCount(
                businessDate, sku.getStructureName());
        if (structureMachineLimit <= 0) {
            return this.buildDecision(
                    true, false, phase, businessDate, formalTargetShift, machineCode,
                    sku, originalPoolDate, null, structureMachineLimit,
                    "正式开产业务日无有效结构计划硫化机台数");
        }
        if (Objects.isNull(context.getStructureShiftInMachineIndex())) {
            return this.buildDecision(
                    true, false, phase, businessDate, formalTargetShift, machineCode,
                    sku, originalPoolDate, null, structureMachineLimit,
                    "结构班次在机索引未初始化");
        }
        String statisticsKey = this.buildStatisticsKey(
                sku.getStructureName(), formalTargetShift.getShiftIndex(),
                productionOccupationStartTime);
        StructureEarlyProductionAdmission statistics = Objects.isNull(roundCache)
                ? null : roundCache.getStructureMachineStatistics(statisticsKey);
        if (Objects.isNull(statistics)) {
            statistics = Objects.isNull(productionOccupationStartTime)
                    ? structureMinMachineRetentionService
                    .resolveEffectiveStructureMachineStatistics(
                            context, sku.getStructureName(), formalTargetShift)
                    : structureMinMachineRetentionService
                    .resolveEffectiveStructureMachineStatisticsAtTime(
                            context, sku.getStructureName(), formalTargetShift,
                            productionOccupationStartTime);
            if (Objects.nonNull(roundCache)) {
                roundCache.putStructureMachineStatistics(statisticsKey, statistics);
            }
        }
        Set<String> effectivePhysicalMachineCodes = Objects.isNull(statistics)
                ? Collections.<String>emptySet()
                : statistics.getScheduledPhysicalMachineCodes();
        String physicalMachineCode =
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        int newMachineDelta = effectivePhysicalMachineCodes.contains(
                physicalMachineCode) ? 0 : 1;
        int effectiveMachineCount = effectivePhysicalMachineCodes.size();
        boolean allowed = effectiveMachineCount + newMachineDelta
                <= structureMachineLimit;
        String reason = allowed
                ? "结构有效在机数与候选新增增量未超过实际开产日上限"
                : new StringBuilder("目标班次结构有效物理机台数加候选增量超过实际开产日上限，")
                .append(effectiveMachineCount).append("+")
                .append(newMachineDelta).append(">")
                .append(structureMachineLimit).toString();
        return new StructureMachineLimitDecision(
                true, allowed, phase, businessDate,
                formalTargetShift.getShiftIndex(),
                formalTargetShift.getShiftStartDateTime(),
                formalTargetShift.getShiftEndDateTime(),
                machineCode, physicalMachineCode, sku.getMaterialCode(),
                sku.getStructureName(), originalPoolDate,
                statistics.getRawScheduledPhysicalMachineCodes(),
                statistics.getExcludedEndingPhysicalMachineCodes(),
                effectivePhysicalMachineCodes, newMachineDelta,
                structureMachineLimit, reason);
    }

    /**
     * 解析当前业务日内最早满足结构在线机台数上限的开产时刻。
     *
     * <p>首检计入在线硫化机。当前时刻已满时，按同结构物理机台的真实收尾时刻顺序
     * 逐点复核；只推进生产时间，不改变机台、SKU、模具或日计划。</p>
     *
     * @param context 排程上下文
     * @param phase 当前排程阶段
     * @param sku 当前SKU
     * @param earlyProductionDecision 当前提前生产准入结论
     * @param machineCode 候选机台编码
     * @param requestedStartTime 当前时间轴给出的首检或生产占用开始时刻
     * @param dayShifts 当前业务日可用班次
     * @param roundCache 当前运行态版本轻量缓存
     * @return 最早合法占用时刻；当前业务日没有结构名额时返回null
     */
    public Date resolveEarliestAllowedProductionOccupationTime(
            LhScheduleContext context,
            DailySchedulePhase phase,
            SkuScheduleDTO sku,
            EarlyProductionDecision earlyProductionDecision,
            String machineCode,
            Date requestedStartTime,
            List<LhShiftConfigVO> dayShifts,
            NewSpecProposalRoundCache roundCache) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || StringUtils.isEmpty(sku.getStructureName())
                || StringUtils.isEmpty(machineCode)
                || Objects.isNull(requestedStartTime)
                || CollectionUtils.isEmpty(dayShifts)) {
            return null;
        }
        if (this.shouldSkipNormalStructureShiftLimit(
                phase, earlyProductionDecision)) {
            return requestedStartTime;
        }
        for (LhShiftConfigVO shift : dayShifts) {
            Date allowedTime = this.resolveAllowedTimeWithinShift(
                    context, phase, sku, earlyProductionDecision,
                    machineCode, requestedStartTime, shift, roundCache);
            if (Objects.nonNull(allowedTime)) {
                return allowedTime;
            }
        }
        return null;
    }

    /**
     * 在单个班次内按当前时刻和结构收尾事件顺序查找合法占用时刻。
     *
     * @param context 排程上下文
     * @param phase 当前排程阶段
     * @param sku 当前SKU
     * @param earlyProductionDecision 当前提前生产准入结论
     * @param machineCode 候选机台编码
     * @param requestedStartTime 当前请求时刻
     * @param shift 待扫描班次
     * @param roundCache 当前运行态版本轻量缓存
     * @return 本班最早合法时刻；没有可用结构名额时返回null
     */
    private Date resolveAllowedTimeWithinShift(
            LhScheduleContext context,
            DailySchedulePhase phase,
            SkuScheduleDTO sku,
            EarlyProductionDecision earlyProductionDecision,
            String machineCode,
            Date requestedStartTime,
            LhShiftConfigVO shift,
            NewSpecProposalRoundCache roundCache) {
        if (Objects.isNull(shift) || Objects.isNull(shift.getShiftStartDateTime())
                || Objects.isNull(shift.getShiftEndDateTime())
                || !requestedStartTime.before(shift.getShiftEndDateTime())) {
            return null;
        }
        Date shiftRequestedTime = requestedStartTime.after(
                shift.getShiftStartDateTime())
                ? requestedStartTime : shift.getShiftStartDateTime();
        List<Date> candidateTimeList = new ArrayList<Date>(4);
        candidateTimeList.add(shiftRequestedTime);
        String releaseTimeKey = this.buildReleaseTimeKey(
                sku.getStructureName(), shift.getShiftIndex());
        Map<String, Date> releaseTimeMap = Objects.isNull(roundCache)
                ? null : roundCache.getStructureMachineReleaseTimeMap(releaseTimeKey);
        if (Objects.isNull(releaseTimeMap)) {
            releaseTimeMap = structureMinMachineRetentionService
                    .resolveStructurePhysicalMachineReleaseTimeMap(
                            context, sku.getStructureName(), shift);
            if (Objects.nonNull(roundCache)) {
                roundCache.putStructureMachineReleaseTimeMap(
                        releaseTimeKey, releaseTimeMap);
            }
        }
        releaseTimeMap.values().stream()
                .filter(Objects::nonNull)
                .filter(releaseTime -> releaseTime.after(shiftRequestedTime))
                .filter(releaseTime -> releaseTime.before(shift.getShiftEndDateTime()))
                .sorted()
                .distinct()
                .forEach(candidateTimeList::add);
        for (Date candidateTime : candidateTimeList) {
            StructureMachineLimitDecision decision = this.evaluate(
                    context, phase, sku, earlyProductionDecision,
                    shift, candidateTime, machineCode, null, roundCache);
            if (Objects.nonNull(decision) && decision.isAllowed()) {
                return candidateTime;
            }
        }
        return null;
    }

    /**
     * 输出结构机台上限准入日志。
     *
     * @param checkStage 预演拒绝或提交复核阶段
     * @param decision 结构准入结果
     * @param remainingMachineCount 拒绝后剩余机台机会
     * @param machineCompetitionBlocked 是否全局阻断机台竞争
     */
    public void logDecision(
            String checkStage,
            StructureMachineLimitDecision decision,
            Integer remainingMachineCount,
            Boolean machineCompetitionBlocked) {
        if (Objects.isNull(decision) || !decision.isApplicable()) {
            return;
        }
        log.info("结构机台上限准入, checkStage: {}, phase: {}, businessDate: {}, "
                        + "productionOccupationShift: class{}, shiftStartTime: {}, shiftEndTime: {}, "
                        + "machineCode: {}, physicalMachineCode: {}, materialCode: {}, "
                        + "structureName: {}, originalPoolDate: {}, rawMachineCount: {}, "
                        + "excludedEndingMachines: {}, effectiveMachineCount: {}, "
                        + "newMachineDelta: {}, structureMachineLimit: {}, allowed: {}, reason: {}, "
                        + "remainingMachineCount: {}, machineCompetitionBlocked: {}",
                checkStage, decision.getPhase(), decision.getBusinessDate(),
                decision.getFormalTargetShiftIndex(), decision.getShiftStartTime(),
                decision.getShiftEndTime(), decision.getMachineCode(),
                decision.getPhysicalMachineCode(), decision.getMaterialCode(),
                decision.getStructureName(), decision.getOriginalPoolDate(),
                decision.getRawMachineCount(),
                decision.getExcludedEndingPhysicalMachineCodes(),
                decision.getEffectiveMachineCount(), decision.getNewMachineDelta(),
                decision.getStructureMachineLimit(), decision.isAllowed(),
                decision.getReason(), remainingMachineCount,
                machineCompetitionBlocked);
    }

    private StructureMachineLimitDecision buildDecision(
            boolean applicable,
            boolean allowed,
            DailySchedulePhase phase,
            LocalDate businessDate,
            LhShiftConfigVO formalTargetShift,
            String machineCode,
            SkuScheduleDTO sku,
            LocalDate originalPoolDate,
            StructureEarlyProductionAdmission statistics,
            int structureMachineLimit,
            String reason) {
        Set<String> rawMachineCodes = Objects.isNull(statistics)
                ? Collections.<String>emptySet()
                : statistics.getRawScheduledPhysicalMachineCodes();
        Set<String> excludedMachineCodes = Objects.isNull(statistics)
                ? Collections.<String>emptySet()
                : statistics.getExcludedEndingPhysicalMachineCodes();
        Set<String> effectiveMachineCodes = Objects.isNull(statistics)
                ? Collections.<String>emptySet()
                : statistics.getScheduledPhysicalMachineCodes();
        return new StructureMachineLimitDecision(
                applicable, allowed, phase, businessDate,
                Objects.isNull(formalTargetShift)
                        ? null : formalTargetShift.getShiftIndex(),
                Objects.isNull(formalTargetShift)
                        ? null : formalTargetShift.getShiftStartDateTime(),
                Objects.isNull(formalTargetShift)
                        ? null : formalTargetShift.getShiftEndDateTime(),
                machineCode,
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode),
                Objects.isNull(sku) ? null : sku.getMaterialCode(),
                Objects.isNull(sku) ? null : sku.getStructureName(),
                originalPoolDate, rawMachineCodes, excludedMachineCodes,
                effectiveMachineCodes, 0, structureMachineLimit, reason);
    }

    private String buildStatisticsKey(
            String structureName,
            Integer shiftIndex,
            Date productionOccupationStartTime) {
        return new StringBuilder(StringUtils.defaultString(structureName))
                .append("|class")
                .append(Objects.isNull(shiftIndex) ? 0 : shiftIndex)
                .append('|')
                .append(Objects.isNull(productionOccupationStartTime)
                        ? "SHIFT_END" : productionOccupationStartTime.getTime())
                .toString();
    }

    /**
     * 构造结构班次释放时刻缓存键。
     *
     * @param structureName 产品结构
     * @param shiftIndex 班次序号
     * @return 结构与班次复合键
     */
    private String buildReleaseTimeKey(
            String structureName,
            Integer shiftIndex) {
        return new StringBuilder(StringUtils.defaultString(structureName))
                .append("|class")
                .append(Objects.isNull(shiftIndex) ? 0 : shiftIndex)
                .toString();
    }

    /**
     * 判断当前提前生产场景是否应跳过普通结构班次机台数门禁。
     *
     * <p>结构切换和结构收尾已在提前生产中心按未来计划日执行专属规则；这里只排除
     * 这两个明确场景，普通提前生产和续作增机提前生产继续执行现有实时班次硬控。</p>
     *
     * @param phase 当前排程阶段
     * @param decision 当前提前生产准入结论
     * @return true-跳过普通结构班次门禁；false-继续执行现有门禁
     */
    private boolean shouldSkipNormalStructureShiftLimit(
            DailySchedulePhase phase,
            EarlyProductionDecision decision) {
        if (phase != DailySchedulePhase.EARLY_PRODUCTION
                || Objects.isNull(decision)
                || !decision.isEarlyProduction()
                || !decision.isAllowed()) {
            return false;
        }
        return StringUtils.equals(
                EarlyProductionDecision.SCENE_STRUCTURE_SWITCH,
                decision.getSceneType())
                || StringUtils.equals(
                EarlyProductionDecision.SCENE_STRUCTURE_ENDING,
                decision.getSceneType());
    }
}
