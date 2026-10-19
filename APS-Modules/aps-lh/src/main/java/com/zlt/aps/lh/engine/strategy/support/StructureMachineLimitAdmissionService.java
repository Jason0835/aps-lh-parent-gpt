package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.StructureEarlyProductionAdmission;
import com.zlt.aps.lh.component.EmbryoEndingStructureMachineExemptionPolicy;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 新增结构物理机台上限统一准入计算器。
 *
 * <p>普通新增、续作加机台和普通结构提前生产统一按生产占用班次所属业务日读取结构机台上限，
 * 并复用结构收尾服务的班次边界判断计算有效在机数。试制、量试豁免普通结构上限，
 * 仍按真实占用计入在机统计；豁免不改变资源、首检及结构切换专用名额。结构切换在提前生产和后续正常资源
 * 竞争阶段统一使用S0/S1专用逐班名额；结构收尾提前生产沿用专用准入，两者都不重复套用
 * 普通结构班次门禁。计算过程只读，不修改结构索引、排程结果、候选运行态或资源账本。</p>
 *
 * <p>预演阶段仅按“结构+正式目标班次”缓存统计快照，禁止缓存Machine×SKU时间计划；
 * 正式提交使用无缓存复核，兼顾候选扫描性能和提交时点业务正确性。</p>
 *
 * @author APS
 * @date 2026/10/14
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
     * 试制、量试和成型收尾后的匹配SKU按各自规则豁免数量上限，必要班次和机台信息仍须完整。
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
        if (StructureSwitchSchedulingPolicy.shouldSkipNormalStructureShiftLimit(
                context, sku, phase, earlyProductionDecision)) {
            return this.buildDecision(
                    false, true, phase, null, formalTargetShift, machineCode,
                    sku, originalPoolDate, null, 0,
                    "结构切换专用逐班名额或结构收尾提前生产不执行普通结构班次机台数门禁");
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
        if (PendingSkuUnscheduledRule.isTrialOrMassTrialSku(sku)) {
            // 与原机台续作一致，试制量试不等待普通结构名额，但提交后仍登记真实在机占用。
            if (log.isInfoEnabled()) {
                log.info("试制量试结构机台上限豁免, factoryCode: {}, batchNo: {}, businessDate: {}, "
                                + "materialCode: {}, productStatus: {}, constructionStage: {}, structureName: {}, "
                                + "machineCode: {}, physicalMachineCode: {}, shiftIndex: {}, "
                                + "structureMachineLimit: {}, actualMachineCount: {}",
                        context.getFactoryCode(), context.getBatchNo(), businessDate,
                        sku.getMaterialCode(), sku.getProductStatus(), sku.getConstructionStage(),
                        sku.getStructureName(), machineCode,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode),
                        formalTargetShift.getShiftIndex(), structureMachineLimit,
                        Objects.isNull(context.getStructureShiftInMachineIndex()) ? null
                                : context.getStructureShiftInMachineIndex().resolveRawInMachinePhysicalCodes(
                                        sku.getStructureName(), formalTargetShift.getShiftIndex()).size());
            }
            return this.buildDecision(
                    false, true, phase, businessDate, formalTargetShift, machineCode,
                    sku, originalPoolDate, null, structureMachineLimit,
                    "试制量试豁免普通结构硫化机台数上限，仍计入实际在机统计");
        }
        if (EmbryoEndingStructureMachineExemptionPolicy.isExempt(context, sku.getStructureName(),
                sku.getEmbryoCode(), sku.getStructureName(), formalTargetShift)) {
            StructureEarlyProductionAdmission exemptStatistics = structureMinMachineRetentionService
                    .resolveEffectiveStructureMachineStatistics(context, sku.getStructureName(), formalTargetShift);
            if (log.isInfoEnabled()) {
                log.info("成型收尾SKU结构配额准入豁免, factoryCode: {}, batchNo: {}, businessDate: {}, shiftIndex: {}, "
                                + "structureName: {}, productStatus: {}, embryoCode: {}, materialCode: {}, "
                                + "machineCode: {}, endingShifts: {}",
                        context.getFactoryCode(), context.getBatchNo(), businessDate, formalTargetShift.getShiftIndex(),
                        sku.getStructureName(), sku.getProductStatus(),
                        sku.getEmbryoCode(), sku.getMaterialCode(), machineCode,
                        EmbryoEndingStructureMachineExemptionPolicy.resolveEndingShifts(
                                context, sku.getEmbryoCode()));
            }
            // 只豁免数量门禁，既有结构资格、实际资源、首检和生产时间轴仍由原链路校验。
            return this.buildDecision(true, true, phase, businessDate, formalTargetShift, machineCode,
                    sku, originalPoolDate, exemptStatistics, structureMachineLimit, "成型胎胚收尾后同胎胚同结构不占机台配额");
        }
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
                sku.getStructureName(), formalTargetShift.getShiftIndex());
        StructureEarlyProductionAdmission statistics = Objects.isNull(roundCache)
                ? null : roundCache.getStructureMachineStatistics(statisticsKey);
        if (Objects.isNull(statistics)) {
            statistics = structureMinMachineRetentionService
                    .resolveEffectiveStructureMachineStatistics(
                            context, sku.getStructureName(), formalTargetShift);
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
                effectivePhysicalMachineCodes, newMachineDelta,
                structureMachineLimit, reason);
    }

    /**
     * 从完整排程窗口中的真实占用班次开始解析最早满足结构机台上限的开产时刻。
     * 试制、量试不因普通结构名额后移首检或生产，结构切换专用名额仍继续生效。
     *
     * <p>首检计入在线硫化机。结构机台数按整个班次统计，当前班次已满时不得因班内某台
     * 收尾而提前释放名额；历史首检班次有名额时保留原时刻，只有真实超限才继续扫描
     * 后续班次。每班按自身业务日期读取上限，不改变机台、SKU、模具或日计划。</p>
     *
     * @param context 排程上下文
     * @param phase 当前排程阶段
     * @param sku 当前SKU
     * @param earlyProductionDecision 当前提前生产准入结论
     * @param machineCode 候选机台编码
     * @param requestedStartTime 当前时间轴给出的首检或生产占用开始时刻
     * @param scheduleShifts 按时间升序排列的完整排程窗口班次
     * @param roundCache 当前运行态版本轻量缓存
     * @return 最早合法占用时刻；窗口内没有结构名额时返回null
     */
    public Date resolveEarliestAllowedProductionOccupationTime(
            LhScheduleContext context,
            DailySchedulePhase phase,
            SkuScheduleDTO sku,
            EarlyProductionDecision earlyProductionDecision,
            String machineCode,
            Date requestedStartTime,
            List<LhShiftConfigVO> scheduleShifts,
            NewSpecProposalRoundCache roundCache) {
        if (Objects.isNull(context) || Objects.isNull(sku)
                || StringUtils.isEmpty(sku.getStructureName())
                || StringUtils.isEmpty(machineCode)
                || Objects.isNull(requestedStartTime)
                || CollectionUtils.isEmpty(scheduleShifts)) {
            return null;
        }
        if (PendingSkuUnscheduledRule.isTrialOrMassTrialSku(sku)
                || StructureSwitchSchedulingPolicy.shouldSkipNormalStructureShiftLimit(
                context, sku, phase, earlyProductionDecision)) {
            // 试制量试和结构切换均跳过普通结构名额，实际首检和生产仍须满足S0/S1约束。
            return StructureSwitchSchedulingPolicy.resolveEarliestOccupationTime(
                    context, sku, machineCode, requestedStartTime);
        }
        for (LhShiftConfigVO shift : scheduleShifts) {
            Date allowedTime = this.resolveAllowedTimeWithinShift(
                    context, phase, sku, earlyProductionDecision,
                    machineCode, requestedStartTime, shift, roundCache);
            if (Objects.nonNull(allowedTime)) {
                // 同时满足普通结构上限和结构切换分班上限，不能在两个门禁之间来回丢失限制。
                Date switchAllowedTime = StructureSwitchSchedulingPolicy.resolveEarliestOccupationTime(
                        context, sku, machineCode, allowedTime);
                if (Objects.nonNull(switchAllowedTime) && switchAllowedTime.before(shift.getShiftEndDateTime())) {
                    return switchAllowedTime;
                }
            }
        }
        return null;
    }

    /**
     * 在单个班次内按统一班次机台数口径查找合法占用时刻。
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
        StructureMachineLimitDecision decision = this.evaluate(
                context, phase, sku, earlyProductionDecision,
                shift, shiftRequestedTime, machineCode, null, roundCache);
        return Objects.nonNull(decision) && decision.isAllowed()
                ? shiftRequestedTime : null;
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
                        + "effectiveMachineCount: {}, "
                        + "newMachineDelta: {}, structureMachineLimit: {}, allowed: {}, reason: {}, "
                        + "remainingMachineCount: {}, machineCompetitionBlocked: {}",
                checkStage, decision.getPhase(), decision.getBusinessDate(),
                decision.getFormalTargetShiftIndex(), decision.getShiftStartTime(),
                decision.getShiftEndTime(), decision.getMachineCode(),
                decision.getPhysicalMachineCode(), decision.getMaterialCode(),
                decision.getStructureName(), decision.getOriginalPoolDate(),
                decision.getRawMachineCount(),
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
                originalPoolDate, rawMachineCodes,
                effectiveMachineCodes, 0, structureMachineLimit, reason);
    }

    private String buildStatisticsKey(String structureName,
                                      Integer shiftIndex) {
        return new StringBuilder(StringUtils.defaultString(structureName))
                .append("|class")
                .append(Objects.isNull(shiftIndex) ? 0 : shiftIndex)
                .toString();
    }

}
