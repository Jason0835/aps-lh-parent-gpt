package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.MonthPlanDayQtyUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 根据原机实际装机模具及未来定稿计划判断置换保护，不修改清洗、资源或数量账本。
 *
 * @author APS
 * @since 2026/11/17
 */
@Slf4j
public final class SharedMouldFuturePlanPolicy {
    /**
     * 禁止实例化无状态公共策略。
     */
    private SharedMouldFuturePlanPolicy() {
    }

    /**
     * 计算观察截止自然日，仅本月有效抓取日且T大于等于该日时观察至次月底。
     *
     * @param scheduleDate 窗口起点T日
     * @param stockCaptureDate 原始库存抓取日；为空或不在T所属月时按无有效抓取日处理
     * @return 包含当天的截止日；缺抓取日返回本月底，缺T日返回null
     */
    public static LocalDate resolveEndDate(LocalDate scheduleDate, LocalDate stockCaptureDate) {
        if (scheduleDate == null) {
            return null;
        }
        YearMonth scheduleMonth = YearMonth.from(scheduleDate);
        boolean includeNextMonth = stockCaptureDate != null
                && YearMonth.from(stockCaptureDate).equals(scheduleMonth)
                && !scheduleDate.isBefore(stockCaptureDate);
        return scheduleMonth.plusMonths(includeNextMonth ? 1 : 0).atEndOfMonth();
    }

    /**
     * 判断是否存在未来共用实际装机模具的同结构同主花纹其他物料。
     * 缺本月抓取日仍检查T日之后至本月底的计划；空属性不能作为相同证据。
     *
     * @param context 持有独立观察快照的批次上下文
     * @param materialCode 原物料编码
     * @param structureName 原物料结构
     * @param mainPattern 原物料主花纹
     * @param originalMouldCodes 置换前实际装机模具，不能传物料全部关系或备用模具
     * @return 至少一副实际装机模具被符合条件的未来物料共用时返回true
     */
    public static boolean isReplacementProhibited(LhScheduleContext context, String materialCode,
            String structureName, String mainPattern, Set<String> originalMouldCodes) {
        if (context.getScheduleDate() == null || StringUtils.isBlank(materialCode)
                || StringUtils.isBlank(structureName) || StringUtils.isBlank(mainPattern)
                || originalMouldCodes == null || originalMouldCodes.isEmpty()) {
            return false;
        }
        LocalDate start = context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = resolveEndDate(start, context.getSandBlastStockCaptureDate());
        if (context.getSandBlastStockCaptureDate() == null
                || !YearMonth.from(start).equals(YearMonth.from(context.getSandBlastStockCaptureDate()))) {
            log.info("喷砂置换保护未取得本月库存抓取日，观察至本月底, factoryCode: {}, batchNo: {}, "
                            + "materialCode: {}, T: {}, endDate: {}",
                    context.getFactoryCode(), context.getBatchNo(), materialCode, start, end);
        }
        Set<String> originals = originalMouldCodes.stream().map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        for (FactoryMonthPlanProductionFinalResult plan : context.getSandBlastFuturePlanList()) {
            if (plan == null || StringUtils.isBlank(plan.getMaterialCode())
                    || StringUtils.equals(materialCode, plan.getMaterialCode())
                    || !StringUtils.equals(structureName.trim(), StringUtils.trimToNull(plan.getStructureName()))
                    || !StringUtils.equals(mainPattern.trim(), StringUtils.trimToNull(plan.getMainPattern()))
                    || plan.getYear() == null || plan.getMonth() == null) {
                continue;
            }
            Set<String> matched = context.getSandBlastFutureMouldRelMap()
                    .getOrDefault(plan.getMaterialCode(), Collections.emptyList()).stream()
                    .filter(Objects::nonNull).map(MdmSkuMouldRel::getMouldCode)
                    .map(StringUtils::trimToEmpty).filter(originals::contains).collect(Collectors.toSet());
            if (matched.isEmpty()) {
                continue;
            }
            for (LocalDate day = start.plusDays(1); !day.isAfter(end); day = day.plusDays(1)) {
                if (Objects.equals(plan.getYear(), day.getYear())
                        && Objects.equals(plan.getMonth(), day.getMonthValue())
                        && MonthPlanDayQtyUtil.resolveDayQty(plan, day.getDayOfMonth()) > 0) {
                    log.info("喷砂置换禁止，实际装机共模未来计划命中, factoryCode: {}, batchNo: {}, T: {}, "
                                    + "stockCaptureDate: {}, endDate: {}, materialCode: {}, originalMoulds: {}, "
                                    + "candidate: {}, sharedMoulds: {}, planDate: {}",
                            context.getFactoryCode(), context.getBatchNo(), start,
                            context.getSandBlastStockCaptureDate(), end, materialCode, originals,
                            plan.getMaterialCode(), matched, day);
                    return true;
                }
            }
        }
        return false;
    }
}
