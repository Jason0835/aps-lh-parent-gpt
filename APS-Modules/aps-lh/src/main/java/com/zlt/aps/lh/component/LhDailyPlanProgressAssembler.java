package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhDailyPlanProgressReportRowVO;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 日排产进度报表行组装器。
 *
 * <p>把「基准判定、增机/延迟判定、T+2日排产信息文案、排序」实现为纯函数，
 * 不依赖 Spring 容器与数据库，便于单元测试，也保证报表口径只有一处实现。</p>
 *
 * <p>口径约定：</p>
 * <ul>
 *   <li>基准：排程结果 class6/7/8（T+2）任一班有计划量，或该班备注为「停产保机」；</li>
 *   <li>基准机台数：按物理机台去重（K1501L/K1501R 合并），停产保机机台同样计入；</li>
 *   <li>增机：在基准时「月计划区间最大机台数 &gt; 基准机台数」，不在基准时「月计划区间最大机台数 &gt; 0」；</li>
 *   <li>延迟：在基准、月计划 T+2 机台数大于基准机台数且有余量。</li>
 * </ul>
 *
 * @author APS
 */
public final class LhDailyPlanProgressAssembler {

    /** T+2 日首班班次索引（class6） */
    private static final int T2_FIRST_SHIFT_INDEX = 6;

    /** T+2 日末班班次索引（class8） */
    private static final int T2_LAST_SHIFT_INDEX = 8;

    /** 换模类型：正规换模（新开） */
    private static final String MOULD_CHANGE_TYPE_REGULAR = MouldChangeTypeEnum.REGULAR.getCode();

    /** 换模类型：更换活字块 */
    private static final String MOULD_CHANGE_TYPE_TYPE_BLOCK = MouldChangeTypeEnum.TYPE_BLOCK.getCode();

    /** 信息片段分隔符 */
    private static final String INFO_SEPARATOR = "；";

    /** 机台号分隔符 */
    private static final String MACHINE_SEPARATOR = "、";

    private LhDailyPlanProgressAssembler() {
    }

    /**
     * 构建 SKU 缓存键：物料编码|产品状态。
     *
     * @param materialCode  物料编码
     * @param productStatus 产品状态
     * @return 缓存键
     */
    public static String buildSkuKey(String materialCode, String productStatus) {
        return StringUtils.trimToEmpty(materialCode) + "|" + StringUtils.trimToEmpty(productStatus);
    }

    /**
     * 判断排程结果在 T+2 日（class6/7/8）是否仍占用硫化机台。
     *
     * <p>停产保机班次计划量为 0，但机台与模具仍归原 SKU 占用，按业务口径同样算作在机。</p>
     *
     * @param result 排程结果
     * @return true-T+2 在机；false-不在机
     */
    public static boolean isT2MachineOccupied(LhScheduleResult result) {
        if (Objects.isNull(result)) {
            return false;
        }
        for (int shiftIndex = T2_FIRST_SHIFT_INDEX; shiftIndex <= T2_LAST_SHIFT_INDEX; shiftIndex++) {
            Integer planQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (Objects.nonNull(planQty) && planQty > 0) {
                return true;
            }
            String analysis = ShiftFieldUtil.getShiftAnalysis(result, shiftIndex);
            if (StringUtils.contains(analysis, ShiftFieldUtil.CONTINUOUS_STOP_HOLD_ANALYSIS)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建基准 SKU 映射（含 T+2 机台数、余量、合格产量）。
     *
     * @param scheduleResultList 当前排程日期的全部排程结果
     * @return key=物料|产品状态，value=基准信息；不在基准的 SKU 不会出现在返回中
     */
    public static Map<String, BaselineSku> buildBaseline(List<LhScheduleResult> scheduleResultList) {
        if (Objects.isNull(scheduleResultList) || scheduleResultList.isEmpty()) {
            return new LinkedHashMap<>(0);
        }
        Map<String, List<LhScheduleResult>> groupedResultMap = scheduleResultList.stream()
                .filter(Objects::nonNull)
                .filter(result -> StringUtils.isNotEmpty(result.getMaterialCode()))
                .collect(Collectors.groupingBy(
                        result -> buildSkuKey(result.getMaterialCode(), result.getProductStatus()),
                        LinkedHashMap::new, Collectors.toList()));

        Map<String, BaselineSku> baselineMap = new LinkedHashMap<>(groupedResultMap.size());
        for (Map.Entry<String, List<LhScheduleResult>> entry : groupedResultMap.entrySet()) {
            List<LhScheduleResult> skuResultList = entry.getValue();
            // 1. T+2 在机机台按物理机台去重（单控 L/R 合并，停产保机计入）
            Set<String> physicalMachineSet = skuResultList.stream()
                    .filter(LhDailyPlanProgressAssembler::isT2MachineOccupied)
                    .map(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (physicalMachineSet.isEmpty()) {
                continue;
            }
            // 2. 余量与合格产量取同批次落库值（同一 SKU 多机台行重复携带，取首行即可）
            LhScheduleResult firstResult = skuResultList.get(0);
            BaselineSku baselineSku = new BaselineSku();
            baselineSku.setMaterialCode(StringUtils.trimToEmpty(firstResult.getMaterialCode()));
            baselineSku.setProductStatus(StringUtils.trimToEmpty(firstResult.getProductStatus()));
            baselineSku.setMachineQty(physicalMachineSet.size());
            baselineSku.setSurplusQty(nvl(firstResult.getMouldSurplusQty()));
            baselineSku.setFinishQty(nvl(firstResult.getTotalFinishQty()));
            baselineMap.put(entry.getKey(), baselineSku);
        }
        return baselineMap;
    }

    /**
     * 判断是否属于「增机」行。
     *
     * @param inBaseline    是否在基准
     * @param baseMachineQty 基准机台数
     * @param maxMachineQty  月计划区间最大机台数
     * @return true-增机
     */
    public static boolean isIncrease(boolean inBaseline, int baseMachineQty, int maxMachineQty) {
        if (inBaseline) {
            return maxMachineQty > baseMachineQty;
        }
        return maxMachineQty > 0;
    }

    /**
     * 判断是否属于「延迟」行。
     *
     * @param inBaseline       是否在基准
     * @param baseMachineQty   基准机台数
     * @param t2MonthMachineQty 月计划 T+2 机台数
     * @param surplusQty       硫化余量
     * @return true-延迟
     */
    public static boolean isDelay(boolean inBaseline, int baseMachineQty,
                                  int t2MonthMachineQty, int surplusQty) {
        return inBaseline && t2MonthMachineQty > baseMachineQty && surplusQty > 0;
    }

    /**
     * 构建「T+2日排产信息」文案。
     *
     * <p>片段顺序：新开 → 换活字块 → 提前 → 超过月计划N台 → 延迟；片段之间用「；」分隔，
     * 同一片段内多机台用「、」连接。月计划 T+2 机台数小于基准机台数时统一输出「提前」
     * （不区分新开或换活字块）。</p>
     *
     * @param changePlanList     该 SKU 在 T+2 的换模计划
     * @param t2MonthMachineQty  月计划 T+2 机台数
     * @param baseMachineQty     基准机台数
     * @param delay              是否延迟
     * @param labels             文案词（由调用方按当前语言传入）
     * @return 排产信息文案，无信息时返回空字符串
     */
    public static String buildT2Info(List<LhMouldChangePlan> changePlanList,
                                     int t2MonthMachineQty,
                                     int baseMachineQty,
                                     boolean delay,
                                     T2InfoLabels labels) {
        List<LhMouldChangePlan> effectivePlanList = Objects.isNull(changePlanList)
                ? Collections.emptyList() : changePlanList;
        Set<String> regularMachineSet = collectMachines(effectivePlanList, MOULD_CHANGE_TYPE_REGULAR);
        Set<String> typeBlockMachineSet = collectMachines(effectivePlanList, MOULD_CHANGE_TYPE_TYPE_BLOCK);
        // 月计划 T+2 机台数小于基准机台数：换模被提前，统一输出「提前」
        boolean advance = baseMachineQty > t2MonthMachineQty;
        Set<String> fragmentSet = new LinkedHashSet<>(4);
        if (advance) {
            Set<String> advanceMachineSet = new LinkedHashSet<>(regularMachineSet.size() + typeBlockMachineSet.size());
            advanceMachineSet.addAll(regularMachineSet);
            advanceMachineSet.addAll(typeBlockMachineSet);
            if (advanceMachineSet.isEmpty()) {
                fragmentSet.add(MessageFormat.format(labels.getExceedMonthPlanPattern(),
                        baseMachineQty - t2MonthMachineQty));
            } else {
                fragmentSet.add(joinMachines(advanceMachineSet) + labels.getAdvance());
            }
        } else {
            if (!regularMachineSet.isEmpty()) {
                fragmentSet.add(joinMachines(regularMachineSet) + labels.getNewOpen());
            }
            if (!typeBlockMachineSet.isEmpty()) {
                fragmentSet.add(joinMachines(typeBlockMachineSet) + labels.getTypeBlock());
            }
        }
        if (delay) {
            fragmentSet.add(labels.getDelay());
        }
        return String.join(INFO_SEPARATOR, fragmentSet);
    }

    /**
     * 报表行排序：产品结构 → 主花纹 → 胎胚描述 → 物料编码。
     *
     * @param rowList 行列表（原地排序）
     */
    public static void sortRows(List<LhDailyPlanProgressReportRowVO> rowList) {
        if (Objects.isNull(rowList) || rowList.isEmpty()) {
            return;
        }
        Comparator<String> nullSafeComparator = Comparator.nullsLast(String::compareTo);
        rowList.sort(Comparator
                .comparing(LhDailyPlanProgressReportRowVO::getStructureName, nullSafeComparator)
                .thenComparing(LhDailyPlanProgressReportRowVO::getMainPattern, nullSafeComparator)
                .thenComparing(LhDailyPlanProgressReportRowVO::getMainMaterialDesc, nullSafeComparator)
                .thenComparing(LhDailyPlanProgressReportRowVO::getMaterialCode, nullSafeComparator));
    }

    /**
     * 汇总指定换模类型的物理机台编码。
     *
     * @param planList          换模计划列表
     * @param changeMouldTypeCode 换模类型编码（01-正规、02-换活字块）
     * @return 去重后的物理机台编码集合（保持出现顺序）
     */
    private static Set<String> collectMachines(List<LhMouldChangePlan> planList, String changeMouldTypeCode) {
        Set<String> machineSet = new LinkedHashSet<>(8);
        for (LhMouldChangePlan plan : planList) {
            if (Objects.isNull(plan)
                    || !MouldChangeTypeEnum.containsCode(plan.getChangeMouldType(), changeMouldTypeCode)) {
                continue;
            }
            String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                    plan.getLhMachineCode());
            if (StringUtils.isNotEmpty(physicalMachineCode)) {
                machineSet.add(physicalMachineCode);
            }
        }
        return machineSet;
    }

    /**
     * 机台号集合转展示文本。
     *
     * @param machineSet 物理机台编码集合
     * @return 「K1511、K1513」形式
     */
    private static String joinMachines(Set<String> machineSet) {
        return String.join(MACHINE_SEPARATOR, new ArrayList<>(machineSet));
    }

    /**
     * 空值转 0。
     *
     * @param value 原值
     * @return 非空值或 0
     */
    private static int nvl(Integer value) {
        return Objects.isNull(value) ? 0 : Math.max(0, value);
    }

    /**
     * 基准 SKU 信息（T+2 在机机台数、余量、合格产量）。
     */
    @Data
    public static class BaselineSku {

        /** 物料编码 */
        private String materialCode;

        /** 产品状态 */
        private String productStatus;

        /** T+2 在机物理机台数（停产保机计入） */
        private int machineQty;

        /** 硫化余量（同批次落库值） */
        private int surplusQty;

        /** 合格产量（同批次落库的月累计已完成量） */
        private int finishQty;
    }

    /**
     * 「T+2日排产信息」文案词（按当前语言传入，保证纯函数可测）。
     */
    @Data
    public static class T2InfoLabels {

        /** 新开 */
        private String newOpen;

        /** 换活字块 */
        private String typeBlock;

        /** 提前 */
        private String advance;

        /** 延迟 */
        private String delay;

        /** 超过月计划{0}台 */
        private String exceedMonthPlanPattern;
    }
}
