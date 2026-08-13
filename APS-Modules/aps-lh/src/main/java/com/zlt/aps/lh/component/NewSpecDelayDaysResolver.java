package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.MonthPlanDayQtyUtil;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * S4.5 新增排产 SKU 延误天数解析器。
 *
 * <p>该组件只在新增排产最终排序前重算当前待排新增 SKU 的延误天数，不参与 S4.3 SKU
 * 初始化和 S4.4 续作排序。计算严格按“续作加机台、窗口内最早计划、窗口无计划但未来有计划、
 * 仅欠产且本月从未排产”依次判断，任一场景命中后立即返回，避免后续场景覆盖前序结果。</p>
 *
 * <p>月计划只读取 S4.2 已加载的 {@code loadedMonthPlanList}，按物料、产品状态和业务日期
 * 聚合同日多条 DAY_N；不读取排程过程中可能被欠产追加、收尾放大或排产扣减的运行态日计划
 * 账本，从而保证排序依据始终是原始月计划口径。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class NewSpecDelayDaysResolver {

    /** 场景1：续作 SKU 因需要新增机台进入新增排产。 */
    private static final String SCENE_CONTINUATION_ADD_MACHINE = "场景1-续作加机台";
    /** 场景4：非续作加机台 SKU 在本次排程窗口内存在正月计划量。 */
    private static final String SCENE_WINDOW_PLAN = "场景4-窗口内最早计划";
    /** 场景2：排程窗口内无正计划量，但已加载的未来月份或日期存在正计划量。 */
    private static final String SCENE_FUTURE_PLAN = "场景2-窗口无计划未来有计划";
    /** 场景3：当前待排量仅来自欠产，且本月从未排产。 */
    private static final String SCENE_SHORTAGE_NEVER_PRODUCED = "场景3-仅欠产且本月从未排产";
    /** 全部业务场景均未命中，按已确认口径将延误天数置为0。 */
    private static final String SCENE_DEFAULT = "默认-未命中业务场景";

    /**
     * 重算 S4.5 当前待排新增 SKU 的延误天数。
     *
     * <p>调用时点必须位于 S4.4 完成之后、S4.5 最终 SKU 排序之前。此时续作加机台补偿 SKU
     * 及首次增机日期已经生成，而新增排序尚未执行，可以只替换排序字段取值而不改变任何分组、
     * 比较层级、排序方向、选机和数量账本。</p>
     *
     * @param context 排程上下文，提供 T 日、排程窗口、待排新增 SKU、已加载月计划及完成量
     */
    public void refreshDelayDays(LhScheduleContext context) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getNewSpecSkuList())) {
            return;
        }
        LocalDate scheduleDate = this.toLocalDate(context.getScheduleDate());
        LocalDate windowEndDate = this.toLocalDate(context.getWindowEndDate());
        Map<String, NavigableMap<LocalDate, Integer>> dailyPlanIndex =
                this.buildOriginalDailyPlanIndex(context.getLoadedMonthPlanList());

        int refreshedCount = 0;
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            if (Objects.isNull(sku)) {
                continue;
            }
            this.resolveAndApplyDelayDays(context, sku, scheduleDate, windowEndDate, dailyPlanIndex);
            refreshedCount++;
        }
        log.info("新增排产SKU延误天数重算完成, factoryCode: {}, batchNo: {}, T日: {}, 窗口结束日: {}, "
                        + "待排SKU数: {}, 月计划索引SKU数: {}",
                context.getFactoryCode(), context.getBatchNo(), scheduleDate, windowEndDate,
                refreshedCount, dailyPlanIndex.size());
    }

    /**
     * 按固定业务优先级解析并写入单个新增 SKU 的延误天数。
     *
     * @param context 排程上下文
     * @param sku 待排新增 SKU
     * @param scheduleDate 排程窗口 T 日
     * @param windowEndDate 排程窗口结束日
     * @param dailyPlanIndex 原始月计划日量聚合索引
     */
    private void resolveAndApplyDelayDays(LhScheduleContext context,
                                          SkuScheduleDTO sku,
                                          LocalDate scheduleDate,
                                          LocalDate windowEndDate,
                                          Map<String, NavigableMap<LocalDate, Integer>> dailyPlanIndex) {
        if (Objects.isNull(scheduleDate) || Objects.isNull(windowEndDate)
                || scheduleDate.isAfter(windowEndDate)) {
            this.applyDelayDays(context, sku, SCENE_DEFAULT, scheduleDate, null, 0);
            return;
        }

        /*
         * 场景1优先级最高。只有 S4.4 按既有 dayN 加机台规则生成的补偿 SKU 才命中；
         * 首次增机日期缺失时仍视为该场景，但按已确认默认值0直接返回，禁止落入其他场景。
         */
        if (sku.isContinuousCompensationSku()
                && SkuScheduleSourceTypeEnum.isContinuationAddMachine(sku.getSourceType())) {
            LocalDate firstAddMachineDate = sku.getFirstAddMachineProductionDate();
            int delayDays = this.calculateSignedDays(scheduleDate, firstAddMachineDate);
            this.applyDelayDays(context, sku, SCENE_CONTINUATION_ADD_MACHINE,
                    scheduleDate, firstAddMachineDate, delayDays);
            return;
        }

        String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus());
        NavigableMap<LocalDate, Integer> dailyPlanMap = dailyPlanIndex.get(materialStatusKey);
        LocalDate firstWindowPlanDate = this.findFirstPositivePlanDate(
                dailyPlanMap, scheduleDate, windowEndDate);

        /*
         * 场景4用于细分原先命中默认值、但排程窗口内实际存在正月计划量的新增 SKU。
         * T2严格取当前物料+产品状态在[T日, 窗口结束日]内最早聚合计划量大于0的日期，
         * 延误天数按“T2-T日”计算。例如3302000156在本次窗口最早计划日为T+1，应得到1而不是0。
         * 该分支位于场景1之后，确保续作加机台仍以首次需要新增机台日期作为唯一T2来源。
         */
        if (Objects.nonNull(firstWindowPlanDate)) {
            int delayDays = this.calculateSignedDays(scheduleDate, firstWindowPlanDate);
            this.applyDelayDays(context, sku, SCENE_WINDOW_PLAN,
                    scheduleDate, firstWindowPlanDate, delayDays);
            return;
        }

        /*
         * 运行到此处已确认窗口内每个业务日的聚合月计划量均不大于0。
         * 未来从窗口结束次日开始，只扫描 S4.2 已加载的月份，命中最早聚合计划量大于0的日期后返回。
         */
        LocalDate firstFuturePlanDate = this.findFirstPositivePlanDate(
                dailyPlanMap, windowEndDate.plusDays(1), null);
        if (Objects.nonNull(firstFuturePlanDate)) {
            int delayDays = this.calculateSignedDays(scheduleDate, firstFuturePlanDate);
            this.applyDelayDays(context, sku, SCENE_FUTURE_PLAN,
                    scheduleDate, firstFuturePlanDate, delayDays);
            return;
        }

        /*
         * 场景3只接收窗口内无原始 dayN、当前余量具有正向历史欠产来源的 SKU。
         * 本月完成量复用“截至T-1月累计完成量 + T日班次完成量”，并保持物料+产品状态隔离；
         * 合计小于等于0才认定本月从未排产，延误天数按“本月最早计划日 - T日”保留负数。
         */
        int currentMonthFinishedQty = this.resolveCurrentMonthFinishedQty(context, sku, scheduleDate);
        if (this.isHistoryShortageOnly(sku) && currentMonthFinishedQty <= 0) {
            LocalDate monthStartDate = scheduleDate.withDayOfMonth(1);
            LocalDate monthEndDate = scheduleDate.withDayOfMonth(scheduleDate.lengthOfMonth());
            LocalDate firstMonthPlanDate = this.findFirstPositivePlanDate(
                    dailyPlanMap, monthStartDate, monthEndDate);
            if (Objects.nonNull(firstMonthPlanDate)) {
                int delayDays = this.calculateSignedDays(scheduleDate, firstMonthPlanDate);
                this.applyDelayDays(context, sku, SCENE_SHORTAGE_NEVER_PRODUCED,
                        scheduleDate, firstMonthPlanDate, delayDays);
                return;
            }
        }

        this.applyDelayDays(context, sku, SCENE_DEFAULT, scheduleDate, null, 0);
    }

    /**
     * 将已加载月计划转换为物料+产品状态+日期维度的原始 dayN 聚合索引。
     *
     * <p>同一物料和产品状态在同一自然月可能存在多条计划记录，必须先按日期求和再判断是否
     * 大于0；不能直接使用全局月计划单记录索引，否则会遗漏后续记录的计划量。</p>
     *
     * @param loadedMonthPlanList S4.2 已加载的全部月计划
     * @return key=物料+产品状态，value=按业务日期排序的聚合计划量
     */
    private Map<String, NavigableMap<LocalDate, Integer>> buildOriginalDailyPlanIndex(
            List<FactoryMonthPlanProductionFinalResult> loadedMonthPlanList) {
        Map<String, NavigableMap<LocalDate, Integer>> dailyPlanIndex = new HashMap<>(128);
        if (CollectionUtils.isEmpty(loadedMonthPlanList)) {
            return dailyPlanIndex;
        }
        for (FactoryMonthPlanProductionFinalResult plan : loadedMonthPlanList) {
            if (!this.isValidMonthPlan(plan)) {
                continue;
            }
            YearMonth planMonth = YearMonth.of(plan.getYear(), plan.getMonth());
            String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    plan.getMaterialCode(), plan.getProductStatus());
            NavigableMap<LocalDate, Integer> dailyPlanMap = dailyPlanIndex.computeIfAbsent(
                    materialStatusKey, key -> new TreeMap<LocalDate, Integer>());
            for (int dayOfMonth = 1; dayOfMonth <= planMonth.lengthOfMonth(); dayOfMonth++) {
                int dayPlanQty = MonthPlanDayQtyUtil.resolveDayQty(plan, dayOfMonth);
                if (dayPlanQty == 0) {
                    continue;
                }
                dailyPlanMap.merge(planMonth.atDay(dayOfMonth), dayPlanQty, Integer::sum);
            }
        }
        return dailyPlanIndex;
    }

    /**
     * 判断月计划是否具备构建业务日期所需的关键字段。
     *
     * @param plan 月计划记录
     * @return true-物料、年份和月份有效；false-不能参与日期索引
     */
    private boolean isValidMonthPlan(FactoryMonthPlanProductionFinalResult plan) {
        return Objects.nonNull(plan)
                && StringUtils.isNotEmpty(plan.getMaterialCode())
                && Objects.nonNull(plan.getYear())
                && plan.getYear() > 0
                && Objects.nonNull(plan.getMonth())
                && plan.getMonth() >= 1
                && plan.getMonth() <= 12;
    }

    /**
     * 查找指定日期范围内最早聚合计划量大于0的日期。
     *
     * @param dailyPlanMap 单个物料+产品状态的日计划索引
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）；为null时扫描至已加载计划末日
     * @return 最早正计划量日期；不存在时返回null
     */
    private LocalDate findFirstPositivePlanDate(NavigableMap<LocalDate, Integer> dailyPlanMap,
                                                LocalDate startDate,
                                                LocalDate endDate) {
        if (CollectionUtils.isEmpty(dailyPlanMap) || Objects.isNull(startDate)
                || (Objects.nonNull(endDate) && startDate.isAfter(endDate))) {
            return null;
        }
        for (Map.Entry<LocalDate, Integer> entry : dailyPlanMap.tailMap(startDate, true).entrySet()) {
            if (Objects.nonNull(endDate) && entry.getKey().isAfter(endDate)) {
                break;
            }
            if (Objects.nonNull(entry.getValue()) && entry.getValue() > 0) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 判断当前待排量是否具有“仅历史欠产”来源。
     *
     * <p>复用项目现有仅历史欠产规则口径：本月历史欠产或有效上月欠产任一为正，并且当前
     * 净硫化余量仍为正。窗口原始 dayN 是否为空由调用方统一判断，避免运行态账本改写来源判断。</p>
     *
     * @param sku 待排新增 SKU
     * @return true-具有正向历史欠产且仍有净余量；false-不是仅欠产来源
     */
    private boolean isHistoryShortageOnly(SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || sku.getSurplusQty() <= 0) {
            return false;
        }
        return sku.getMonthlyHistoryShortageQty() > 0
                || sku.getEffectiveLastMonthOverdueQty() > 0;
    }

    /**
     * 汇总 SKU 在 T 日所属月份的已完成量。
     *
     * <p>月累计完成量已由 S4.2 按物料+产品状态+年月聚合到 T-1；T 日班次完成量来自独立
     * 物料+产品状态 Map。两部分统一按非负值相加，合计为0即代表本月尚无实际完成量。</p>
     *
     * @param context 排程上下文
     * @param sku 待排新增 SKU
     * @param scheduleDate 排程窗口 T 日
     * @return 当前月已完成量
     */
    private int resolveCurrentMonthFinishedQty(LhScheduleContext context,
                                               SkuScheduleDTO sku,
                                               LocalDate scheduleDate) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(scheduleDate)
                || StringUtils.isEmpty(sku.getMaterialCode())) {
            return 0;
        }
        String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus());
        String materialMonthKey = MonthPlanDateResolver.buildMaterialMonthKey(
                materialStatusKey, scheduleDate.getYear(), scheduleDate.getMonthValue());
        int monthFinishedQty = this.resolveNonNegativeQty(
                context.getMaterialMonthFinishedQtyByMonthMap().get(materialMonthKey));
        int scheduleDayFinishedQty = this.resolveNonNegativeQty(
                context.getMaterialScheDayFinishQtyMap().get(materialStatusKey));
        return monthFinishedQty + scheduleDayFinishedQty;
    }

    /**
     * 按自然日计算有符号日期差。
     *
     * @param scheduleDate 排程窗口 T 日
     * @param targetDate 场景命中的 T2/本月最早计划日
     * @return targetDate - scheduleDate；任一日期缺失时返回0
     */
    private int calculateSignedDays(LocalDate scheduleDate, LocalDate targetDate) {
        if (Objects.isNull(scheduleDate) || Objects.isNull(targetDate)) {
            return 0;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(scheduleDate, targetDate));
    }

    /**
     * 写入并记录单个新增 SKU 的延误天数审计日志。
     *
     * @param context 排程上下文
     * @param sku 待排新增 SKU
     * @param scene 命中场景
     * @param scheduleDate 排程窗口 T 日
     * @param targetDate T2；场景3为本月最早计划日；默认场景为空
     * @param delayDays 有符号延误天数
     */
    private void applyDelayDays(LhScheduleContext context,
                                SkuScheduleDTO sku,
                                String scene,
                                LocalDate scheduleDate,
                                LocalDate targetDate,
                                int delayDays) {
        sku.setDelayDays(delayDays);
        log.info("新增排产SKU延误天数重算, factoryCode: {}, batchNo: {}, materialCode: {}, "
                        + "productStatus: {}, scene: {}, T日: {}, T2/最早计划日: {}, delayDays: {}",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                scene, scheduleDate, targetDate, delayDays);
    }

    /**
     * 将可能为空或为负的完成量转换为完成量判断口径。
     *
     * @param qty 完成量
     * @return 大于0时返回原值，否则返回0
     */
    private int resolveNonNegativeQty(Integer qty) {
        return Objects.nonNull(qty) ? Math.max(0, qty) : 0;
    }

    /**
     * 将旧日期类型转换为排程自然日。
     *
     * @param date 日期
     * @return 系统时区下的自然日；日期为空时返回null
     */
    private LocalDate toLocalDate(Date date) {
        if (Objects.isNull(date)) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
