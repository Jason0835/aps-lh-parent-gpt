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
 * 默认共用胎胚/同SKU多机台收尾均衡策略实现。
 *
 * <p>核心思路：</p>
 * <ul>
 *   <li>只处理同一胎胚编码下存在两台及以上收尾机台的场景；单胎胚单机台不触发；</li>
 *   <li>场景一：同SKU多机台同班次收尾时，通过同SKU尾量分摊错开收尾班次；</li>
 *   <li>场景二：共用胎胚多物料组级统一均衡，胎胚收尾以组级胎胚库存账本为唯一硬约束，
 *       允许同组跨物料互转尾量，互转成功后通过 SKU 内部额度重分配落地归属；</li>
 *   <li>SKU余量收尾保持该SKU总计划量不变，只能在同SKU多机台之间分摊；</li>
 *   <li>按时间下机收尾允许补量/后延或减量/提前，每次最多移动一个班次；</li>
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
    private static final String BALANCE_TRANSFER_ANALYSIS = "收尾均衡尾量分摊";

    /** 后延补量班次原因分析备注 */
    private static final String BALANCE_POSTPONE_ANALYSIS = "收尾均衡后延补量";

    /** 提前减量班次原因分析备注 */
    private static final String BALANCE_ADVANCE_ANALYSIS = "收尾均衡提前减量";

    /** 均衡调整最大轮次，避免极端数据死循环 */
    private static final int MAX_BALANCE_ROUNDS = 32;

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
    private CapsuleReplacementRuleService capsuleReplacementRuleService = new CapsuleReplacementRuleService();

    /**
     * 执行共用胎胚/同SKU多机台收尾均衡。
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
        // 收集全部续作收尾候选机台（含胎胚收尾、SKU余量收尾、按时间下机收尾）。
        List<LhScheduleResult> candidateList = new ArrayList<LhScheduleResult>(16);
        Map<LhScheduleResult, Integer> endingShiftMap = new IdentityHashMap<LhScheduleResult, Integer>(16);
        Map<LhScheduleResult, Integer> endingQtyMap = new IdentityHashMap<LhScheduleResult, Integer>(16);
        Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap = new IdentityHashMap<LhScheduleResult, SkuScheduleDTO>(16);
        Map<LhScheduleResult, String> endingTypeMap = new IdentityHashMap<LhScheduleResult, String>(16);
        Map<LhScheduleResult, LhScheduleResult> pairResultMap =
                new IdentityHashMap<LhScheduleResult, LhScheduleResult>(8);
        collectBalanceCandidates(
                context, candidateList, endingShiftMap, endingQtyMap, sourceSkuMap, endingTypeMap, pairResultMap);
        if (candidateList.size() < MIN_GROUP_MACHINE_COUNT) {
            return false;
        }
        // 按胎胚编码分组；单胎胚同SKU多机台、共用胎胚多物料组级均衡统一按胎胚组处理。
        Map<String, List<LhScheduleResult>> embryoGroupMap =
                groupCandidatesByEmbryo(candidateList, sourceSkuMap);
        // 全部胎胚组共用一份本地预演账本：前一组预测出的换模次数会成为后一组的基线，
        // 但不写真实dailyMouldChangeCountMap，避免均衡预演与后续正式换模分配重复计数。
        Map<String, int[]> sharedSimulationCountMap =
                copyDailyMouldChangeCountMap(context.getDailyMouldChangeCountMap());
        boolean adjusted = false;
        for (Map.Entry<String, List<LhScheduleResult>> entry : embryoGroupMap.entrySet()) {
            List<LhScheduleResult> groupResults = entry.getValue();
            if (balanceEmbryoGroup(context, shifts, entry.getKey(), groupResults,
                    sourceSkuMap, endingTypeMap, pairResultMap, sharedSimulationCountMap)) {
                adjusted = true;
            }
        }
        return adjusted;
    }

    /**
     * 收集续作收尾均衡候选机台。
     * <p>排除：非续作结果、换活字块结果、零计划结果、结构停产保机机台、
     * 同物料多产品状态续作切换结果。候选保留结果行的收尾班次、收尾班次计划量和来源SKU。</p>
     *
     * @param context 排程上下文
     * @param candidateList 候选结果列表
     * @param endingShiftMap 收尾班次映射
     * @param endingQtyMap 收尾班次计划量映射
     * @param sourceSkuMap 来源SKU映射
     * @param endingTypeMap 收尾类型映射
     * @param pairResultMap 双模SKU单控整机代表结果到配对侧结果的映射
     */
    private void collectBalanceCandidates(LhScheduleContext context,
                                          List<LhScheduleResult> candidateList,
                                          Map<LhScheduleResult, Integer> endingShiftMap,
                                          Map<LhScheduleResult, Integer> endingQtyMap,
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
                endingShiftMap.put(representative, representativeEndingShift);
                endingQtyMap.put(representative,
                        ShiftFieldUtil.getShiftPlanQty(representative, representativeEndingShift));
                sourceSkuMap.put(representative, sourceSku);
                endingTypeMap.put(representative, endingType);
                continue;
            }
            candidateList.add(result);
            endingShiftMap.put(result, endingShiftIndex);
            endingQtyMap.put(result, endingQty);
            sourceSkuMap.put(result, sourceSku);
            endingTypeMap.put(result, endingType);
        }
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
        // 结构停产保机结果保持原样，不参与收尾均衡。
        if (context.isStructureMinMachineRetained(result.getLhMachineCode())) {
            return false;
        }
        // 停产保机零产量/占用结果保持原样，不参与收尾均衡。
        if (context.isContinuousStopHoldMachine(result.getLhMachineCode())) {
            return false;
        }
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
     * 对单个胎胚组执行收尾均衡。
     *
     * @param context 排程上下文
     * @param shifts 排程窗口班次
     * @param embryoCode 胎胚编码
     * @param groupResults 组内候选机台结果
     * @param sourceSkuMap 来源SKU映射
     * @param endingTypeMap 收尾类型映射
     * @param pairResultMap 双模SKU单控整机代表结果到配对侧结果的映射
     * @param sharedSimulationCountMap 全部胎胚组共用的换模预演账本
     * @return true-执行了至少一次调整
     */
    private boolean balanceEmbryoGroup(LhScheduleContext context,
                                       List<LhShiftConfigVO> shifts,
                                       String embryoCode,
                                       List<LhScheduleResult> groupResults,
                                       Map<LhScheduleResult, SkuScheduleDTO> sourceSkuMap,
                                       Map<LhScheduleResult, String> endingTypeMap,
                                       Map<LhScheduleResult, LhScheduleResult> pairResultMap,
                                       Map<String, int[]> sharedSimulationCountMap) {
        EmbryoEndingBalanceState state = buildBalanceState(
                context, shifts, groupResults, sourceSkuMap, endingTypeMap,
                pairResultMap, sharedSimulationCountMap);
        // 单机台组不执行尾量调整，但其预测换模动作仍需占用全局预演次数，供后续胎胚组判断每日硬上限。
        if (groupResults.size() < MIN_GROUP_MACHINE_COUNT) {
            replaceSimulationCountMap(sharedSimulationCountMap, state.getSimulatedCountMap());
            return false;
        }
        logBalanceState(context, embryoCode, state, "均衡前");
        // 没有集中、超限或同班次收尾风险时不做调整。
        if (!needsBalance(state)) {
            log.info("共用胎胚收尾均衡无需调整, scheduleDate: {}, embryoCode: {}, 机台数: {}, "
                            + "原因: 无同班次集中收尾且预测换模次数未超过软目标/硬限制",
                    context.getScheduleDate(), embryoCode, groupResults.size());
            replaceSimulationCountMap(sharedSimulationCountMap, state.getSimulatedCountMap());
            return false;
        }
        boolean adjusted = false;
        int round = 0;
        // 每轮只提交一个使整体评分严格变优的移动，直到风险消除或无法继续改善。
        // 硬限制/超软目标/同班次集中都已消除后立即停止，不再仅为早中班比例偏差
        // 继续执行提前减量或后延补量，避免对按时间下机机台产生非必要的整班减产。
        while (round < MAX_BALANCE_ROUNDS && needsBalance(state)) {
            round++;
            EmbryoEndingBalanceMove bestMove = findFirstImprovingMove(
                    context, shifts, state, pairResultMap, sharedSimulationCountMap);
            if (Objects.isNull(bestMove)) {
                break;
            }
            state = buildBalanceState(
                    context, shifts, groupResults, sourceSkuMap, endingTypeMap,
                    pairResultMap, sharedSimulationCountMap);
            adjusted = true;
            log.info("共用胎胚收尾均衡提交, scheduleDate: {}, embryoCode: {}, 移动类型: {}, 轮次: {}, "
                            + "调整后详情: {}",
                    context.getScheduleDate(), embryoCode, bestMove.getMoveType(), round,
                    buildStateSummary(context, state));
        }
        if (needsBalance(state)) {
            log.info("共用胎胚收尾均衡无法继续改善, scheduleDate: {}, embryoCode: {}, 原因: "
                            + "候选移动均无法降低换模集中/超限评分，保留当前排程结果",
                    context.getScheduleDate(), embryoCode);
        }
        // 当前组最终预测结果提交到全局本地账本，后一组必须在该次数基础上继续预演。
        replaceSimulationCountMap(sharedSimulationCountMap, state.getSimulatedCountMap());
        return adjusted;
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
        for (EmbryoEndingBalanceMove move : moveList) {
            BalanceSnapshot snapshot = snapshotGroupState(
                    context, state.getSourceSkuMap().keySet(), pairResultMap);
            boolean applied = executeMove(context, shifts, move, state);
            if (!applied) {
                restoreGroupState(snapshot);
                continue;
            }
            EmbryoEndingBalanceState nextState = buildBalanceState(
                    context, shifts, new ArrayList<LhScheduleResult>(state.getSourceSkuMap().keySet()),
                    state.getSourceSkuMap(), state.getEndingTypeMap(), pairResultMap,
                    baseSimulationCountMap);
            if (compareScore(nextState, state) < 0) {
                // 评分严格变优后才落地跨物料互转的SKU额度重分配，避免失败尝试污染账本。
                if (!commitMoveQuotaChanges(context, move, state)) {
                    restoreGroupState(snapshot);
                    continue;
                }
                appendMoveProcessLog(context, shifts, move, state, nextState);
                return move;
            }
            restoreGroupState(snapshot);
        }
        return null;
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
            Date endingTime = result.getSpecEndTime();
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
                || state.getExceededShiftCount() > 0
                || state.getSameShiftPairCount() > 0;
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
        // 同班次收尾集中度：每个收尾班次桶内超过1台的机台数之和。
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
     * <p>优先级与需求一致：先保证每日换模总次数硬限制，再消除同班次集中收尾，
     * 然后才优化早班8/中班7软目标和早中班比例偏差。</p>
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
        comparison = Integer.compare(left.getSameShiftPairCount(), right.getSameShiftPairCount());
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
        return Long.compare(left.getBalanceDeviation(), right.getBalanceDeviation());
    }

    /**
     * 构建候选移动列表，顺序：尾量分摊 -> 后延补量 -> 提前减量，组内按机台编码升序。
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
        return moveList;
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
            LhScheduleResult donorPair = state.getPairResultMap().get(donor);
            boolean donorPairUnit = Objects.nonNull(donorPair);
            int donorEndingGroupQty = donorPairUnit ? donorEndingQty * 2 : donorEndingQty;
            int donorTotalGroupQty = ShiftFieldUtil.resolveScheduledQty(donor)
                    + (donorPairUnit ? ShiftFieldUtil.resolveScheduledQty(donorPair) : 0);
            for (LhScheduleResult receiver : sortedResults) {
                if (receiver == donor) {
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
                // 尾量分摊只解决同一收尾班次集中收尾，转出方与接收方原收尾班次必须相同，
                // 不允许把尾量跨多个班次甚至跨业务日搬运。
                if (!Objects.equals(donorEndingShift, receiverEndingShift)
                        || receiverEndingShift >= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT) {
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
            if (Objects.isNull(endingShift)
                    || endingShift >= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT) {
                continue;
            }
            LhScheduleResult pairResult = state.getPairResultMap().get(result);
            boolean pairUnit = Objects.nonNull(pairResult);
            // 后延不得越过降模释放边界。
            if (isBeyondReleaseBoundary(context, result, endingShift + 1)
                    || (pairUnit && isBeyondReleaseBoundary(
                    context, pairResult, endingShift + 1))) {
                continue;
            }
            LhShiftConfigVO nextShift = findShiftByIndex(shifts, endingShift + 1);
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
                    MOVE_TYPE_POSTPONE, result, null, postponeQty, endingShift + 1));
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
            // 首班收尾机台不能再提前。
            if (Objects.isNull(endingShift) || endingShift <= 1
                    || Objects.isNull(endingQty) || endingQty <= 0) {
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
        refreshResultSummary(context, receiver, shifts);
        syncMachineEstimatedEndTime(context, donor);
        syncMachineEstimatedEndTime(context, receiver);
        if (donorPairUnit) {
            refreshResultSummary(context, donorPair, shifts);
            syncMachineEstimatedEndTime(context, donorPair);
        }
        if (receiverPairUnit) {
            refreshResultSummary(context, receiverPair, shifts);
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
        syncMachineEstimatedEndTime(context, result);
        if (pairUnit) {
            refreshResultSummary(context, pairResult, shifts);
            syncMachineEstimatedEndTime(context, pairResult);
        }
        move.setAppliedQty(actualAddQty * (pairUnit ? 2 : 1));
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
        if (Objects.isNull(endingShift) || endingShift <= 1) {
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
        // 清空收尾班次，机台提前到前一班次收尾。
        ShiftFieldUtil.setShiftPlanQty(result, endingShift, 0, null, null);
        ShiftFieldUtil.clearShiftPlanAuxFields(result, endingShift);
        if (pairUnit) {
            ShiftFieldUtil.setShiftPlanQty(pairResult, endingShift, 0, null, null);
            ShiftFieldUtil.clearShiftPlanAuxFields(pairResult, endingShift);
        }
        // 提前减量备注写入新的收尾班次，便于结果对账。
        ShiftFieldUtil.appendShiftAnalysis(result, endingShift - 1, BALANCE_ADVANCE_ANALYSIS);
        if (pairUnit) {
            ShiftFieldUtil.appendShiftAnalysis(pairResult, endingShift - 1, BALANCE_ADVANCE_ANALYSIS);
        }
        refreshResultSummary(context, result, shifts);
        syncMachineEstimatedEndTime(context, result);
        if (pairUnit) {
            refreshResultSummary(context, pairResult, shifts);
            syncMachineEstimatedEndTime(context, pairResult);
        }
        return true;
    }

    /**
     * 快照当前胎胚组的排程结果、机台结束时间和胶囊/允许超量运行态。
     *
     * @param context 排程上下文
     * @param groupResults 组内候选机台结果
     * @param pairResultMap 双模SKU单控整机代表结果到配对侧结果的映射
     * @return 快照
     */
    private BalanceSnapshot snapshotGroupState(LhScheduleContext context,
                                               Set<LhScheduleResult> groupResults,
                                               Map<LhScheduleResult, LhScheduleResult> pairResultMap) {
        BalanceSnapshot snapshot = new BalanceSnapshot();
        snapshot.setContext(context);
        Set<LhScheduleResult> expandedResults = expandGroupResults(groupResults, pairResultMap);
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
        return snapshot;
    }

    /**
     * 展开均衡候选集合，把单控整机的配对侧结果一并纳入快照。
     *
     * @param groupResults 组内候选代表结果
     * @param pairResultMap 代表结果到配对侧结果的映射
     * @return 包含配对侧的结果集合
     */
    private Set<LhScheduleResult> expandGroupResults(
            Set<LhScheduleResult> groupResults,
            Map<LhScheduleResult, LhScheduleResult> pairResultMap) {
        Set<LhScheduleResult> expandedResults =
                new LinkedHashSet<LhScheduleResult>(Math.max(8, groupResults.size() * 2));
        for (LhScheduleResult result : groupResults) {
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
            if (isShiftOccupiedByOtherSku(assignedResults, currentResult, targetShift)) {
                return true;
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
     * 使用当前胎胚组的最终预演结果覆盖全局本地预演账本。
     * <p>覆盖时再次深拷贝次数数组，避免下一组预演修改当前状态对象中的历史评分数据。</p>
     *
     * @param targetCountMap 全部胎胚组共用的预演账本
     * @param sourceCountMap 当前胎胚组最终预演结果
     */
    private void replaceSimulationCountMap(Map<String, int[]> targetCountMap,
                                           Map<String, int[]> sourceCountMap) {
        targetCountMap.clear();
        targetCountMap.putAll(copyDailyMouldChangeCountMap(sourceCountMap));
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
     * 输出均衡前/后状态日志。
     *
     * @param context 排程上下文
     * @param embryoCode 胎胚编码
     * @param state 均衡状态
     * @param phase 阶段描述
     */
    private void logBalanceState(LhScheduleContext context,
                                 String embryoCode,
                                 EmbryoEndingBalanceState state,
                                 String phase) {
        log.info("共用胎胚收尾均衡{}状态, scheduleDate: {}, embryoCode: {}, 机台数: {}, 详情: {}",
                phase, context.getScheduleDate(), embryoCode,
                state.getEndingShiftMap().size(), buildStateSummary(context, state));
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
            countTextList.add(entry.getKey() + "早" + morningCount + "中" + afternoonCount);
        }
        return StringUtils.join(countTextList, ",");
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
                .append(", 移动类型=").append(move.getMoveType())
                .append(", 转出机台=").append(donor.getLhMachineCode())
                .append(", 转出物料=").append(donor.getMaterialCode())
                .append(", 转出收尾类型=").append(beforeState.getEndingTypeMap().get(donor))
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
                .append(", 调整后评分[硬限制风险=").append(afterState.getHardViolationCount())
                .append(", 超软目标=").append(afterState.getExceededShiftCount())
                .append(", 同班次集中=").append(afterState.getSameShiftPairCount()).append("]")
                .append(", 调整后预测换模分布=").append(buildStateSummary(context, afterState));
        PriorityTraceLogHelper.appendProcessLog(context, "共用胎胚收尾均衡", detail.toString());
        log.info("共用胎胚收尾均衡移动提交, {}", detail);
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
