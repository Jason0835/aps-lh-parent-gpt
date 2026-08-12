/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy.impl;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.IEndingJudgmentStrategy;
import com.zlt.aps.lh.engine.strategy.ISkuPriorityStrategy;
import com.zlt.aps.lh.util.LhSpecialMaterialUtil;
import com.zlt.aps.lh.util.LhSpecifyMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 默认SKU排产优先级策略实现。
 *
 * <p>业务定位：</p>
 * <ul>
 *   <li>同时服务 S4.4 续作和 S4.5 新增排产的 SKU 顺序整理；</li>
 *   <li>主排序优先考虑交期锁定、延误天数、结构全收尾、供应链优先级等全局因素；</li>
 *   <li>试制、量试按新增 SKU 类型分组优先；小批量并入正规组，不再天然排在正规 SKU 之前；</li>
 *   <li>排序完成后回写 {@code scheduleOrder}，供最终结果展示和日志追踪。</li>
 * </ul>
 *
 * <p>注意：该类只负责 SKU 顺序，不负责机台可用性、单控约束或排产量计算；
 * 这些规则分别在机台匹配策略和目标量策略中处理。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class DefaultSkuPriorityStrategy implements ISkuPriorityStrategy {

    /** 特殊材料标识 */
    private static final String SPECIAL_MATERIAL_YES_FLAG = "1";
    /** 雪地胎关键词分隔正则 */
    private static final String WINTER_TIRE_KEYWORD_SEPARATOR_REGEX = "[,，]";

    @Resource
    private IEndingJudgmentStrategy endingJudgmentStrategy;

    @Override
    public void sortByPriority(LhScheduleContext context) {
        log.info("执行SKU优先级排序, 续作SKU数: {}, 新增SKU数: {}",
                context.getContinuousSkuList().size(), context.getNewSpecSkuList().size());

        /*
         * 排序开始时按结构转产表最大END_DAY构建一次结构收尾标记：同结构只计算一次包含首尾的
         * 日期距离，命中后将当前参与排产的同结构全部SKU写入对象身份快照。后续比较器、排序日志
         * 和结果描述只读该快照，既不逐SKU查询数据库，也不重复执行结构日期计算。
         */
        Map<SkuScheduleDTO, Integer> structureEndingDaysMap = new IdentityHashMap<>(16);
        Map<String, StructurePriorityMeta> structurePriorityMap = buildStructurePriorityMap(
                context, structureEndingDaysMap);
        Comparator<SkuScheduleDTO> priorityComparator = buildPriorityComparator(
                structurePriorityMap, structureEndingDaysMap);
        Comparator<SkuScheduleDTO> tailComparator = buildTailComparator(context);
        Comparator<SkuScheduleDTO> comparator = priorityComparator.thenComparing(tailComparator)
                .thenComparing(SkuScheduleDTO::getMaterialCode, Comparator.nullsLast(String::compareTo));
        // 新增规格按试制、量试、正规组分层；小批量作为正规组 SKU 继续走通用排序。
        Comparator<SkuScheduleDTO> newSpecComparator = buildNewSpecComparator(
                context, structurePriorityMap, structureEndingDaysMap, tailComparator);
        sortSkuList(context.getContinuousSkuList(), context.getStructureEarliestLhTimeMap(),comparator);
        sortSkuList(context.getNewSpecSkuList(), context.getStructureEarliestLhTimeMap(),newSpecComparator);

        // 同时对每个结构下的SKU列表排序，保证结构内顺序与主排序一致。
        for (Map.Entry<String, List<SkuScheduleDTO>> entry : context.getStructureSkuMap().entrySet()) {
            entry.getValue().sort(comparator);
            //根据胎胚最早可供硫化时间重新排序 sandy+ 2026.7.7
            reorderByEarliestLhTime(entry.getValue(),context.getStructureEarliestLhTimeMap());
        }

        // 按统一优先级回写顺序号，供后续结果对象复用。
        List<SkuScheduleDTO> orderedSkuList = buildOrderedSkuList(context, newSpecComparator);
        reorderByEarliestLhTime(orderedSkuList,context.getStructureEarliestLhTimeMap());
        int order = 1;
        for (SkuScheduleDTO sku : orderedSkuList) {
            sku.setScheduleOrder(order++);
        }

        // 按续作/新增分别回写排序名次（rank）和单行描述，作为排程结果落库 skuSortRank/skuSortDesc 的来源；
        // 与“SKU排序优先级汇总”日志同源，TOP N 日志仅做截断展示。
        // 仅在当前步骤对应的列表上回写：S4.4 写续作列表，S4.5 写新增列表，
        // 避免 S4.4 先对未稳定的新增列表写入了过期快照。
        if (ScheduleStepEnum.S4_4_CONTINUOUS_PRODUCTION.getCode().equals(context.getCurrentStep())) {
            writeSkuSortRankAndDesc(context, context.getContinuousSkuList(), false,
                    structurePriorityMap, structureEndingDaysMap);
        }
        if (ScheduleStepEnum.S4_5_NEW_PRODUCTION.getCode().equals(context.getCurrentStep())) {
            writeSkuSortRankAndDesc(context, context.getNewSpecSkuList(), true,
                    structurePriorityMap, structureEndingDaysMap);
        }

        traceOpenProductionLateScore(context, orderedSkuList);
        traceSortedSkuList(context, structurePriorityMap, structureEndingDaysMap);
        // S4.4 续作排序、排产消费前，orderedSkuList 为续作+新增全量未消费候选，输出全量统一排序日志。
        if (ScheduleStepEnum.S4_4_CONTINUOUS_PRODUCTION.getCode().equals(context.getCurrentStep())) {
            traceFullSortedSkuList(context, orderedSkuList, structurePriorityMap, structureEndingDaysMap);
        }
        log.debug("SKU优先级排序完成, 排序后第一位: {}",
                CollectionUtils.isEmpty(orderedSkuList) ? "空" : orderedSkuList.get(0).getMaterialCode());
    }


    /**
     * 根据胎胚最早可供硫化时间重新排序。
     *
     * <p>本次仅修正历史注释口径；续作与新增 SKU 的既有排序行为继续保留，
     * S4.5 的生产时间限制由新增排产中心解析器独立处理。</p>
     * @param skuList SKU列表
     * @param embryoLhTimeMap 胎胚最早可供硫化时间 Map，key=结构名称，value=最早可供时间
     */
    private void reorderByEarliestLhTime(
            List<SkuScheduleDTO> skuList,
            Map<String,Date> embryoLhTimeMap) {

        if (PubUtil.isEmpty(embryoLhTimeMap)){
            return;
        }

        // 2. 找出所有有时间的元素，按时间排序
        List<SkuScheduleDTO> sortedWithTime = skuList.stream()
                .filter(sku -> embryoLhTimeMap.containsKey(sku.getStructureName()))
                .sorted(Comparator.comparing(sku -> embryoLhTimeMap.get(sku.getStructureName())))
                .collect(Collectors.toList());

        // 3. 按原列表顺序，将排序后的"有时间"元素替换回原来的"有时间"位置
        int timeIndex = 0;
        for (int i = 0; i < skuList.size(); i++) {
            SkuScheduleDTO sku = skuList.get(i);
            if (embryoLhTimeMap.containsKey(sku.getStructureName())) {
                // 这个位置原本是有时间的，用排序后的元素替换
                skuList.set(i, sortedWithTime.get(timeIndex++));
            }
            // 无时间的元素保持不变
        }
    }

    /**
     * 构建SKU多维度比较器
     * <p>
     * 排序规则（优先级从高到低）：
     * <ol>
     *   <li>有发货要求优先（deliveryLocked=true 排前）</li>
     *   <li>延误天数负值越小越优先（delayDays 升序，负数<0<正数）</li>
     *   <li>结构N天内收尾优先：结构转产表最大END_DAY与T日的包含首尾距离严格小于
     *       {@code SYS0304002}时，同结构全部候选SKU进入原结构收尾层级，结构收尾日越晚越优先</li>
     *   <li>供应链优先级：高优先级(04) → 周期排产(05) → 中优先级(06) → 搭配排产(07)</li>
     * </ol>
     * </p>
     *
     * @return SKU比较器
     */
    private Comparator<SkuScheduleDTO> buildPriorityComparator(Map<String, StructurePriorityMeta> structurePriorityMap,
                                                               Map<SkuScheduleDTO, Integer> structureEndingDaysMap) {
        return Comparator
                // 顺序1：锁定上机日期的优先。
                .comparingInt((SkuScheduleDTO s) -> s.isDeliveryLocked() ? 0 : 1)
                // 顺序2：延迟上机越久越优先（负数越小越优先），未知值排后。
                .thenComparing(SkuScheduleDTO::getDelayDays, Comparator.nullsLast(Comparator.naturalOrder()))
                // 顺序3：结构N天内收尾优先，继续沿用原层级位置，并按结构最大END_DAY距T日天数降序。
                .thenComparingInt((SkuScheduleDTO s) -> isStructureAllEndingPriority(structurePriorityMap, s) ? 0 : 1)
                .thenComparingInt((SkuScheduleDTO s) -> isStructureAllEndingPriority(structurePriorityMap, s)
                        && hasKnownStructureEndingDays(structureEndingDaysMap, s) ? 0 : 1)
                .thenComparingInt((SkuScheduleDTO s) -> isStructureAllEndingPriority(structurePriorityMap, s)
                        && hasKnownStructureEndingDays(structureEndingDaysMap, s)
                        ? -resolveStructureEndingDays(structureEndingDaysMap, s) : 0);
    }

    /**
     * 构建结构优先级后的尾部比较器。
     *
     * @param context 排程上下文
     * @return 尾部比较器
     */
    private Comparator<SkuScheduleDTO> buildTailComparator(LhScheduleContext context) {
        return Comparator
                // 顺序4：供应链优先按四类待排量逐级比较。
                .comparingInt((SkuScheduleDTO s) -> -s.getHighPriorityPendingQty())
                .thenComparingInt((SkuScheduleDTO s) -> -s.getMidPriorityPendingQty())
                .thenComparingInt((SkuScheduleDTO s) -> -s.getCycleProductionPendingQty())
                .thenComparingInt((SkuScheduleDTO s) -> -s.getConventionProductionPendingQty())
                // 顺序5：开产模式下雪地胎、不同英寸、特殊材料仅在同等条件下靠后。
                .thenComparingInt((SkuScheduleDTO s) -> resolveOpenProductionLateScore(context, s));
    }

    /**
     * 构建新增SKU比较器。
     * <p>试制、量试先于正规组；小批量并入正规组，继续复用正规新增排序。</p>
     *
     * @param context 排程上下文
     * @param priorityComparator 锁交期/延期/结构优先比较器
     * @param tailComparator 供应链及尾部比较器
     * @return 新增SKU比较器
     */
    private Comparator<SkuScheduleDTO> buildNewSpecComparator(LhScheduleContext context,
                                                              Map<String, StructurePriorityMeta> structurePriorityMap,
                                                              Map<SkuScheduleDTO, Integer> structureEndingDaysMap,
                                                              Comparator<SkuScheduleDTO> tailComparator) {
        return (left, right) -> compareNewSpecSku(context, structurePriorityMap,
                structureEndingDaysMap, tailComparator, left, right);
    }

    private int compareNewSpecSku(LhScheduleContext context,
                                  Map<String, StructurePriorityMeta> structurePriorityMap,
                                  Map<SkuScheduleDTO, Integer> structureEndingDaysMap,
                                  Comparator<SkuScheduleDTO> tailComparator,
                                  SkuScheduleDTO left,
                                  SkuScheduleDTO right) {
        // 新增SKU先按施工阶段分组：试制、量试、正规组分层；小批量归入正规组。
        // 续作欠产转入新增的补偿SKU不再享有同组内置顶优先权，统一按现有新增排序规则参与排序。
        int compareResult = Integer.compare(resolveNewSpecGroupScore(left), resolveNewSpecGroupScore(right));
        if (compareResult != 0) {
            return compareResult;
        }

        if (isTrialGroupSku(left) || isMassTrialGroupSku(left)) {
            // 试制/量试组不套用完整供应链排序，避免被正规SKU的供应链量级干扰。
            compareResult = compareTrialOrMassTrialGroup(left, right);
        } else {
            // 正规组继续复用交期、延误、结构收尾、供应链等主排序。
            compareResult = compareFormalGroupSku(context, structurePriorityMap,
                    structureEndingDaysMap, tailComparator, left, right);
        }
        if (compareResult != 0) {
            return compareResult;
        }
        return Comparator.nullsLast(String::compareTo).compare(
                left == null ? null : left.getMaterialCode(),
                right == null ? null : right.getMaterialCode());
    }

    /**
     * 判断 SKU 是否属于试制/量试。
     *
     * @param sku SKU
     * @return true-试制或量试
     */
    private boolean isTrialOrMassTrialSku(SkuScheduleDTO sku) {
        if (sku == null) {
            return false;
        }
        return StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())
                || StringUtils.equals(ConstructionStageEnum.MASS_TRIAL.getCode(), sku.getConstructionStage());
    }

    /**
     * 解析新增SKU类型补充排序分。
     * <p>试制 -> 量试 -> 正规组；小批量归入正规组，不再单独给排序分。</p>
     *
     * @param sku SKU
     * @return 排序分，值越小越优先
     */
    private int resolveNewSpecSkuTypeScore(SkuScheduleDTO sku) {
        if (sku == null) {
            return 2;
        }
        if (StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())) {
            return 0;
        }
        if (StringUtils.equals(ConstructionStageEnum.MASS_TRIAL.getCode(), sku.getConstructionStage())) {
            return 1;
        }
        return 2;
    }

    /**
     * 解析新增SKU分组分值。
     *
     * @param sku SKU
     * @return 0-试制组，1-量试组，2-正规组
     */
    private int resolveNewSpecGroupScore(SkuScheduleDTO sku) {
        return resolveNewSpecSkuTypeScore(sku);
    }

    /**
     * 正规组内继续复用现有新增排序逻辑。
     *
     * <p>排序层级：定点机台优先、交期锁定、延误天数、结构全收尾、供应链待排量和开产靠后因素。</p>
     */
    private int compareFormalGroupSku(LhScheduleContext context,
                                      Map<String, StructurePriorityMeta> structurePriorityMap,
                                      Map<SkuScheduleDTO, Integer> structureEndingDaysMap,
                                      Comparator<SkuScheduleDTO> tailComparator,
                                      SkuScheduleDTO left,
                                      SkuScheduleDTO right) {
        int compareResult = Integer.compare(resolveSpecifyMachineScore(context, left),
                resolveSpecifyMachineScore(context, right));
        if (compareResult != 0) {
            return compareResult;
        }
        compareResult = Integer.compare(resolveDeliveryLockedScore(left), resolveDeliveryLockedScore(right));
        if (compareResult != 0) {
            return compareResult;
        }
        compareResult = compareDelayDays(left, right);
        if (compareResult != 0) {
            return compareResult;
        }
        compareResult = compareStructurePriority(structurePriorityMap, structureEndingDaysMap, left, right);
        if (compareResult != 0) {
            return compareResult;
        }
        return tailComparator.compare(left, right);
    }

    /**
     * 试制/量试组内只按延误、排产量、物料编码排序。
     *
     * <p>该分支用于避免试制/量试 SKU 在同组内继续受正规供应链字段影响。</p>
     */
    private int compareTrialOrMassTrialGroup(SkuScheduleDTO left, SkuScheduleDTO right) {
        int compareResult = compareDelayDays(left, right);
        if (compareResult != 0) {
            return compareResult;
        }
        compareResult = Integer.compare(resolveNewSpecSortTargetQty(right), resolveNewSpecSortTargetQty(left));
        if (compareResult != 0) {
            return compareResult;
        }
        return Comparator.nullsLast(String::compareTo).compare(
                left == null ? null : left.getMaterialCode(),
                right == null ? null : right.getMaterialCode());
    }

    private int resolveNewSpecSortTargetQty(SkuScheduleDTO sku) {
        return sku == null ? 0 : sku.resolveTargetScheduleQty();
    }

    private int resolveSpecifyMachineScore(LhScheduleContext context, SkuScheduleDTO sku) {
        return LhSpecifyMachineUtil.hasLimitSpecifyMachine(context, sku.getMaterialCode()) ? 0 : 1;
    }

    private int resolveDeliveryLockedScore(SkuScheduleDTO sku) {
        return sku != null && sku.isDeliveryLocked() ? 0 : 1;
    }

    private int compareDelayDays(SkuScheduleDTO left, SkuScheduleDTO right) {
        return Comparator.nullsLast(Comparator.<Integer>naturalOrder()).compare(
                left == null ? null : left.getDelayDays(),
                right == null ? null : right.getDelayDays());
    }

    private boolean hasSameDelayDays(SkuScheduleDTO left, SkuScheduleDTO right) {
        return left != null
                && right != null
                && left.getDelayDays() != null
                && Objects.equals(left.getDelayDays(), right.getDelayDays());
    }

    private boolean shouldApplyDelayDaysSkuTypeTieBreaker(SkuScheduleDTO left, SkuScheduleDTO right) {
        return hasSameDelayDays(left, right)
                && !Objects.equals(Integer.valueOf(0), left.getDelayDays());
    }

    private int compareStructurePriority(Map<String, StructurePriorityMeta> structurePriorityMap,
                                         Map<SkuScheduleDTO, Integer> structureEndingDaysMap,
                                         SkuScheduleDTO left,
                                         SkuScheduleDTO right) {
        int compareResult = Integer.compare(
                isStructureAllEndingPriority(structurePriorityMap, left) ? 0 : 1,
                isStructureAllEndingPriority(structurePriorityMap, right) ? 0 : 1);
        if (compareResult != 0) {
            return compareResult;
        }
        compareResult = Integer.compare(
                isStructureAllEndingPriority(structurePriorityMap, left)
                        && hasKnownStructureEndingDays(structureEndingDaysMap, left) ? 0 : 1,
                isStructureAllEndingPriority(structurePriorityMap, right)
                        && hasKnownStructureEndingDays(structureEndingDaysMap, right) ? 0 : 1);
        if (compareResult != 0) {
            return compareResult;
        }
        int leftEndingDays = isStructureAllEndingPriority(structurePriorityMap, left)
                && hasKnownStructureEndingDays(structureEndingDaysMap, left)
                ? -resolveStructureEndingDays(structureEndingDaysMap, left) : 0;
        int rightEndingDays = isStructureAllEndingPriority(structurePriorityMap, right)
                && hasKnownStructureEndingDays(structureEndingDaysMap, right)
                ? -resolveStructureEndingDays(structureEndingDaysMap, right) : 0;
        return Integer.compare(leftEndingDays, rightEndingDays);
    }

    private boolean shouldApplyStructureSkuTypeTieBreaker(Map<String, StructurePriorityMeta> structurePriorityMap,
                                                          Map<SkuScheduleDTO, Integer> structureEndingDaysMap,
                                                          SkuScheduleDTO left,
                                                          SkuScheduleDTO right) {
        return isStructureAllEndingPriority(structurePriorityMap, left)
                && isStructureAllEndingPriority(structurePriorityMap, right)
                && hasKnownStructureEndingDays(structureEndingDaysMap, left)
                && hasKnownStructureEndingDays(structureEndingDaysMap, right)
                && resolveStructureEndingDays(structureEndingDaysMap, left)
                == resolveStructureEndingDays(structureEndingDaysMap, right);
    }

    private int compareNewSpecSkuTypeWithinLevel(SkuScheduleDTO left,
                                                 SkuScheduleDTO right,
                                                 boolean shouldCompare) {
        if (!shouldCompare) {
            return 0;
        }
        return Integer.compare(resolveNewSpecSkuTypeScore(left), resolveNewSpecSkuTypeScore(right));
    }

    /**
     * 按结构转产表最大END_DAY构建结构N天内收尾优先级快照。
     *
     * <p>判断口径固定为：END_DAY不早于上下文T日，且包含首尾的距离天数
     * {@code END_DAY - T + 1}严格小于参数{@code SYS0304002}。基础数据阶段已经按结构取最大
     * 完整自然日并缓存，本方法只按结构计算一次；命中后将当前structureSkuMap中同结构全部SKU
     * 写入structureEndingDaysMap，使原比较器、排序层级和后续排产逻辑保持不变。</p>
     *
     * @param context                排程上下文，scheduleDate为本次排程T日
     * @param structureEndingDaysMap SKU对象对应的结构收尾距离天数快照
     * @return 结构名称对应的排序优先级元数据
     */
    private Map<String, StructurePriorityMeta> buildStructurePriorityMap(LhScheduleContext context,
                                                                         Map<SkuScheduleDTO, Integer> structureEndingDaysMap) {
        Map<String, StructurePriorityMeta> structurePriorityMap = new LinkedHashMap<>(16);
        if (CollectionUtils.isEmpty(context.getStructureSkuMap())) {
            return structurePriorityMap;
        }
        int structureEndingDays = context.getScheduleConfig() != null
                ? context.getScheduleConfig().getStructureEndingDays()
                : context.getParamIntValue(LhScheduleParamConstant.STRUCTURE_ENDING_DAYS,
                LhScheduleConstant.DEFAULT_STRUCTURE_ENDING_DAYS);
        LocalDate scheduleDate = this.toLocalDate(context.getScheduleDate());
        Map<String, LocalDate> structureMaxEndingDateMap =
                context.getStructurePriorityMaxEndingDateMap();
        int hitStructureCount = 0;
        int markedSkuCount = 0;
        for (Map.Entry<String, List<SkuScheduleDTO>> entry : context.getStructureSkuMap().entrySet()) {
            if (StringUtils.isEmpty(entry.getKey()) || CollectionUtils.isEmpty(entry.getValue())) {
                continue;
            }
            List<SkuScheduleDTO> structureSkuList = entry.getValue().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            int totalSkuCount = structureSkuList.size();
            LocalDate maxEndingDate = CollectionUtils.isEmpty(structureMaxEndingDateMap)
                    ? null : structureMaxEndingDateMap.get(entry.getKey());
            int distanceDays = this.calculateInclusiveDistanceDays(scheduleDate, maxEndingDate);
            boolean structureEndingPriority = totalSkuCount > 0
                    && distanceDays > 0
                    && distanceDays < structureEndingDays;
            if (structureEndingPriority) {
                /*
                 * 结构命中后统一标记本次仍参与排产的全部SKU，不再逐SKU判断预计收尾、余量或产能。
                 * 同一SKU在续作/新增主列表与structureSkuMap中可能是不同运行对象，因此三个入口都要
                 * 写入IdentityHashMap，保证每个实际参与排序的对象都获得相同结构距离天数。
                 */
                int markedSkuCountBefore = structureEndingDaysMap.size();
                this.markStructureEndingSkuList(
                        context.getContinuousSkuList(), entry.getKey(), distanceDays,
                        structureEndingDaysMap);
                this.markStructureEndingSkuList(
                        context.getNewSpecSkuList(), entry.getKey(), distanceDays,
                        structureEndingDaysMap);
                this.markStructureEndingSkuList(
                        structureSkuList, entry.getKey(), distanceDays, structureEndingDaysMap);
                hitStructureCount++;
                markedSkuCount += structureEndingDaysMap.size() - markedSkuCountBefore;
            }
            StructurePriorityMeta meta = new StructurePriorityMeta();
            meta.setTotalSkuCount(totalSkuCount);
            meta.setEndingSkuCount(structureEndingPriority ? totalSkuCount : 0);
            meta.setAllSkusEnding(structureEndingPriority);
            meta.setLatestEndingDays(distanceDays);
            meta.setAllSkusEndingPriority(structureEndingPriority);
            structurePriorityMap.put(entry.getKey(), meta);
            log.debug("结构N天内收尾排序判断, batchNo: {}, structureName: {}, scheduleDate: {}, "
                            + "maxEndingDate: {}, inclusiveDistanceDays: {}, thresholdDays: {}, "
                            + "candidateSkuCount: {}, hit: {}",
                    context.getBatchNo(), entry.getKey(), scheduleDate, maxEndingDate, distanceDays,
                    structureEndingDays, totalSkuCount, structureEndingPriority);
        }
        log.info("结构N天内收尾排序标记完成, batchNo: {}, scheduleDate: {}, thresholdDays: {}, "
                        + "structureCount: {}, hitStructureCount: {}, markedSkuCount: {}",
                context.getBatchNo(), scheduleDate, structureEndingDays, structurePriorityMap.size(),
                hitStructureCount, markedSkuCount);
        return structurePriorityMap;
    }

    /**
     * 将指定结构下实际参与本轮排序的SKU运行对象写入结构收尾距离缓存。
     *
     * <p>同物料可能因续作转新增、产品状态或阶段拆分形成多个DTO对象，不能只按物料编码去重，
     * 也不能只缓存structureSkuMap中的对象。本方法逐列表按结构名称筛选，并依赖调用方传入的
     * IdentityHashMap按对象身份保存，确保所有实际排序对象都获得同一结构判断结果。</p>
     *
     * @param skuList 当前参与排序的SKU列表
     * @param structureName 已命中的结构名称
     * @param distanceDays 结构最大END_DAY与T日之间包含首尾的距离天数
     * @param structureEndingDaysMap SKU运行对象对应的结构收尾距离缓存
     */
    private void markStructureEndingSkuList(
            List<SkuScheduleDTO> skuList,
            String structureName,
            int distanceDays,
            Map<SkuScheduleDTO, Integer> structureEndingDaysMap) {
        if (CollectionUtils.isEmpty(skuList)) {
            return;
        }
        skuList.stream()
                .filter(Objects::nonNull)
                .filter(sku -> Objects.equals(structureName, sku.getStructureName()))
                .forEach(sku -> structureEndingDaysMap.put(sku, distanceDays));
    }

    /**
     * 计算两个完整自然日之间包含首尾的距离天数。
     *
     * <p>使用LocalDate和ChronoUnit计算，能够正确处理跨月、跨年和不同月份天数；结束日早于T日、
     * 任一日期为空时返回-1，调用方据此判定结构不命中。</p>
     *
     * @param scheduleDate 排程上下文T日
     * @param maxEndingDate 结构转产表中该结构的最大END_DAY完整自然日
     * @return 包含首尾的距离天数；无法判断或结束日早于T日时返回-1
     */
    private int calculateInclusiveDistanceDays(LocalDate scheduleDate, LocalDate maxEndingDate) {
        if (Objects.isNull(scheduleDate)
                || Objects.isNull(maxEndingDate)
                || maxEndingDate.isBefore(scheduleDate)) {
            return -1;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(scheduleDate, maxEndingDate) + 1L);
    }

    /**
     * 将带时间部分的排程T日转换为系统时区自然日，避免时分秒影响结构收尾距离。
     *
     * @param date 排程上下文T日
     * @return T日自然日；日期为空时返回null
     */
    private LocalDate toLocalDate(Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 判断SKU所属结构是否进入原“结构全收尾”排序层级。
     * <p>方法名和排序层级保持兼容，实际标记口径已调整为结构转产表最大END_DAY距T日严格小于
     * {@code SYS0304002}，不再逐SKU判断预计收尾。</p>
     */
    private boolean isStructureAllEndingPriority(Map<String, StructurePriorityMeta> structurePriorityMap, SkuScheduleDTO sku) {
        if (sku == null || StringUtils.isEmpty(sku.getStructureName())) {
            return false;
        }
        StructurePriorityMeta meta = structurePriorityMap.get(sku.getStructureName());
        return meta != null && meta.isAllSkusEndingPriority();
    }

    /**
     * 判断SKU是否具备可比较的收尾天数。
     */
    private boolean hasKnownStructureEndingDays(Map<SkuScheduleDTO, Integer> structureEndingDaysMap, SkuScheduleDTO sku) {
        return resolveStructureEndingDays(structureEndingDaysMap, sku) >= 0;
    }

    private int resolveStructureEndingDays(Map<SkuScheduleDTO, Integer> structureEndingDaysMap, SkuScheduleDTO sku) {
        if (sku == null || structureEndingDaysMap == null) {
            return -1;
        }
        Integer endingDays = structureEndingDaysMap.get(sku);
        return endingDays == null ? -1 : endingDays;
    }

    /**
     * 解析开产模式 SKU 靠后分。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return 靠后分
     */
    private int resolveOpenProductionLateScore(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku) || !context.isOpenProductionMode()) {
            return 0;
        }
        int score = 0;
        if (isWinterTire(context, sku)) {
            score += LhScheduleConstant.OPEN_PRODUCTION_WINTER_TIRE_PENALTY;
        }
        if (isDifferentInch(context, sku)) {
            score += LhScheduleConstant.OPEN_PRODUCTION_DIFFERENT_INCH_PENALTY;
        }
        return score;
    }

    /**
     * 判断是否为雪地胎。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return true-雪地胎，false-非雪地胎
     */
    private boolean isWinterTire(LhScheduleContext context, SkuScheduleDTO sku) {
        String keywords = Objects.nonNull(context.getScheduleConfig())
                ? context.getScheduleConfig().getOpenProductionWinterTireKeywords()
                : context.getParamValue(LhScheduleParamConstant.OPEN_PRODUCTION_WINTER_TIRE_KEYWORDS,
                LhScheduleConstant.OPEN_PRODUCTION_WINTER_TIRE_KEYWORDS);
        if (StringUtils.isEmpty(keywords)) {
            return false;
        }
        String[] keywordArray = keywords.split(WINTER_TIRE_KEYWORD_SEPARATOR_REGEX);
        for (String keyword : keywordArray) {
            if (StringUtils.isEmpty(keyword)) {
                continue;
            }
            String trimmedKeyword = keyword.trim();
            if (StringUtils.containsIgnoreCase(sku.getMaterialDesc(), trimmedKeyword)
                    || StringUtils.containsIgnoreCase(sku.getSpecDesc(), trimmedKeyword)
                    || StringUtils.containsIgnoreCase(sku.getPattern(), trimmedKeyword)
                    || StringUtils.containsIgnoreCase(sku.getMainPattern(), trimmedKeyword)
                    || StringUtils.containsIgnoreCase(sku.getBrand(), trimmedKeyword)
                    || StringUtils.containsIgnoreCase(sku.getMainMaterialDesc(), trimmedKeyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 SKU 是否与当前在机或续作英寸不同。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return true-不同英寸，false-相同或无比较基准
     */
    private boolean isDifferentInch(LhScheduleContext context, SkuScheduleDTO sku) {
        if (StringUtils.isEmpty(sku.getProSize())) {
            return false;
        }
        boolean hasReference = false;
        if (!CollectionUtils.isEmpty(context.getMachineScheduleMap())) {
            for (MachineScheduleDTO machine : context.getMachineScheduleMap().values()) {
                if (Objects.isNull(machine) || StringUtils.isEmpty(machine.getPreviousProSize())) {
                    continue;
                }
                hasReference = true;
                if (StringUtils.equals(sku.getProSize(), machine.getPreviousProSize())) {
                    return false;
                }
            }
        }
        if (!CollectionUtils.isEmpty(context.getContinuousSkuList())) {
            for (SkuScheduleDTO continuousSku : context.getContinuousSkuList()) {
                if (Objects.isNull(continuousSku) || StringUtils.isEmpty(continuousSku.getProSize())) {
                    continue;
                }
                hasReference = true;
                if (StringUtils.equals(sku.getProSize(), continuousSku.getProSize())) {
                    return false;
                }
            }
        }
        return hasReference;
    }

    /**
     * 解析SKU类型描述，用于新增SKU补充排序日志。
     *
     * @param sku SKU
     * @return 类型描述
     */
    private String resolveNewSpecSkuTypeDesc(SkuScheduleDTO sku) {
        int typeScore = resolveNewSpecSkuTypeScore(sku);
        if (typeScore == 0) {
            return "试制";
        }
        if (typeScore == 1) {
            return "量试";
        }
        return sku != null && sku.isSmallBatchValidation() ? "小批量" : "正规";
    }

    /**
     * 解析新增SKU排序分组描述。
     *
     * @param sku SKU
     * @return 分组描述
     */
    private String resolveNewSpecGroupDesc(SkuScheduleDTO sku) {
        if (isTrialGroupSku(sku)) {
            return "试制组";
        }
        if (isMassTrialGroupSku(sku)) {
            return "量试组";
        }
        return "正规组";
    }

    private boolean isTrialGroupSku(SkuScheduleDTO sku) {
        return sku != null
                && StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage());
    }

    private boolean isMassTrialGroupSku(SkuScheduleDTO sku) {
        return sku != null
                && !isTrialGroupSku(sku)
                && StringUtils.equals(ConstructionStageEnum.MASS_TRIAL.getCode(), sku.getConstructionStage());
    }

    /**
     * 判断是否为特殊材料。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return true-特殊材料，false-非特殊材料
     */
    private boolean isSpecialMaterial(LhScheduleContext context, SkuScheduleDTO sku) {
        return StringUtils.equals(SPECIAL_MATERIAL_YES_FLAG, LhSpecialMaterialUtil.resolveHasSpecialMaterial(context, sku));
    }

    /**
     * 排序列表，为空时直接跳过。
     */
    private void sortSkuList(List<SkuScheduleDTO> skuList, Map<String,Date> embryoLhTimeMap,Comparator<SkuScheduleDTO> comparator) {
        if (CollectionUtils.isEmpty(skuList)) {
            return;
        }
        skuList.sort(comparator);
        //根据胎胚最早可供硫化时间重新排序 sandy+ 2026.7.7
        reorderByEarliestLhTime(skuList,embryoLhTimeMap);
    }

    /**
     * 汇总所有SKU并按统一优先级排序，用于回写顺序号。
     */
    private List<SkuScheduleDTO> buildOrderedSkuList(LhScheduleContext context, Comparator<SkuScheduleDTO> comparator) {
        List<SkuScheduleDTO> orderedSkus = new ArrayList<>(
                context.getContinuousSkuList().size() + context.getNewSpecSkuList().size());
        orderedSkus.addAll(context.getContinuousSkuList());
        orderedSkus.addAll(context.getNewSpecSkuList());
        orderedSkus.sort(comparator);
        return orderedSkus;
    }

    /**
     * 输出开产模式 SKU 靠后排序原因。
     *
     * @param context 排程上下文
     * @param orderedSkuList 排序后 SKU 列表
     * @return void
     */
    private void traceOpenProductionLateScore(LhScheduleContext context, List<SkuScheduleDTO> orderedSkuList) {
        if (Objects.isNull(context) || !context.isOpenProductionMode()
                || context.isPriorityTraceMuted() || CollectionUtils.isEmpty(orderedSkuList)) {
            return;
        }
        StringBuilder detailBuilder = new StringBuilder(256);
        for (SkuScheduleDTO sku : orderedSkuList) {
            int score = resolveOpenProductionLateScore(context, sku);
            if (score <= 0) {
                continue;
            }
            detailBuilder.append("materialCode=").append(sku.getMaterialCode())
                    .append(", score=").append(score)
                    .append(", winterTire=").append(isWinterTire(context, sku))
                    .append(", differentInch=").append(isDifferentInch(context, sku))
                    .append('\n');
        }
        if (detailBuilder.length() > 0) {
            log.info("开产SKU靠后排序原因\n{}", detailBuilder.toString().trim());
        }
    }

    /**
     * 输出排序后的SKU优先级跟踪日志（含汇总标题、TOP N、SortKey、HitLevel）。
     *
     * @param context 排程上下文
     * @param structurePriorityMap 结构收尾优先级快照
     */
    private void traceSortedSkuList(LhScheduleContext context,
                                    Map<String, StructurePriorityMeta> structurePriorityMap,
                                    Map<SkuScheduleDTO, Integer> structureEndingDaysMap) {
        if (!PriorityTraceLogHelper.isEnabled(context)) {
            return;
        }
        String currentStep = context.getCurrentStep();
        String title;
        List<SkuScheduleDTO> traceSkuList;
        boolean isNewSpec;
        if (StringUtils.equals(ScheduleStepEnum.S4_4_CONTINUOUS_PRODUCTION.getCode(), currentStep)) {
            title = "SKU排序优先级汇总【续作】";
            traceSkuList = context.getContinuousSkuList();
            isNewSpec = false;
        } else if (StringUtils.equals(ScheduleStepEnum.S4_5_NEW_PRODUCTION.getCode(), currentStep)) {
            title = "SKU排序优先级汇总【新增】";
            traceSkuList = context.getNewSpecSkuList();
            isNewSpec = true;
        } else {
            return;
        }

        int topN = LhScheduleConstant.SKU_SORT_TRACE_TOP_N;
        int skuCount = PriorityTraceLogHelper.sizeOf(traceSkuList);
        int outputCount = Math.min(topN, skuCount);

        StringBuilder detailBuilder = new StringBuilder(1024);
        PriorityTraceLogHelper.appendTitleHeader(detailBuilder, title);
        PriorityTraceLogHelper.appendLine(detailBuilder,
                PriorityTraceLogHelper.kv("排程日期", PriorityTraceLogHelper.formatDateTime(context.getScheduleDate()))
                        + ", " + PriorityTraceLogHelper.kv("步骤", currentStep)
                        + ", " + PriorityTraceLogHelper.kv("排序场景", isNewSpec ? "新增SKU排序" : "续作SKU排序")
                        + ", " + PriorityTraceLogHelper.kv("SKU数量", skuCount)
                        + ", " + PriorityTraceLogHelper.kv("输出范围", "TOP" + outputCount));

        if (CollectionUtils.isEmpty(traceSkuList)) {
            PriorityTraceLogHelper.appendLine(detailBuilder, "无可输出的SKU排序结果");
        } else {
            for (int i = 0; i < outputCount; i++) {
                SkuScheduleDTO sku = traceSkuList.get(i);
                // 复用 buildSkuSortDesc 生成单行描述，保证日志、运行态、落库三处口径完全一致。
                String desc = buildSkuSortDesc(context, sku, i + 1, isNewSpec,
                        structurePriorityMap, structureEndingDaysMap);
                PriorityTraceLogHelper.appendLine(detailBuilder, desc);
            }
            if (skuCount > topN) {
                PriorityTraceLogHelper.appendLine(detailBuilder,
                        "... 共" + skuCount + "条，仅展示前" + topN + "条");
            }
            if (isNewSpec) {
                appendTargetSkuSortTrace(detailBuilder, traceSkuList, "3302002637");
                appendNewSpecSupplyChainTieBreakTrace(detailBuilder, traceSkuList);
                appendNewSpecSkuTypeTieBreakTrace(context, detailBuilder, traceSkuList,
                        structurePriorityMap, structureEndingDaysMap);
            }
        }
        PriorityTraceLogHelper.appendTitleFooter(detailBuilder);
        String detail = detailBuilder.toString().trim();
        PriorityTraceLogHelper.logSortSummary(log, context, title, detail);
    }

    /**
     * 输出全量SKU排序优先级跟踪日志（续作+新增统一排序，含换活字块候选）。
     *
     * <p>仅在 S4.4 续作排序、排产消费前调用，此时 orderedSkuList 为续作(01)+新增(02)全量未消费候选，
     * 已按统一比较器排序并回写 scheduleOrder。按月计划起产日(beginDay)阈值筛选：仅输出
     * beginDay 非空且小于等于阈值的SKU，命中筛选的全部输出（不做 TOP N 截断）。
     * 单行内容复用 {@link #buildSkuSortDesc}，并额外追加结构名称、起产日、结束日，便于聚焦排查。</p>
     *
     * @param context 排程上下文
     * @param orderedSkuList 续作+新增统一排序后的全量候选列表
     * @param structurePriorityMap 结构收尾优先级快照
     * @param structureEndingDaysMap 结构收尾天数快照
     */
    private void traceFullSortedSkuList(LhScheduleContext context,
                                        List<SkuScheduleDTO> orderedSkuList,
                                        Map<String, StructurePriorityMeta> structurePriorityMap,
                                        Map<SkuScheduleDTO, Integer> structureEndingDaysMap) {
        if (!PriorityTraceLogHelper.isEnabled(context)) {
            return;
        }
        String title = "SKU排序优先级汇总【全量】";
        int beginDayThreshold = context.getScheduleConfig().getFullSkuSortLogBeginDayThreshold();
        int totalCount = PriorityTraceLogHelper.sizeOf(orderedSkuList);

        // 先筛选出月计划起产日(beginDay)小于等于阈值的SKU，beginDay为空的不输出。
        List<SkuScheduleDTO> hitSkuList = new ArrayList<>(totalCount);
        if (!CollectionUtils.isEmpty(orderedSkuList)) {
            for (SkuScheduleDTO sku : orderedSkuList) {
                if (Objects.isNull(sku) || Objects.isNull(sku.getBeginDay())) {
                    continue;
                }
                if (sku.getBeginDay() <= beginDayThreshold) {
                    hitSkuList.add(sku);
                }
            }
        }

        StringBuilder detailBuilder = new StringBuilder(1024);
        PriorityTraceLogHelper.appendTitleHeader(detailBuilder, title);
        PriorityTraceLogHelper.appendLine(detailBuilder,
                PriorityTraceLogHelper.kv("排程日期", PriorityTraceLogHelper.formatDateTime(context.getScheduleDate()))
                        + ", " + PriorityTraceLogHelper.kv("步骤", context.getCurrentStep())
                        + ", " + PriorityTraceLogHelper.kv("排序场景", "全量统一排序")
                        + ", " + PriorityTraceLogHelper.kv("候选总数", totalCount)
                        + ", " + PriorityTraceLogHelper.kv("命中筛选数", hitSkuList.size())
                        + ", " + PriorityTraceLogHelper.kv("起产日阈值", beginDayThreshold));

        if (CollectionUtils.isEmpty(hitSkuList)) {
            PriorityTraceLogHelper.appendLine(detailBuilder, "无可输出的SKU排序结果");
        } else {
            for (SkuScheduleDTO sku : hitSkuList) {
                // 按SKU类型自适应排序维度：02新增走新增SortKey，01续作走续作SortKey。
                boolean isNewSpec = StringUtils.equals(
                        ScheduleTypeEnum.NEW_SPEC.getCode(), sku.getScheduleType());
                String desc = buildSkuSortDesc(context, sku, sku.getScheduleOrder(), isNewSpec,
                        structurePriorityMap, structureEndingDaysMap)
                        + ", " + PriorityTraceLogHelper.kv("结构", sku.getStructureName())
                        + ", " + PriorityTraceLogHelper.kv("起产日", sku.getBeginDay())
                        + ", " + PriorityTraceLogHelper.kv("结束日", sku.getEndDay());
                PriorityTraceLogHelper.appendLine(detailBuilder, desc);
            }
        }
        PriorityTraceLogHelper.appendTitleFooter(detailBuilder);
        String detail = detailBuilder.toString().trim();
        PriorityTraceLogHelper.logSortSummary(log, context, title, detail);
    }

    /**
     * 按续作/新增列表回写 SKU 排序名次（rank=1~N）和单行描述（sortDesc）。
     *
     * <p>名次与”SKU排序优先级汇总【新增】/【续作】”日志中 rank 字段同源；
     * 描述与日志单行内容完全一致，便于排程结果落库后按 SKU 还原排序原因。
     * 调用方通过 {@code currentStep} 保证每个列表仅在所属步骤回写一次。</p>
     *
     * @param context 排程上下文
     * @param skuList 排序后的 SKU 列表
     * @param isNewSpec 是否为新增 SKU 列表
     * @param structurePriorityMap 结构收尾优先级快照
     * @param structureEndingDaysMap 结构收尾天数快照
     */
    private void writeSkuSortRankAndDesc(LhScheduleContext context,
                                         List<SkuScheduleDTO> skuList,
                                         boolean isNewSpec,
                                         Map<String, StructurePriorityMeta> structurePriorityMap,
                                         Map<SkuScheduleDTO, Integer> structureEndingDaysMap) {
        if (CollectionUtils.isEmpty(skuList)) {
            return;
        }
        for (int i = 0; i < skuList.size(); i++) {
            SkuScheduleDTO sku = skuList.get(i);
            if (Objects.isNull(sku)) {
                continue;
            }
            int rank = i + 1;
            sku.setSortRank(rank);
            sku.setSortDesc(buildSkuSortDesc(context, sku, rank, isNewSpec,
                    structurePriorityMap, structureEndingDaysMap));
        }
    }

    /**
     * 生成 SKU 排序单行描述。
     *
     * <p>与原 traceSortedSkuList 拼装的“[新增排产SKU排序] rank=… 描述=… SortKey=… HitLevel=…”单行内容完全一致，
     * 续作场景使用“N. 物料编码=…”形式（与原日志一致）。新增 SKU 走两种 SortKey 维度：试制/量试组与普通组。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param rank 排序名次（1~N）
     * @param isNewSpec 是否为新增 SKU 列表
     * @param structurePriorityMap 结构收尾优先级快照
     * @param structureEndingDaysMap 结构收尾天数快照
     * @return 单行描述
     */
    private String buildSkuSortDesc(LhScheduleContext context,
                                    SkuScheduleDTO sku,
                                    int rank,
                                    boolean isNewSpec,
                                    Map<String, StructurePriorityMeta> structurePriorityMap,
                                    Map<SkuScheduleDTO, Integer> structureEndingDaysMap) {
        if (Objects.isNull(sku)) {
            return StringUtils.EMPTY;
        }
        boolean structureAllEndingPriority = isStructureAllEndingPriority(structurePriorityMap, sku);
        boolean ending = endingJudgmentStrategy.isExpectedEnding(context, sku);
        boolean isSpecifyMachine = LhSpecifyMachineUtil.hasLimitSpecifyMachine(context, sku.getMaterialCode());
        boolean isSpecial = isSpecialMaterial(context, sku);
        String constructionStageDesc = resolveConstructionStageDesc(sku);
        int structureEndingDays = resolveStructureEndingDays(structureEndingDaysMap, sku);
        String skuTypeDesc = resolveNewSpecSkuTypeDesc(sku);
        String groupDesc = resolveNewSpecGroupDesc(sku);
        int groupScore = resolveNewSpecGroupScore(sku);
        int targetScheduleQty = resolveNewSpecSortTargetQty(sku);

        // 提取延误天数，避免三元表达式中Integer/int类型推断问题
        Integer delayDays = sku.getDelayDays();
        boolean delayKnown = delayDays != null;
        List<String> levelNames;
        List<String> sortKeyLevels;
        List<Integer> scores;
        List<Integer> defaultScores;
        if (isNewSpec) {
            if (isTrialGroupSku(sku) || isMassTrialGroupSku(sku)) {
                levelNames = Arrays.asList(
                        "L1_分组优先级", "L2_延误天数", "L3_排产量", "L4_物料编码");
                sortKeyLevels = Arrays.asList(
                        "L1_分组优先级=" + groupDesc,
                        "L2_延误天数=" + (delayKnown ? delayDays : 0),
                        "L3_排产量=" + targetScheduleQty,
                        "L4_物料编码=" + PriorityTraceLogHelper.safeText(sku.getMaterialCode()));
                scores = Arrays.asList(
                        groupScore,
                        delayKnown ? delayDays : 0,
                        -targetScheduleQty,
                        0);
                defaultScores = Arrays.asList(2, 0, 0, 0);
            } else {
                levelNames = Arrays.asList(
                        "L1_分组优先级", "L2_定点机台", "L3_锁交期", "L4_延误天数",
                        "L5_结构全收尾", "L6_最晚收尾日", "L7_高优待排", "L8_周期待排",
                        "L9_中优待排", "L10_常规待排", "L11_开产靠后分",
                        "L12_排产量", "L13_物料编码");
                sortKeyLevels = Arrays.asList(
                        "L1_分组优先级=" + groupDesc,
                        "L2_定点机台=" + (isSpecifyMachine ? 1 : 0),
                        "L3_锁交期=" + (sku.isDeliveryLocked() ? 1 : 0),
                        "L4_延误天数=" + (delayKnown ? delayDays : 0),
                        "L5_结构全收尾=" + (structureAllEndingPriority ? 1 : 0),
                        "L6_最晚收尾日=" + (structureAllEndingPriority && structureEndingDays >= 0 ? structureEndingDays : 0),
                        "L7_高优待排=" + sku.getHighPriorityPendingQty(),
                        "L8_周期待排=" + sku.getCycleProductionPendingQty(),
                        "L9_中优待排=" + sku.getMidPriorityPendingQty(),
                        "L10_常规待排=" + sku.getConventionProductionPendingQty(),
                        "L11_开产靠后分=" + resolveOpenProductionLateScore(context, sku),
                        "L12_排产量=" + targetScheduleQty,
                        "L13_物料编码=" + PriorityTraceLogHelper.safeText(sku.getMaterialCode()));
                scores = Arrays.asList(
                        groupScore,
                        isSpecifyMachine ? 0 : 1,
                        sku.isDeliveryLocked() ? 0 : 1,
                        delayKnown ? delayDays : 0,
                        structureAllEndingPriority ? 0 : 1,
                        structureAllEndingPriority && structureEndingDays >= 0 ? -structureEndingDays : 0,
                        -sku.getHighPriorityPendingQty(),
                        -sku.getCycleProductionPendingQty(),
                        -sku.getMidPriorityPendingQty(),
                        -sku.getConventionProductionPendingQty(),
                        resolveOpenProductionLateScore(context, sku),
                        -targetScheduleQty,
                        0);
                defaultScores = Arrays.asList(2, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0);
            }
        } else {
            levelNames = Arrays.asList(
                    "L1_锁交期", "L2_延误天数", "L3_结构全收尾", "L4_最晚收尾日",
                    "L5_高优待排", "L6_周期待排", "L7_中优待排", "L8_常规待排",
                    "L9_开产靠后分");
            sortKeyLevels = Arrays.asList(
                    "L1_锁交期=" + (sku.isDeliveryLocked() ? 1 : 0),
                    "L2_延误天数=" + (delayKnown ? delayDays : 0),
                    "L3_结构全收尾=" + (structureAllEndingPriority ? 1 : 0),
                    "L4_最晚收尾日=" + (structureAllEndingPriority && structureEndingDays >= 0 ? structureEndingDays : 0),
                    "L5_高优待排=" + sku.getHighPriorityPendingQty(),
                    "L6_周期待排=" + sku.getCycleProductionPendingQty(),
                    "L7_中优待排=" + sku.getMidPriorityPendingQty(),
                    "L8_常规待排=" + sku.getConventionProductionPendingQty(),
                    "L9_开产靠后分=" + resolveOpenProductionLateScore(context, sku));
            scores = Arrays.asList(
                    sku.isDeliveryLocked() ? 0 : 1,
                    delayKnown ? 0 : 1,
                    structureAllEndingPriority ? 0 : 1,
                    structureAllEndingPriority && structureEndingDays >= 0 ? -structureEndingDays : 0,
                    -sku.getHighPriorityPendingQty(),
                    -sku.getCycleProductionPendingQty(),
                    -sku.getMidPriorityPendingQty(),
                    -sku.getConventionProductionPendingQty(),
                    resolveOpenProductionLateScore(context, sku));
            defaultScores = Arrays.asList(1, 1, 1, 0, 0, 0, 0, 0, 0);
        }
        String sortKey = PriorityTraceLogHelper.formatSortKey(sortKeyLevels);
        String hitLevel = PriorityTraceLogHelper.resolveHitLevel(levelNames, scores, defaultScores);

        String tracePrefix = isNewSpec
                ? "[新增排产SKU排序] rank=" + rank
                + ", sku=" + sku.getMaterialCode()
                + ", " + PriorityTraceLogHelper.kv("交付锁定", PriorityTraceLogHelper.oneZero(sku.isDeliveryLocked()))
                + ", " + PriorityTraceLogHelper.kv("延期天数", delayKnown ? delayDays : 0)
                + ", " + PriorityTraceLogHelper.kv("结构五天收尾", PriorityTraceLogHelper.oneZero(structureAllEndingPriority))
                + ", " + PriorityTraceLogHelper.kv("SKU类型", skuTypeDesc)
                + ", " + PriorityTraceLogHelper.kv("高优先级数量", sku.getHighPriorityPendingQty())
                + ", " + PriorityTraceLogHelper.kv("周期排产数量", sku.getCycleProductionPendingQty())
                + ", " + PriorityTraceLogHelper.kv("中优先级数量", sku.getMidPriorityPendingQty())
                + ", " + PriorityTraceLogHelper.kv("常规数量", sku.getConventionProductionPendingQty())
                + ", " + PriorityTraceLogHelper.kv("最终排序原因", hitLevel)
                : rank + ". " + PriorityTraceLogHelper.kv("物料编码", sku.getMaterialCode());

        return tracePrefix
                + ", " + PriorityTraceLogHelper.kv("描述", sku.getMaterialDesc())
                + ", " + PriorityTraceLogHelper.kv("排产类型", sku.getScheduleType())
                + ", " + PriorityTraceLogHelper.kv("分组优先级", groupDesc)
                + ", " + PriorityTraceLogHelper.kv("SKU类型", skuTypeDesc)
                + ", " + PriorityTraceLogHelper.kv("SKU类型优先级", groupScore)
                + ", " + PriorityTraceLogHelper.kv("最终排序名次", sku.getScheduleOrder())
                + ", " + PriorityTraceLogHelper.kv("续作", oneZeroFromScheduleType(sku.getScheduleType()))
                + ", " + PriorityTraceLogHelper.kv("收尾", PriorityTraceLogHelper.oneZero(ending))
                + ", " + PriorityTraceLogHelper.kv("阶段", constructionStageDesc)
                + ", " + PriorityTraceLogHelper.kv("试制量试", PriorityTraceLogHelper.oneZero(isTrialOrMassTrialSku(sku)))
                + ", " + PriorityTraceLogHelper.kv("特殊材料", PriorityTraceLogHelper.oneZero(isSpecial))
                + ", " + PriorityTraceLogHelper.kv("排产量", targetScheduleQty)
                + ", " + PriorityTraceLogHelper.kv("定点机台", PriorityTraceLogHelper.oneZero(isSpecifyMachine))
                + ", " + PriorityTraceLogHelper.kv("月计划量", sku.getMonthPlanQty())
                + ", " + PriorityTraceLogHelper.kv("余量", sku.getSurplusQty())
                + ", " + PriorityTraceLogHelper.kv("胎胚库存", sku.getEmbryoStock())
                + ", " + PriorityTraceLogHelper.kv("班产", sku.getShiftCapacity())
                + ", " + PriorityTraceLogHelper.kv("规格", sku.getSpecCode())
                + ", " + PriorityTraceLogHelper.kv("花纹", sku.getMainPattern())
                + ", " + PriorityTraceLogHelper.kv("胎胚描述", sku.getMainMaterialDesc())
                + ", " + PriorityTraceLogHelper.kv("SortKey", sortKey)
                + ", " + PriorityTraceLogHelper.kv("HitLevel", hitLevel);
    }

    /**
     * 输出重点SKU的全局排序位置和前序SKU列表。
     *
     * @param detailBuilder 日志明细
     * @param traceSkuList 排序后SKU列表
     * @param targetMaterialCode 目标物料编码
     */
    private void appendTargetSkuSortTrace(StringBuilder detailBuilder,
                                          List<SkuScheduleDTO> traceSkuList,
                                          String targetMaterialCode) {
        if (detailBuilder == null || CollectionUtils.isEmpty(traceSkuList)
                || StringUtils.isEmpty(targetMaterialCode)) {
            return;
        }
        for (int i = 0; i < traceSkuList.size(); i++) {
            SkuScheduleDTO sku = traceSkuList.get(i);
            if (sku == null || !StringUtils.equals(targetMaterialCode, sku.getMaterialCode())) {
                continue;
            }
            List<String> previousSkuCodes = new ArrayList<>(i);
            for (int j = 0; j < i; j++) {
                SkuScheduleDTO previousSku = traceSkuList.get(j);
                if (previousSku != null && StringUtils.isNotEmpty(previousSku.getMaterialCode())) {
                    previousSkuCodes.add(previousSku.getMaterialCode());
                }
            }
            PriorityTraceLogHelper.appendLine(detailBuilder,
                    "重点SKU排序追踪: " + targetMaterialCode
                            + ", 全局排序名次=" + (i + 1)
                            + ", 前序SKU=" + (CollectionUtils.isEmpty(previousSkuCodes)
                            ? "-" : String.join(",", previousSkuCodes)));
            return;
        }
    }

    private void appendNewSpecSupplyChainTieBreakTrace(StringBuilder detailBuilder,
                                                       List<SkuScheduleDTO> traceSkuList) {
        if (detailBuilder == null || CollectionUtils.isEmpty(traceSkuList)) {
            return;
        }
        int explainedCount = 0;
        for (int i = 0; i < traceSkuList.size() - 1; i++) {
            SkuScheduleDTO currentSku = traceSkuList.get(i);
            SkuScheduleDTO nextSku = traceSkuList.get(i + 1);
            String levelDesc = resolveSupplyChainTieBreakLevelDesc(currentSku, nextSku);
            if (StringUtils.isEmpty(levelDesc)) {
                continue;
            }
            PriorityTraceLogHelper.appendLine(detailBuilder,
                    "供应链数量比较命中: "
                            + currentSku.getMaterialCode() + " > " + nextSku.getMaterialCode()
                            + ", 层级=" + levelDesc
                            + ", 当前SKU[高优先级数量=" + currentSku.getHighPriorityPendingQty()
                            + ",周期排产数量=" + currentSku.getCycleProductionPendingQty()
                            + ",中优先级数量=" + currentSku.getMidPriorityPendingQty()
                            + ",常规数量=" + currentSku.getConventionProductionPendingQty()
                            + "], 后续SKU[高优先级数量=" + nextSku.getHighPriorityPendingQty()
                            + ",周期排产数量=" + nextSku.getCycleProductionPendingQty()
                            + ",中优先级数量=" + nextSku.getMidPriorityPendingQty()
                            + ",常规数量=" + nextSku.getConventionProductionPendingQty() + "]");
            explainedCount++;
            if (explainedCount >= 10) {
                break;
            }
        }
    }

    private void appendNewSpecSkuTypeTieBreakTrace(LhScheduleContext context,
                                                   StringBuilder detailBuilder,
                                                   List<SkuScheduleDTO> traceSkuList,
                                                   Map<String, StructurePriorityMeta> structurePriorityMap,
                                                   Map<SkuScheduleDTO, Integer> structureEndingDaysMap) {
        if (detailBuilder == null || CollectionUtils.isEmpty(traceSkuList)) {
            return;
        }
        int explainedCount = 0;
        for (int i = 0; i < traceSkuList.size() - 1; i++) {
            SkuScheduleDTO currentSku = traceSkuList.get(i);
            SkuScheduleDTO nextSku = traceSkuList.get(i + 1);
            String levelDesc = resolveSkuTypeTieBreakLevelDesc(context, structurePriorityMap,
                    structureEndingDaysMap, currentSku, nextSku);
            if (StringUtils.isEmpty(levelDesc)) {
                continue;
            }
            PriorityTraceLogHelper.appendLine(detailBuilder,
                    "类型兜底命中: "
                            + currentSku.getMaterialCode() + "(" + resolveNewSpecSkuTypeDesc(currentSku)
                            + "," + resolveNewSpecSkuTypeScore(currentSku) + ") > "
                            + nextSku.getMaterialCode() + "(" + resolveNewSpecSkuTypeDesc(nextSku)
                            + "," + resolveNewSpecSkuTypeScore(nextSku) + ")"
                            + ", 层级=" + levelDesc);
            explainedCount++;
            if (explainedCount >= 10) {
                break;
            }
        }
    }

    private String resolveSupplyChainTieBreakLevelDesc(SkuScheduleDTO left, SkuScheduleDTO right) {
        if (left == null || right == null) {
            return null;
        }
        if (left.getHighPriorityPendingQty() != right.getHighPriorityPendingQty()) {
            return "高优先级数量";
        }
        if (left.getCycleProductionPendingQty() != right.getCycleProductionPendingQty()) {
            return "周期排产数量";
        }
        if (left.getMidPriorityPendingQty() != right.getMidPriorityPendingQty()) {
            return "中优先级数量";
        }
        if (left.getConventionProductionPendingQty() != right.getConventionProductionPendingQty()) {
            return "常规数量";
        }
        return null;
    }

    private String resolveSkuTypeTieBreakLevelDesc(LhScheduleContext context,
                                                   Map<String, StructurePriorityMeta> structurePriorityMap,
                                                   Map<SkuScheduleDTO, Integer> structureEndingDaysMap,
                                                   SkuScheduleDTO left,
                                                   SkuScheduleDTO right) {
        if (left == null || right == null) {
            return null;
        }
        if (resolveNewSpecSkuTypeScore(left) == resolveNewSpecSkuTypeScore(right)) {
            return null;
        }
        if (resolveNewSpecGroupScore(left) != resolveNewSpecGroupScore(right)) {
            return "分组优先级";
        }
        if (StringUtils.isNotEmpty(resolveSupplyChainTieBreakLevelDesc(left, right))) {
            return null;
        }
        if (isTrialGroupSku(left) || isMassTrialGroupSku(left)) {
            return "试制/量试组内排序";
        }
        return isComparableBySkuTypeFallback(context, structurePriorityMap,
                structureEndingDaysMap, left, right) ? "主排序和供应链数量同层" : null;
    }

    private boolean isComparableBySkuTypeFallback(LhScheduleContext context,
                                                  Map<String, StructurePriorityMeta> structurePriorityMap,
                                                  Map<SkuScheduleDTO, Integer> structureEndingDaysMap,
                                                  SkuScheduleDTO left,
                                                  SkuScheduleDTO right) {
        return resolveDeliveryLockedScore(left) == resolveDeliveryLockedScore(right)
                && resolveSpecifyMachineScore(context, left) == resolveSpecifyMachineScore(context, right)
                && Objects.equals(left.getDelayDays(), right.getDelayDays())
                && compareStructurePriority(structurePriorityMap, structureEndingDaysMap, left, right) == 0
                && resolveOpenProductionLateScore(context, left) == resolveOpenProductionLateScore(context, right);
    }

    /**
     * 根据排产类型判断是否续作，输出 1/0 标识。
     *
     * @param scheduleType 排产类型编码
     * @return 1/0
     */
    private static String oneZeroFromScheduleType(String scheduleType) {
        return "01".equals(scheduleType) ? "1" : "0";
    }

    /**
     * 解析SKU施工阶段描述。
     *
     * @param sku SKU
     * @return 阶段描述
     */
    private static String resolveConstructionStageDesc(SkuScheduleDTO sku) {
        if (sku == null) {
            return "-";
        }
        if (sku.isSmallBatchValidation()) {
            return "小批量";
        }
        ConstructionStageEnum stage = ConstructionStageEnum.getByCode(sku.getConstructionStage());
        return stage != null ? stage.getDescription() : "-";
    }

    /**
     * 结构收尾排序元数据。
     * <p>为保持既有比较器和排序日志层级稳定，字段名称继续沿用“全部SKU收尾”语义；其值改为
     * 按结构转产表最大END_DAY统一判断，结构命中后当前参与排产的同结构SKU全部视为命中。</p>
     */
    @lombok.Data
    private static class StructurePriorityMeta {
        /** 结构内SKU总数 */
        private int totalSkuCount;
        /** 结构命中后统一标记的SKU数量；未命中时为0 */
        private int endingSkuCount;
        /** 当前参与排产的同结构SKU是否已统一命中结构收尾标记 */
        private boolean allSkusEnding;
        /** 结构是否进入原结构收尾排序优先级 */
        private boolean allSkusEndingPriority;
        /** 结构最大END_DAY与T日之间包含首尾的距离天数 */
        private int latestEndingDays;
    }
}
