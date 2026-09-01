package com.zlt.aps.lh.component;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.CapsuleReplacementTimeWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.SingleControlMachineModeEnum;
import com.zlt.aps.lh.context.LhScheduleConfig;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 胶囊使用次数与换胶囊产能调整公共规则。
 *
 * <p>业务口径：</p>
 * <ul>
 *   <li>判断基数必须是停机、清洗、保养、首检、日计划额度、余量和收尾目标等既有规则
 *   收口后的“扣减前实际可排量”；</li>
 *   <li>本批初始次数取左右模次数最大值，普通双模按单侧生产循环数累计，其他机台沿用现有累计口径；</li>
 *   <li>只有当前次数加扣减前胶囊次数增量严格大于上限时才首次换胶囊，刚好达到上限不触发；</li>
 *   <li>本批首次跨限时，满产班次固定扣减配置量；未满产班次登记换胶囊时间窗口，二者互斥；</li>
 *   <li>L/R整机结果按左右实际量合计一次，结果复制和同班多个结果不得重复累计；</li>
 *   <li>候选预演只能调用无副作用 {@link #previewActualPlanQty}；正式登记统一调用
 *       {@link #applyPreviewedPlanQty} 并校验预演与提交结果一致。</li>
 * </ul>
 *
 * <p>本组件不保存批次状态。所有可变状态均放在 {@link LhScheduleContext}，并且每次正式分配前
 * 都可根据当前结果重建，因此胎胚裁剪、降模、特殊材料置换等删除或回滚结果后不会残留旧贡献。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class CapsuleReplacementRuleService {

    /** 排程结果班次备注 */
    public static final String CAPSULE_REPLACEMENT_ANALYSIS = "换胶囊";

    /** 运行态复合键分隔符 */
    private static final String KEY_SEPARATOR = "::";

    /** 普通双模结果的模台数 */
    private static final int DOUBLE_MOULD_QTY = 2;

    /**
     * 对正式落班候选量执行换胶囊判断、产能调整和次数累计。
     *
     * <p>调用位置必须在现有数量约束全部收口之后、班次计划量写入和SKU余量扣账之前。
     * 返回值才是允许写入结果并消费账本的实际排产量。</p>
     *
     * @param context 排程上下文
     * @param result 当前排程结果
     * @param shift 当前班次
     * @param candidateQty 扣减前实际可排量
     * @param shiftCapacityBeforeReplacement 换胶囊前已按现有停机、清洗、保养、首检和班次管控收口的实际班产
     * @param effectiveStartTime 当前候选的实际开产时间；为空时按结果已有班次开始时间或班次起点处理
     * @param scene 调用场景，用于对账日志
     * @return 换胶囊规则收口后的实际排产量
     */
    public int resolveActualPlanQty(LhScheduleContext context,
                                    LhScheduleResult result,
                                    LhShiftConfigVO shift,
                                    int candidateQty,
                                    int shiftCapacityBeforeReplacement,
                                    Date effectiveStartTime,
                                    String scene) {
        int normalizedCandidateQty = Math.max(0, candidateQty);
        int normalizedShiftCapacity = Math.max(0, shiftCapacityBeforeReplacement);
        if (Objects.isNull(context) || Objects.isNull(result) || Objects.isNull(shift)
                || Objects.isNull(shift.getShiftIndex()) || normalizedCandidateQty <= 0
                || StringUtils.isEmpty(result.getLhMachineCode())) {
            return normalizedCandidateQty;
        }
        if (normalizedShiftCapacity <= 0) {
            log.warn("换胶囊规则未执行，原因: 未取得换胶囊前实际班产, batchNo: {}, materialCode: {}, machineCode: {}, "
                            + "shiftIndex: {}, candidateQty: {}, scene: {}",
                    context.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(),
                    shift.getShiftIndex(), normalizedCandidateQty, scene);
            return normalizedCandidateQty;
        }

        // 每次正式增量落班前按当前结果重建胶囊运行态，确保后置缩量或置换回滚不会污染本次判断。
        rebuildRuntimeState(context, result);

        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                result.getLhMachineCode());
        String shiftKey = buildShiftKey(physicalMachineCode, shift);
        if (StringUtils.isEmpty(shiftKey)) {
            return normalizedCandidateQty;
        }
        initializePhysicalMachineUsage(context, result.getLhMachineCode(), physicalMachineCode);

        int usageUpperLimit = resolveUsageUpperLimit(context);
        int configuredLossQty = resolveChangeLossQty(context);
        boolean wholeSingleControlPair = isWholeSingleControlPairResult(context, result);
        boolean shiftAlreadyReplaced = context.getCapsuleReplacementShiftKeySet().contains(shiftKey);
        String capacityLimitKey = buildCapacityLimitKey(shiftKey, result);
        Integer recordedCapacityLimit = context.getCapsuleReplacementShiftCapacityLimitMap().get(capacityLimitKey);
        if (shiftAlreadyReplaced && Objects.nonNull(recordedCapacityLimit)) {
            /*
             * 同一结果班次首次换胶囊后，后续补量只能使用已扣减上限内的剩余空间。
             * 例如16条候选已收口为14条时，重复补量不得把2条重新补回，也不得再次扣2条。
             */
            int existingPlanQty = resolveShiftPlanQty(result, shift.getShiftIndex());
            normalizedCandidateQty = Math.min(normalizedCandidateQty,
                    Math.max(0, recordedCapacityLimit - existingPlanQty));
            if (normalizedCandidateQty <= 0) {
                return 0;
            }
        }
        int existingPlanQty = resolveShiftPlanQty(result, shift.getShiftIndex());
        int effectiveLossQty = resolveEffectiveLossQty(
                configuredLossQty, wholeSingleControlPair, result.getLhMachineCode());
        int beforeUsage = getMachineRuntimeUsage(context, physicalMachineCode);
        // 使用统一胶囊增量口径判断是否跨限，普通双模按单侧生产循环数计算。
        int candidateUsageIncrement = resolveCapsuleUsageIncrement(
                result, normalizedCandidateQty, wholeSingleControlPair);
        boolean thresholdHandled = context.getCapsuleThresholdHandledMachineSet()
                .contains(physicalMachineCode);
        boolean firstThresholdCrossing = !shiftAlreadyReplaced
                && !thresholdHandled
                && beforeUsage + candidateUsageIncrement > usageUpperLimit;
        int physicalShiftCapacity = resolvePhysicalShiftCapacity(
                normalizedShiftCapacity, wholeSingleControlPair, result.getLhMachineCode());
        int existingPhysicalShiftQty = resolveExistingPhysicalShiftPlanQty(
                context, result, physicalMachineCode, shift.getShiftIndex(), wholeSingleControlPair);
        int candidatePhysicalShiftQty = resolvePhysicalShiftQty(
                normalizedCandidateQty, wholeSingleControlPair, result.getLhMachineCode());
        boolean shiftFullBeforeReplacement = existingPhysicalShiftQty + candidatePhysicalShiftQty
                >= physicalShiftCapacity;
        int actualPlanQty = firstThresholdCrossing && shiftFullBeforeReplacement
                ? Math.max(0, normalizedCandidateQty - effectiveLossQty) : normalizedCandidateQty;

        if (firstThresholdCrossing) {
            // 首次严格跨限后登记物理机台，本批后续班次只累计次数，不再重复触发换胶囊。
            context.getCapsuleThresholdHandledMachineSet().add(physicalMachineCode);
            context.getCapsuleReplacementShiftKeySet().add(shiftKey);
            if (shiftFullBeforeReplacement) {
                // 满产班次只保留既有固定扣量上限，禁止同时追加换胶囊时间窗口。
                context.getCapsuleReplacementShiftCapacityLimitMap().put(
                        capacityLimitKey, existingPlanQty + actualPlanQty);
            } else {
                // 未满产班次不扣量，当前候选生产完成后占用机台换胶囊时长，后续时间轴自然重新归属产能。
                registerCapsuleReplacementTimeWindow(context, result, shift, shiftKey,
                        existingPlanQty + normalizedCandidateQty, normalizedShiftCapacity, effectiveStartTime);
            }
            ShiftFieldUtil.appendShiftAnalysis(
                    result, shift.getShiftIndex(), CAPSULE_REPLACEMENT_ANALYSIS);
        }
        // 满产扣量后的损失不计入胶囊次数；未满产时间模式保留当前候选真实生产量。
        int actualUsageIncrement = resolveCapsuleUsageIncrement(
                result, actualPlanQty, wholeSingleControlPair);
        applyActualUsageIncrement(context, physicalMachineCode, actualUsageIncrement);

        if (firstThresholdCrossing) {
            log.info("换胶囊产能调整, batchNo: {}, scheduleDate: {}, scene: {}, materialCode: {}, "
                            + "machineCode: {}, physicalMachineCode: {}, shiftIndex: {}, 当前机台胶囊次数: {}, "
                            + "胶囊上限: {}, 扣减前结果可排量: {}, 候选胶囊次数增量: {}, 换胶囊前实际班产: {}, "
                            + "班次是否满产: {}, 调整方式: {}, 配置扣减量: {}, 换胶囊时长小时: {}, "
                            + "调整前后计划量: {}/{}，换胶囊开始结束时间: {}/{}，实际胶囊次数增量: {}, "
                            + "本批首次严格跨限: {}, 累计后机台胶囊次数: {}",
                    context.getBatchNo(), LhScheduleTimeUtil.formatDate(context.getScheduleDate()), scene,
                    result.getMaterialCode(), result.getLhMachineCode(), physicalMachineCode,
                    shift.getShiftIndex(), beforeUsage,
                    usageUpperLimit, normalizedCandidateQty, candidateUsageIncrement, normalizedShiftCapacity,
                    shiftFullBeforeReplacement, shiftFullBeforeReplacement ? "扣量" : "延时",
                    configuredLossQty, shiftFullBeforeReplacement
                            ? 0 : resolveReplacementDurationHours(context),
                    normalizedCandidateQty, actualPlanQty,
                    resolveReplacementWindowStartTime(context, shiftKey),
                    resolveReplacementWindowEndTime(context, shiftKey), actualUsageIncrement,
                    true, getMachineRuntimeUsage(context, physicalMachineCode));
        }
        return actualPlanQty;
    }

    /**
     * 无副作用预演正式落班候选的换胶囊调整量。
     *
     * <p>预演使用结果副本，并在结束后恢复胶囊次数、阈值标记、班次上限和时间窗口；
     * 调用方只能把返回值写入Proposal，最终提交仍需调用 {@link #applyPreviewedPlanQty}。</p>
     *
     * @param context 排程上下文
     * @param result 当前排程结果
     * @param shift 当前班次
     * @param candidateQty 扣减前候选量
     * @param shiftCapacityBeforeReplacement 换胶囊前实际班产
     * @param effectiveStartTime 实际开产时间
     * @param scene 调用场景
     * @return 无副作用预演后的实际排产量
     */
    public int previewActualPlanQty(LhScheduleContext context,
                                    LhScheduleResult result,
                                    LhShiftConfigVO shift,
                                    int candidateQty,
                                    int shiftCapacityBeforeReplacement,
                                    Date effectiveStartTime,
                                    String scene) {
        if (Objects.isNull(context) || Objects.isNull(result)) {
            return Math.max(0, candidateQty);
        }
        Map<String, Integer> usageBaseline =
                new LinkedHashMap<String, Integer>(context.getCapsuleRuntimeUsageMap());
        Set<String> shiftKeyBaseline =
                new LinkedHashSet<String>(context.getCapsuleReplacementShiftKeySet());
        Set<String> thresholdBaseline =
                new LinkedHashSet<String>(context.getCapsuleThresholdHandledMachineSet());
        Map<String, Integer> capacityLimitBaseline =
                new LinkedHashMap<String, Integer>(
                        context.getCapsuleReplacementShiftCapacityLimitMap());
        Map<String, CapsuleReplacementTimeWindowDTO> timeWindowBaseline =
                this.copyReplacementTimeWindowMap(
                        context.getCapsuleReplacementTimeWindowMap());
        LhScheduleResult previewResult = new LhScheduleResult();
        BeanUtil.copyProperties(result, previewResult);
        try {
            return this.resolveActualPlanQty(
                    context, previewResult, shift, candidateQty,
                    shiftCapacityBeforeReplacement, effectiveStartTime,
                    scene + "预演");
        } finally {
            context.setCapsuleRuntimeUsageMap(usageBaseline);
            context.setCapsuleReplacementShiftKeySet(shiftKeyBaseline);
            context.setCapsuleThresholdHandledMachineSet(thresholdBaseline);
            context.setCapsuleReplacementShiftCapacityLimitMap(capacityLimitBaseline);
            context.setCapsuleReplacementTimeWindowMap(timeWindowBaseline);
        }
    }

    /**
     * 按无副作用预演结果正式登记换胶囊运行态。
     *
     * @param context 排程上下文
     * @param result 当前排程结果
     * @param shift 当前班次
     * @param candidateQty 扣减前候选量
     * @param shiftCapacityBeforeReplacement 换胶囊前实际班产
     * @param effectiveStartTime 实际开产时间
     * @param scene 调用场景
     * @param previewQty 预演实际量
     * @return 正式登记后的实际量
     */
    public int applyPreviewedPlanQty(LhScheduleContext context,
                                     LhScheduleResult result,
                                     LhShiftConfigVO shift,
                                     int candidateQty,
                                     int shiftCapacityBeforeReplacement,
                                     Date effectiveStartTime,
                                     String scene,
                                     int previewQty) {
        int actualQty = this.resolveActualPlanQty(
                context, result, shift, candidateQty,
                shiftCapacityBeforeReplacement, effectiveStartTime, scene);
        if (actualQty != previewQty) {
            throw new IllegalStateException(new StringBuilder(
                    "换胶囊预演与正式登记结果不一致, materialCode=")
                    .append(result.getMaterialCode())
                    .append(", machineCode=").append(result.getLhMachineCode())
                    .append(", shiftIndex=").append(
                            Objects.isNull(shift) ? null : shift.getShiftIndex())
                    .append(", previewQty=").append(previewQty)
                    .append(", actualQty=").append(actualQty)
                    .toString());
        }
        return actualQty;
    }

    private Map<String, CapsuleReplacementTimeWindowDTO> copyReplacementTimeWindowMap(
            Map<String, CapsuleReplacementTimeWindowDTO> sourceMap) {
        Map<String, CapsuleReplacementTimeWindowDTO> targetMap =
                new LinkedHashMap<String, CapsuleReplacementTimeWindowDTO>(
                        Math.max(8, CollectionUtils.isEmpty(sourceMap) ? 0 : sourceMap.size() * 2));
        if (CollectionUtils.isEmpty(sourceMap)) {
            return targetMap;
        }
        for (Map.Entry<String, CapsuleReplacementTimeWindowDTO> entry : sourceMap.entrySet()) {
            if (Objects.isNull(entry.getValue())) {
                targetMap.put(entry.getKey(), null);
                continue;
            }
            CapsuleReplacementTimeWindowDTO copiedWindow =
                    new CapsuleReplacementTimeWindowDTO();
            BeanUtil.copyProperties(entry.getValue(), copiedWindow);
            targetMap.put(entry.getKey(), copiedWindow);
        }
        return targetMap;
    }

    /**
     * 同包历史单元测试的完整班次便捷入口。
     *
     * <p>正式排程必须传入换胶囊前实际班产和实际开产时间，避免把部分班次误判为满产。
     * 该入口仅保留给既有同包测试构造“候选量即完整班次”的场景，实际决策仍委托统一主方法。</p>
     */
    int resolveActualPlanQty(LhScheduleContext context,
                             LhScheduleResult result,
                             LhShiftConfigVO shift,
                             int candidateQty,
                             String scene) {
        int existingPlanQty = Objects.isNull(result) || Objects.isNull(shift)
                || Objects.isNull(shift.getShiftIndex()) ? 0
                : resolveShiftPlanQty(result, shift.getShiftIndex());
        Date effectiveStartTime = Objects.isNull(result) || Objects.isNull(shift)
                || Objects.isNull(shift.getShiftIndex()) ? null
                : ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
        return this.resolveActualPlanQty(context, result, shift, candidateQty,
                Math.max(0, existingPlanQty + candidateQty), effectiveStartTime, scene);
    }

    /**
     * 获取已换胶囊班次在后置处理阶段允许使用的最大产能。
     *
     * <p>续作日标准收敛、班次重分配和收尾补量会根据停机、清洗、保养等规则重新计算
     * “换胶囊前”的班次物理产能。数量模式必须继续保留首次扣除的固定产能；时间模式的
     * 不可生产窗口已在底层时间轴扣除，本方法不得再次扣量。</p>
     *
     * <p>调用方传入的 {@code capacityBeforeReplacement} 必须是不含换胶囊损失的理论产能。
     * 普通机台按配置值扣减；L/R整机的结果量按单侧保存，因此沿用正式落班规则折算为
     * 单侧扣减量，确保物理整机仍只损失配置的总产能。</p>
     *
     * @param context 排程上下文
     * @param result 当前排程结果
     * @param shift 当前班次
     * @param capacityBeforeReplacement 扣除换胶囊前的班次理论产能
     * @return 数量模式保留固定损失后的班次产能上限；时间模式和未触发时返回传入产能
     */
    public int resolveReplacementShiftCapacityUpperLimit(LhScheduleContext context,
                                                          LhScheduleResult result,
                                                          LhShiftConfigVO shift,
                                                          int capacityBeforeReplacement) {
        int normalizedCapacity = Math.max(0, capacityBeforeReplacement);
        if (Objects.isNull(context) || Objects.isNull(result) || Objects.isNull(shift)
                || Objects.isNull(shift.getShiftIndex())
                || StringUtils.isEmpty(result.getLhMachineCode())) {
            return normalizedCapacity;
        }

        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                result.getLhMachineCode());
        String shiftKey = buildShiftKey(physicalMachineCode, shift);
        if (StringUtils.isEmpty(shiftKey)
                || !containsReplacementAnalysisForPhysicalShift(
                        context, result, physicalMachineCode, shift.getShiftIndex())) {
            return normalizedCapacity;
        }
        if (containsReplacementTimeWindow(context, shiftKey)) {
            // 时间模式的容量损失已由 ShiftCapacityResolverUtil 合并不可生产区间计算，禁止二次扣量。
            return normalizedCapacity;
        }

        int effectiveLossQty = resolveEffectiveLossQty(
                resolveChangeLossQty(context), isWholeSingleControlPairResult(context, result),
                result.getLhMachineCode());
        Integer recordedCapacityLimit = context.getCapsuleReplacementShiftCapacityLimitMap().get(
                buildCapacityLimitKey(shiftKey, result));
        if (Objects.nonNull(recordedCapacityLimit)) {
            // 优先复用首次落班记录的精确上限，既不补回损失，也不会把已扣量重复再扣一次。
            return Math.min(normalizedCapacity, Math.max(0, recordedCapacityLimit));
        }
        return Math.max(0, normalizedCapacity - effectiveLossQty);
    }

    /**
     * 使用首次换胶囊时记录的精确上限收口当前结果班次量。
     *
     * <p>该方法只用于所有普通数量修改器执行完成后的最终落班收敛。存在精确记录时，
     * 当前量只能缩小到记录上限；没有精确记录或当前物理班次已无“换胶囊”备注时原值返回。
     * 与理论产能查询不同，本方法不会使用配置值再次扣减，因此不会产生二次扣量。</p>
     *
     * @param context 排程上下文
     * @param result 当前排程结果
     * @param shift 当前班次
     * @param currentPlanQty 当前班次计划量
     * @return 精确上限收口后的班次计划量
     */
    public int limitByRecordedReplacementCapacity(LhScheduleContext context,
                                                   LhScheduleResult result,
                                                   LhShiftConfigVO shift,
                                                   int currentPlanQty) {
        int normalizedPlanQty = Math.max(0, currentPlanQty);
        if (Objects.isNull(context) || Objects.isNull(result) || Objects.isNull(shift)
                || Objects.isNull(shift.getShiftIndex())
                || StringUtils.isEmpty(result.getLhMachineCode())) {
            return normalizedPlanQty;
        }
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                result.getLhMachineCode());
        String shiftKey = buildShiftKey(physicalMachineCode, shift);
        if (StringUtils.isEmpty(shiftKey)
                || !containsReplacementAnalysisForPhysicalShift(
                        context, result, physicalMachineCode, shift.getShiftIndex())) {
            return normalizedPlanQty;
        }
        if (containsReplacementTimeWindow(context, shiftKey)) {
            // 时间模式没有固定数量上限，后续容量由换胶囊窗口重新折算。
            return normalizedPlanQty;
        }
        Integer recordedCapacityLimit = context.getCapsuleReplacementShiftCapacityLimitMap().get(
                buildCapacityLimitKey(shiftKey, result));
        return Objects.isNull(recordedCapacityLimit)
                ? normalizedPlanQty : Math.min(normalizedPlanQty, Math.max(0, recordedCapacityLimit));
    }

    /**
     * 按最终结果备注判断物理机台班次是否已经换胶囊。
     *
     * <p>产能查询会在日标准、降模和补量计算中高频调用，不能在每次查询时重放整个结果集。
     * “换胶囊”备注是班次已执行更换的最终事实标识，因此只需检查当前结果和同物理机台结果；
     * 这样也能自然排除已被删除结果留下的旧运行态键。</p>
     *
     * @param context 排程上下文
     * @param currentResult 当前结果
     * @param physicalMachineCode 物理机台编码
     * @param shiftIndex 班次索引
     * @return true-该物理机台班次已换胶囊；false-未换胶囊
     */
    private boolean containsReplacementAnalysisForPhysicalShift(LhScheduleContext context,
                                                                  LhScheduleResult currentResult,
                                                                  String physicalMachineCode,
                                                                  int shiftIndex) {
        if (containsReplacementAnalysis(currentResult, shiftIndex)) {
            return true;
        }
        if (CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return false;
        }
        for (LhScheduleResult scheduledResult : context.getScheduleResultList()) {
            if (Objects.isNull(scheduledResult) || scheduledResult == currentResult
                    || StringUtils.isEmpty(scheduledResult.getLhMachineCode())) {
                continue;
            }
            String scheduledPhysicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                    scheduledResult.getLhMachineCode());
            if (StringUtils.equals(physicalMachineCode, scheduledPhysicalMachineCode)
                    && containsReplacementAnalysis(scheduledResult, shiftIndex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据当前排程结果重建胶囊运行态。
     *
     * <p>重建以基础胶囊次数为起点，按班次、物理机台和结果开始时间顺序累计实际胶囊次数增量。
     * 结果中的“换胶囊”备注是该班已执行更换的事实标识；同一物理机台同班存在多个结果时
     * 只识别第一条备注。L/R整机结果按一个物理排产组处理，避免左右结果重复累计。</p>
     *
     * @param context 排程上下文
     * @param currentResult 当前尚未加入结果列表但正在分配的结果，可为空
     */
    public void rebuildRuntimeState(LhScheduleContext context, LhScheduleResult currentResult) {
        if (Objects.isNull(context)) {
            return;
        }
        context.getCapsuleRuntimeUsageMap().clear();
        context.getCapsuleReplacementShiftKeySet().clear();
        context.getCapsuleThresholdHandledMachineSet().clear();

        List<LhScheduleResult> resultList = collectCurrentResults(context, currentResult);
        List<LhShiftConfigVO> shifts = resolveScheduleShifts(context);
        Set<String> validReplacementShiftKeySet = new LinkedHashSet<String>();
        Map<String, CapsuleReplacementTimeWindowDTO> originalTimeWindowMap =
                new LinkedHashMap<String, CapsuleReplacementTimeWindowDTO>(
                        context.getCapsuleReplacementTimeWindowMap());
        Map<String, CapsuleReplacementTimeWindowDTO> validTimeWindowMap =
                new LinkedHashMap<String, CapsuleReplacementTimeWindowDTO>();

        for (LhShiftConfigVO shift : shifts) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                continue;
            }
            List<LhScheduleResult> orderedShiftResults = resolveOrderedShiftResults(resultList, shift.getShiftIndex());
            Set<String> processedWholePairGroupSet = new LinkedHashSet<String>();
            for (LhScheduleResult result : orderedShiftResults) {
                if (Objects.isNull(result) || StringUtils.isEmpty(result.getLhMachineCode())) {
                    continue;
                }
                int actualQty = resolveShiftPlanQty(result, shift.getShiftIndex());
                boolean replacementMarked = containsReplacementAnalysis(result, shift.getShiftIndex());
                if (actualQty <= 0 && !replacementMarked) {
                    continue;
                }
                String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        result.getLhMachineCode());
                boolean wholeSingleControlPair = isWholeSingleControlPairResult(context, result);
                if (wholeSingleControlPair) {
                    String wholePairGroupKey = buildWholePairGroupKey(
                            result, physicalMachineCode, shift.getShiftIndex(), actualQty);
                    if (!processedWholePairGroupSet.add(wholePairGroupKey)) {
                        continue;
                    }
                }
                initializePhysicalMachineUsage(context, result.getLhMachineCode(), physicalMachineCode);
                String shiftKey = buildShiftKey(physicalMachineCode, shift);
                if (StringUtils.isEmpty(shiftKey)) {
                    continue;
                }
                if (replacementMarked) {
                    // 结果备注是本批已执行首次跨限扣量的事实，重建时恢复机台级处理状态。
                    context.getCapsuleThresholdHandledMachineSet().add(physicalMachineCode);
                    if (context.getCapsuleReplacementShiftKeySet().add(shiftKey)) {
                        validReplacementShiftKeySet.add(shiftKey);
                    }
                    CapsuleReplacementTimeWindowDTO timeWindow = originalTimeWindowMap.get(shiftKey);
                    if (Objects.nonNull(timeWindow)) {
                        validTimeWindowMap.put(shiftKey, timeWindow);
                    }
                }
                // 重建与正式落班复用同一胶囊增量口径，防止后置缩量后运行态漂移。
                int actualUsageIncrement = resolveCapsuleUsageIncrement(
                        result, actualQty, wholeSingleControlPair);
                applyActualUsageIncrement(context, physicalMachineCode, actualUsageIncrement);
            }
        }
        context.getCapsuleReplacementShiftKeySet().retainAll(validReplacementShiftKeySet);
        context.setCapsuleReplacementTimeWindowMap(validTimeWindowMap);
    }

    /**
     * 保存前按最终结果重建并输出胶囊规则一致性日志。
     *
     * <p>该方法只核对和重建运行态，不再次扣减班次计划量，避免S4.6重复扣量后余量无法续排。</p>
     *
     * @param context 排程上下文
     */
    public void verifyFinalState(LhScheduleContext context) {
        int zeroQtyRemarkCleanedCount = removeReplacementAnalysisOnZeroQtyShifts(context);
        int duplicateAnalysisCount = removeDuplicateReplacementAnalysis(context);
        rebuildRuntimeState(context, null);
        if (Objects.isNull(context)) {
            return;
        }
        log.info("换胶囊规则最终核对完成, batchNo: {}, scheduleDate: {}, 换胶囊班次数: {}, "
                        + "时间模式窗口数: {}, 清理重复备注数: {}, 清理零量班次备注数: {}, 胶囊运行态: {}",
                context.getBatchNo(), LhScheduleTimeUtil.formatDate(context.getScheduleDate()),
                context.getCapsuleReplacementShiftKeySet().size(),
                context.getCapsuleReplacementTimeWindowMap().size(), duplicateAnalysisCount,
                zeroQtyRemarkCleanedCount, context.getCapsuleRuntimeUsageMap());
    }

    /**
     * 清理最终结果中“计划量为 0 但残留换胶囊备注”的班次。
     * <p>降模、收尾裁剪、停产保机等后置逻辑可能把已写备注的班次清零，备注必须随清零一并移除，
     * 否则会出现“零量班次仍备注换胶囊”的虚假对账信息（如 3302001761/K2016 class7）。</p>
     *
     * @param context 排程上下文
     * @return 清理的零量班次备注数量
     */
    private int removeReplacementAnalysisOnZeroQtyShifts(LhScheduleContext context) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return 0;
        }
        List<LhShiftConfigVO> shifts = resolveScheduleShifts(context);
        if (CollectionUtils.isEmpty(shifts)) {
            return 0;
        }
        int cleanedCount = 0;
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.isNull(result) || StringUtils.isEmpty(result.getLhMachineCode())) {
                continue;
            }
            String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                    result.getLhMachineCode());
            for (LhShiftConfigVO shift : shifts) {
                if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                    continue;
                }
                Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                if (Objects.nonNull(planQty) && planQty > 0) {
                    continue;
                }
                String analysis = ShiftFieldUtil.getShiftAnalysis(result, shift.getShiftIndex());
                if (StringUtils.isEmpty(analysis)
                        || !analysis.contains(CAPSULE_REPLACEMENT_ANALYSIS)) {
                    continue;
                }
                String shiftKey = buildShiftKey(physicalMachineCode, shift);
                if (containsReplacementTimeWindow(context, shiftKey)) {
                    /*
                     * 时间模式的换胶囊已经在当前班次实际发生，后续产量可能被时间轴顺延到下一班。
                     * 此时保留事实备注和时间窗口，避免最终重建把已占用机台时间错误释放。
                     */
                    continue;
                }
                // 清零班次不属于数量模式的真实换胶囊班次：移除备注并同步移除本批登记。
                ShiftFieldUtil.removeShiftAnalysis(
                        result, shift.getShiftIndex(), CAPSULE_REPLACEMENT_ANALYSIS);
                if (StringUtils.isNotEmpty(shiftKey)) {
                    context.getCapsuleReplacementShiftKeySet().remove(shiftKey);
                }
                cleanedCount++;
            }
        }
        if (cleanedCount > 0) {
            log.info("换胶囊零量班次备注清理, batchNo: {}, 清理数量: {}, reason: 班次计划量为0不保留换胶囊备注",
                    context.getBatchNo(), cleanedCount);
        }
        return cleanedCount;
    }

    /**
     * 清理同一物理机台在本批排程中的重复“换胶囊”备注。
     *
     * <p>首次严格跨限只允许扣减和备注一次。该核对只规范备注，不修改班次计划量；按班次、
     * 结果开始时间顺序保留第一条备注，清理L/R复制、结果合并或后置处理产生的重复备注。</p>
     *
     * @param context 排程上下文
     * @return 清理的重复备注数量
     */
    private int removeDuplicateReplacementAnalysis(LhScheduleContext context) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return 0;
        }
        Set<String> markedPhysicalMachineSet = new LinkedHashSet<String>();
        int removedCount = 0;
        for (LhShiftConfigVO shift : resolveScheduleShifts(context)) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                continue;
            }
            List<LhScheduleResult> orderedResults = resolveOrderedShiftResults(
                    context.getScheduleResultList(), shift.getShiftIndex());
            for (LhScheduleResult result : orderedResults) {
                if (Objects.isNull(result) || StringUtils.isEmpty(result.getLhMachineCode())
                        || !containsReplacementAnalysis(result, shift.getShiftIndex())) {
                    continue;
                }
                String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        result.getLhMachineCode());
                if (markedPhysicalMachineSet.add(physicalMachineCode)) {
                    continue;
                }
                ShiftFieldUtil.removeShiftAnalysis(
                        result, shift.getShiftIndex(), CAPSULE_REPLACEMENT_ANALYSIS);
                removedCount++;
            }
        }
        return removedCount;
    }

    /**
     * 获取指定物理机台的当前胶囊运行态次数，供单元测试和诊断使用。
     *
     * @param context 排程上下文
     * @param machineCode 运行态或物理机台编码
     * @return 当前物理机台胶囊使用次数
     */
    public int getMachineRuntimeUsage(LhScheduleContext context, String machineCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(machineCode)) {
            return 0;
        }
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return context.getCapsuleRuntimeUsageMap().getOrDefault(physicalMachineCode, 0);
    }

    /**
     * 按当前结果对应的胶囊次数增量累计，首次跨限后也不减去上限。
     *
     * @param context 排程上下文
     * @param physicalMachineCode 物理机台编码
     * @param actualUsageIncrement 本次实际胶囊次数增量
     */
    private void applyActualUsageIncrement(LhScheduleContext context,
                                           String physicalMachineCode,
                                           int actualUsageIncrement) {
        int normalizedIncrement = Math.max(0, actualUsageIncrement);
        context.getCapsuleRuntimeUsageMap().merge(
                physicalMachineCode, normalizedIncrement, Integer::sum);
    }

    /**
     * 将结果计划量转换为胶囊使用次数增量。
     * <p>普通双模一个生产循环同时生产两条，单个胶囊只累计其中一侧对应的循环次数；
     * 奇数计划量复用现有模台数向上收敛规则后再折半，等价于取左右侧实际次数的较大值。
     * 普通单模直接按结果量累计；L/R整机配对结果按单侧保存，需要乘以2还原既有物理机台
     * 累计口径；L/R独立排产仍由两侧结果分别进入规则并自然累加。</p>
     *
     * @param result 当前排程结果
     * @param resultPlanQty 当前结果计划量
     * @param wholeSingleControlPair 是否为L/R整机配对结果
     * @return 本次胶囊使用次数增量
     */
    private int resolveCapsuleUsageIncrement(LhScheduleResult result,
                                             int resultPlanQty,
                                             boolean wholeSingleControlPair) {
        int normalizedPlanQty = Math.max(0, resultPlanQty);
        if (wholeSingleControlPair) {
            return normalizedPlanQty * DOUBLE_MOULD_QTY;
        }
        if (isOrdinaryDoubleMouldResult(result)) {
            int normalizedDoubleMouldQty = ShiftCapacityResolverUtil.roundUpQtyToMouldMultiple(
                    normalizedPlanQty, DOUBLE_MOULD_QTY);
            return normalizedDoubleMouldQty / DOUBLE_MOULD_QTY;
        }
        return normalizedPlanQty;
    }

    /**
     * 判断当前结果是否为普通双模机台结果。
     * <p>单控运行态机台以L/R结尾，必须继续沿用单控累计口径；只有非单控且结果模台数
     * 明确为2时，才应用普通双模单侧循环次数规则，不扩大到普通单模或多模结果。</p>
     *
     * @param result 当前排程结果
     * @return true-普通双模结果；false-其他机台结果
     */
    private boolean isOrdinaryDoubleMouldResult(LhScheduleResult result) {
        return Objects.nonNull(result)
                && !LhSingleControlMachineUtil.isSingleMouldMachine(result.getLhMachineCode())
                && Objects.nonNull(result.getMouldQty())
                && result.getMouldQty() == DOUBLE_MOULD_QTY;
    }

    private int resolveEffectiveLossQty(int configuredLossQty,
                                        boolean wholeSingleControlPair,
                                        String machineCode) {
        int normalizedLossQty = Math.max(0, configuredLossQty);
        if (!wholeSingleControlPair || !LhSingleControlMachineUtil.isSingleMouldMachine(machineCode)) {
            return normalizedLossQty;
        }
        /*
         * L/R整机分配循环处理的是单侧数量，配置值是物理整机总扣减量。
         * 为保持两侧等量，奇数配置按整机模数向上归整后再折半；默认2条对应单侧扣1条。
         */
        return (normalizedLossQty + 1) / 2;
    }

    private void initializePhysicalMachineUsage(LhScheduleContext context,
                                                String machineCode,
                                                String physicalMachineCode) {
        if (context.getCapsuleRuntimeUsageMap().containsKey(physicalMachineCode)) {
            return;
        }
        /*
         * 初始化运行态与胶囊感知收尾分摊必须共用同一份“排程开始前快照”口径。
         * 这里不再复制左右模取值逻辑，避免后续一处修正规则而另一处仍使用旧值。
         */
        int machineUsage = this.resolveInitialMachineUsage(context, machineCode);
        context.getCapsuleRuntimeUsageMap().put(physicalMachineCode, machineUsage);
        if (machineUsage >= this.resolveUsageUpperLimit(context)) {
            /*
             * 初始快照已经达到或超过上限，说明本批开始前已越过阈值。
             * 按确认口径仅继续累计，不在本批首个生产班次补扣换胶囊产能。
             */
            context.getCapsuleThresholdHandledMachineSet().add(physicalMachineCode);
        }
    }

    /**
     * 读取物理机台在本批排程开始前的胶囊使用次数。
     *
     * <p>该方法只读取基础数据快照，不读取、也不修改排程中的动态累计次数。普通机台取
     * {@code REPLACE_CAPSULE_COUNT/REPLACE_CAPSULE_COUNT2} 最大值；单控L/R机台先归一到
     * 物理机台，再分别读取左右侧记录并取最大值。基础快照缺失时继续沿用项目已有的
     * {@link MachineScheduleDTO} 初值回退口径。</p>
     *
     * @param context 排程上下文
     * @param machineCode 当前结果机台编码；单控机台可传L/R侧编码
     * @return 排程开始前的物理机台胶囊使用次数，缺失时返回0
     */
    public int resolveInitialMachineUsage(LhScheduleContext context, String machineCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(machineCode)) {
            return 0;
        }
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        int positionOneUsage = 0;
        int positionTwoUsage = 0;
        Map<String, LhRepairCapsule> capsuleUsageMap = context.getCapsuleUsageMap();
        if (!CollectionUtils.isEmpty(capsuleUsageMap)) {
            LhRepairCapsule physicalCapsule = capsuleUsageMap.get(physicalMachineCode);
            if (LhSingleControlMachineUtil.isSingleMouldMachine(machineCode)) {
                LhRepairCapsule leftCapsule = capsuleUsageMap.get(
                        LhSingleControlMachineUtil.resolveLeftMachineCode(machineCode));
                LhRepairCapsule rightCapsule = capsuleUsageMap.get(
                        LhSingleControlMachineUtil.resolveRightMachineCode(machineCode));
                positionOneUsage = Objects.nonNull(leftCapsule)
                        ? this.resolveCapsuleCount(leftCapsule, false)
                        : this.resolveCapsuleCount(physicalCapsule, false);
                positionTwoUsage = Objects.nonNull(rightCapsule)
                        ? this.resolveCapsuleCount(rightCapsule, false)
                        : this.resolveCapsuleCount(physicalCapsule, true);
            } else {
                positionOneUsage = this.resolveCapsuleCount(physicalCapsule, false);
                positionTwoUsage = this.resolveCapsuleCount(physicalCapsule, true);
            }
        }
        if (positionOneUsage == 0 && positionTwoUsage == 0
                && !CollectionUtils.isEmpty(context.getMachineScheduleMap())) {
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
            if (Objects.isNull(machine)) {
                machine = context.getMachineScheduleMap().get(physicalMachineCode);
            }
            if (Objects.nonNull(machine)) {
                positionOneUsage = Math.max(0, machine.getCapsuleUsageCount());
                positionTwoUsage = Math.max(0, machine.getCapsuleUsageCount2());
            }
        }
        return Math.max(positionOneUsage, positionTwoUsage);
    }

    private int resolveCapsuleCount(LhRepairCapsule capsule, boolean secondPosition) {
        if (Objects.isNull(capsule)) {
            return 0;
        }
        Integer usageCount = secondPosition
                ? capsule.getReplaceCapsuleCount2() : capsule.getReplaceCapsuleCount();
        return Objects.isNull(usageCount) ? 0 : Math.max(0, usageCount);
    }

    private int resolveUsageUpperLimit(LhScheduleContext context) {
        LhScheduleConfig scheduleConfig = context.getScheduleConfig();
        return Objects.nonNull(scheduleConfig)
                ? scheduleConfig.getCapsuleUsageUpperLimit()
                : Math.max(1, context.getParamIntValue(
                LhScheduleParamConstant.CAPSULE_FORCE_DOWN_COUNT,
                LhScheduleConstant.CAPSULE_FORCE_DOWN_COUNT));
    }

    private int resolveChangeLossQty(LhScheduleContext context) {
        LhScheduleConfig scheduleConfig = context.getScheduleConfig();
        return Objects.nonNull(scheduleConfig)
                ? scheduleConfig.getCapsuleChangeLossQty()
                : Math.max(0, context.getParamIntValue(
                LhScheduleParamConstant.CAPSULE_CHANGE_LOSS_QTY,
                LhScheduleConstant.CAPSULE_CHANGE_LOSS_QTY));
    }

    /**
     * 读取换胶囊时间模式的占用时长。
     *
     * @param context 排程上下文
     * @return 换胶囊时长（小时）
     */
    private int resolveReplacementDurationHours(LhScheduleContext context) {
        LhScheduleConfig scheduleConfig = context.getScheduleConfig();
        return Objects.nonNull(scheduleConfig)
                ? scheduleConfig.getCapsuleReplacementDurationHours()
                : Math.max(1, context.getParamIntValue(
                LhScheduleParamConstant.CAPSULE_REPLACEMENT_DURATION_HOURS,
                LhScheduleConstant.CAPSULE_REPLACEMENT_DURATION_HOURS));
    }

    /**
     * 登记未满产换胶囊的机台时间占用。
     *
     * <p>时间窗口从当前候选生产段结束时开始。当前候选量不因换胶囊直接减量，后续同机台
     * 的生产、换模、首检和选机时间统一由容量时间轴感知该窗口并顺延。</p>
     *
     * @param context 排程上下文
     * @param result 当前排程结果
     * @param shift 当前班次
     * @param shiftKey 物理机台班次键
     * @param plannedQtyAfterCandidate 写入当前候选后的班次计划量
     * @param shiftCapacityBeforeReplacement 换胶囊前实际班产
     * @param effectiveStartTime 当前候选实际开产时间
     */
    private void registerCapsuleReplacementTimeWindow(LhScheduleContext context,
                                                      LhScheduleResult result,
                                                      LhShiftConfigVO shift,
                                                      String shiftKey,
                                                      int plannedQtyAfterCandidate,
                                                      int shiftCapacityBeforeReplacement,
                                                      Date effectiveStartTime) {
        if (StringUtils.isEmpty(shiftKey) || containsReplacementTimeWindow(context, shiftKey)) {
            return;
        }
        Date replacementStartTime = resolveReplacementStartTime(context, result, shift,
                plannedQtyAfterCandidate, shiftCapacityBeforeReplacement, effectiveStartTime);
        if (Objects.isNull(replacementStartTime)) {
            return;
        }
        CapsuleReplacementTimeWindowDTO timeWindow = new CapsuleReplacementTimeWindowDTO();
        timeWindow.setPhysicalMachineCode(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                result.getLhMachineCode()));
        timeWindow.setMaterialCode(result.getMaterialCode());
        timeWindow.setShiftIndex(shift.getShiftIndex());
        timeWindow.setReplacementStartTime(replacementStartTime);
        timeWindow.setReplacementEndTime(LhScheduleTimeUtil.addHours(replacementStartTime,
                resolveReplacementDurationHours(context)));
        context.getCapsuleReplacementTimeWindowMap().put(shiftKey, timeWindow);
    }

    /**
     * 解析未满产换胶囊的实际开始时间。
     *
     * <p>必须复用既有停机、清洗、保养时间轴推导当前候选生产段结束时间，不能以班次结束
     * 时间或固定整点代替。这样临近班次结束触发时，窗口会自然跨入下一班。</p>
     *
     * @param context 排程上下文
     * @param result 当前排程结果
     * @param shift 当前班次
     * @param plannedQtyAfterCandidate 写入当前候选后的班次计划量
     * @param shiftCapacityBeforeReplacement 换胶囊前实际班产
     * @param effectiveStartTime 当前候选实际开产时间
     * @return 换胶囊开始时间
     */
    private Date resolveReplacementStartTime(LhScheduleContext context,
                                             LhScheduleResult result,
                                             LhShiftConfigVO shift,
                                             int plannedQtyAfterCandidate,
                                             int shiftCapacityBeforeReplacement,
                                             Date effectiveStartTime) {
        Date shiftStartTime = Objects.nonNull(effectiveStartTime)
                ? effectiveStartTime : ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
        if (Objects.isNull(shiftStartTime)) {
            shiftStartTime = shift.getShiftStartDateTime();
        }
        if (Objects.isNull(shiftStartTime) || Objects.isNull(shift.getShiftEndDateTime())) {
            return shiftStartTime;
        }
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
        List<MachineCleaningWindowDTO> cleaningWindowList = Objects.isNull(machine)
                || CollectionUtils.isEmpty(machine.getCleaningWindowList())
                ? Collections.<MachineCleaningWindowDTO>emptyList() : machine.getCleaningWindowList();
        List<MachineMaintenanceWindowDTO> maintenanceWindowList =
                ShiftCapacityResolverUtil.resolveCapacityMaintenanceWindowList(context,
                        context.getDevicePlanShutList(), result.getLhMachineCode(),
                        Objects.isNull(machine) ? Collections.<MachineMaintenanceWindowDTO>emptyList()
                                : machine.getMaintenanceWindowList());
        Date plannedEndTime = ShiftCapacityResolverUtil.resolveShiftPlanEndTime(
                context.getDevicePlanShutList(), cleaningWindowList, maintenanceWindowList,
                result.getLhMachineCode(), shiftStartTime, shift.getShiftEndDateTime(),
                Math.max(0, plannedQtyAfterCandidate), Math.max(1, shiftCapacityBeforeReplacement));
        return Objects.nonNull(plannedEndTime) ? plannedEndTime : shiftStartTime;
    }

    /**
     * 判断物理机台班次是否采用换胶囊时间模式。
     *
     * @param context 排程上下文
     * @param shiftKey 物理机台班次键
     * @return true-时间模式；false-数量模式或未触发
     */
    private boolean containsReplacementTimeWindow(LhScheduleContext context, String shiftKey) {
        return Objects.nonNull(context) && StringUtils.isNotEmpty(shiftKey)
                && !CollectionUtils.isEmpty(context.getCapsuleReplacementTimeWindowMap())
                && context.getCapsuleReplacementTimeWindowMap().containsKey(shiftKey);
    }

    /** 获取换胶囊时间窗口开始时间，用于对账日志。 */
    private Date resolveReplacementWindowStartTime(LhScheduleContext context, String shiftKey) {
        CapsuleReplacementTimeWindowDTO timeWindow = containsReplacementTimeWindow(context, shiftKey)
                ? context.getCapsuleReplacementTimeWindowMap().get(shiftKey) : null;
        return Objects.isNull(timeWindow) ? null : timeWindow.getReplacementStartTime();
    }

    /** 获取换胶囊时间窗口结束时间，用于对账日志。 */
    private Date resolveReplacementWindowEndTime(LhScheduleContext context, String shiftKey) {
        CapsuleReplacementTimeWindowDTO timeWindow = containsReplacementTimeWindow(context, shiftKey)
                ? context.getCapsuleReplacementTimeWindowMap().get(shiftKey) : null;
        return Objects.isNull(timeWindow) ? null : timeWindow.getReplacementEndTime();
    }

    private boolean isWholeSingleControlPairResult(LhScheduleContext context, LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || !LhSingleControlMachineUtil.isSingleMouldMachine(result.getLhMachineCode())) {
            return false;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                result.getMaterialCode(), result.getProductStatus());
        return SingleControlMachineModeEnum.WHOLE_PAIR
                == context.getSingleControlModeSnapshotMap().get(skuKey);
    }

    private List<LhScheduleResult> collectCurrentResults(LhScheduleContext context,
                                                         LhScheduleResult currentResult) {
        List<LhScheduleResult> resultList = new ArrayList<LhScheduleResult>(
                context.getScheduleResultList().size() + 1);
        Set<LhScheduleResult> identitySet = java.util.Collections.newSetFromMap(
                new IdentityHashMap<LhScheduleResult, Boolean>());
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.nonNull(result) && identitySet.add(result)) {
                resultList.add(result);
            }
        }
        if (Objects.nonNull(currentResult) && identitySet.add(currentResult)) {
            resultList.add(currentResult);
        }
        return resultList;
    }

    private List<LhShiftConfigVO> resolveScheduleShifts(LhScheduleContext context) {
        if (!CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            return context.getScheduleWindowShifts();
        }
        if (Objects.isNull(context.getScheduleDate())) {
            return new ArrayList<LhShiftConfigVO>(0);
        }
        return LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
    }

    private List<LhScheduleResult> resolveOrderedShiftResults(List<LhScheduleResult> resultList,
                                                              int shiftIndex) {
        List<LhScheduleResult> orderedResultList = new ArrayList<LhScheduleResult>(resultList.size());
        for (LhScheduleResult result : resultList) {
            if (Objects.isNull(result)) {
                continue;
            }
            int planQty = resolveShiftPlanQty(result, shiftIndex);
            if (planQty > 0 || containsReplacementAnalysis(result, shiftIndex)) {
                orderedResultList.add(result);
            }
        }
        orderedResultList.sort(Comparator
                .comparing((LhScheduleResult result) -> StringUtils.defaultString(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode())))
                .thenComparing(result -> ShiftFieldUtil.getShiftStartTime(result, shiftIndex),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(result -> containsReplacementAnalysis(result, shiftIndex) ? 0 : 1)
                .thenComparing(result -> StringUtils.defaultString(result.getLhMachineCode())));
        return orderedResultList;
    }

    private int resolveShiftPlanQty(LhScheduleResult result, int shiftIndex) {
        Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
        return Objects.isNull(planQty) ? 0 : Math.max(0, planQty);
    }

    /**
     * 按物理机台口径汇总当前班次已写入计划量。
     *
     * <p>同一普通机台可能存在同班不同 SKU，未满产判断必须汇总机台实际已占用量。单控 L/R
     * 独立排产沿用既有“单侧为独立班产单元”口径；只有整机配对结果才按当前单侧数量折算整机量。</p>
     *
     * @param context 排程上下文
     * @param currentResult 当前正在追加计划量的结果
     * @param physicalMachineCode 物理机台编号
     * @param shiftIndex 班次序号
     * @param wholeSingleControlPair 是否单控整机配对结果
     * @return 当前物理机台已写入的班次计划量
     */
    private int resolveExistingPhysicalShiftPlanQty(LhScheduleContext context,
                                                    LhScheduleResult currentResult,
                                                    String physicalMachineCode,
                                                    int shiftIndex,
                                                    boolean wholeSingleControlPair) {
        int currentResultPlanQty = resolveShiftPlanQty(currentResult, shiftIndex);
        if (wholeSingleControlPair
                || LhSingleControlMachineUtil.isSingleMouldMachine(currentResult.getLhMachineCode())) {
            return resolvePhysicalShiftQty(currentResultPlanQty, wholeSingleControlPair,
                    currentResult.getLhMachineCode());
        }
        int totalPlanQty = 0;
        for (LhScheduleResult scheduledResult : collectCurrentResults(context, currentResult)) {
            if (Objects.isNull(scheduledResult) || StringUtils.isEmpty(scheduledResult.getLhMachineCode())) {
                continue;
            }
            String scheduledPhysicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                    scheduledResult.getLhMachineCode());
            if (StringUtils.equals(physicalMachineCode, scheduledPhysicalMachineCode)) {
                totalPlanQty += resolveShiftPlanQty(scheduledResult, shiftIndex);
            }
        }
        return Math.max(0, totalPlanQty);
    }

    /**
     * 将当前结果侧别班产转换为物理机台班产。
     *
     * @param shiftQty 当前结果班产
     * @param wholeSingleControlPair 是否单控整机配对
     * @param machineCode 当前结果机台
     * @return 物理机台班产
     */
    private int resolvePhysicalShiftQty(int shiftQty,
                                        boolean wholeSingleControlPair,
                                        String machineCode) {
        int normalizedShiftQty = Math.max(0, shiftQty);
        return wholeSingleControlPair && LhSingleControlMachineUtil.isSingleMouldMachine(machineCode)
                ? normalizedShiftQty * DOUBLE_MOULD_QTY : normalizedShiftQty;
    }

    /**
     * 将当前结果班产上限转换为物理机台班产上限。
     *
     * @param shiftCapacity 当前结果班产上限
     * @param wholeSingleControlPair 是否单控整机配对
     * @param machineCode 当前结果机台
     * @return 物理机台班产上限
     */
    private int resolvePhysicalShiftCapacity(int shiftCapacity,
                                             boolean wholeSingleControlPair,
                                             String machineCode) {
        int normalizedShiftCapacity = Math.max(0, shiftCapacity);
        return wholeSingleControlPair && LhSingleControlMachineUtil.isSingleMouldMachine(machineCode)
                ? normalizedShiftCapacity * DOUBLE_MOULD_QTY : normalizedShiftCapacity;
    }

    private boolean containsReplacementAnalysis(LhScheduleResult result, int shiftIndex) {
        String analysis = ShiftFieldUtil.getShiftAnalysis(result, shiftIndex);
        if (StringUtils.isEmpty(analysis)) {
            return false;
        }
        String[] analysisArray = analysis.split(",");
        for (String currentAnalysis : analysisArray) {
            if (StringUtils.equals(StringUtils.trim(currentAnalysis), CAPSULE_REPLACEMENT_ANALYSIS)) {
                return true;
            }
        }
        return false;
    }

    private String buildWholePairGroupKey(LhScheduleResult result,
                                          String physicalMachineCode,
                                          int shiftIndex,
                                          int actualQty) {
        Date startTime = ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
        return physicalMachineCode + KEY_SEPARATOR
                + MonthPlanDateResolver.buildMaterialStatusKey(
                result.getMaterialCode(), result.getProductStatus()) + KEY_SEPARATOR
                + shiftIndex + KEY_SEPARATOR
                + (Objects.isNull(startTime) ? 0L : startTime.getTime()) + KEY_SEPARATOR
                + actualQty;
    }

    private String buildShiftKey(String physicalMachineCode, LhShiftConfigVO shift) {
        if (StringUtils.isEmpty(physicalMachineCode) || Objects.isNull(shift)
                || Objects.isNull(shift.getShiftIndex())) {
            return null;
        }
        Date workDate = shift.getWorkDate();
        String dateText = Objects.isNull(workDate) ? "UNKNOWN" : LhScheduleTimeUtil.formatDate(workDate);
        return physicalMachineCode + KEY_SEPARATOR + dateText + KEY_SEPARATOR + shift.getShiftIndex();
    }

    /**
     * 构建换胶囊结果班次精确产能上限键。
     *
     * @param shiftKey 物理机台班次键
     * @param result 排程结果
     * @return 产能上限业务键
     */
    private String buildCapacityLimitKey(String shiftKey, LhScheduleResult result) {
        return shiftKey + KEY_SEPARATOR
                + StringUtils.defaultString(result.getLhMachineCode()) + KEY_SEPARATOR
                + MonthPlanDateResolver.buildMaterialStatusKey(
                result.getMaterialCode(), result.getProductStatus());
    }

}
