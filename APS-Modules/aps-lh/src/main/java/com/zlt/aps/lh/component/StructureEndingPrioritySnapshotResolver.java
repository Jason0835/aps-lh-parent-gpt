package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.StructureEndingPrioritySnapshot;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 解析结构 N 天内收尾优先级共享快照。
 *
 * <p>唯一口径为结构转产表最大 END_DAY 与排程 T 日的包含首尾距离严格小于
 * {@code SYS0304002}。本组件不调用 {@code isExpectedEnding}，不查询数据库，也不修改
 * SKU、排序名次或排程运行态。</p>
 *
 * @author APS
 */
@Component
public class StructureEndingPrioritySnapshotResolver {

    /**
     * 构建当前排程上下文的结构优先级快照。
     *
     * @param context 排程上下文
     * @return 结构维度只读快照
     */
    public StructureEndingPrioritySnapshot resolve(LhScheduleContext context) {
        int thresholdDays = Objects.nonNull(context)
                && Objects.nonNull(context.getScheduleConfig())
                ? context.getScheduleConfig().getStructureEndingDays()
                : Objects.isNull(context)
                ? LhScheduleConstant.DEFAULT_STRUCTURE_ENDING_DAYS
                : context.getParamIntValue(
                LhScheduleParamConstant.STRUCTURE_ENDING_DAYS,
                LhScheduleConstant.DEFAULT_STRUCTURE_ENDING_DAYS);
        LocalDate scheduleDate = Objects.isNull(context)
                || Objects.isNull(context.getScheduleDate())
                ? null : context.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        Map<String, LocalDate> maximumEndingDateMap = Objects.isNull(context)
                || CollectionUtils.isEmpty(context.getStructurePriorityMaxEndingDateMap())
                ? new LinkedHashMap<String, LocalDate>(0)
                : context.getStructurePriorityMaxEndingDateMap();
        Map<String, Integer> inclusiveDistanceDaysMap =
                new LinkedHashMap<String, Integer>(
                        Math.max(16, maximumEndingDateMap.size() * 2));
        for (Map.Entry<String, LocalDate> entry : maximumEndingDateMap.entrySet()) {
            inclusiveDistanceDaysMap.put(
                    entry.getKey(), this.calculateInclusiveDistanceDays(
                            scheduleDate, entry.getValue()));
        }
        return new StructureEndingPrioritySnapshot(
                scheduleDate, thresholdDays,
                maximumEndingDateMap, inclusiveDistanceDaysMap);
    }

    private int calculateInclusiveDistanceDays(
            LocalDate scheduleDate,
            LocalDate maximumEndingDate) {
        if (Objects.isNull(scheduleDate) || Objects.isNull(maximumEndingDate)
                || maximumEndingDate.isBefore(scheduleDate)) {
            return -1;
        }
        return Math.toIntExact(
                ChronoUnit.DAYS.between(scheduleDate, maximumEndingDate) + 1L);
    }
}
