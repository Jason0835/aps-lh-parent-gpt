/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.ShiftProductionControlDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.api.enums.SkuTagEnum;
import com.zlt.aps.lh.component.CapsuleReplacementRuleService;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.StructureMinMachineRetentionService;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.IEmbryoEndingBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IMouldChangeBalanceStrategy;
import com.zlt.aps.lh.util.CleaningScheduleRuleUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ResultDowntimeSummaryUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.util.ShiftProductionControlUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 默认共用胎胚多机台收尾均衡策略实现。
 *
 * <p>核心思路：</p>
 * <ul>
 *   <li>只处理运行态共用胎胚，且同胎胚组当前仍有两台及以上可调整续作收尾机台；</li>
 *   <li>按日期升序、早班后中班检查班次次数，只有超过早8/中7参考上限或日15硬上限风险时才调整；</li>
 *   <li>可调整对象按业务优先级选择：同物料（物料编码+产品状态）多机台第一优先级，
 *       三种收尾类型（胎胚收尾/余量收尾/按时间下机）均参与；共用胎胚组第二优先级，
 *       组间按机台数量降序、胎胚编码升序，组内按机台编码升序；</li>
 *   <li>结构停产保机与续作停产保机机台仍参与收尾均衡，保机只拦截后续不同结构SKU上机，
 *       不影响当前收尾时间与换模次数均衡；调整后同步保机冻结快照和占用边界；</li>
 *   <li>同一SKU多机台与同一胎胚多SKU统一纳入全局预演，候选始终按机台编码升序；</li>
 *   <li>胎胚收尾以组级胎胚库存账本为唯一硬约束，
 *       允许同组跨物料互转尾量，互转成功后通过 SKU 内部额度重分配落地归属；</li>
 *   <li>SKU余量收尾保持该SKU总计划量不变，只能在同SKU多机台之间分摊；</li>
 *   <li>按时间下机收尾允许补量/后延或减量/提前；日内每次最多移动一个班次，
 *       跨天时必须连续补满中间班次并在目标早班06:00真实收尾；</li>
 *   <li>双模SKU单控整机L/R按一台物理机台成对参与均衡，两侧计划量始终一致；</li>
 *   <li>换模班次预测复用 {@link IMouldChangeBalanceStrategy#previewEndingStaggerMouldChange}，
 *       只做模拟计数，不预占真实换模次数；</li>
 *   <li>所有调整失败时恢复尝试前状态，保证不产生计划量丢失、重复排产或负数计划量。</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component
public class DefaultEmbryoEndingBalanceStrategy implements IEmbryoEndingBalanceStrategy {

    /** 续作排程类型编码 */
    private static final String CONTINUOUS_SCHEDULE_TYPE = "01";

    /** 胎胚在机标识（embryoEndingFlagMap 中 0 表示非胎胚收尾） */
    private static final int EMBRYO_ON_MACHINE_ENDING_FLAG = 0;

    /** 收尾类型：胎胚收尾 */
    private static final String ENDING_TYPE_EMBRYO = "胎胚收尾";

    /** 收尾类型：SKU余量收尾 */
    private static final String ENDING_TYPE_SKU = "SKU余量收尾";

    /** 收尾类型：按时间下机收尾 */
    private static final String ENDING_TYPE_TIME = "按时间下机收尾";

    /** 尾量分摊班次原因分析备注 */
    private static final String BALANCE_TRANSFER_ANALYSIS = "均衡分摊";

    /** 后延补量班次原因分析备注 */
    private static final String BALANCE_POSTPONE_ANALYSIS = "均衡补量";

    /** 提前减量班次原因分析备注 */
    private static final String BALANCE_ADVANCE_ANALYSIS = "均衡减量";

    /** 跨天补量班次原因分析备注 */
    private static final String BALANCE_POSTPONE_CROSS_DAY_ANALYSIS = "均衡补量（跨天）";

    /** 触发均衡的最少收尾机台数 */
    private static final int MIN_GROUP_MACHINE_COUNT = 2;

    /** 移动类型：尾量分摊 */
    private static final String MOVE_TYPE_TRANSFER = "尾量分摊";

    /** 移动类型：后延补量 */
    private static final String MOVE_TYPE_POSTPONE = "后延补量";

    /** 移动类型：提前减量 */
    private static final String MOVE_TYPE_ADVANCE = "提前减量";

    @Resource
    private IMouldChangeBalanceStrategy mouldChangeBalanceStrategy;

    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;

    @Resource
    private StructureMinMachineRetentionService structureMinMachineRetentionService;

    @Resource
    private CapsuleReplacementRuleService capsuleReplacementRuleService = new CapsuleReplacementRuleService();

    /**
     * 执行共用胎胚多机台收尾均衡。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @return true-至少执行了一次均衡调整；false-未触发调整
     */
    @Override
    public boolean balanceSharedEmbryoEnding(LhScheduleContext context, List<LhShiftConfigVO> shifts) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(shifts)
                || CollectionUtils.isEmpty(context.getScheduleResultList())) {
            return false;
        }
        // 换模均衡开关(SYS0304021)关闭时不执行任何收尾均衡，保持原排程结果。
        if (!isChangeoverBalanceEnabled(context)) {
            log.info("共用胎胚收尾均衡跳过, scheduleDate: {}, 原因: 换模均衡开关(SYS0304021)未启用",
                    context.getScheduleDate());
            return false;
        }
        // 首轮按最终续作结果收集候选；每次提交后还会重新收集，避免沿用已经失效的收尾类型或分组。
        Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap = new IdentityHashMap<LhScheduleResult, SkuScheduleDTO>(16);
        Map<LhScheduleResult, String> endingTypeMap = new IdentityHashMap<LhScheduleResult, String>(16);
        Map<LhScheduleResult, LhScheduleResult> pairResultMap =
                new IdentityHashMap<LhScheduleResult, LhScheduleResult>(8);
        List<LhScheduleResult> fixedCountResultList = new ArrayList<LhScheduleResult>(16);
        List<LhScheduleResult> adjustableResultList = this.collectAdjustableBalanceCandidates(
                context, sourceSkuMap, endingTypeMap, pairResultMap, fixedCountResultList);
        if (adjustableResultList.size() < MIN_GROUP_MACHINE_COUNT) {
            return false;
        }
        /*
         * 所有共用胎胚组一次性进入同一个预演状态，不能按胎胚组各自从旧统计开始判断。
         * 这样首个超限桶严格由“日期升序、早班后中班”确定，实际尝试机台再按编码升序，
         * 避免胎胚分组插入顺序改变最终结果。
         */
        Map<String, int[]> originalCountMap =
                this.copyDailyMouldChangeCountMap(context.getDailyMouldChangeCountMap());
        Map<String, int[]> baseSimulationCountMap = this.buildFixedSimulationCountMap(
                context, fixedCountResultList, sourceSkuMap, originalCountMap);
        EmbryoEndingBalanceState state = this.buildBalanceState(
                context, shifts, adjustableResultList, sourceSkuMap, endingTypeMap,
                pairResultMap, baseSimulationCountMap);
        if (!this.needsBalance(state)) {
            log.info("共用胎胚收尾均衡无需调整, scheduleDate: {}, 可调整机台数: {}, "
                            + "原因: 早班/中班未超参考上限且每日总数无硬限风险",
                    context.getScheduleDate(), adjustableResultList.size());
            return false;
        }
        boolean adjusted = false;
        int maxRounds = Math.max(1, adjustableResultList.size() * shifts.size());
        Set<String> visitedStateKeySet = new LinkedHashSet<String>(maxRounds);
        for (int round = 1; round <= maxRounds && this.needsBalance(state); round++) {
            String stateKey = this.buildBalanceStateKey(state);
            if (!visitedStateKeySet.add(stateKey)) {
                this.recordUnresolvedBalanceReason(context, state, "调整状态重复，为避免无效循环终止当前均衡");
                break;
            }
            EmbryoEndingBalanceMove move = this.findFirstImprovingMove(
                    context, shifts, state, pairResultMap, baseSimulationCountMap);
            if (Objects.isNull(move)) {
                this.recordUnresolvedBalanceReason(
                        context, state, "总量守恒、班次产能、机台占用或禁止换模时间限制下无可行候选");
                break;
            }
            adjusted = true;
            /*
             * 分摊可能改变SKU内部目标额度，补量/减量也可能改变最后有量班次。
             * 每次提交后必须从真实结果重新解析候选、收尾类型和共用胎胚分组；若某胎胚组
             * 已不足两台，立即从后续轮次移除，不能继续使用提交前的静态候选集合。
             */
            sourceSkuMap = new IdentityHashMap<LhScheduleResult, SkuScheduleDTO>(16);
            endingTypeMap = new IdentityHashMap<LhScheduleResult, String>(16);
            pairResultMap = new IdentityHashMap<LhScheduleResult, LhScheduleResult>(8);
            fixedCountResultList = new ArrayList<LhScheduleResult>(16);
            adjustableResultList = this.collectAdjustableBalanceCandidates(
                    context, sourceSkuMap, endingTypeMap, pairResultMap, fixedCountResultList);
            if (adjustableResultList.size() < MIN_GROUP_MACHINE_COUNT) {
                log.info("共用胎胚收尾均衡结束, scheduleDate: {}, 轮次: {}, 原因: 调整后无两台以上可调整胎胚组",
                        context.getScheduleDate(), round);
                break;
            }
            baseSimulationCountMap = this.buildFixedSimulationCountMap(
                    context, fixedCountResultList, sourceSkuMap, originalCountMap);
            state = this.buildBalanceState(
                    context, shifts, adjustableResultList, sourceSkuMap, endingTypeMap,
                    pairResultMap, baseSimulationCountMap);
            log.info("共用胎胚收尾均衡提交, scheduleDate: {}, 轮次: {}, 移动类型: {}, 调整后计数: {}",
                    context.getScheduleDate(), round, move.getMoveType(),
                    this.buildCountSummary(state.getSimulatedCountMap()));
        }
        return adjusted;
    }

    /**
     * 收集续作收尾均衡候选机台。
     * <p>排除：非续作结果、换活字块结果、零计划结果、同物料多产品状态续作切换结果。
     * 候选保留结果行的收尾班次、收尾班次计划量和来源SKU。</p>
     * <p>结构停产保机与续作停产保机机台不再排除：保机仅拦截后续不同结构SKU上机，
     * 不影响该机台参与当前收尾时间与换模次数均衡。</p>
     *
     * @param context 排程上下文
     * @param candidateList 候选结果列表
     * @param sourceSkuMap 来源SKU映射
     * @param endingTypeMap 收尾类型映射
     * @param pairResultMap 双模SKU单控整机代表结果到配对侧结果的映射
     */
    private void collectBalanceCandidates(LhScheduleContext context,
                                          List<LhScheduleResult> candidateList,
                                          Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap,
                                          Map<LhScheduleResult, String> endingTypeMap,
                                          Map<LhScheduleResult, LhScheduleResult> pairResultMap) {
        // 双模SKU单控整机按物理机台去重，L/R两侧只保留一台代表结果，避免重复预测和重复计数。
        Set<String> processedWholeMachineCodeSet = new HashSet<String>(4);
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!isBalanceCandidateResult(context, result)) {
                continue;
            }
            SkuScheduleDTO sourceSku = resolveResultSourceSku(context, result);
            if (Objects.isNull(sourceSku) || StringUtils.isEmpty(sourceSku.getEmbryoCode())) {
                continue;
            }
            int endingShiftIndex = ShiftFieldUtil.resolveLastPlannedShiftIndex(result);
            if (endingShiftIndex <= 0) {
                continue;
            }
            Integer endingQty = ShiftFieldUtil.getShiftPlanQty(result, endingShiftIndex);
            if (Objects.isNull(endingQty) || endingQty <= 0) {
                continue;
            }
            String endingType = resolveEndingType(context, result, sourceSku);
            // 普通续作结果不属于任何可均衡收尾类型时直接排除，不参与任何移动。
            if (StringUtils.isEmpty(endingType)) {
                continue;
            }
            // 双模SKU单控整机：L/R必须作为同一台物理机台参与均衡，两侧同步调整。
            LhScheduleResult pairResult = resolveWholePairResult(context, result, sourceSku);
            if (Objects.nonNull(pairResult)) {
                String physicalMachineCode =
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode());
                if (!processedWholeMachineCodeSet.add(physicalMachineCode)) {
                    // 配对侧已经由代表结果登记，跳过避免重复候选。
                    continue;
                }
                LhScheduleResult representative =
                        LhSingleControlMachineUtil.isLeftSide(result.getLhMachineCode())
                                ? result : pairResult;
                LhScheduleResult pairSide = representative == result ? pairResult : result;
                int representativeEndingShift = ShiftFieldUtil.resolveLastPlannedShiftIndex(representative);
                // 两侧收尾班次不一致时不能成对均衡，整机组跳过，交由保存前整机校验阻断。
                if (representativeEndingShift <= 0
                        || representativeEndingShift
                        != ShiftFieldUtil.resolveLastPlannedShiftIndex(pairSide)) {
                    log.warn("共用胎胚收尾均衡跳过单控整机组, scheduleDate: {}, materialCode: {}, "
                                    + "physicalMachineCode: {}, 原因: L/R两侧收尾班次不一致",
                            context.getScheduleDate(), sourceSku.getMaterialCode(), physicalMachineCode);
                    continue;
                }
                pairResultMap.put(representative, pairSide);
                candidateList.add(representative);
                sourceSkuMap.put(representative, sourceSku);
                endingTypeMap.put(representative, endingType);
                continue;
            }
            candidateList.add(result);
            sourceSkuMap.put(result, sourceSku);
            endingTypeMap.put(result, endingType);
        }
    }

    /**
     * 重新收集当前仍可调整的共用胎胚多机台组。
     * <p>先复用统一候选判断排除零量等不产生可统计收尾事件的结果，再按胎胚编码预分组；
     * 停产保机机台只要仍有正量收尾班次，同样进入候选。
     * 运行态非共用胎胚或不足两台的组只进入固定计数，不进入调整集合。该方法在首轮和每次
     * 成功提交后调用，保证收尾类型、SKU额度、固定计数和分组判断始终来自最新排程结果。</p>
     *
     * @param context 排程上下文
     * @param sourceSkuMap 返回结果到来源SKU的身份映射
     * @param endingTypeMap 返回结果到收尾类型的映射
     * @param pairResultMap 返回单控整机代表侧到配对侧的映射
     * @param fixedCountResultList 返回不可调整、但仍需占用换模统计的结果
     * @return 当前仍可调整的结果列表
     */
    private List<LhScheduleResult> collectAdjustableBalanceCandidates(
            LhScheduleContext context,
            Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap,
            Map<LhScheduleResult, String> endingTypeMap,
            Map<LhScheduleResult, LhScheduleResult> pairResultMap,
            List<LhScheduleResult> fixedCountResultList) {
        List<LhScheduleResult> candidateList = new ArrayList<LhScheduleResult>(16);
        this.collectBalanceCandidates(
                context, candidateList, sourceSkuMap, endingTypeMap, pairResultMap);
        if (candidateList.size() < MIN_GROUP_MACHINE_COUNT) {
            return new ArrayList<LhScheduleResult>(0);
        }
        Map<String, List<LhScheduleResult>> embryoGroupMap =
                this.groupCandidatesByEmbryo(candidateList, sourceSkuMap);
        List<LhScheduleResult> adjustableResultList =
                new ArrayList<LhScheduleResult>(candidateList.size());
        for (List<LhScheduleResult> groupResultList : embryoGroupMap.values()) {
            SkuScheduleDTO groupSourceSku = CollectionUtils.isEmpty(groupResultList)
                    ? null : sourceSkuMap.get(groupResultList.get(0));
            /*
             * 非共用胎胚和不足两台的组禁止调整，但其后物料换模事件仍属于当天真实次数，
             * 必须先写入本地统计基线，不能因为“不参与均衡”就从早/中/日次数中消失。
             */
            if (groupResultList.size() >= MIN_GROUP_MACHINE_COUNT
                    && this.isRuntimeSharedEmbryo(context, groupSourceSku)) {
                adjustableResultList.addAll(groupResultList);
            }
        }
        // 登记本轮仍满足均衡适用范围的物理机台快照（单控整机按物理机台去重），
        // 供过程对账和最终未均衡原因分类使用。
        for (LhScheduleResult adjustableResult : adjustableResultList) {
            if (Objects.nonNull(adjustableResult)
                    && StringUtils.isNotEmpty(adjustableResult.getLhMachineCode())) {
                context.getSharedEmbryoEndingBalanceEligibleMachineCodeSet()
                        .add(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                                adjustableResult.getLhMachineCode()));
            }
        }
        Set<LhScheduleResult> adjustableResultSet =
                Collections.newSetFromMap(new IdentityHashMap<LhScheduleResult, Boolean>(
                        Math.max(1, adjustableResultList.size())));
        adjustableResultSet.addAll(adjustableResultList);
        for (LhScheduleResult candidate : candidateList) {
            if (!adjustableResultSet.contains(candidate)) {
                fixedCountResultList.add(candidate);
            }
        }
        return adjustableResultList;
    }

    /**
     * 将不可调整的续作收尾事件预演到统计基线。
     * <p>非共用胎胚、共用胎胚单机台组等结果虽然不能搬量，仍须按项目现有预演口径占用一次
     * 换模/换活字块次数，保证可调整组看到的是完整的早/中/日负荷。
     * 停产保机机台即使未进入调整组，也按实际生产结束时间参与固定计数。</p>
     *
     * @param context 排程上下文
     * @param fixedCountResultList 不可调整但需要计数的结果
     * @param sourceSkuMap 结果到来源SKU的映射
     * @param originalCountMap 当前真实次数账本
     * @return 已加入固定事件的本地预演基线
     */
    private Map<String, int[]> buildFixedSimulationCountMap(
            LhScheduleContext context,
            List<LhScheduleResult> fixedCountResultList,
            Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap,
            Map<String, int[]> originalCountMap) {
        Map<String, int[]> fixedSimulationCountMap =
                this.copyDailyMouldChangeCountMap(originalCountMap);
        if (CollectionUtils.isEmpty(fixedCountResultList)) {
            return fixedSimulationCountMap;
        }
        List<LhScheduleResult> sortedFixedResultList =
                new ArrayList<LhScheduleResult>(fixedCountResultList);
        sortedFixedResultList.sort(Comparator.comparing(
                result -> StringUtils.defaultString(result.getLhMachineCode())));
        for (LhScheduleResult result : sortedFixedResultList) {
            int endingShiftIndex = ShiftFieldUtil.resolveLastPlannedShiftIndex(result);
            // 固定计数使用实际生产结束时间，避免停产保机机台被占用边界（统一释放时间/窗口末班）后移的结束时间
            // 把后物料换模错误预测到窗口末班之后。
            Date endingTime = this.resolveBalanceEndingTime(context, result, endingShiftIndex);
            if (Objects.isNull(endingTime)) {
                continue;
            }
            this.mouldChangeBalanceStrategy.previewEndingStaggerMouldChange(
                    context, result.getLhMachineCode(), endingTime,
                    LhScheduleTimeUtil.getMouldChangeTotalHours(context),
                    sourceSkuMap.get(result), fixedSimulationCountMap);
        }
        return fixedSimulationCountMap;
    }

    /**
     * 查找双模SKU单控整机当前结果的配对侧候选结果。
     * <p>只有冻结为整机粒度的正规SKU且左右侧同时存在、同物料同状态、同为有效候选时返回配对侧，
     * 保证均衡阶段不会把L/R拆成两台独立机台。</p>
     *
     * @param context 排程上下文
     * @param result 当前结果
     * @param sourceSku 当前结果来源SKU
     * @return 配对侧结果；不适用或配对侧缺失时返回null
     */
    private LhScheduleResult resolveWholePairResult(LhScheduleContext context,
                                                    LhScheduleResult result,
                                                    SkuScheduleDTO sourceSku) {
        if (Objects.isNull(context) || Objects.isNull(result) || Objects.isNull(sourceSku)
                || StringUtils.isEmpty(result.getLhMachineCode())
                || !LhSingleControlMachineUtil.isConfiguredSingleControlMachine(
                        context, result.getLhMachineCode())
                || !LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sourceSku)) {
            return null;
        }
        String pairMachineCode = LhSingleControlMachineUtil.resolvePairMachineCode(result.getLhMachineCode());
        if (StringUtils.isEmpty(pairMachineCode)) {
            return null;
        }
        for (LhScheduleResult candidate : context.getScheduleResultList()) {
            if (Objects.isNull(candidate) || candidate == result
                    || !StringUtils.equals(pairMachineCode, candidate.getLhMachineCode())) {
                continue;
            }
            if (StringUtils.equals(result.getMaterialCode(), candidate.getMaterialCode())
                    && StringUtils.equals(StringUtils.trimToEmpty(result.getProductStatus()),
                    StringUtils.trimToEmpty(candidate.getProductStatus()))
                    && isBalanceCandidateResult(context, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 判断结果是否允许进入收尾均衡候选。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @return true-允许进入候选；false-跳过
     */
    private boolean isBalanceCandidateResult(LhScheduleContext context, LhScheduleResult result) {
        if (Objects.isNull(result) || !CONTINUOUS_SCHEDULE_TYPE.equals(result.getScheduleType())
                || "1".equals(result.getIsTypeBlock())
                || StringUtils.isEmpty(result.getLhMachineCode())) {
            return false;
        }
        /*
         * 结构停产保机与续作停产保机机台必须参与收尾均衡：保机只影响后续不同结构SKU
         * 能否上机，不影响该机台当前收尾时间和换模次数的均衡调整。零计划量占位结果
         * 仍由下方正量检查自然排除。
         */
        // 同物料多产品状态续作切换保持专用链精确尾量，不参与收尾均衡。
        if (isSameMaterialMultiStatusSwitchResult(result)) {
            return false;
        }
        return ShiftFieldUtil.resolveScheduledQty(result) > 0;
    }

    /**
     * 判断结果是否属于同物料多产品状态续作切换。
     *
     * @param result 排程结果
     * @return true-命中；false-未命中
     */
    private boolean isSameMaterialMultiStatusSwitchResult(LhScheduleResult result) {
        if (Objects.isNull(result)) {
            return false;
        }
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            String analysis = ShiftFieldUtil.getShiftAnalysis(result, shiftIndex);
            if (StringUtils.isNotEmpty(analysis)
                    && analysis.contains(LhScheduleConstant.SAME_MATERIAL_STATUS_CONTINUATION_ANALYSIS)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析结果对应的来源SKU。
     * <p>优先命中运行态身份映射，未命中时按物料+产品状态回退到全量SKU索引。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @return 来源SKU；无法解析时返回null
     */
    private SkuScheduleDTO resolveResultSourceSku(LhScheduleContext context, LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)) {
            return null;
        }
        SkuScheduleDTO sourceSku = context.getScheduleResultSourceSkuMap().get(result);
        if (Objects.nonNull(sourceSku)) {
            return sourceSku;
        }
        Map<String, SkuScheduleDTO> allSkuMap = context.getAllSkuScheduleDtoMap();
        if (CollectionUtils.isEmpty(allSkuMap) || StringUtils.isEmpty(result.getMaterialCode())) {
            return null;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                result.getMaterialCode(), StringUtils.trimToEmpty(result.getProductStatus()));
        return allSkuMap.get(skuKey);
    }

    /**
     * 解析收尾类型。
     * <p>优先级：胎胚收尾（胎胚收尾标识=1）-> SKU余量收尾（SKU收尾标记）-> 按时间下机收尾。</p>
     *
     * @param context 排程上下文
     * @param sourceSku 来源SKU
     * @return 收尾类型
     */
    private String resolveEndingType(LhScheduleContext context,
                                     LhScheduleResult result,
                                     SkuScheduleDTO sourceSku) {
        if (Objects.isNull(context) || Objects.isNull(result) || Objects.isNull(sourceSku)) {
            return ENDING_TYPE_TIME;
        }
        // 胎胚收尾：必须命中胎胚库存硬目标口径（含共用胎胚T日收尾/单胎胚收尾门控），
        // 不能只看胎胚收尾标识，否则会把普通共用胎胚也误判为胎胚收尾并允许跨物料互转。
        if (targetScheduleQtyResolver.isEmbryoStockEnding(context, sourceSku)) {
            return ENDING_TYPE_EMBRYO;
        }
        if (StringUtils.equals(SkuTagEnum.ENDING.getCode(), sourceSku.getSkuTag())) {
            return ENDING_TYPE_SKU;
        }
        // 按时间下机：只允许真正降模释放机台，且胎胚仍在机、非胎胚收尾。
        if (isReducedTimeOfflineResult(context, result)
                && isRuntimeSharedEmbryo(context, sourceSku)
                && isEmbryoOnMachine(context, sourceSku)) {
            return ENDING_TYPE_TIME;
        }
        // 普通续作结果既不收尾也不按时间下机，不进入均衡候选。
        return null;
    }

    /**
     * 判断结果是否属于真正降模释放的按时间下机机台。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @return true-降模释放机台；false-普通续作机台
     */
    private boolean isReducedTimeOfflineResult(LhScheduleContext context, LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || StringUtils.isEmpty(result.getLhMachineCode())) {
            return false;
        }
        // 真正降模决策会登记最后允许生产班次，或把机台挂到“降模前物料”运行态映射。
        return Objects.nonNull(context.getContinuousReducedMachineReleaseBoundaryShiftIndex(
                result.getLhMachineCode()))
                || !CollectionUtils.isEmpty(context.getReducedContinuationMachineBeforeSkuMap())
                && context.getReducedContinuationMachineBeforeSkuMap().containsKey(result.getLhMachineCode());
    }

    /**
     * 判断SKU是否仍属于运行态共用胎胚。
     *
     * @param context 排程上下文
     * @param sourceSku 来源SKU
     * @return true-共用胎胚；false-单胎胚或已动态剔除
     */
    private boolean isRuntimeSharedEmbryo(LhScheduleContext context, SkuScheduleDTO sourceSku) {
        return Objects.nonNull(context) && Objects.nonNull(sourceSku)
                && targetScheduleQtyResolver.isSharedEmbryoInWindow(context, sourceSku);
    }

    /**
     * 判断胎胚是否仍在机（胎胚收尾标识为0）。
     *
     * @param context 排程上下文
     * @param sourceSku 来源SKU
     * @return true-胎胚在机；false-缺失标识或非0
     */
    private boolean isEmbryoOnMachine(LhScheduleContext context, SkuScheduleDTO sourceSku) {
        if (Objects.isNull(context) || Objects.isNull(sourceSku)
                || StringUtils.isEmpty(sourceSku.getEmbryoCode())
                || CollectionUtils.isEmpty(context.getEmbryoEndingFlagMap())) {
            return false;
        }
        return EMBRYO_ON_MACHINE_ENDING_FLAG == context.getEmbryoEndingFlagMap()
                .getOrDefault(sourceSku.getEmbryoCode(), -1);
    }

    /**
     * 解析均衡预演使用的真实生产收尾时间。
     *
     * <p>停产保机机台的 {@code specEndTime} 可能已被占用边界后移（结构保机统一释放时间、
     * 续作停产保机窗口末班占用），换模/换活字块预演必须使用实际生产结束时间：
     * 结构保机优先取冻结的实际生产结束时间快照，否则取最后有量班次结束时间。</p>
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param endingShiftIndex 最后有量班次序号
     * @return 实际生产结束时间；无法解析时返回null
     */
    private Date resolveBalanceEndingTime(LhScheduleContext context,
                                          LhScheduleResult result,
                                          int endingShiftIndex) {
        if (Objects.isNull(result)) {
            return null;
        }
        String machineCode = result.getLhMachineCode();
        if (Objects.nonNull(context) && context.isStructureMinMachineRetained(machineCode)) {
            // 结构停产保机机台优先读取冻结的实际生产结束时间，与保机快照保持一致。
            Date actualEndTime = context.getStructureMinMachineRetentionActualEndTimeMap()
                    .get(machineCode);
            if (Objects.nonNull(actualEndTime)) {
                return actualEndTime;
            }
        }
        // 普通机台直接取最后有量班次的结束时间，不依赖可能被占用边界后移的specEndTime。
        Date shiftEndTime = endingShiftIndex > 0
                ? ShiftFieldUtil.getShiftEndTime(result, endingShiftIndex) : null;
        return Objects.nonNull(shiftEndTime) ? shiftEndTime : result.getSpecEndTime();
    }

    /**
     * 按胎胚编码对候选机台分组。
     *
     * @param candidateList 候选结果列表
     * @param sourceSkuMap 来源SKU映射
     * @return 胎胚编码到候选机台列表的映射
     */
    private Map<String, List<LhScheduleResult>> groupCandidatesByEmbryo(
            List<LhScheduleResult> candidateList,
            Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap) {
        Map<String, List<LhScheduleResult>> embryoGroupMap = new LinkedHashMap<String, List<LhScheduleResult>>(8);
        for (LhScheduleResult result : candidateList) {
            SkuScheduleDTO sourceSku = sourceSkuMap.get(result);
            String embryoCode = Objects.nonNull(sourceSku) ? sourceSku.getEmbryoCode() : result.getEmbryoCode();
            if (StringUtils.isEmpty(embryoCode)) {
                continue;
            }
            embryoGroupMap.computeIfAbsent(embryoCode, key -> new ArrayList<LhScheduleResult>(2)).add(result);
        }
        return embryoGroupMap;
    }

    /**
     * 按确定性顺序查找第一个能让整体评分严格变优的移动，并直接提交。
     * <p>移动顺序：尾量分摊（机台编码升序）-> 后延补量 -> 提前减量。
     * 每个移动先快照再执行，若评分未改善则恢复尝试前状态，保证失败无脏数据。</p>
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param state 当前均衡状态
     * @param pairResultMap 双模SKU单控整机代表结果到配对侧结果的映射
     * @param baseSimulationCountMap 当前胎胚组开始前的全局换模预演基线
     * @return 已提交的移动；无可行改善时返回null
     */
    private EmbryoEndingBalanceMove findFirstImprovingMove(LhScheduleContext context,
                                                           List<LhShiftConfigVO> shifts,
                                                           EmbryoEndingBalanceState state,
                                                           Map<LhScheduleResult, LhScheduleResult> pairResultMap,
                                                           Map<String, int[]> baseSimulationCountMap) {
        List<EmbryoEndingBalanceMove> moveList = buildCandidateMoves(context, shifts, state);
        if (CollectionUtils.isEmpty(moveList)) {
            return null;
        }
        BalanceOverflowBucket overflowBucket = this.resolveFirstOverflowBucket(context, state);
        for (EmbryoEndingBalanceMove move : moveList) {
            // 每轮只处理日期升序下的首个超限班次，同日先早班后中班。
            if (Objects.nonNull(overflowBucket)
                    && !this.isMoveRelatedToOverflowBucket(context, state, move, overflowBucket)) {
                continue;
            }
            Set<LhScheduleResult> affectedResultSet = new LinkedHashSet<LhScheduleResult>(2);
            affectedResultSet.add(move.getDonor());
            if (Objects.nonNull(move.getReceiver())) {
                affectedResultSet.add(move.getReceiver());
            }
            // 每次尝试只复制转出/承接结果及其单控配对侧，不深拷贝全部候选或完整排程结果。
            BalanceSnapshot snapshot = snapshotGroupState(
                    context, affectedResultSet, pairResultMap);
            boolean applied = executeMove(context, shifts, move, state);
            if (!applied) {
                this.appendRejectedMoveProcessLog(context, shifts, move, state, null,
                        "执行失败：班次产能、其他SKU占用、降模边界、胶囊或单控配对校验未通过");
                restoreGroupState(snapshot);
                continue;
            }
            EmbryoEndingBalanceState nextState = buildBalanceState(
                    context, shifts, new ArrayList<LhScheduleResult>(state.getSourceSkuMap().keySet()),
                    state.getSourceSkuMap(), state.getEndingTypeMap(), pairResultMap,
                    baseSimulationCountMap);
            if (compareScore(nextState, state) >= 0) {
                this.appendRejectedMoveProcessLog(context, shifts, move, state, nextState,
                        "评分未严格变优（目标班次次数未下降或产生其他超限）");
                restoreGroupState(snapshot);
                continue;
            }
            if (!this.isOverflowBucketImproved(state, nextState, overflowBucket)) {
                this.appendRejectedMoveProcessLog(context, shifts, move, state, nextState,
                        "目标班次次数没有下降");
                restoreGroupState(snapshot);
                continue;
            }
            if (!this.doesNotCreateNewShiftOverflow(context, state, nextState)) {
                this.appendRejectedMoveProcessLog(context, shifts, move, state, nextState,
                        "制造另一班次超限");
                restoreGroupState(snapshot);
                continue;
            }
            if (!this.isCrossDayProductionCompleted(context, state, nextState, move)) {
                this.appendRejectedMoveProcessLog(context, shifts, move, state, nextState,
                        "跨天未连续生产到目标06:00");
                restoreGroupState(snapshot);
                continue;
            }
            // 评分严格变优后才落地跨物料互转的SKU额度重分配，避免失败尝试污染账本。
            if (!commitMoveQuotaChanges(context, move, state)) {
                this.appendRejectedMoveProcessLog(context, shifts, move, state, nextState,
                        "总量/额度账本不守恒");
                restoreGroupState(snapshot);
                continue;
            }
            appendMoveProcessLog(context, shifts, move, state, nextState);
            return move;
        }
        return null;
    }

    /**
     * 校验因每日硬上限跨天的调整已经把前物料真实生产到目标早班06:00。
     * <p>若分摊后该机台的换模已经回到原日期，说明通过合并换模事件解决，
     * 无需跨天补量；若仍落在原跨天日期，则收尾时间必须与换模可就绪的06:00完全一致。</p>
     *
     * @param context 排程上下文
     * @param beforeState 调整前状态
     * @param afterState 调整后状态
     * @param move 当前移动
     * @return true-非跨天或已连续生产到目标时间
     */
    private boolean isCrossDayProductionCompleted(LhScheduleContext context,
                                                  EmbryoEndingBalanceState beforeState,
                                                  EmbryoEndingBalanceState afterState,
                                                  EmbryoEndingBalanceMove move) {
        List<LhScheduleResult> affectedResultList = new ArrayList<LhScheduleResult>(2);
        affectedResultList.add(move.getDonor());
        if (Objects.nonNull(move.getReceiver())) {
            affectedResultList.add(move.getReceiver());
        }
        for (LhScheduleResult result : affectedResultList) {
            if (!this.isMouldChangeDeferredByDailyLimit(context, beforeState, result)) {
                continue;
            }
            Date beforeAssignedTime = beforeState.getChangeTimeMap().get(result);
            Date afterChangeTime = afterState.getChangeTimeMap().get(result);
            Date afterEndingTime = afterState.getEndingTimeMap().get(result);
            if (Objects.isNull(afterChangeTime)) {
                return false;
            }
            if (LhScheduleTimeUtil.clearTime(afterChangeTime)
                    .before(LhScheduleTimeUtil.clearTime(beforeAssignedTime))) {
                continue;
            }
            if (!afterChangeTime.equals(afterEndingTime)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析首个需要处理的日期班次桶。
     * <p>日期使用yyyy-MM-dd自然顺序，同日固定先检查早班再检查中班。
     * 预演被每日硬上限拒绝时无法归属某个已计数班次，返回null交由硬限评分处理。</p>
     *
     * @param context 排程上下文
     * @param state 当前均衡状态
     * @return 首个超限桶；无班次超限时返回null
     */
    private BalanceOverflowBucket resolveFirstOverflowBucket(LhScheduleContext context,
                                                             EmbryoEndingBalanceState state) {
        List<String> dateKeyList = new ArrayList<String>(state.getSimulatedCountMap().keySet());
        dateKeyList.sort(String::compareTo);
        int morningLimit = LhScheduleTimeUtil.getMorningMouldChangeLimit(context);
        int afternoonLimit = LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context);
        for (String dateKey : dateKeyList) {
            int[] counts = state.getSimulatedCountMap().get(dateKey);
            int morningCount = Objects.nonNull(counts) && counts.length > 0 ? counts[0] : 0;
            int afternoonCount = Objects.nonNull(counts) && counts.length > 1 ? counts[1] : 0;
            if (morningCount > morningLimit
                    && this.hasCandidateInCountBucket(context, state, dateKey, 0)) {
                return new BalanceOverflowBucket(dateKey, 0, morningCount, morningLimit);
            }
            if (afternoonCount > afternoonLimit
                    && this.hasCandidateInCountBucket(context, state, dateKey, 1)) {
                return new BalanceOverflowBucket(dateKey, 1, afternoonCount, afternoonLimit);
            }
        }
        return null;
    }

    /**
     * 判断计数桶内是否至少有一台当前共用胎胚候选机台。
     * <p>真实账本基线中已存在、但不属于当前均衡候选的历史超限不能通过修改
     * 其他日期的收尾机台解决，因此跳过该桶，最终校验仍会保留实际问题。</p>
     *
     * @param context 排程上下文
     * @param state 当前状态
     * @param dateKey 日期键
     * @param shiftCountIndex 班次计数下标：0-早班，1-中班
     * @return true-存在可调整候选
     */
    private boolean hasCandidateInCountBucket(LhScheduleContext context,
                                              EmbryoEndingBalanceState state,
                                              String dateKey,
                                              int shiftCountIndex) {
        BalanceOverflowBucket bucket = new BalanceOverflowBucket(dateKey, shiftCountIndex, 0, 0);
        for (LhScheduleResult result : state.getChangeTimeMap().keySet()) {
            if (this.isResultInOverflowBucket(context, state, result, bucket)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断移动是否直接调整当前超限班次已登记的机台。
     *
     * @param context 排程上下文
     * @param state 当前状态
     * @param move 候选移动
     * @param bucket 当前超限桶
     * @return true-移动的转出或承接机台位于该桶
     */
    private boolean isMoveRelatedToOverflowBucket(LhScheduleContext context,
                                                  EmbryoEndingBalanceState state,
                                                  EmbryoEndingBalanceMove move,
                                                  BalanceOverflowBucket bucket) {
        return this.isResultInOverflowBucket(context, state, move.getDonor(), bucket)
                || this.isResultInOverflowBucket(context, state, move.getReceiver(), bucket);
    }

    /**
     * 判断指定结果的预测换模落点是否位于目标桶。
     *
     * @param context 排程上下文
     * @param state 当前状态
     * @param result 排程结果
     * @param bucket 目标桶
     * @return true-位于目标桶
     */
    private boolean isResultInOverflowBucket(LhScheduleContext context,
                                             EmbryoEndingBalanceState state,
                                             LhScheduleResult result,
                                             BalanceOverflowBucket bucket) {
        if (Objects.isNull(result) || Objects.isNull(bucket)) {
            return false;
        }
        Date changeTime = state.getChangeTimeMap().get(result);
        if (Objects.isNull(changeTime)
                || !StringUtils.equals(bucket.getDateKey(), LhScheduleTimeUtil.formatDate(changeTime))) {
            return false;
        }
        return bucket.getShiftCountIndex() == 0
                ? LhScheduleTimeUtil.isMorningShift(context, changeTime)
                : LhScheduleTimeUtil.isAfternoonShift(context, changeTime);
    }

    /**
     * 校验当前调整已使目标班次次数严格下降。
     *
     * @param beforeState 调整前状态
     * @param afterState 调整后状态
     * @param bucket 目标超限桶；null表示当前只处理硬限风险
     * @return true-目标桶严格改善或硬限评分改善
     */
    private boolean isOverflowBucketImproved(EmbryoEndingBalanceState beforeState,
                                             EmbryoEndingBalanceState afterState,
                                             BalanceOverflowBucket bucket) {
        if (Objects.isNull(bucket)) {
            return afterState.getHardViolationCount() < beforeState.getHardViolationCount()
                    || afterState.getDailyLimitDeferredCount()
                    < beforeState.getDailyLimitDeferredCount();
        }
        return this.resolveBucketCount(afterState, bucket) < this.resolveBucketCount(beforeState, bucket);
    }

    /**
     * 取状态中指定班次桶的次数。
     *
     * @param state 均衡状态
     * @param bucket 班次桶
     * @return 当前次数
     */
    private int resolveBucketCount(EmbryoEndingBalanceState state, BalanceOverflowBucket bucket) {
        int[] counts = state.getSimulatedCountMap().get(bucket.getDateKey());
        return Objects.nonNull(counts) && counts.length > bucket.getShiftCountIndex()
                ? counts[bucket.getShiftCountIndex()] : 0;
    }

    /**
     * 校验调整不会把原本未超限的早班/中班/全日推成新的超限。
     * <p>已经超限的班次或日期只允许严格降低次数，从而支持多轮逐台消减初始超限；
     * 禁止为了降低当前早班或中班次数，把其他班次或日期的超限程度加重。</p>
     *
     * @param context 排程上下文
     * @param beforeState 调整前状态
     * @param afterState 调整后状态
     * @return true-没有新超限；false-会制造或加重其他班次超限
     */
    private boolean doesNotCreateNewShiftOverflow(LhScheduleContext context,
                                                  EmbryoEndingBalanceState beforeState,
                                                  EmbryoEndingBalanceState afterState) {
        Set<String> dateKeySet = new LinkedHashSet<String>(beforeState.getSimulatedCountMap().keySet());
        dateKeySet.addAll(afterState.getSimulatedCountMap().keySet());
        int[] limits = new int[]{LhScheduleTimeUtil.getMorningMouldChangeLimit(context),
                LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context)};
        int dailyLimit = LhScheduleTimeUtil.getDailyMouldChangeLimit(context);
        for (String dateKey : dateKeySet) {
            int[] beforeCounts = beforeState.getSimulatedCountMap().getOrDefault(dateKey, new int[]{0, 0});
            int[] afterCounts = afterState.getSimulatedCountMap().getOrDefault(dateKey, new int[]{0, 0});
            int beforeDailyCount = beforeCounts[0] + beforeCounts[1];
            int afterDailyCount = afterCounts[0] + afterCounts[1];
            if (afterDailyCount > dailyLimit && afterDailyCount >= beforeDailyCount) {
                return false;
            }
            for (int shiftCountIndex = 0; shiftCountIndex < limits.length; shiftCountIndex++) {
                if (afterCounts[shiftCountIndex] > limits[shiftCountIndex]
                        && afterCounts[shiftCountIndex] > beforeCounts[shiftCountIndex]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 构建当前胎胚组的均衡状态：预测各机台后物料的换模落点并计算评分。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param groupResults 组内候选机台结果
     * @param sourceSkuMap 来源SKU映射
     * @param endingTypeMap 收尾类型映射
     * @param pairResultMap 双模SKU单控整机代表结果到配对侧结果的映射
     * @param baseSimulationCountMap 当前胎胚组开始前的全局换模预演基线
     * @return 均衡状态
     */
    private EmbryoEndingBalanceState buildBalanceState(LhScheduleContext context,
                                                       List<LhShiftConfigVO> shifts,
                                                       List<LhScheduleResult> groupResults,
                                                       Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap,
                                                       Map<LhScheduleResult, String> endingTypeMap,
                                                       Map<LhScheduleResult, LhScheduleResult> pairResultMap,
                                                       Map<String, int[]> baseSimulationCountMap) {
        EmbryoEndingBalanceState state = new EmbryoEndingBalanceState();
        state.setPairResultMap(pairResultMap);
        List<LhScheduleResult> sortedResults = new ArrayList<LhScheduleResult>(groupResults);
        // 机台编码升序保证预测和移动顺序稳定一致。
        sortedResults.sort(Comparator.comparing(
                result -> StringUtils.defaultString(result.getLhMachineCode())));
        Map<String, int[]> simulatedCountMap = copyDailyMouldChangeCountMap(baseSimulationCountMap);
        for (LhScheduleResult result : sortedResults) {
            int endingShiftIndex = ShiftFieldUtil.resolveLastPlannedShiftIndex(result);
            if (endingShiftIndex <= 0) {
                continue;
            }
            Integer endingQty = ShiftFieldUtil.getShiftPlanQty(result, endingShiftIndex);
            if (Objects.isNull(endingQty) || endingQty <= 0) {
                continue;
            }
            LhShiftConfigVO endingShift = findShiftByIndex(shifts, endingShiftIndex);
            if (Objects.isNull(endingShift)) {
                continue;
            }
            // 停产保机机台的specEndTime可能已被占用边界后移，预演统一使用实际生产结束时间。
            Date endingTime = this.resolveBalanceEndingTime(context, result, endingShiftIndex);
            if (Objects.isNull(endingTime)) {
                endingTime = endingShift.getShiftEndDateTime();
            }
            SkuScheduleDTO sourceSku = sourceSkuMap.get(result);
            Date changeTime = mouldChangeBalanceStrategy.previewEndingStaggerMouldChange(
                    context, result.getLhMachineCode(), endingTime,
                    LhScheduleTimeUtil.getMouldChangeTotalHours(context), sourceSku, simulatedCountMap);
            state.getEndingShiftMap().put(result, endingShiftIndex);
            state.getEndingQtyMap().put(result, endingQty);
            state.getEndingTimeMap().put(result, endingTime);
            state.getChangeTimeMap().put(result, changeTime);
            state.getSourceSkuMap().put(result, sourceSku);
            state.getEndingTypeMap().put(result, endingTypeMap.get(result));
        }
        state.setSimulatedCountMap(simulatedCountMap);
        computeScore(context, state);
        int dailyLimitDeferredCount = 0;
        for (LhScheduleResult result : state.getChangeTimeMap().keySet()) {
            if (this.isMouldChangeDeferredByDailyLimit(context, state, result)) {
                dailyLimitDeferredCount++;
            }
        }
        state.setDailyLimitDeferredCount(dailyLimitDeferredCount);
        return state;
    }

    /**
     * 判断当前状态是否需要继续均衡。
     *
     * @param state 均衡状态
     * @return true-需要；false-无需
     */
    private boolean needsBalance(EmbryoEndingBalanceState state) {
        return state.getHardViolationCount() > 0
                || state.getDailyLimitDeferredCount() > 0
                || state.getExceededShiftCount() > 0;
    }

    /**
     * 计算状态评分：硬限制违反数、超软目标班次数、超目标累计次数、早中班比例偏差、同班次收尾集中度。
     *
     * @param context 排程上下文
     * @param state 均衡状态
     */
    private void computeScore(LhScheduleContext context, EmbryoEndingBalanceState state) {
        int hardViolationCount = 0;
        for (Date changeTime : state.getChangeTimeMap().values()) {
            // 预演返回null表示该落点被每日上限或禁换模规则拒绝，属于硬限制风险。
            if (Objects.isNull(changeTime)) {
                hardViolationCount++;
            }
        }
        int morningLimit = LhScheduleTimeUtil.getMorningMouldChangeLimit(context);
        int afternoonLimit = LhScheduleTimeUtil.getAfternoonMouldChangeLimit(context);
        int dailyLimit = LhScheduleTimeUtil.getDailyMouldChangeLimit(context);
        int exceededShiftCount = 0;
        int overflowQty = 0;
        long balanceDeviation = 0L;
        for (int[] counts : state.getSimulatedCountMap().values()) {
            int morningCount = counts.length > 0 ? counts[0] : 0;
            int afternoonCount = counts.length > 1 ? counts[1] : 0;
            if (morningCount + afternoonCount > dailyLimit) {
                hardViolationCount++;
            }
            if (morningCount > morningLimit) {
                exceededShiftCount++;
                overflowQty += morningCount - morningLimit;
            }
            if (afternoonCount > afternoonLimit) {
                exceededShiftCount++;
                overflowQty += afternoonCount - afternoonLimit;
            }
            balanceDeviation += Math.abs((long) morningCount * afternoonLimit
                    - (long) afternoonCount * morningLimit);
        }
        // 同班次收尾集中度仅用于日志对账，不是独立触发条件。
        Map<Integer, Integer> shiftBucketMap = new LinkedHashMap<Integer, Integer>(8);
        for (Integer endingShiftIndex : state.getEndingShiftMap().values()) {
            shiftBucketMap.merge(endingShiftIndex, 1, Integer::sum);
        }
        int sameShiftPairCount = 0;
        for (Integer bucketSize : shiftBucketMap.values()) {
            sameShiftPairCount += Math.max(0, bucketSize - 1);
        }
        state.setHardViolationCount(hardViolationCount);
        state.setExceededShiftCount(exceededShiftCount);
        state.setOverflowQty(overflowQty);
        state.setBalanceDeviation(balanceDeviation);
        state.setSameShiftPairCount(sameShiftPairCount);
    }

    /**
     * 比较两个状态的评分，返回小于0表示左侧更优。
     * <p>优先级与需求一致：先保证每日换模总次数硬限制，
     * 再消除早班8/中班7超限，同班次收尾集中度仅用于同分方案的稳定性比较。</p>
     *
     * @param left 左侧状态
     * @param right 右侧状态
     * @return 比较结果
     */
    private int compareScore(EmbryoEndingBalanceState left, EmbryoEndingBalanceState right) {
        int comparison = Integer.compare(left.getHardViolationCount(), right.getHardViolationCount());
        if (comparison != 0) {
            return comparison;
        }
        comparison = Integer.compare(
                left.getDailyLimitDeferredCount(), right.getDailyLimitDeferredCount());
        if (comparison != 0) {
            return comparison;
        }
        comparison = Integer.compare(left.getExceededShiftCount(), right.getExceededShiftCount());
        if (comparison != 0) {
            return comparison;
        }
        comparison = Integer.compare(left.getOverflowQty(), right.getOverflowQty());
        if (comparison != 0) {
            return comparison;
        }
        comparison = Long.compare(left.getBalanceDeviation(), right.getBalanceDeviation());
        if (comparison != 0) {
            return comparison;
        }
        return Integer.compare(left.getSameShiftPairCount(), right.getSameShiftPairCount());
    }

    /**
     * 构建候选移动列表，顺序：尾量分摊 -> 后延补量 -> 提前减量，组内按机台编码升序。
     * <p>对象优先级：同物料多机台（第一优先级）优先于共用胎胚组（第二优先级）；
     * 共用胎胚组间按机台数量降序、胎胚编码升序；同一对象组内按机台编码升序，
     * 同一机台再按 分摊->补量->减量 尝试，承接机台编码作为最后稳定键。</p>
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param state 当前均衡状态
     * @return 候选移动列表
     */
    private List<EmbryoEndingBalanceMove> buildCandidateMoves(LhScheduleContext context,
                                                              List<LhShiftConfigVO> shifts,
                                                              EmbryoEndingBalanceState state) {
        List<EmbryoEndingBalanceMove> moveList = new ArrayList<EmbryoEndingBalanceMove>(16);
        List<LhScheduleResult> sortedResults = new ArrayList<LhScheduleResult>(state.getSourceSkuMap().keySet());
        sortedResults.sort(Comparator.comparing(
                result -> StringUtils.defaultString(result.getLhMachineCode())));
        buildTransferMoves(context, shifts, state, sortedResults, moveList);
        buildPostponeMoves(context, shifts, state, sortedResults, moveList);
        buildAdvanceMoves(context, shifts, state, sortedResults, moveList);
        /*
         * 每轮从当前可调整候选重算同物料多机台分组和共用胎胚组排行，保证对象优先级
         * 不依赖集合原始遍历顺序：
         * 阶段（同物料多机台=0，共用胎胚组=1）-> 对象组排行 -> 组内机台编码升序 ->
         * 同一机台的移动类型顺序（分摊<补量<减量）-> 承接机台编码升序。
         */
        Map<LhScheduleResult, String> sameMaterialGroupKeyMap =
                this.resolveSameMaterialGroupKeyMap(state.getSourceSkuMap());
        Map<String, Integer> embryoRankMap =
                this.resolveEmbryoGroupRankMap(state.getSourceSkuMap());
        moveList.sort(Comparator
                .comparingInt((EmbryoEndingBalanceMove move) ->
                        this.resolveMovePhase(move, sameMaterialGroupKeyMap))
                .thenComparingInt(move -> this.resolveMoveGroupRank(
                        move, sameMaterialGroupKeyMap, state.getSourceSkuMap(), embryoRankMap))
                .thenComparing(move -> this.resolveMoveGroupKey(
                        move, sameMaterialGroupKeyMap, state.getSourceSkuMap()))
                .thenComparing(move ->
                        StringUtils.defaultString(move.getDonor().getLhMachineCode()))
                .thenComparingInt(move -> this.resolveMoveTypeOrder(move.getMoveType()))
                .thenComparing(move -> Objects.isNull(move.getReceiver()) ? ""
                        : StringUtils.defaultString(move.getReceiver().getLhMachineCode())));
        return moveList;
    }

    /**
     * 解析同一机台的均衡策略尝试顺序。
     *
     * @param moveType 移动类型
     * @return 顺序值，越小越优先
     */
    private int resolveMoveTypeOrder(String moveType) {
        if (MOVE_TYPE_TRANSFER.equals(moveType)) {
            return 0;
        }
        if (MOVE_TYPE_POSTPONE.equals(moveType)) {
            return 1;
        }
        return 2;
    }

    /**
     * 计算当前可调整候选中的同物料多机台分组键。
     *
     * <p>同物料口径为“物料编码+产品状态”（即SKU）。同一SKU存在两台及以上可调整
     * 收尾机台时，组内所有机台进入第一优先级，且不因收尾类型（胎胚收尾/余量收尾/
     * 按时间下机）不同而排除。</p>
     *
     * @param sourceSkuMap 当前可调整结果到来源SKU的映射
     * @return 结果到同物料分组键（SKU key）的映射；不在同物料多机台组中的结果不包含
     */
    private Map<LhScheduleResult, String> resolveSameMaterialGroupKeyMap(
            Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap) {
        Map<LhScheduleResult, String> groupKeyMap =
                new IdentityHashMap<LhScheduleResult, String>(16);
        if (CollectionUtils.isEmpty(sourceSkuMap)) {
            return groupKeyMap;
        }
        Map<String, Integer> skuMachineCountMap = new LinkedHashMap<String, Integer>(8);
        for (SkuScheduleDTO sourceSku : sourceSkuMap.values()) {
            if (Objects.isNull(sourceSku)) {
                continue;
            }
            skuMachineCountMap.merge(this.resolveSkuKey(sourceSku), 1, Integer::sum);
        }
        for (Map.Entry<LhScheduleResult, SkuScheduleDTO> entry : sourceSkuMap.entrySet()) {
            SkuScheduleDTO sourceSku = entry.getValue();
            String skuKey = Objects.isNull(sourceSku) ? null : this.resolveSkuKey(sourceSku);
            if (Objects.nonNull(skuKey)
                    && skuMachineCountMap.getOrDefault(skuKey, 0) >= MIN_GROUP_MACHINE_COUNT) {
                groupKeyMap.put(entry.getKey(), skuKey);
            }
        }
        return groupKeyMap;
    }

    /**
     * 计算共用胎胚组的优先级排行：机台数量降序，数量相同时胎胚编码升序。
     *
     * @param sourceSkuMap 当前可调整结果到来源SKU的映射
     * @return 胎胚编码到优先级序号（0最高）的映射
     */
    private Map<String, Integer> resolveEmbryoGroupRankMap(
            Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap) {
        Map<String, Integer> machineCountMap = new LinkedHashMap<String, Integer>(8);
        if (CollectionUtils.isEmpty(sourceSkuMap)) {
            return machineCountMap;
        }
        for (SkuScheduleDTO sourceSku : sourceSkuMap.values()) {
            if (Objects.isNull(sourceSku) || StringUtils.isEmpty(sourceSku.getEmbryoCode())) {
                continue;
            }
            machineCountMap.merge(sourceSku.getEmbryoCode(), 1, Integer::sum);
        }
        List<String> embryoCodeList = new ArrayList<String>(machineCountMap.keySet());
        embryoCodeList.sort(Comparator
                .comparingInt((String embryoCode) -> machineCountMap.getOrDefault(embryoCode, 0))
                .reversed()
                .thenComparing(String::compareTo));
        Map<String, Integer> rankMap = new LinkedHashMap<String, Integer>(embryoCodeList.size());
        for (int rank = 0; rank < embryoCodeList.size(); rank++) {
            rankMap.put(embryoCodeList.get(rank), rank);
        }
        return rankMap;
    }

    /**
     * 解析移动所属的对象优先级阶段。
     *
     * <p>阶段0：同物料多机台移动（同SKU尾量分摊、同物料组内机台的补量/减量）；
     * 阶段1：共用胎胚组移动（跨SKU胎胚收尾互转、非同物料组机台的补量/减量）。
     * 同物料多机台必须优先于共用胎胚补量、减量、分摊。</p>
     *
     * @param move 候选移动
     * @param sameMaterialGroupKeyMap 同物料多机台分组键映射
     * @return 0-同物料多机台；1-共用胎胚组
     */
    private int resolveMovePhase(EmbryoEndingBalanceMove move,
                                 Map<LhScheduleResult, String> sameMaterialGroupKeyMap) {
        if (Objects.isNull(move) || Objects.isNull(move.getDonor())) {
            return 1;
        }
        if (MOVE_TYPE_TRANSFER.equals(move.getMoveType())) {
            // 同SKU分摊属于同物料多机台；跨SKU胎胚收尾互转属于共用胎胚组。
            String donorSkuKey = sameMaterialGroupKeyMap.get(move.getDonor());
            if (Objects.nonNull(move.getReceiver()) && StringUtils.isNotEmpty(donorSkuKey)
                    && StringUtils.equals(donorSkuKey,
                    sameMaterialGroupKeyMap.get(move.getReceiver()))) {
                return 0;
            }
            return 1;
        }
        // 后延补量/提前减量：机台本身属于同物料多机台组时进入第一优先级。
        return sameMaterialGroupKeyMap.containsKey(move.getDonor()) ? 0 : 1;
    }

    /**
     * 解析移动所属对象组的优先级序号。
     *
     * @param move 候选移动
     * @param sameMaterialGroupKeyMap 同物料多机台分组键映射
     * @param sourceSkuMap 当前可调整结果到来源SKU的映射
     * @param embryoRankMap 共用胎胚组优先级序号映射
     * @return 同物料组固定为0；共用胎胚组返回组排行（0最高）
     */
    private int resolveMoveGroupRank(EmbryoEndingBalanceMove move,
                                     Map<LhScheduleResult, String> sameMaterialGroupKeyMap,
                                     Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap,
                                     Map<String, Integer> embryoRankMap) {
        if (this.resolveMovePhase(move, sameMaterialGroupKeyMap) == 0) {
            return 0;
        }
        SkuScheduleDTO sourceSku = sourceSkuMap.get(move.getDonor());
        String embryoCode = Objects.isNull(sourceSku) ? null : sourceSku.getEmbryoCode();
        return Objects.isNull(embryoCode) ? Integer.MAX_VALUE
                : embryoRankMap.getOrDefault(embryoCode, Integer.MAX_VALUE);
    }

    /**
     * 解析移动所属对象组的组内稳定键。
     *
     * @param move 候选移动
     * @param sameMaterialGroupKeyMap 同物料多机台分组键映射
     * @param sourceSkuMap 当前可调整结果到来源SKU的映射
     * @return 同物料组返回SKU key；共用胎胚组返回胎胚编码
     */
    private String resolveMoveGroupKey(EmbryoEndingBalanceMove move,
                                       Map<LhScheduleResult, String> sameMaterialGroupKeyMap,
                                       Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap) {
        if (this.resolveMovePhase(move, sameMaterialGroupKeyMap) == 0) {
            return sameMaterialGroupKeyMap.get(move.getDonor());
        }
        SkuScheduleDTO sourceSku = sourceSkuMap.get(move.getDonor());
        return Objects.isNull(sourceSku) ? ""
                : StringUtils.defaultString(sourceSku.getEmbryoCode());
    }

    /**
     * 构建尾量分摊移动：同SKU之间，或胎胚收尾允许同组跨物料互转。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param state 当前均衡状态
     * @param sortedResults 按机台编码升序的候选结果
     * @param moveList 移动列表
     */
    private void buildTransferMoves(LhScheduleContext context,
                                    List<LhShiftConfigVO> shifts,
                                    EmbryoEndingBalanceState state,
                                    List<LhScheduleResult> sortedResults,
                                    List<EmbryoEndingBalanceMove> moveList) {
        for (LhScheduleResult donor : sortedResults) {
            Integer donorEndingShift = state.getEndingShiftMap().get(donor);
            Integer donorEndingQty = state.getEndingQtyMap().get(donor);
            // 首班收尾机台不能再提前，不作为尾量转出方。
            if (Objects.isNull(donorEndingShift) || donorEndingShift <= 1
                    || Objects.isNull(donorEndingQty) || donorEndingQty <= 0) {
                continue;
            }
            String donorEndingType = state.getEndingTypeMap().get(donor);
            String donorSkuKey = resolveSkuKey(state.getSourceSkuMap().get(donor));
            String donorEmbryoCode = Objects.isNull(state.getSourceSkuMap().get(donor))
                    ? null : state.getSourceSkuMap().get(donor).getEmbryoCode();
            LhScheduleResult donorPair = state.getPairResultMap().get(donor);
            boolean donorPairUnit = Objects.nonNull(donorPair);
            int donorEndingGroupQty = donorPairUnit ? donorEndingQty * 2 : donorEndingQty;
            int donorTotalGroupQty = ShiftFieldUtil.resolveScheduledQty(donor)
                    + (donorPairUnit ? ShiftFieldUtil.resolveScheduledQty(donorPair) : 0);
            for (LhScheduleResult receiver : sortedResults) {
                if (receiver == donor) {
                    continue;
                }
                String receiverEmbryoCode = Objects.isNull(state.getSourceSkuMap().get(receiver))
                        ? null : state.getSourceSkuMap().get(receiver).getEmbryoCode();
                // 全局预演中同时存在多个胎胚组，尾量只能在同一共用胎胚组内流转。
                if (!StringUtils.equals(donorEmbryoCode, receiverEmbryoCode)) {
                    continue;
                }
                LhScheduleResult receiverPair = state.getPairResultMap().get(receiver);
                boolean receiverPairUnit = Objects.nonNull(receiverPair);
                // 单边机台与单控整机不能互转尾量，避免两侧数量口径不同导致组级总量失衡。
                if (donorPairUnit != receiverPairUnit) {
                    continue;
                }
                String receiverEndingType = state.getEndingTypeMap().get(receiver);
                // SKU余量收尾和按时间下机收尾只能在同SKU之间分摊；
                // 胎胚收尾跨物料互转要求转出方、接收方都命中胎胚收尾硬目标口径。
                boolean sameSkuTransfer = StringUtils.equals(
                        donorSkuKey, resolveSkuKey(state.getSourceSkuMap().get(receiver)));
                boolean embryoEndingTransfer = StringUtils.equals(ENDING_TYPE_EMBRYO, donorEndingType)
                        && StringUtils.equals(ENDING_TYPE_EMBRYO, receiverEndingType);
                if (!sameSkuTransfer && !embryoEndingTransfer) {
                    continue;
                }
                Integer receiverEndingShift = state.getEndingShiftMap().get(receiver);
                // 分摊只在原收尾班次相同的机台间尝试，避免跨多班次搬运造成时间轴断层。
                if (!Objects.equals(donorEndingShift, receiverEndingShift)) {
                    continue;
                }
                /*
                 * 中班超限时，先尝试把一台机的完整收尾残量并入另一台机的当前收尾班次。
                 * 转出机提前收尾，承接机仍只产生一次换模，因此能在不改变严格总量的前提下减少一次当班换模。
                 */
                LhShiftConfigVO currentEndingShift = findShiftByIndex(shifts, receiverEndingShift);
                int currentShiftFreeCapacity = Objects.isNull(currentEndingShift)
                        ? 0 : resolveShiftFreeCapacity(context, receiver, currentEndingShift);
                if (receiverPairUnit && Objects.nonNull(currentEndingShift)) {
                    currentShiftFreeCapacity = Math.min(currentShiftFreeCapacity,
                            resolveShiftFreeCapacity(context, receiverPair, currentEndingShift));
                }
                if (donorTotalGroupQty - donorEndingGroupQty > 0
                        && currentShiftFreeCapacity >= donorEndingQty) {
                    moveList.add(new EmbryoEndingBalanceMove(
                            MOVE_TYPE_TRANSFER, donor, receiver,
                            donorEndingGroupQty, receiverEndingShift));
                }
                if (receiverEndingShift >= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT) {
                    continue;
                }
                // 承接机台若存在降模释放边界，后延班次不得越过该边界（单控整机两侧都要校验）。
                if (isBeyondReleaseBoundary(context, receiver, receiverEndingShift + 1)
                        || (receiverPairUnit
                        && isBeyondReleaseBoundary(context, receiverPair, receiverEndingShift + 1))) {
                    continue;
                }
                LhShiftConfigVO nextShift = findShiftByIndex(shifts, receiverEndingShift + 1);
                if (Objects.isNull(nextShift)
                        || isShiftOccupiedByOtherSku(context, receiver, nextShift)
                        || (receiverPairUnit
                        && isShiftOccupiedByOtherSku(context, receiverPair, nextShift))) {
                    continue;
                }
                int freeCapacity = resolveShiftFreeCapacity(context, receiver, nextShift);
                if (receiverPairUnit) {
                    freeCapacity = Math.min(freeCapacity,
                            resolveShiftFreeCapacity(context, receiverPair, nextShift));
                }
                if (freeCapacity <= 0) {
                    continue;
                }
                // 单控整机两侧同时等量转移，移动量使用组级口径（每侧量*2）。
                int perSideTransferQty = Math.min(donorEndingQty, freeCapacity);
                int transferQty = donorPairUnit ? perSideTransferQty * 2 : perSideTransferQty;
                if (transferQty <= 0) {
                    continue;
                }
                // 单控整机配对侧当前收尾班次也必须足量，避免移动执行阶段才失败。
                if (donorPairUnit) {
                    Integer donorPairEndingQty =
                            ShiftFieldUtil.getShiftPlanQty(donorPair, donorEndingShift);
                    if (Objects.isNull(donorPairEndingQty) || donorPairEndingQty < perSideTransferQty) {
                        continue;
                    }
                }
                // 转出机台必须保留前一班次正量（单控整机按两侧合计），避免把整台机台清空成释放。
                if (donorTotalGroupQty - donorEndingGroupQty <= 0) {
                    continue;
                }
                moveList.add(new EmbryoEndingBalanceMove(
                        MOVE_TYPE_TRANSFER, donor, receiver, transferQty, receiverEndingShift + 1));
            }
        }
    }

    /**
     * 构建后延补量移动：仅按时间下机收尾允许补量后延一个班次。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param state 当前均衡状态
     * @param sortedResults 按机台编码升序的候选结果
     * @param moveList 移动列表
     */
    private void buildPostponeMoves(LhScheduleContext context,
                                    List<LhShiftConfigVO> shifts,
                                    EmbryoEndingBalanceState state,
                                    List<LhScheduleResult> sortedResults,
                                    List<EmbryoEndingBalanceMove> moveList) {
        for (LhScheduleResult result : sortedResults) {
            if (!StringUtils.equals(ENDING_TYPE_TIME, state.getEndingTypeMap().get(result))) {
                continue;
            }
            Integer endingShift = state.getEndingShiftMap().get(result);
            if (Objects.isNull(endingShift)) {
                continue;
            }
            LhScheduleResult pairResult = state.getPairResultMap().get(result);
            boolean pairUnit = Objects.nonNull(pairResult);
            int targetShiftIndex = endingShift + 1;
            LhShiftConfigVO currentEndingShift = findShiftByIndex(shifts, endingShift);
            Date changeTime = state.getChangeTimeMap().get(result);
            /*
             * 早班换模超限且前物料本身在早班内提前收尾时，
             * 应先补满当前早班到14:00，使换模真实落到中班；不能直接跳到中班再补满，
             * 否则前物料可能在20:00后才收尾而被错误顺延到次日。
             */
            if (Objects.nonNull(currentEndingShift) && Objects.nonNull(changeTime)
                    && LhScheduleTimeUtil.isMorningShift(context, changeTime)
                    && LhScheduleTimeUtil.isMorningShift(
                    context, currentEndingShift.getShiftStartDateTime())
                    && resolveShiftFreeCapacity(context, result, currentEndingShift) > 0) {
                targetShiftIndex = endingShift;
            }
            if (targetShiftIndex > LhScheduleConstant.MAX_SHIFT_SLOT_COUNT) {
                continue;
            }
            // 后延不得越过降模释放边界。
            if (isBeyondReleaseBoundary(context, result, targetShiftIndex)
                    || (pairUnit && isBeyondReleaseBoundary(
                    context, pairResult, targetShiftIndex))) {
                continue;
            }
            LhShiftConfigVO nextShift = findShiftByIndex(shifts, targetShiftIndex);
            if (Objects.isNull(nextShift)
                    || isShiftOccupiedByOtherSku(context, result, nextShift)
                    || (pairUnit && isShiftOccupiedByOtherSku(context, pairResult, nextShift))) {
                continue;
            }
            int freeCapacity = resolveShiftFreeCapacity(context, result, nextShift);
            if (pairUnit) {
                freeCapacity = Math.min(freeCapacity,
                        resolveShiftFreeCapacity(context, pairResult, nextShift));
            }
            if (freeCapacity <= 0) {
                continue;
            }
            int postponeQty = pairUnit ? freeCapacity * 2 : freeCapacity;
            moveList.add(new EmbryoEndingBalanceMove(
                    MOVE_TYPE_POSTPONE, result, null, postponeQty, targetShiftIndex));
        }
    }

    /**
     * 构建提前减量移动：仅按时间下机收尾允许减量提前一个班次。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param state 当前均衡状态
     * @param sortedResults 按机台编码升序的候选结果
     * @param moveList 移动列表
     */
    private void buildAdvanceMoves(LhScheduleContext context,
                                   List<LhShiftConfigVO> shifts,
                                   EmbryoEndingBalanceState state,
                                   List<LhScheduleResult> sortedResults,
                                   List<EmbryoEndingBalanceMove> moveList) {
        for (LhScheduleResult result : sortedResults) {
            if (!StringUtils.equals(ENDING_TYPE_TIME, state.getEndingTypeMap().get(result))) {
                continue;
            }
            Integer endingShift = state.getEndingShiftMap().get(result);
            Integer endingQty = state.getEndingQtyMap().get(result);
            if (Objects.isNull(endingShift) || endingShift <= 0
                    || Objects.isNull(endingQty) || endingQty <= 0) {
                continue;
            }
            LhScheduleResult pairResult = state.getPairResultMap().get(result);
            int resultTotalQty = ShiftFieldUtil.resolveScheduledQty(result);
            int pairTotalQty = Objects.isNull(pairResult)
                    ? Integer.MAX_VALUE : ShiftFieldUtil.resolveScheduledQty(pairResult);
            // 提前减量不能把续作结果整行清空，避免均衡变成未经业务决策的降模释放。
            if (resultTotalQty <= endingQty || pairTotalQty <= endingQty) {
                continue;
            }
            moveList.add(new EmbryoEndingBalanceMove(
                    MOVE_TYPE_ADVANCE, result, null, endingQty, endingShift));
        }
    }

    /**
     * 执行单个均衡移动。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param move 移动
     * @param state 当前均衡状态
     * @return true-执行成功；false-执行失败（调用方负责恢复快照）
     */
    private boolean executeMove(LhScheduleContext context,
                                List<LhShiftConfigVO> shifts,
                                EmbryoEndingBalanceMove move,
                                EmbryoEndingBalanceState state) {
        if (MOVE_TYPE_TRANSFER.equals(move.getMoveType())) {
            return applyTransferMove(context, shifts, move, state);
        }
        if (MOVE_TYPE_POSTPONE.equals(move.getMoveType())) {
            return applyPostponeMove(context, shifts, move, state);
        }
        if (MOVE_TYPE_ADVANCE.equals(move.getMoveType())) {
            return applyAdvanceMove(context, shifts, move, state);
        }
        return false;
    }

    /**
     * 执行尾量分摊：转出机台收尾班次减量，承接机台下一班次增量，总量保持不变。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param move 尾量分摊移动
     * @param state 当前均衡状态
     * @return true-执行成功；false-执行失败
     */
    private boolean applyTransferMove(LhScheduleContext context,
                                      List<LhShiftConfigVO> shifts,
                                      EmbryoEndingBalanceMove move,
                                      EmbryoEndingBalanceState state) {
        LhScheduleResult donor = move.getDonor();
        LhScheduleResult receiver = move.getReceiver();
        LhScheduleResult donorPair = state.getPairResultMap().get(donor);
        LhScheduleResult receiverPair = state.getPairResultMap().get(receiver);
        boolean donorPairUnit = Objects.nonNull(donorPair);
        boolean receiverPairUnit = Objects.nonNull(receiverPair);
        // 单边机台与单控整机不能互转，候选构建阶段已排除，这里再次防御。
        if (donorPairUnit != receiverPairUnit) {
            return false;
        }
        Integer donorEndingShift = state.getEndingShiftMap().get(donor);
        LhShiftConfigVO nextShift = findShiftByIndex(shifts, move.getTargetShiftIndex());
        if (Objects.isNull(donorEndingShift) || Objects.isNull(nextShift)) {
            return false;
        }
        Integer donorCurrentQty = ShiftFieldUtil.getShiftPlanQty(donor, donorEndingShift);
        if (Objects.isNull(donorCurrentQty) || donorCurrentQty <= 0) {
            return false;
        }
        // 单控整机移动量按组级口径传递，落到每侧时折半，保证L/R两侧始终等量。
        int perSideTransferQty = donorPairUnit ? move.getQty() / 2 : move.getQty();
        if (perSideTransferQty <= 0) {
            return false;
        }
        int actualPerSideQty = capsuleReplacementRuleService.resolveActualPlanQty(
                context, receiver, nextShift, perSideTransferQty, "共用胎胚收尾均衡尾量分摊");
        if (receiverPairUnit) {
            int actualPairSideQty = capsuleReplacementRuleService.resolveActualPlanQty(
                    context, receiverPair, nextShift, perSideTransferQty,
                    "共用胎胚收尾均衡尾量分摊");
            actualPerSideQty = Math.min(actualPerSideQty, actualPairSideQty);
        }
        if (actualPerSideQty <= 0) {
            return false;
        }
        // 转出侧每侧减量，单控整机两侧同步减。
        if (!applyDonorTransferSide(context, shifts, donor, donorEndingShift, actualPerSideQty)) {
            return false;
        }
        if (donorPairUnit) {
            Integer pairCurrentQty = ShiftFieldUtil.getShiftPlanQty(donorPair, donorEndingShift);
            if (Objects.isNull(pairCurrentQty) || pairCurrentQty < actualPerSideQty
                    || !applyDonorTransferSide(
                    context, shifts, donorPair, donorEndingShift, actualPerSideQty)) {
                // 配对侧数量不足或修改失败，交给快照统一恢复。
                return false;
            }
        }
        // 承接侧每侧增量，单控整机两侧同步增。
        if (!applyReceiverTransferSide(context, shifts, receiver, nextShift, actualPerSideQty)) {
            return false;
        }
        if (receiverPairUnit
                && !applyReceiverTransferSide(context, shifts, receiverPair, nextShift, actualPerSideQty)) {
            return false;
        }
        refreshResultSummary(context, donor, shifts);
        syncStopHoldBoundaryAfterBalance(context, donor, shifts);
        refreshResultSummary(context, receiver, shifts);
        syncStopHoldBoundaryAfterBalance(context, receiver, shifts);
        syncMachineEstimatedEndTime(context, donor);
        syncMachineEstimatedEndTime(context, receiver);
        if (donorPairUnit) {
            refreshResultSummary(context, donorPair, shifts);
            syncStopHoldBoundaryAfterBalance(context, donorPair, shifts);
            syncMachineEstimatedEndTime(context, donorPair);
        }
        if (receiverPairUnit) {
            refreshResultSummary(context, receiverPair, shifts);
            syncStopHoldBoundaryAfterBalance(context, receiverPair, shifts);
            syncMachineEstimatedEndTime(context, receiverPair);
        }
        // 记录实际落地的组级互转量，供提交阶段重分配SKU内部额度。
        move.setAppliedQty(actualPerSideQty * (donorPairUnit ? 2 : 1));
        return true;
    }

    /**
     * 转出机台单侧执行尾量减量。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param result 转出机台结果（单控整机传单侧结果）
     * @param endingShift 原收尾班次
     * @param transferQty 单侧减量
     * @return true-执行成功；false-当前班次计划量不足
     */
    private boolean applyDonorTransferSide(LhScheduleContext context,
                                           List<LhShiftConfigVO> shifts,
                                           LhScheduleResult result,
                                           int endingShift,
                                           int transferQty) {
        Integer currentQty = ShiftFieldUtil.getShiftPlanQty(result, endingShift);
        if (Objects.isNull(currentQty) || currentQty <= 0 || currentQty < transferQty) {
            return false;
        }
        int newQty = currentQty - transferQty;
        if (newQty > 0) {
            // 转出机台保留部分尾量，按剩余量重算当前班次结束时间。
            Date startTime = ShiftFieldUtil.getShiftStartTime(result, endingShift);
            LhShiftConfigVO donorShift = findShiftByIndex(shifts, endingShift);
            if (Objects.isNull(startTime) && Objects.nonNull(donorShift)) {
                startTime = donorShift.getShiftStartDateTime();
            }
            Date endTime = resolveShiftCompletionTime(
                    context, shifts, result, endingShift, newQty);
            ShiftFieldUtil.setShiftPlanQty(result, endingShift, newQty, startTime, endTime);
        } else {
            // 尾量全部转出：清空收尾班次，机台提前到前一班次收尾。
            ShiftFieldUtil.setShiftPlanQty(result, endingShift, 0, null, null);
            ShiftFieldUtil.clearShiftPlanAuxFields(result, endingShift);
        }
        // 减量发生在转出班次，备注必须写在该班次而不是新收尾班次。
        ShiftFieldUtil.appendShiftAnalysis(result, endingShift, BALANCE_TRANSFER_ANALYSIS);
        return true;
    }

    /**
     * 承接机台单侧执行尾量增量。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param result 承接机台结果（单控整机传单侧结果）
     * @param nextShift 承接班次
     * @param transferQty 单侧增量
     * @return true-执行成功
     */
    private boolean applyReceiverTransferSide(LhScheduleContext context,
                                              List<LhShiftConfigVO> shifts,
                                              LhScheduleResult result,
                                              LhShiftConfigVO nextShift,
                                              int transferQty) {
        if (transferQty <= 0) {
            return false;
        }
        int existingQty = resolveShiftPlanQty(result, nextShift.getShiftIndex());
        int newQty = existingQty + transferQty;
        Date startTime = ShiftFieldUtil.getShiftStartTime(result, nextShift.getShiftIndex());
        if (Objects.isNull(startTime)) {
            startTime = nextShift.getShiftStartDateTime();
        }
        Date endTime = resolveShiftCompletionTime(
                context, shifts, result, nextShift.getShiftIndex(), newQty);
        ShiftFieldUtil.setShiftPlanQty(result, nextShift.getShiftIndex(), newQty, startTime, endTime);
        // 承接班次实际新增尾量，追加分摊备注便于结果对账。
        ShiftFieldUtil.appendShiftAnalysis(result, nextShift.getShiftIndex(), BALANCE_TRANSFER_ANALYSIS);
        return true;
    }

    /**
     * 判断后延班次是否越过机台降模释放边界。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param targetShiftIndex 目标班次序号
     * @return true-越过边界；false-未越过或无边界
     */
    private boolean isBeyondReleaseBoundary(LhScheduleContext context,
                                            LhScheduleResult result,
                                            int targetShiftIndex) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || StringUtils.isEmpty(result.getLhMachineCode())) {
            return false;
        }
        Integer releaseBoundary = context.getContinuousReducedMachineReleaseBoundaryShiftIndexMap()
                .get(result.getLhMachineCode());
        return Objects.nonNull(releaseBoundary) && releaseBoundary > 0
                && targetShiftIndex > releaseBoundary;
    }

    /**
     * 提交阶段落地跨物料互转的SKU内部额度重分配。
     * <p>只有移动评分严格变优后才调用，失败尝试不会触碰SKU额度账本；
     * 单控整机互转量按组级口径传递，两侧合计重分配。</p>
     *
     * @param context 排程上下文
     * @param move 已提交移动
     * @param state 均衡状态
     * @return true-无需重分配或重分配成功；false-重分配失败（调用方应恢复快照并放弃该移动）
     */
    private boolean commitMoveQuotaChanges(LhScheduleContext context,
                                           EmbryoEndingBalanceMove move,
                                           EmbryoEndingBalanceState state) {
        if (!MOVE_TYPE_TRANSFER.equals(move.getMoveType()) || move.getAppliedQty() <= 0) {
            return true;
        }
        LhScheduleResult donor = move.getDonor();
        LhScheduleResult receiver = move.getReceiver();
        SkuScheduleDTO donorSku = state.getSourceSkuMap().get(donor);
        SkuScheduleDTO receiverSku = state.getSourceSkuMap().get(receiver);
        boolean crossMaterialEmbryoTransfer = StringUtils.equals(
                ENDING_TYPE_EMBRYO, state.getEndingTypeMap().get(donor))
                && StringUtils.equals(ENDING_TYPE_EMBRYO, state.getEndingTypeMap().get(receiver))
                && !StringUtils.equals(resolveSkuKey(donorSku), resolveSkuKey(receiverSku));
        if (!crossMaterialEmbryoTransfer) {
            return true;
        }
        return targetScheduleQtyResolver.reallocateEmbryoStockSkuQuota(
                context, donorSku, receiverSku, move.getAppliedQty(), "共用胎胚收尾均衡");
    }

    /**
     * 执行后延补量：按时间下机机台在当前收尾班次的下一班次补量，收尾后延一个班次。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param move 后延补量移动
     * @return true-执行成功；false-执行失败
     */
    private boolean applyPostponeMove(LhScheduleContext context,
                                      List<LhShiftConfigVO> shifts,
                                      EmbryoEndingBalanceMove move,
                                      EmbryoEndingBalanceState state) {
        LhScheduleResult result = move.getDonor();
        LhScheduleResult pairResult = state.getPairResultMap().get(result);
        boolean pairUnit = Objects.nonNull(pairResult);
        LhShiftConfigVO nextShift = findShiftByIndex(shifts, move.getTargetShiftIndex());
        if (Objects.isNull(nextShift)) {
            return false;
        }
        /*
         * 若当前换模落点是因每日硬上限从自然最早班次推到次日早班，
         * 不允许只改换模时间或留一段空等；必须连续补满中间各班次，
         * 让前物料在目标早班06:00真实收尾。
         */
        if (this.isMouldChangeDeferredByDailyLimit(context, state, result)) {
            return this.applyCrossDayPostponeMove(
                    context, shifts, move, state, result, pairResult,
                    state.getChangeTimeMap().get(result));
        }
        /*
         * 中班超限机台后延到夜班时，目标班次结束点是次日06:00。此时也必须走连续补量：
         * 先补满当前中班剩余产能，再补满夜班，禁止从当前收尾时间空等到20:00后只补夜班。
         */
        Date currentEndingTime = state.getEndingTimeMap().get(result);
        Date targetShiftEndTime = nextShift.getShiftEndDateTime();
        if (Objects.nonNull(currentEndingTime) && Objects.nonNull(targetShiftEndTime)
                && LhScheduleTimeUtil.clearTime(targetShiftEndTime)
                .after(LhScheduleTimeUtil.clearTime(currentEndingTime))
                && LhScheduleTimeUtil.isMorningShift(context, targetShiftEndTime)) {
            return this.applyCrossDayPostponeMove(
                    context, shifts, move, state, result, pairResult, targetShiftEndTime);
        }
        int freeCapacity = resolveShiftFreeCapacity(context, result, nextShift);
        if (pairUnit) {
            freeCapacity = Math.min(freeCapacity,
                    resolveShiftFreeCapacity(context, pairResult, nextShift));
        }
        if (freeCapacity <= 0) {
            return false;
        }
        if (pairUnit && isShiftOccupiedByOtherSku(context, pairResult, nextShift)) {
            return false;
        }
        // 单控整机每侧等量补量，移动量按组级口径折半到每侧。
        int perSideAddQty = pairUnit ? move.getQty() / 2 : move.getQty();
        if (perSideAddQty <= 0) {
            return false;
        }
        int actualAddQty = capsuleReplacementRuleService.resolveActualPlanQty(
                context, result, nextShift, Math.min(perSideAddQty, freeCapacity),
                "共用胎胚收尾均衡后延补量");
        if (pairUnit) {
            int actualPairAddQty = capsuleReplacementRuleService.resolveActualPlanQty(
                    context, pairResult, nextShift, Math.min(perSideAddQty, freeCapacity),
                    "共用胎胚收尾均衡后延补量");
            actualAddQty = Math.min(actualAddQty, actualPairAddQty);
        }
        if (actualAddQty <= 0) {
            return false;
        }
        if (!applyReceiverPostponeSide(context, shifts, result, nextShift, actualAddQty)) {
            return false;
        }
        if (pairUnit && !applyReceiverPostponeSide(
                context, shifts, pairResult, nextShift, actualAddQty)) {
            return false;
        }
        // 后延补量属于收尾规则例外，登记允许超量，防止严格收口和SKU账本把补量回裁。
        context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().merge(
                result, actualAddQty, Integer::sum);
        if (pairUnit) {
            context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().merge(
                    pairResult, actualAddQty, Integer::sum);
        }
        refreshResultSummary(context, result, shifts);
        syncStopHoldBoundaryAfterBalance(context, result, shifts);
        syncMachineEstimatedEndTime(context, result);
        if (pairUnit) {
            refreshResultSummary(context, pairResult, shifts);
            syncStopHoldBoundaryAfterBalance(context, pairResult, shifts);
            syncMachineEstimatedEndTime(context, pairResult);
        }
        move.setAppliedQty(actualAddQty * (pairUnit ? 2 : 1));
        return true;
    }

    /**
     * 判断后物料换模是否因每日总次数硬上限被顺延到更晚日期。
     *
     * @param context 排程上下文
     * @param state 当前均衡状态
     * @param result 待判断结果
     * @return true-模拟账本使换模日期晚于无次数占用时的自然落点
     */
    private boolean isMouldChangeDeferredByDailyLimit(LhScheduleContext context,
                                                      EmbryoEndingBalanceState state,
                                                      LhScheduleResult result) {
        Date assignedChangeTime = state.getChangeTimeMap().get(result);
        Date endingTime = state.getEndingTimeMap().get(result);
        SkuScheduleDTO sourceSku = state.getSourceSkuMap().get(result);
        if (Objects.isNull(assignedChangeTime) || Objects.isNull(endingTime)
                || !assignedChangeTime.after(endingTime)
                || !LhScheduleTimeUtil.isMorningShift(context, assignedChangeTime)) {
            return false;
        }
        Map<String, int[]> emptyCountMap = new LinkedHashMap<String, int[]>(2);
        Date naturalChangeTime = mouldChangeBalanceStrategy.previewEndingStaggerMouldChange(
                context, result.getLhMachineCode(), endingTime,
                LhScheduleTimeUtil.getMouldChangeTotalHours(context), sourceSku, emptyCountMap);
        return Objects.nonNull(naturalChangeTime)
                && LhScheduleTimeUtil.clearTime(assignedChangeTime)
                .after(LhScheduleTimeUtil.clearTime(naturalChangeTime));
    }

    /**
     * 执行跨天补量：从当前收尾班次连续补到目标早班06:00。
     * <p>任一中间班次被其他SKU占用、不可排、越过降模边界、产能不足或胶囊规则
     * 无法精确补满时整笔失败，由外层快照恢复，不会留下半条连续生产时间轴。</p>
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param move 均衡移动
     * @param state 调整前状态
     * @param result 待补量主结果
     * @param pairResult 单控整机配对侧；普通机台为null
     * @param targetMorning 目标早班06:00
     * @return true-精确生产到目标06:00；false-无法连续补满
     */
    private boolean applyCrossDayPostponeMove(LhScheduleContext context,
                                              List<LhShiftConfigVO> shifts,
                                              EmbryoEndingBalanceMove move,
                                              EmbryoEndingBalanceState state,
                                              LhScheduleResult result,
                                              LhScheduleResult pairResult,
                                              Date targetMorning) {
        if (Objects.isNull(targetMorning)) {
            return false;
        }
        int currentEndingShiftIndex = state.getEndingShiftMap().get(result);
        int targetNightShiftIndex = -1;
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.nonNull(shift) && Objects.nonNull(shift.getShiftEndDateTime())
                    && shift.getShiftEndDateTime().equals(targetMorning)) {
                targetNightShiftIndex = shift.getShiftIndex();
                break;
            }
        }
        if (targetNightShiftIndex < currentEndingShiftIndex || targetNightShiftIndex <= 0) {
            return false;
        }
        boolean pairUnit = Objects.nonNull(pairResult);
        int totalAddedPerSideQty = 0;
        for (int shiftIndex = currentEndingShiftIndex;
             shiftIndex <= targetNightShiftIndex; shiftIndex++) {
            LhShiftConfigVO targetShift = this.findShiftByIndex(shifts, shiftIndex);
            if (Objects.isNull(targetShift)
                    || this.isBeyondReleaseBoundary(context, result, shiftIndex)
                    || (pairUnit && this.isBeyondReleaseBoundary(context, pairResult, shiftIndex))
                    || this.isShiftOccupiedByOtherSku(context, result, targetShift)
                    || (pairUnit && this.isShiftOccupiedByOtherSku(context, pairResult, targetShift))) {
                return false;
            }
            int freeCapacity = this.resolveShiftFreeCapacity(context, result, targetShift);
            if (pairUnit) {
                freeCapacity = Math.min(freeCapacity,
                        this.resolveShiftFreeCapacity(context, pairResult, targetShift));
            }
            if (freeCapacity <= 0) {
                // 已经满产的中间班次可直接继续，但最终收尾班次必须由本次调整延伸到。
                if (shiftIndex < targetNightShiftIndex
                        && this.resolveShiftPlanQty(result, shiftIndex)
                        >= this.resolveResultShiftCapacity(context, result, targetShift)) {
                    continue;
                }
                return false;
            }
            int actualAddQty = capsuleReplacementRuleService.resolveActualPlanQty(
                    context, result, targetShift, freeCapacity, "共用胎胚收尾均衡补量（跨天）");
            if (pairUnit) {
                int pairActualAddQty = capsuleReplacementRuleService.resolveActualPlanQty(
                        context, pairResult, targetShift, freeCapacity,
                        "共用胎胚收尾均衡补量（跨天）");
                actualAddQty = Math.min(actualAddQty, pairActualAddQty);
            }
            if (actualAddQty != freeCapacity
                    || !this.applyCrossDayPostponeSide(
                    context, shifts, result, targetShift, actualAddQty)
                    || (pairUnit && !this.applyCrossDayPostponeSide(
                    context, shifts, pairResult, targetShift, actualAddQty))) {
                return false;
            }
            totalAddedPerSideQty += actualAddQty;
        }
        this.refreshResultSummary(context, result, shifts);
        if (!targetMorning.equals(result.getSpecEndTime())) {
            return false;
        }
        context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().merge(
                result, totalAddedPerSideQty, Integer::sum);
        this.syncStopHoldBoundaryAfterBalance(context, result, shifts);
        this.syncMachineEstimatedEndTime(context, result);
        if (pairUnit) {
            this.refreshResultSummary(context, pairResult, shifts);
            if (!targetMorning.equals(pairResult.getSpecEndTime())) {
                return false;
            }
            context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().merge(
                    pairResult, totalAddedPerSideQty, Integer::sum);
            this.syncStopHoldBoundaryAfterBalance(context, pairResult, shifts);
            this.syncMachineEstimatedEndTime(context, pairResult);
        }
        move.setAppliedQty(totalAddedPerSideQty * (pairUnit ? 2 : 1));
        move.setCrossDay(true);
        return totalAddedPerSideQty > 0;
    }

    /**
     * 将跨天补量写入单侧结果，并在实际补量班次追加原因。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param result 待补量结果
     * @param targetShift 目标班次
     * @param addQty 补量数量
     * @return true-写入成功
     */
    private boolean applyCrossDayPostponeSide(LhScheduleContext context,
                                              List<LhShiftConfigVO> shifts,
                                              LhScheduleResult result,
                                              LhShiftConfigVO targetShift,
                                              int addQty) {
        if (addQty <= 0) {
            return false;
        }
        int existingQty = this.resolveShiftPlanQty(result, targetShift.getShiftIndex());
        int newQty = existingQty + addQty;
        Date startTime = ShiftFieldUtil.getShiftStartTime(result, targetShift.getShiftIndex());
        if (Objects.isNull(startTime)) {
            startTime = targetShift.getShiftStartDateTime();
        }
        Date endTime = this.resolveShiftCompletionTime(
                context, shifts, result, targetShift.getShiftIndex(), newQty);
        ShiftFieldUtil.setShiftPlanQty(
                result, targetShift.getShiftIndex(), newQty, startTime, endTime);
        ShiftFieldUtil.appendShiftAnalysis(
                result, targetShift.getShiftIndex(), BALANCE_POSTPONE_CROSS_DAY_ANALYSIS);
        return true;
    }

    /**
     * 后延补量单侧写入承接班次。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param result 后延机台结果（单控整机传单侧结果）
     * @param nextShift 后延班次
     * @param addQty 单侧补量
     * @return true-执行成功
     */
    private boolean applyReceiverPostponeSide(LhScheduleContext context,
                                              List<LhShiftConfigVO> shifts,
                                              LhScheduleResult result,
                                              LhShiftConfigVO nextShift,
                                              int addQty) {
        if (addQty <= 0) {
            return false;
        }
        int existingQty = resolveShiftPlanQty(result, nextShift.getShiftIndex());
        int newQty = existingQty + addQty;
        Date startTime = ShiftFieldUtil.getShiftStartTime(result, nextShift.getShiftIndex());
        if (Objects.isNull(startTime)) {
            startTime = nextShift.getShiftStartDateTime();
        }
        Date endTime = resolveShiftCompletionTime(
                context, shifts, result, nextShift.getShiftIndex(), newQty);
        ShiftFieldUtil.setShiftPlanQty(result, nextShift.getShiftIndex(), newQty, startTime, endTime);
        ShiftFieldUtil.appendShiftAnalysis(result, nextShift.getShiftIndex(), BALANCE_POSTPONE_ANALYSIS);
        return true;
    }

    /**
     * 执行提前减量：按时间下机机台清空当前收尾班次计划量，收尾提前一个班次。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param move 提前减量移动
     * @return true-执行成功；false-执行失败
     */
    private boolean applyAdvanceMove(LhScheduleContext context,
                                     List<LhShiftConfigVO> shifts,
                                     EmbryoEndingBalanceMove move,
                                     EmbryoEndingBalanceState state) {
        LhScheduleResult result = move.getDonor();
        LhScheduleResult pairResult = state.getPairResultMap().get(result);
        boolean pairUnit = Objects.nonNull(pairResult);
        Integer endingShift = move.getTargetShiftIndex();
        if (Objects.isNull(endingShift) || endingShift <= 0) {
            return false;
        }
        Integer currentQty = ShiftFieldUtil.getShiftPlanQty(result, endingShift);
        if (Objects.isNull(currentQty) || currentQty <= 0) {
            return false;
        }
        if (pairUnit) {
            Integer pairCurrentQty = ShiftFieldUtil.getShiftPlanQty(pairResult, endingShift);
            if (Objects.isNull(pairCurrentQty) || pairCurrentQty <= 0
                    || !Objects.equals(currentQty, pairCurrentQty)) {
                return false;
            }
        }
        // 先清空当前收尾班次，把前物料收尾点提前到上一个真实有量班次。
        ShiftFieldUtil.setShiftPlanQty(result, endingShift, 0, null, null);
        ShiftFieldUtil.clearShiftPlanAuxFields(result, endingShift);
        ShiftFieldUtil.appendShiftAnalysis(result, endingShift, BALANCE_ADVANCE_ANALYSIS);
        if (pairUnit) {
            ShiftFieldUtil.setShiftPlanQty(pairResult, endingShift, 0, null, null);
            ShiftFieldUtil.clearShiftPlanAuxFields(pairResult, endingShift);
            ShiftFieldUtil.appendShiftAnalysis(pairResult, endingShift, BALANCE_ADVANCE_ANALYSIS);
        }
        refreshResultSummary(context, result, shifts);
        if (pairUnit) {
            refreshResultSummary(context, pairResult, shifts);
        }
        /*
         * 14:00整点已属于中班；若清空中班尾量后前一早班仍恰好在14:00收尾，
         * 需再减少一个模数单位，使后物料真正落入当日早班换模。
         */
        Date originalChangeTime = state.getChangeTimeMap().get(result);
        if (Objects.nonNull(originalChangeTime)
                && LhScheduleTimeUtil.isAfternoonShift(context, originalChangeTime)
                && Objects.nonNull(result.getSpecEndTime())) {
            Date afternoonStart = LhScheduleTimeUtil.getAfternoonShiftStart(context, originalChangeTime);
            if (!result.getSpecEndTime().before(afternoonStart)
                    && LhScheduleTimeUtil.isSameDay(result.getSpecEndTime(), afternoonStart)
                    && !this.reduceOneMouldUnitFromEndingShift(context, shifts, result)) {
                return false;
            }
            if (pairUnit && !pairResult.getSpecEndTime().before(afternoonStart)
                    && LhScheduleTimeUtil.isSameDay(pairResult.getSpecEndTime(), afternoonStart)
                    && !this.reduceOneMouldUnitFromEndingShift(context, shifts, pairResult)) {
                return false;
            }
        }
        refreshResultSummary(context, result, shifts);
        syncStopHoldBoundaryAfterBalance(context, result, shifts);
        syncMachineEstimatedEndTime(context, result);
        if (pairUnit) {
            refreshResultSummary(context, pairResult, shifts);
            syncStopHoldBoundaryAfterBalance(context, pairResult, shifts);
            syncMachineEstimatedEndTime(context, pairResult);
        }
        move.setAppliedQty(currentQty * (pairUnit ? 2 : 1));
        return true;
    }

    /**
     * 从结果当前最后一个有量班次减少一个模数单位并重算收尾时间。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param result 待减量结果
     * @return true-减量成功；false-无可减量或减量会清空结果
     */
    private boolean reduceOneMouldUnitFromEndingShift(LhScheduleContext context,
                                                      List<LhShiftConfigVO> shifts,
                                                      LhScheduleResult result) {
        int endingShiftIndex = ShiftFieldUtil.resolveLastPlannedShiftIndex(result);
        int endingQty = this.resolveShiftPlanQty(result, endingShiftIndex);
        int mouldUnitQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(result.getMouldQty());
        if (endingShiftIndex <= 0 || endingQty <= mouldUnitQty
                || ShiftFieldUtil.resolveScheduledQty(result) <= mouldUnitQty) {
            return false;
        }
        int adjustedQty = endingQty - mouldUnitQty;
        Date startTime = ShiftFieldUtil.getShiftStartTime(result, endingShiftIndex);
        LhShiftConfigVO endingShift = this.findShiftByIndex(shifts, endingShiftIndex);
        if (Objects.isNull(startTime) && Objects.nonNull(endingShift)) {
            startTime = endingShift.getShiftStartDateTime();
        }
        Date endTime = this.resolveShiftCompletionTime(
                context, shifts, result, endingShiftIndex, adjustedQty);
        ShiftFieldUtil.setShiftPlanQty(
                result, endingShiftIndex, adjustedQty, startTime, endTime);
        ShiftFieldUtil.appendShiftAnalysis(result, endingShiftIndex, BALANCE_ADVANCE_ANALYSIS);
        return true;
    }

    /**
     * 快照当前移动实际影响的排程结果、机台结束时间和胶囊/允许超量运行态。
     *
     * @param context 排程上下文
     * @param affectedResults 当前移动的转出/承接机台结果
     * @param pairResultMap 双模SKU单控整机代表结果到配对侧结果的映射
     * @return 快照
     */
    private BalanceSnapshot snapshotGroupState(LhScheduleContext context,
                                               Set<LhScheduleResult> affectedResults,
                                               Map<LhScheduleResult, LhScheduleResult> pairResultMap) {
        BalanceSnapshot snapshot = new BalanceSnapshot();
        snapshot.setContext(context);
        Set<LhScheduleResult> expandedResults = expandGroupResults(affectedResults, pairResultMap);
        for (LhScheduleResult result : expandedResults) {
            LhScheduleResult resultCopy = new LhScheduleResult();
            BeanUtil.copyProperties(result, resultCopy);
            snapshot.getResultCopyMap().put(result, resultCopy);
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
            if (Objects.nonNull(machine)) {
                snapshot.getMachineEndTimeMap().put(result.getLhMachineCode(), machine.getEstimatedEndTime());
            }
        }
        snapshot.setCapsuleRuntimeUsageMap(new LinkedHashMap<String, Integer>(context.getCapsuleRuntimeUsageMap()));
        snapshot.setCapsuleReplacementShiftKeySet(
                new LinkedHashSet<String>(context.getCapsuleReplacementShiftKeySet()));
        snapshot.setCapsuleThresholdHandledMachineSet(
                new LinkedHashSet<String>(context.getCapsuleThresholdHandledMachineSet()));
        snapshot.setCapsuleReplacementCapacityLimitMap(
                new LinkedHashMap<String, Integer>(context.getCapsuleReplacementShiftCapacityLimitMap()));
        snapshot.setSharedEmbryoEndingAllowedOverQtyMap(
                new IdentityHashMap<LhScheduleResult, Integer>(
                        context.getSharedEmbryoEndingStaggerAllowedOverQtyMap()));
        snapshot.setEndingFillAllowedOverQtyMap(
                new IdentityHashMap<LhScheduleResult, Integer>(context.getEndingFillAllowedOverQtyMap()));
        // 结构停产保机机台参与均衡后，其冻结的实际生产结束时间快照也会变化，必须一并快照回滚。
        snapshot.setRetentionActualEndTimeMap(
                new LinkedHashMap<String, Date>(
                        context.getStructureMinMachineRetentionActualEndTimeMap()));
        return snapshot;
    }

    /**
     * 展开均衡候选集合，把单控整机的配对侧结果一并纳入快照。
     *
     * @param affectedResults 当前移动涉及的代表结果
     * @param pairResultMap 代表结果到配对侧结果的映射
     * @return 包含配对侧的结果集合
     */
    private Set<LhScheduleResult> expandGroupResults(
            Set<LhScheduleResult> affectedResults,
            Map<LhScheduleResult, LhScheduleResult> pairResultMap) {
        Set<LhScheduleResult> expandedResults =
                new LinkedHashSet<LhScheduleResult>(Math.max(4, affectedResults.size() * 2));
        for (LhScheduleResult result : affectedResults) {
            if (Objects.isNull(result)) {
                continue;
            }
            expandedResults.add(result);
            LhScheduleResult pairResult = pairResultMap.get(result);
            if (Objects.nonNull(pairResult)) {
                expandedResults.add(pairResult);
            }
        }
        return expandedResults;
    }

    /**
     * 恢复快照前的排程结果和运行态。
     *
     * @param snapshot 快照
     */
    private void restoreGroupState(BalanceSnapshot snapshot) {
        for (Map.Entry<LhScheduleResult, LhScheduleResult> entry : snapshot.getResultCopyMap().entrySet()) {
            BeanUtil.copyProperties(entry.getValue(), entry.getKey());
        }
        LhScheduleContext context = snapshot.getContext();
        if (Objects.isNull(context)) {
            return;
        }
        for (Map.Entry<String, Date> machineEntry : snapshot.getMachineEndTimeMap().entrySet()) {
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineEntry.getKey());
            if (Objects.nonNull(machine)) {
                machine.setEstimatedEndTime(machineEntry.getValue());
            }
        }
        context.getCapsuleRuntimeUsageMap().clear();
        context.getCapsuleRuntimeUsageMap().putAll(snapshot.getCapsuleRuntimeUsageMap());
        context.getCapsuleReplacementShiftKeySet().clear();
        context.getCapsuleReplacementShiftKeySet().addAll(snapshot.getCapsuleReplacementShiftKeySet());
        context.getCapsuleThresholdHandledMachineSet().clear();
        context.getCapsuleThresholdHandledMachineSet().addAll(snapshot.getCapsuleThresholdHandledMachineSet());
        context.getCapsuleReplacementShiftCapacityLimitMap().clear();
        context.getCapsuleReplacementShiftCapacityLimitMap().putAll(
                snapshot.getCapsuleReplacementCapacityLimitMap());
        context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().clear();
        context.getSharedEmbryoEndingStaggerAllowedOverQtyMap().putAll(
                snapshot.getSharedEmbryoEndingAllowedOverQtyMap());
        context.getEndingFillAllowedOverQtyMap().clear();
        context.getEndingFillAllowedOverQtyMap().putAll(snapshot.getEndingFillAllowedOverQtyMap());
        context.getStructureMinMachineRetentionActualEndTimeMap().clear();
        context.getStructureMinMachineRetentionActualEndTimeMap().putAll(
                snapshot.getRetentionActualEndTimeMap());
    }

    /**
     * 刷新结果行汇总计划量和收尾时间。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shifts 排程窗口班次
     */
    private void refreshResultSummary(LhScheduleContext context,
                                      LhScheduleResult result,
                                      List<LhShiftConfigVO> shifts) {
        if (Objects.isNull(result)) {
            return;
        }
        ShiftFieldUtil.syncDailyPlanQty(result);
        result.setTotalDailyPlanQty(result.getDailyPlanQty());
        if (Objects.isNull(result.getDailyPlanQty()) || result.getDailyPlanQty() <= 0) {
            result.setSpecEndTime(null);
            result.setTdaySpecEndTime(null);
            ResultDowntimeSummaryUtil.clearDowntimeSummary(result);
            return;
        }
        int lastShiftIndex = ShiftFieldUtil.applyLastPlannedShiftEndMark(
                result, "1".equals(result.getIsEnd()));
        Date specEndTime = lastShiftIndex > 0
                ? ShiftFieldUtil.getShiftEndTime(result, lastShiftIndex) : null;
        result.setSpecEndTime(specEndTime);
        result.setTdaySpecEndTime(specEndTime);
        syncDowntimeSummary(context, result);
    }

    /**
     * 同步结果的停机/清洗/保养摘要。
     *
     * @param context 排程上下文
     * @param result 排程结果
     */
    private void syncDowntimeSummary(LhScheduleContext context, LhScheduleResult result) {
        Date firstStartTime = resolveFirstPlannedShiftStartTime(result);
        if (Objects.isNull(firstStartTime) || Objects.isNull(result.getSpecEndTime())) {
            ResultDowntimeSummaryUtil.clearDowntimeSummary(result);
            return;
        }
        ResultDowntimeSummaryUtil.fillDowntimeSummary(
                result,
                resolveActualMaintenanceWindows(context, result.getLhMachineCode()),
                resolveEffectiveCleaningWindows(context, result),
                resolveMachineShutdownWindows(context, result.getLhMachineCode()),
                context.getScheduleWindowShifts());
    }

    /**
     * 将结果收尾时间同步到运行态机台。
     *
     * @param context 排程上下文
     * @param result 排程结果
     */
    private void syncMachineEstimatedEndTime(LhScheduleContext context, LhScheduleResult result) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || StringUtils.isEmpty(result.getLhMachineCode())) {
            return;
        }
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
        if (Objects.nonNull(machine)) {
            machine.setEstimatedEndTime(result.getSpecEndTime());
        }
    }

    /**
     * 均衡调整后同步停产保机机台的占用边界与冻结快照。
     *
     * <p>结构停产保机：更新冻结的实际生产结束时间快照，并把结果/运行态机台的占用结束
     * 时间按 max(实际生产结束时间, 统一释放时间) 回延，保机决策本身不重算，后续不同结构
     * SKU上机拦截仍读取统一释放时间。续作停产保机：恢复“占用延续到窗口末班”的边界语义，
     * 与续作主链 {@code extendContinuousStopHoldOccupancyToWindowEnd} 保持同一口径。</p>
     *
     * @param context 排程上下文
     * @param result 刚被均衡调整的结果
     * @param shifts 排程窗口班次
     */
    private void syncStopHoldBoundaryAfterBalance(LhScheduleContext context,
                                                  LhScheduleResult result,
                                                  List<LhShiftConfigVO> shifts) {
        if (Objects.isNull(context) || Objects.isNull(result)
                || StringUtils.isEmpty(result.getLhMachineCode())) {
            return;
        }
        String machineCode = result.getLhMachineCode();
        if (context.isStructureMinMachineRetained(machineCode)) {
            // 快照同步必须在 refreshResultSummary 之后、占用边界回延之前执行，
            // 此时 result.specEndTime 即为实际生产结束时间。
            structureMinMachineRetentionService.synchronizeRetainedMachineAfterEndingBalance(
                    context, machineCode, result.getSpecEndTime());
        }
        if (context.isContinuousStopHoldMachine(machineCode)
                && Objects.nonNull(result.getDailyPlanQty()) && result.getDailyPlanQty() > 0) {
            // 有量停产保机结果只延长机台/模具占用结束时间，不改变班次计划量。
            Date occupiedEndTime = CollectionUtils.isEmpty(shifts)
                    ? context.getWindowEndDate() : shifts.get(shifts.size() - 1).getShiftEndDateTime();
            if (Objects.nonNull(occupiedEndTime)) {
                result.setSpecEndTime(occupiedEndTime);
                result.setTdaySpecEndTime(occupiedEndTime);
            }
        }
    }

    /**
     * 判断机台是否处于停产保机状态（结构停产保机或续作停产保机）。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return true-处于停产保机；false-未处于停产保机
     */
    private boolean isStopHoldMachine(LhScheduleContext context, String machineCode) {
        return Objects.nonNull(context) && StringUtils.isNotEmpty(machineCode)
                && (context.isStructureMinMachineRetained(machineCode)
                || context.isContinuousStopHoldMachine(machineCode));
    }

    /**
     * 解析指定班次的剩余可排产能。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shift 目标班次
     * @return 剩余可排产能
     */
    private int resolveShiftFreeCapacity(LhScheduleContext context,
                                         LhScheduleResult result,
                                         LhShiftConfigVO shift) {
        int capacity = resolveResultShiftCapacity(context, result, shift);
        if (capacity <= 0) {
            return 0;
        }
        return Math.max(0, capacity - resolveShiftPlanQty(result, shift.getShiftIndex()));
    }

    /**
     * 解析结果在指定班次的最大可排产能（含停机、清洗、保养、班次管控和换胶囊口径）。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @param shift 目标班次
     * @return 班次最大可排产能
     */
    private int resolveResultShiftCapacity(LhScheduleContext context,
                                           LhScheduleResult result,
                                           LhShiftConfigVO shift) {
        if (Objects.isNull(context) || Objects.isNull(result) || Objects.isNull(shift)
                || Objects.isNull(result.getLhTime()) || result.getLhTime() <= 0) {
            return 0;
        }
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(result.getMouldQty());
        int baseShiftCapacity = Objects.isNull(result.getSingleMouldShiftQty())
                ? 0 : Math.max(0, result.getSingleMouldShiftQty());
        if (mouldQty <= 0 || baseShiftCapacity <= 0) {
            return 0;
        }
        ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(
                context, shift, shift.getShiftStartDateTime());
        if (Objects.isNull(control) || !control.isCanSchedule()) {
            return 0;
        }
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
        int runtimeShiftCapacity = Objects.isNull(machine)
                ? baseShiftCapacity
                : ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(context, machine, baseShiftCapacity);
        if (runtimeShiftCapacity <= 0) {
            return 0;
        }
        int dryIceLossQty = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_LOSS_QTY, LhScheduleConstant.DRY_ICE_LOSS_QTY);
        int dryIceDurationHours = context.getParamIntValue(
                LhScheduleParamConstant.DRY_ICE_DURATION_HOURS, LhScheduleConstant.DRY_ICE_DURATION_HOURS);
        int plannedRepairFixedQty = context.getParamIntValue(
                LhScheduleParamConstant.PLANNED_REPAIR_FIXED_QTY, LhScheduleConstant.PLANNED_REPAIR_FIXED_QTY);
        int shiftMaxQty = ShiftCapacityResolverUtil.resolveShiftCapacityWithDowntime(
                context.getDevicePlanShutList(),
                resolveEffectiveCleaningWindows(context, result),
                resolveCapacityMaintenanceWindows(context, result.getLhMachineCode()),
                result.getLhMachineCode(),
                control.getEffectiveStartTime(),
                control.getEffectiveEndTime(),
                runtimeShiftCapacity,
                result.getLhTime(),
                mouldQty,
                ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift),
                dryIceLossQty,
                dryIceDurationHours,
                shift,
                ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context),
                ScheduleTypeEnum.CONTINUOUS.getCode(),
                plannedRepairFixedQty);
        return ShiftProductionControlUtil.deductCapacityByControl(control, shiftMaxQty, mouldQty);
    }

    /**
     * 计算指定班次给定计划量对应的实际结束时间。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param result 排程结果
     * @param shiftIndex 班次序号
     * @param planQty 计划量
     * @return 实际结束时间
     */
    private Date resolveShiftCompletionTime(LhScheduleContext context,
                                            List<LhShiftConfigVO> shifts,
                                            LhScheduleResult result,
                                            int shiftIndex,
                                            int planQty) {
        LhShiftConfigVO shift = findShiftByIndex(shifts, shiftIndex);
        if (Objects.isNull(shift) || planQty <= 0) {
            return Objects.isNull(shift) ? null : shift.getShiftEndDateTime();
        }
        Date startTime = ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
        if (Objects.isNull(startTime)) {
            startTime = shift.getShiftStartDateTime();
        }
        int capacity = resolveResultShiftCapacity(context, result, shift);
        if (capacity <= 0) {
            return shift.getShiftEndDateTime();
        }
        Date endTime = ShiftCapacityResolverUtil.resolveShiftPlanEndTime(
                context.getDevicePlanShutList(),
                resolveEffectiveCleaningWindows(context, result),
                resolveCapacityMaintenanceWindows(context, result.getLhMachineCode()),
                result.getLhMachineCode(),
                startTime,
                shift.getShiftEndDateTime(),
                planQty,
                capacity);
        return Objects.isNull(endTime) ? shift.getShiftEndDateTime() : endTime;
    }

    /**
     * 解析结果生效的清洗窗口（收尾结果跳过清洗、清洗与换模重叠时剔除重叠）。
     *
     * @param context 排程上下文
     * @param result 排程结果
     * @return 有效清洗窗口
     */
    private List<MachineCleaningWindowDTO> resolveEffectiveCleaningWindows(LhScheduleContext context,
                                                                          LhScheduleResult result) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
        List<MachineCleaningWindowDTO> cleaningWindowList = Objects.isNull(machine)
                ? new ArrayList<MachineCleaningWindowDTO>(0) : machine.getCleaningWindowList();
        if (CollectionUtils.isEmpty(cleaningWindowList)
                || CleaningScheduleRuleUtil.shouldSkipCleaningByResultEnding(result)) {
            return new ArrayList<MachineCleaningWindowDTO>(0);
        }
        Date firstStartTime = resolveFirstPlannedShiftStartTime(result);
        Date switchEndTime = Objects.nonNull(result.getMouldChangeStartTime())
                ? LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(),
                LhScheduleTimeUtil.getMouldChangeTotalHours(context)) : firstStartTime;
        List<MachineCleaningWindowDTO> effectiveWindows = MachineCleaningOverlapUtil.excludeOverlapWindows(
                cleaningWindowList, result.getMouldChangeStartTime(), switchEndTime);
        return CollectionUtils.isEmpty(effectiveWindows)
                ? new ArrayList<MachineCleaningWindowDTO>(0)
                : new ArrayList<MachineCleaningWindowDTO>(effectiveWindows);
    }

    /**
     * 解析产能计算使用的保养窗口（精度保养 + 计划性维修容量窗口）。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return 容量保养窗口
     */
    private List<MachineMaintenanceWindowDTO> resolveCapacityMaintenanceWindows(LhScheduleContext context,
                                                                                String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        List<MachineMaintenanceWindowDTO> maintenanceWindowList = Objects.isNull(machine)
                ? new ArrayList<MachineMaintenanceWindowDTO>(0) : machine.getMaintenanceWindowList();
        return ShiftCapacityResolverUtil.resolveCapacityMaintenanceWindowList(
                context, context.getDevicePlanShutList(), machineCode, maintenanceWindowList);
    }

    /**
     * 解析真实精度保养窗口（不含计划性维修容量窗口）。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return 真实精度保养窗口
     */
    private List<MachineMaintenanceWindowDTO> resolveActualMaintenanceWindows(LhScheduleContext context,
                                                                              String machineCode) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(machineCode);
        return Objects.isNull(machine) || CollectionUtils.isEmpty(machine.getMaintenanceWindowList())
                ? new ArrayList<MachineMaintenanceWindowDTO>(0)
                : machine.getMaintenanceWindowList();
    }

    /**
     * 解析机台设备停机窗口。
     *
     * @param context 排程上下文
     * @param machineCode 机台编码
     * @return 设备停机窗口
     */
    private List<MdmDevicePlanShut> resolveMachineShutdownWindows(LhScheduleContext context,
                                                                  String machineCode) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getDevicePlanShutList())
                || StringUtils.isEmpty(machineCode)) {
            return new ArrayList<MdmDevicePlanShut>(0);
        }
        List<MdmDevicePlanShut> shutdownWindowList = new ArrayList<MdmDevicePlanShut>(4);
        for (MdmDevicePlanShut planShut : context.getDevicePlanShutList()) {
            if (Objects.nonNull(planShut) && StringUtils.equals(machineCode, planShut.getMachineCode())) {
                shutdownWindowList.add(planShut);
            }
        }
        return shutdownWindowList;
    }

    /**
     * 判断目标班次是否已被其他SKU占用。
     *
     * @param context 排程上下文
     * @param currentResult 当前结果
     * @param targetShift 目标班次
     * @return true-其他SKU已占用
     */
    private boolean isShiftOccupiedByOtherSku(LhScheduleContext context,
                                              LhScheduleResult currentResult,
                                              LhShiftConfigVO targetShift) {
        if (Objects.isNull(context) || Objects.isNull(currentResult) || Objects.isNull(targetShift)
                || StringUtils.isEmpty(currentResult.getLhMachineCode())) {
            return false;
        }
        if (!CollectionUtils.isEmpty(context.getMachineAssignmentMap())) {
            List<LhScheduleResult> assignedResults =
                    context.getMachineAssignmentMap().get(currentResult.getLhMachineCode());
            if (Objects.nonNull(assignedResults)) {
                // 机台分配索引已按机台预分组，命中时直接在小集合中判断，避免每个候选重复扫描全部排程结果。
                return this.isShiftOccupiedByOtherSku(assignedResults, currentResult, targetShift);
            }
        }
        return isShiftOccupiedByOtherSku(context.getScheduleResultList(), currentResult, targetShift);
    }

    /**
     * 判断结果列表中是否存在同机台同班次其他SKU计划。
     *
     * @param results 结果列表
     * @param currentResult 当前结果
     * @param targetShift 目标班次
     * @return true-其他SKU已占用
     */
    private boolean isShiftOccupiedByOtherSku(List<LhScheduleResult> results,
                                              LhScheduleResult currentResult,
                                              LhShiftConfigVO targetShift) {
        if (CollectionUtils.isEmpty(results) || Objects.isNull(targetShift)) {
            return false;
        }
        for (LhScheduleResult result : results) {
            if (Objects.isNull(result) || result == currentResult
                    || !StringUtils.equals(currentResult.getLhMachineCode(), result.getLhMachineCode())) {
                continue;
            }
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, targetShift.getShiftIndex());
            if (Objects.isNull(planQty) || planQty <= 0) {
                continue;
            }
            if (!StringUtils.equals(currentResult.getMaterialCode(), result.getMaterialCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 深拷贝每日早/中班换模模拟计数。
     *
     * @param sourceCountMap 来源计数
     * @return 可独立修改的计数副本
     */
    private Map<String, int[]> copyDailyMouldChangeCountMap(Map<String, int[]> sourceCountMap) {
        int initialCapacity = CollectionUtils.isEmpty(sourceCountMap) ? 4 : sourceCountMap.size();
        Map<String, int[]> copiedCountMap = new LinkedHashMap<String, int[]>(initialCapacity);
        if (CollectionUtils.isEmpty(sourceCountMap)) {
            return copiedCountMap;
        }
        for (Map.Entry<String, int[]> entry : sourceCountMap.entrySet()) {
            int[] sourceCounts = entry.getValue();
            int morningCount = Objects.nonNull(sourceCounts) && sourceCounts.length > 0 ? sourceCounts[0] : 0;
            int afternoonCount = Objects.nonNull(sourceCounts) && sourceCounts.length > 1 ? sourceCounts[1] : 0;
            copiedCountMap.put(entry.getKey(), new int[]{morningCount, afternoonCount});
        }
        return copiedCountMap;
    }

    /**
     * 解析SKU分组键（物料+产品状态）。
     *
     * @param sourceSku 来源SKU
     * @return 分组键
     */
    private String resolveSkuKey(SkuScheduleDTO sourceSku) {
        if (Objects.isNull(sourceSku)) {
            return "";
        }
        return MonthPlanDateResolver.buildMaterialStatusKey(
                sourceSku.getMaterialCode(), StringUtils.trimToEmpty(sourceSku.getProductStatus()));
    }

    /**
     * 按班次序号查找班次。
     *
     * @param shifts 班次列表
     * @param shiftIndex 班次序号
     * @return 班次配置；未找到返回null
     */
    private LhShiftConfigVO findShiftByIndex(List<LhShiftConfigVO> shifts, int shiftIndex) {
        if (CollectionUtils.isEmpty(shifts)) {
            return null;
        }
        for (LhShiftConfigVO shift : shifts) {
            if (Objects.nonNull(shift) && shift.getShiftIndex() == shiftIndex) {
                return shift;
            }
        }
        return null;
    }

    /**
     * 读取指定班次当前计划量。
     *
     * @param result 排程结果
     * @param shiftIndex 班次序号
     * @return 当前计划量
     */
    private int resolveShiftPlanQty(LhScheduleResult result, int shiftIndex) {
        Integer shiftQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
        return Objects.isNull(shiftQty) ? 0 : Math.max(0, shiftQty);
    }

    /**
     * 解析首个有计划量班次的开始时间。
     *
     * @param result 排程结果
     * @return 开始时间；无计划量时返回null
     */
    private Date resolveFirstPlannedShiftStartTime(LhScheduleResult result) {
        if (Objects.isNull(result)) {
            return null;
        }
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (Objects.nonNull(planQty) && planQty > 0) {
                return ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
            }
        }
        return null;
    }

    /**
     * 判断是否启用换模均衡新口径。
     *
     * @param context 排程上下文
     * @return true-启用；false-关闭
     */
    private boolean isChangeoverBalanceEnabled(LhScheduleContext context) {
        return Objects.nonNull(context)
                && Objects.nonNull(context.getScheduleConfig())
                && context.getScheduleConfig().isChangeoverBalanceEnabled();
    }

    /**
     * 构建状态摘要：机台、收尾类型、收尾班次、预测换模时间及评分。
     *
     * @param context 排程上下文
     * @param state 均衡状态
     * @return 状态摘要
     */
    private String buildStateSummary(LhScheduleContext context, EmbryoEndingBalanceState state) {
        StringBuilder detail = new StringBuilder(256);
        List<LhScheduleResult> sortedResults = new ArrayList<LhScheduleResult>(state.getEndingShiftMap().keySet());
        sortedResults.sort(Comparator.comparing(
                result -> StringUtils.defaultString(result.getLhMachineCode())));
        for (LhScheduleResult result : sortedResults) {
            SkuScheduleDTO sourceSku = state.getSourceSkuMap().get(result);
            detail.append("[机台=").append(result.getLhMachineCode())
                    .append(", 物料=").append(result.getMaterialCode())
                    .append(", 收尾类型=").append(state.getEndingTypeMap().get(result))
                    .append(", 收尾班次=").append(state.getEndingShiftMap().get(result))
                    .append(", 收尾时间=").append(LhScheduleTimeUtil.formatDateTime(
                            state.getEndingTimeMap().get(result)))
                    .append(", 预测换模时间=").append(
                            Objects.isNull(state.getChangeTimeMap().get(result))
                                    ? "被拒绝" : LhScheduleTimeUtil.formatDateTime(
                                    state.getChangeTimeMap().get(result)))
                    .append(", 换模班次=").append(resolveChangeShiftName(
                            context, state.getChangeTimeMap().get(result)))
                    .append("]");
        }
        detail.append(", 评分[硬限制风险=").append(state.getHardViolationCount())
                .append(", 日上限跨天待补满=").append(state.getDailyLimitDeferredCount())
                .append(", 超软目标班次数=").append(state.getExceededShiftCount())
                .append(", 超目标累计次数=").append(state.getOverflowQty())
                .append(", 早中班比例偏差=").append(state.getBalanceDeviation())
                .append(", 同班次收尾集中=").append(state.getSameShiftPairCount())
                .append("], 模拟计数=").append(buildCountSummary(state.getSimulatedCountMap()));
        return detail.toString();
    }

    /**
     * 解析预测换模时间所属班次名称。
     *
     * @param context 排程上下文
     * @param changeTime 预测换模时间
     * @return 班次名称
     */
    private String resolveChangeShiftName(LhScheduleContext context, Date changeTime) {
        if (Objects.isNull(changeTime)) {
            return "无";
        }
        if (LhScheduleTimeUtil.isMorningShift(context, changeTime)) {
            return "早班";
        }
        if (LhScheduleTimeUtil.isAfternoonShift(context, changeTime)) {
            return "中班";
        }
        return "夜班";
    }

    /**
     * 构建模拟换模计数摘要。
     *
     * @param countMap 每日早/中班计数
     * @return 计数摘要
     */
    private String buildCountSummary(Map<String, int[]> countMap) {
        if (CollectionUtils.isEmpty(countMap)) {
            return "-";
        }
        List<String> countTextList = new ArrayList<String>(countMap.size());
        for (Map.Entry<String, int[]> entry : countMap.entrySet()) {
            int[] counts = entry.getValue();
            int morningCount = Objects.nonNull(counts) && counts.length > 0 ? counts[0] : 0;
            int afternoonCount = Objects.nonNull(counts) && counts.length > 1 ? counts[1] : 0;
            StringBuilder countText = new StringBuilder(48);
            countText.append(entry.getKey())
                    .append("[早=").append(morningCount)
                    .append(",中=").append(afternoonCount)
                    .append(",日=").append(morningCount + afternoonCount)
                    .append(']');
            countTextList.add(countText.toString());
        }
        return StringUtils.join(countTextList, ",");
    }

    /**
     * 构建防循环状态键。
     * <p>只拼接稳定的机台编码、收尾班次、收尾量和换模计数，
     * 不持有排程结果副本，避免均衡循环创建大量临时对象。</p>
     *
     * @param state 当前均衡状态
     * @return 稳定状态键
     */
    private String buildBalanceStateKey(EmbryoEndingBalanceState state) {
        StringBuilder stateKey = new StringBuilder(256);
        List<LhScheduleResult> resultList =
                new ArrayList<LhScheduleResult>(state.getEndingShiftMap().keySet());
        resultList.sort(Comparator.comparing(
                result -> StringUtils.defaultString(result.getLhMachineCode())));
        for (LhScheduleResult result : resultList) {
            stateKey.append(result.getLhMachineCode()).append(':')
                    .append(state.getEndingShiftMap().get(result)).append(':')
                    .append(state.getEndingQtyMap().get(result)).append(';');
        }
        stateKey.append('|').append(this.buildCountSummary(state.getSimulatedCountMap()));
        return stateKey.toString();
    }

    /**
     * 记录无法完全均衡的非阻断问题。
     * <p>班次参考上限在总量守恒或产能等硬约束下允许暂时超出，
     * 因此只追加到过程日志和响应校验问题，不抛异常、不中断后续排程。</p>
     *
     * @param context 排程上下文
     * @param state 当前均衡状态
     * @param reason 无法继续均衡的原因
     */
    private void recordUnresolvedBalanceReason(LhScheduleContext context,
                                               EmbryoEndingBalanceState state,
                                               String reason) {
        String detail = "共用胎胚收尾均衡未完全解决: scheduleDate="
                + LhScheduleTimeUtil.formatDate(context.getScheduleDate())
                + ", 原因=" + reason
                + ", 当前换模分布=" + this.buildCountSummary(state.getSimulatedCountMap());
        PriorityTraceLogHelper.appendProcessLog(context, "共用胎胚收尾均衡", detail);
        log.warn("{}", detail);
    }

    /**
     * 追加已提交移动的过程日志。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param move 已提交移动
     * @param beforeState 调整前状态
     * @param afterState 调整后状态
     */
    private void appendMoveProcessLog(LhScheduleContext context,
                                      List<LhShiftConfigVO> shifts,
                                      EmbryoEndingBalanceMove move,
                                      EmbryoEndingBalanceState beforeState,
                                      EmbryoEndingBalanceState afterState) {
        LhScheduleResult donor = move.getDonor();
        SkuScheduleDTO donorSku = beforeState.getSourceSkuMap().get(donor);
        LhScheduleResult receiver = move.getReceiver();
        StringBuilder detail = new StringBuilder(256);
        detail.append("scheduleDate=").append(context.getScheduleDate())
                .append(", embryoCode=").append(
                        Objects.isNull(donorSku) ? null : donorSku.getEmbryoCode())
                .append(", 移动类型=").append(move.isCrossDay()
                        ? MOVE_TYPE_POSTPONE + "（跨天）" : move.getMoveType())
                .append(", 转出机台=").append(donor.getLhMachineCode())
                .append(", 转出物料=").append(donor.getMaterialCode())
                .append(", 转出收尾类型=").append(beforeState.getEndingTypeMap().get(donor))
                .append(", 是否停产保机=").append(
                        this.isStopHoldMachine(context, donor.getLhMachineCode()))
                .append(", 转出调整前=[班次").append(beforeState.getEndingShiftMap().get(donor))
                .append("=").append(beforeState.getEndingQtyMap().get(donor))
                .append(", 收尾时间=").append(LhScheduleTimeUtil.formatDateTime(
                        beforeState.getEndingTimeMap().get(donor)))
                .append(", 预测换模=").append(formatDateTimeSafe(
                        beforeState.getChangeTimeMap().get(donor))).append("]");
        if (Objects.nonNull(receiver)) {
            SkuScheduleDTO receiverSku = beforeState.getSourceSkuMap().get(receiver);
            detail.append(", 承接机台=").append(receiver.getLhMachineCode())
                    .append(", 承接物料=").append(receiver.getMaterialCode())
                    .append(", 承接收尾类型=").append(beforeState.getEndingTypeMap().get(receiver))
                    .append(", 承接是否停产保机=").append(
                            this.isStopHoldMachine(context, receiver.getLhMachineCode()))
                    .append(", 承接调整前=[班次").append(beforeState.getEndingShiftMap().get(receiver))
                    .append("=").append(beforeState.getEndingQtyMap().get(receiver))
                    .append(", 收尾时间=").append(LhScheduleTimeUtil.formatDateTime(
                            beforeState.getEndingTimeMap().get(receiver)))
                    .append(", 预测换模=").append(formatDateTimeSafe(
                            beforeState.getChangeTimeMap().get(receiver))).append("]");
        }
        detail.append(", 调整前评分[硬限制风险=").append(beforeState.getHardViolationCount())
                .append(", 超软目标=").append(beforeState.getExceededShiftCount())
                .append(", 同班次集中=").append(beforeState.getSameShiftPairCount()).append("]")
                .append(", 调整前换模分布=").append(
                        this.buildCountSummary(beforeState.getSimulatedCountMap()))
                .append(", 调整后评分[硬限制风险=").append(afterState.getHardViolationCount())
                .append(", 超软目标=").append(afterState.getExceededShiftCount())
                .append(", 同班次集中=").append(afterState.getSameShiftPairCount()).append("]")
                .append(", 调整后预测换模分布=").append(buildStateSummary(context, afterState));
        PriorityTraceLogHelper.appendProcessLog(context, "共用胎胚收尾均衡", detail.toString());
        log.info("共用胎胚收尾均衡移动提交, {}", detail);
    }

    /**
     * 追加候选被拒绝并完整回滚的过程日志。
     *
     * <p>按 spec 要求逐台记录日期、班次、胎胚、SKU、机台、收尾类型、是否停产保机、
     * 尝试策略、承接机台、调整前后收尾信息、调整前后早/中/日次数和具体拒绝原因。
     * 失败候选已完整恢复快照，不会残留班次备注。</p>
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param move 被拒绝的移动
     * @param beforeState 调整前状态
     * @param afterState 调整后状态；执行阶段失败时为null
     * @param rejectReason 具体拒绝原因
     */
    private void appendRejectedMoveProcessLog(LhScheduleContext context,
                                              List<LhShiftConfigVO> shifts,
                                              EmbryoEndingBalanceMove move,
                                              EmbryoEndingBalanceState beforeState,
                                              EmbryoEndingBalanceState afterState,
                                              String rejectReason) {
        if (Objects.isNull(context) || Objects.isNull(move) || Objects.isNull(move.getDonor())) {
            return;
        }
        LhScheduleResult donor = move.getDonor();
        SkuScheduleDTO donorSku = beforeState.getSourceSkuMap().get(donor);
        StringBuilder detail = new StringBuilder(256);
        detail.append("scheduleDate=").append(context.getScheduleDate())
                .append(", embryoCode=").append(
                        Objects.isNull(donorSku) ? null : donorSku.getEmbryoCode())
                .append(", materialCode=").append(donor.getMaterialCode())
                .append(", 机台=").append(donor.getLhMachineCode())
                .append(", 收尾类型=").append(beforeState.getEndingTypeMap().get(donor))
                .append(", 是否停产保机=").append(
                        this.isStopHoldMachine(context, donor.getLhMachineCode()))
                .append(", 尝试策略=").append(move.isCrossDay()
                        ? MOVE_TYPE_POSTPONE + "（跨天）" : move.getMoveType())
                .append(", 承接机台=").append(Objects.isNull(move.getReceiver()) ? "无"
                        : move.getReceiver().getLhMachineCode())
                .append(", 调整前=[班次").append(beforeState.getEndingShiftMap().get(donor))
                .append("=").append(beforeState.getEndingQtyMap().get(donor))
                .append(", 收尾时间=").append(LhScheduleTimeUtil.formatDateTime(
                        beforeState.getEndingTimeMap().get(donor)))
                .append(", 预测换模=").append(formatDateTimeSafe(
                        beforeState.getChangeTimeMap().get(donor))).append("]")
                .append(", 调整前换模分布=").append(
                        this.buildCountSummary(beforeState.getSimulatedCountMap()));
        if (Objects.nonNull(afterState)) {
            detail.append(", 调整后=[班次").append(afterState.getEndingShiftMap().get(donor))
                    .append("=").append(afterState.getEndingQtyMap().get(donor))
                    .append(", 收尾时间=").append(LhScheduleTimeUtil.formatDateTime(
                            afterState.getEndingTimeMap().get(donor)))
                    .append(", 预测换模=").append(formatDateTimeSafe(
                            afterState.getChangeTimeMap().get(donor))).append("]")
                    .append(", 调整后换模分布=").append(
                            this.buildCountSummary(afterState.getSimulatedCountMap()));
        }
        detail.append(", 结果=已拒绝并回滚")
                .append(", 原因=").append(StringUtils.defaultString(rejectReason));
        PriorityTraceLogHelper.appendProcessLog(context, "共用胎胚收尾均衡候选拒绝", detail.toString());
        log.info("共用胎胚收尾均衡候选拒绝, {}", detail);
    }

    /**
     * 安全格式化日期时间，null返回“无”。
     *
     * @param dateTime 日期时间
     * @return 格式化文本
     */
    private String formatDateTimeSafe(Date dateTime) {
        return Objects.isNull(dateTime) ? "无" : LhScheduleTimeUtil.formatDateTime(dateTime);
    }
}

/**
 * 共用胎胚收尾均衡的日期班次超限桶（包级可见，仅供默认策略使用）。
 */
class BalanceOverflowBucket {

    /** yyyy-MM-dd日期键 */
    private final String dateKey;

    /** 计数数组下标：0-早班，1-中班 */
    private final int shiftCountIndex;

    /** 调整前班次次数 */
    private final int count;

    /** 班次参考上限 */
    private final int limit;

    public BalanceOverflowBucket(String dateKey, int shiftCountIndex, int count, int limit) {
        this.dateKey = dateKey;
        this.shiftCountIndex = shiftCountIndex;
        this.count = count;
        this.limit = limit;
    }

    public String getDateKey() {
        return dateKey;
    }

    public int getShiftCountIndex() {
        return shiftCountIndex;
    }

    public int getCount() {
        return count;
    }

    public int getLimit() {
        return limit;
    }
}

/**
 * 共用胎胚收尾均衡状态（包级可见，仅供默认均衡策略使用）。
 */
class EmbryoEndingBalanceState {

    /** 机台当前收尾班次 */
    private Map<LhScheduleResult, Integer> endingShiftMap =
            new IdentityHashMap<LhScheduleResult, Integer>(8);

    /** 机台当前收尾班次计划量 */
    private Map<LhScheduleResult, Integer> endingQtyMap =
            new IdentityHashMap<LhScheduleResult, Integer>(8);

    /** 机台当前收尾时间 */
    private Map<LhScheduleResult, Date> endingTimeMap =
            new IdentityHashMap<LhScheduleResult, Date>(8);

    /** 机台后物料预测换模时间（null表示被拒绝） */
    private Map<LhScheduleResult, Date> changeTimeMap =
            new IdentityHashMap<LhScheduleResult, Date>(8);

    /** 机台来源SKU */
    private Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap =
            new IdentityHashMap<LhScheduleResult, SkuScheduleDTO>(8);

    /** 机台收尾类型 */
    private Map<LhScheduleResult, String> endingTypeMap =
            new IdentityHashMap<LhScheduleResult, String>(8);

    /** 双模SKU单控整机代表结果到配对侧结果的映射 */
    private Map<LhScheduleResult, LhScheduleResult> pairResultMap =
            new IdentityHashMap<LhScheduleResult, LhScheduleResult>(4);

    /** 按机台编码升序预演后的每日早/中班模拟计数 */
    private Map<String, int[]> simulatedCountMap = new LinkedHashMap<String, int[]>(4);

    /** 硬限制风险数（预测被拒绝或每日总次数超过15） */
    private int hardViolationCount;

    /** 因每日总次数上限跨天，但前物料尚未连续生产到目标06:00的机台数 */
    private int dailyLimitDeferredCount;

    /** 超过早班8/中班7软目标的班次数 */
    private int exceededShiftCount;

    /** 超过软目标的累计次数 */
    private int overflowQty;

    /** 早中班比例偏差合计 */
    private long balanceDeviation;

    /** 同班次收尾集中度（每个收尾班次桶内超过1台的机台数之和） */
    private int sameShiftPairCount;

    public Map<LhScheduleResult, Integer> getEndingShiftMap() {
        return endingShiftMap;
    }

    public Map<LhScheduleResult, Integer> getEndingQtyMap() {
        return endingQtyMap;
    }

    public Map<LhScheduleResult, Date> getEndingTimeMap() {
        return endingTimeMap;
    }

    public Map<LhScheduleResult, Date> getChangeTimeMap() {
        return changeTimeMap;
    }

    public Map<LhScheduleResult, SkuScheduleDTO> getSourceSkuMap() {
        return sourceSkuMap;
    }

    public Map<LhScheduleResult, String> getEndingTypeMap() {
        return endingTypeMap;
    }

    public Map<LhScheduleResult, LhScheduleResult> getPairResultMap() {
        return pairResultMap;
    }

    public void setPairResultMap(Map<LhScheduleResult, LhScheduleResult> pairResultMap) {
        this.pairResultMap = pairResultMap;
    }

    public Map<String, int[]> getSimulatedCountMap() {
        return simulatedCountMap;
    }

    public void setSimulatedCountMap(Map<String, int[]> simulatedCountMap) {
        this.simulatedCountMap = simulatedCountMap;
    }

    public int getHardViolationCount() {
        return hardViolationCount;
    }

    public void setHardViolationCount(int hardViolationCount) {
        this.hardViolationCount = hardViolationCount;
    }

    public int getDailyLimitDeferredCount() {
        return dailyLimitDeferredCount;
    }

    public void setDailyLimitDeferredCount(int dailyLimitDeferredCount) {
        this.dailyLimitDeferredCount = dailyLimitDeferredCount;
    }

    public int getExceededShiftCount() {
        return exceededShiftCount;
    }

    public void setExceededShiftCount(int exceededShiftCount) {
        this.exceededShiftCount = exceededShiftCount;
    }

    public int getOverflowQty() {
        return overflowQty;
    }

    public void setOverflowQty(int overflowQty) {
        this.overflowQty = overflowQty;
    }

    public long getBalanceDeviation() {
        return balanceDeviation;
    }

    public void setBalanceDeviation(long balanceDeviation) {
        this.balanceDeviation = balanceDeviation;
    }

    public int getSameShiftPairCount() {
        return sameShiftPairCount;
    }

    public void setSameShiftPairCount(int sameShiftPairCount) {
        this.sameShiftPairCount = sameShiftPairCount;
    }
}

/**
 * 共用胎胚收尾均衡移动描述（包级可见，仅供默认均衡策略使用）。
 */
class EmbryoEndingBalanceMove {

    /** 移动类型：尾量分摊/后延补量/提前减量 */
    private final String moveType;

    /** 移动主结果（转出机台或单独调整机台） */
    private final LhScheduleResult donor;

    /** 承接机台（尾量分摊时使用） */
    private final LhScheduleResult receiver;

    /** 移动数量 */
    private final int qty;

    /** 目标班次序号 */
    private final int targetShiftIndex;

    /** 实际落地数量（尾量分摊/后延补量成功后写入，单控整机为组级合计） */
    private int appliedQty;

    /** 是否为连续生产到次日06:00的跨天补量 */
    private boolean crossDay;

    public EmbryoEndingBalanceMove(String moveType,
                                   LhScheduleResult donor,
                                   LhScheduleResult receiver,
                                   int qty,
                                   int targetShiftIndex) {
        this.moveType = moveType;
        this.donor = donor;
        this.receiver = receiver;
        this.qty = qty;
        this.targetShiftIndex = targetShiftIndex;
    }

    public String getMoveType() {
        return moveType;
    }

    public LhScheduleResult getDonor() {
        return donor;
    }

    public LhScheduleResult getReceiver() {
        return receiver;
    }

    public int getQty() {
        return qty;
    }

    public int getTargetShiftIndex() {
        return targetShiftIndex;
    }

    public int getAppliedQty() {
        return appliedQty;
    }

    public void setAppliedQty(int appliedQty) {
        this.appliedQty = appliedQty;
    }

    public boolean isCrossDay() {
        return crossDay;
    }

    public void setCrossDay(boolean crossDay) {
        this.crossDay = crossDay;
    }
}

/**
 * 共用胎胚收尾均衡尝试快照（包级可见，仅供默认均衡策略使用）。
 */
class BalanceSnapshot {

    /** 排程上下文 */
    private LhScheduleContext context;

    /** 结果对象副本 */
    private final Map<LhScheduleResult, LhScheduleResult> resultCopyMap =
            new IdentityHashMap<LhScheduleResult, LhScheduleResult>(8);

    /** 机台结束时间快照 */
    private final Map<String, Date> machineEndTimeMap = new LinkedHashMap<String, Date>(8);

    /** 胶囊使用次数快照 */
    private Map<String, Integer> capsuleRuntimeUsageMap = new LinkedHashMap<String, Integer>(4);

    /** 换胶囊班次集合快照 */
    private Set<String> capsuleReplacementShiftKeySet = new LinkedHashSet<String>(4);

    /** 胶囊阈值已处理机台集合快照 */
    private Set<String> capsuleThresholdHandledMachineSet = new LinkedHashSet<String>(4);

    /** 换胶囊班次产能上限快照 */
    private Map<String, Integer> capsuleReplacementCapacityLimitMap = new LinkedHashMap<String, Integer>(4);

    /** 共用胎胚收尾允许超量快照 */
    private Map<LhScheduleResult, Integer> sharedEmbryoEndingAllowedOverQtyMap =
            new IdentityHashMap<LhScheduleResult, Integer>(4);

    /** 收尾补满允许超量快照 */
    private Map<LhScheduleResult, Integer> endingFillAllowedOverQtyMap =
            new IdentityHashMap<LhScheduleResult, Integer>(4);

    /** 结构停产保机机台冻结的实际生产结束时间快照 */
    private Map<String, Date> retentionActualEndTimeMap = new LinkedHashMap<String, Date>(4);

    public LhScheduleContext getContext() {
        return context;
    }

    public void setContext(LhScheduleContext context) {
        this.context = context;
    }

    public void setCapsuleRuntimeUsageMap(Map<String, Integer> capsuleRuntimeUsageMap) {
        this.capsuleRuntimeUsageMap = capsuleRuntimeUsageMap;
    }

    public void setCapsuleReplacementShiftKeySet(Set<String> capsuleReplacementShiftKeySet) {
        this.capsuleReplacementShiftKeySet = capsuleReplacementShiftKeySet;
    }

    public void setCapsuleThresholdHandledMachineSet(Set<String> capsuleThresholdHandledMachineSet) {
        this.capsuleThresholdHandledMachineSet = capsuleThresholdHandledMachineSet;
    }

    public void setCapsuleReplacementCapacityLimitMap(Map<String, Integer> capsuleReplacementCapacityLimitMap) {
        this.capsuleReplacementCapacityLimitMap = capsuleReplacementCapacityLimitMap;
    }

    public void setSharedEmbryoEndingAllowedOverQtyMap(
            Map<LhScheduleResult, Integer> sharedEmbryoEndingAllowedOverQtyMap) {
        this.sharedEmbryoEndingAllowedOverQtyMap = sharedEmbryoEndingAllowedOverQtyMap;
    }

    public void setEndingFillAllowedOverQtyMap(Map<LhScheduleResult, Integer> endingFillAllowedOverQtyMap) {
        this.endingFillAllowedOverQtyMap = endingFillAllowedOverQtyMap;
    }

    public Map<String, Date> getRetentionActualEndTimeMap() {
        return retentionActualEndTimeMap;
    }

    public void setRetentionActualEndTimeMap(Map<String, Date> retentionActualEndTimeMap) {
        this.retentionActualEndTimeMap = retentionActualEndTimeMap;
    }

    public Map<LhScheduleResult, LhScheduleResult> getResultCopyMap() {
        return resultCopyMap;
    }

    public Map<String, Date> getMachineEndTimeMap() {
        return machineEndTimeMap;
    }

    public Map<String, Integer> getCapsuleRuntimeUsageMap() {
        return capsuleRuntimeUsageMap;
    }

    public Set<String> getCapsuleReplacementShiftKeySet() {
        return capsuleReplacementShiftKeySet;
    }

    public Set<String> getCapsuleThresholdHandledMachineSet() {
        return capsuleThresholdHandledMachineSet;
    }

    public Map<String, Integer> getCapsuleReplacementCapacityLimitMap() {
        return capsuleReplacementCapacityLimitMap;
    }

    public Map<LhScheduleResult, Integer> getSharedEmbryoEndingAllowedOverQtyMap() {
        return sharedEmbryoEndingAllowedOverQtyMap;
    }

    public Map<LhScheduleResult, Integer> getEndingFillAllowedOverQtyMap() {
        return endingFillAllowedOverQtyMap;
    }
}
