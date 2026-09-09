package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import org.springframework.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.context.LhScheduleContext;

import java.util.Objects;

/**
 * 清洗计划业务规则工具。
 *
 * <p>该工具只承载清洗专用判断，避免干冰/喷砂清洗规则散落到新增、续作、换活字块等排程策略中。</p>
 */
public final class CleaningScheduleRuleUtil {

    /** SKU 从清洗时间点开始 3 天内可收尾时跳过清洗 */
    public static final int CLEANING_SKIP_ENDING_DAYS = 3;

    private CleaningScheduleRuleUtil() {
    }

    /**
     * 取得结束时间归入当前计算窗口的喷砂首检事件。
     * <p>只使用初始化时的条数快照，普通清洗窗口不补量；按来源、机台和时间去重。
     * 班内游标已经到达首检结束时不得再次领取首检，标准班次边界归属除外。</p>
     * @param windows 本结果实际生效的清洗窗口
     * @param startTime 计算起点
     * @param endTime 计算终点；标准班次不含终点，实际回裁终点允许完成本班首检
     * @return 按首检结束时间排序的独立事件
     */
    public static List<MachineCleaningWindowDTO> resolveSandBlastInspectionWindows(
            List<MachineCleaningWindowDTO> windows, Date startTime, Date endTime) {
        if (CollectionUtils.isEmpty(windows) || Objects.isNull(startTime) || Objects.isNull(endTime)) {
            return Collections.emptyList();
        }
        Map<String, MachineCleaningWindowDTO> events = new LinkedHashMap<>(windows.size());
        for (MachineCleaningWindowDTO window : windows) {
            if (!MachineCleaningOverlapUtil.isSandBlastCleaning(window)
                    || Objects.isNull(window.getSandBlastFirstInspectionQty())
                    || window.getSandBlastFirstInspectionQty() <= 0
                    || Objects.isNull(window.getCleanEndTime()) || Objects.isNull(window.getReadyTime())
                    || !window.getCleanEndTime().before(window.getReadyTime())) {
                continue;
            }
            // 已在首检中途才满足生产门禁的后料不能承接整次首检；跨标准班次的持续首检除外。
            if (startTime.after(window.getCleanEndTime())
                    && !startTime.equals(window.getSandBlastInspectionShiftStartTime())) {
                continue;
            }
            Date readyTime = window.getReadyTime();
            boolean atShiftBoundary = readyTime.equals(startTime)
                    && readyTime.equals(window.getSandBlastInspectionShiftStartTime());
            // 回裁窗口可以恰好截到首检完成点；该点须已归属当前标准班次，不能借此跨班加量。
            boolean completedAtCutoff = readyTime.equals(endTime)
                    && Objects.nonNull(window.getSandBlastInspectionShiftStartTime())
                    && window.getSandBlastInspectionShiftStartTime().before(endTime);
            if ((!startTime.before(readyTime) && !atShiftBoundary)
                    || (!readyTime.before(endTime) && !completedAtCutoff)) {
                continue;
            }
            String eventKey = window.getSourcePlanId() + "|" + window.getLhCode() + "|" + readyTime.getTime();
            events.putIfAbsent(eventKey, window);
        }
        List<MachineCleaningWindowDTO> result = new ArrayList<>(events.values());
        result.sort(java.util.Comparator.comparing(MachineCleaningWindowDTO::getReadyTime));
        return result;
    }

    /**
     * 后料只能消费自身开始后的清洗事件，防止恰好在前料首检结束时衔接又领取首检量。
     * @param windows 已排清洗窗口
     * @param productionStartTime 当前结果的首次生产起点
     * @return 当前生产链尚未结束的清洗窗口
     */
    public static List<MachineCleaningWindowDTO> excludeCompletedWindows(
            List<MachineCleaningWindowDTO> windows, Date productionStartTime) {
        if (CollectionUtils.isEmpty(windows) || Objects.isNull(productionStartTime)) {
            return windows;
        }
        List<MachineCleaningWindowDTO> result = new ArrayList<>(windows.size());
        for (MachineCleaningWindowDTO window : windows) {
            if (Objects.nonNull(window) && (!MachineCleaningOverlapUtil.isSandBlastCleaning(window)
                    || Objects.isNull(window.getSandBlastFirstInspectionQty())
                    || Objects.isNull(window.getReadyTime()) || window.getReadyTime().after(productionStartTime))) {
                result.add(window);
            }
        }
        return result;
    }

    /**
     * 判断 SKU 是否命中 3 天内收尾跳过清洗规则。
     *
     * @param sku SKU 排程数据
     * @return true-应跳过清洗；false-不跳过清洗
     */
    public static boolean shouldSkipCleaningBySkuEnding(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(sku)) {
            return false;
        }
        int dailyCapacity = sku.getDailyCapacity();
        if (dailyCapacity <= 0) {
            // 清洗跳过按“SKU日标准产量”判断；SKU DTO 未带日标准产能时，从上下文主数据回查。
            dailyCapacity = ShiftCapacityResolverUtil.resolveDailyStandardQty(context, sku.getMaterialCode());
        }
        return isEndingWithinDays(sku.getSurplusQty(), dailyCapacity, CLEANING_SKIP_ENDING_DAYS);
    }

    /**
     * 判断排程结果是否命中 3 天内收尾跳过清洗规则。
     *
     * @param result 排程结果
     * @return true-应跳过清洗；false-不跳过清洗
     */
    public static boolean shouldSkipCleaningByResultEnding(LhScheduleResult result) {
        if (Objects.isNull(result)) {
            return false;
        }
        int surplusQty = Objects.isNull(result.getMouldSurplusQty()) ? 0 : result.getMouldSurplusQty();
        int dailyCapacity = Objects.isNull(result.getStandardCapacity()) ? 0 : result.getStandardCapacity();
        return isEndingWithinDays(surplusQty, dailyCapacity, CLEANING_SKIP_ENDING_DAYS);
    }

    /**
     * 判断指定余量按日标准产能是否可在阈值天数内收尾。
     *
     * @param surplusQty 硫化余量
     * @param dailyCapacity 日标准产能
     * @param thresholdDays 收尾天数阈值
     * @return true-阈值天数内可收尾；false-无法判定或超过阈值
     */
    public static boolean isEndingWithinDays(int surplusQty, int dailyCapacity, int thresholdDays) {
        if (surplusQty <= 0) {
            return true;
        }
        if (dailyCapacity <= 0 || thresholdDays <= 0) {
            return false;
        }
        int remainingDays = (surplusQty + dailyCapacity - 1) / dailyCapacity;
        return remainingDays <= thresholdDays;
    }
}
