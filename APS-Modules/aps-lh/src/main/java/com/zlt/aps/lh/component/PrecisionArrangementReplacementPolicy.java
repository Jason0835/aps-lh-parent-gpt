package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.entity.LhScheduleProcessLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 从正式安排及撤销日志还原被替换批次的精度日期归属，禁止按机台或日期范围批量清空。
 *
 * @author APS
 * @date 2026/10/20
 */
@Slf4j
public class PrecisionArrangementReplacementPolicy {

    /**
     * 正式接受精度安排的既有过程日志标题。
     */
    public static final String ARRANGED_LOG_TITLE = "精准计划最终安排";

    /**
     * 撤销精度安排的既有过程日志标题。
     */
    public static final String REVOKED_LOG_TITLE = "精准计划安排撤销";

    /**
     * 正式安排日志必须同时提供批次、工厂、物理机台、计划主键及完整开始时间。
     */
    private static final Pattern ARRANGED_PATTERN = Pattern.compile(
            "^批次=([^，]+)，工厂=([^，]+)，物理机台=([^，]+)，计划主键=([0-9]+)，保养开始="
                    + "([0-9]{4}-[0-9]{2}-[0-9]{2}) [0-9]{2}:[0-9]{2}:[0-9]{2}，");

    /**
     * 撤销日志仅作用于同一批次已经登记的计划主键。
     */
    private static final Pattern REVOKED_PATTERN = Pattern.compile("^计划主键=([0-9]+)，");

    /**
     * 按日志顺序恢复每个批次最后仍有效的正式安排日期。
     * 无法证明身份或日期的记录不作为撤销依据；同批后续撤销会消除早先接受记录。
     *
     * @param factoryCode 当前工厂
     * @param processLogs 当前有效的正式安排及撤销日志
     * @return 批次对应的计划主键和执行自然日；没有可靠证据时返回空Map
     */
    public Map<String, Map<Long, Date>> resolveEffectiveDates(String factoryCode,
                                                           List<LhScheduleProcessLog> processLogs) {
        Map<String, Map<Long, Date>> datesByBatch = new LinkedHashMap<>();
        List<LhScheduleProcessLog> orderedLogs = processLogs.stream()
                .filter(Objects::nonNull)
                .filter(processLog -> StringUtils.equals(factoryCode, processLog.getBusiCode()))
                .filter(processLog -> StringUtils.isNotBlank(processLog.getBatchNo()))
                .filter(processLog -> StringUtils.isNotBlank(processLog.getLogDetail()))
                .sorted(Comparator.comparing(LhScheduleProcessLog::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
        for (LhScheduleProcessLog processLog : orderedLogs) {
            Map<Long, Date> batchDates = datesByBatch.computeIfAbsent(processLog.getBatchNo(),
                    batchNo -> new LinkedHashMap<>());
            try {
                this.applyLog(factoryCode, processLog, batchDates);
            } catch (NumberFormatException | DateTimeParseException exception) {
                // 无效日志不能扩大撤销范围；保留原记录并输出可定位证据。
                log.warn("精度安排归属日志无法解析，保留原日期, 工厂: {}, 批次: {}, 日志主键: {}",
                        factoryCode, processLog.getBatchNo(), processLog.getId(), exception);
            }
        }
        return datesByBatch;
    }

    /**
     * 从被替换批次中选取唯一旧日期，保护新批安排及其他有效批次对同计划同日期的占用。
     *
     * @param replacedDates 被替换批次的有效安排
     * @param retainedDates 其他有效批次的安排
     * @param finalDates 本轮最终安排日期
     * @return 可提交条件撤销的计划主键及唯一旧日期
     */
    public Map<Long, Date> resolveClearableDates(Map<String, Map<Long, Date>> replacedDates,
                                               Map<String, Map<Long, Date>> retainedDates,
                                               Map<Long, Date> finalDates) {
        Map<Long, Set<Date>> retainedDateMap = new LinkedHashMap<>();
        retainedDates.values().forEach(batchDates -> batchDates.forEach((planId, retainedDate) ->
                retainedDateMap.computeIfAbsent(planId, key -> new LinkedHashSet<>()).add(retainedDate)));
        Map<Long, Set<Date>> oldDates = new LinkedHashMap<>();
        replacedDates.values().forEach(batchDates -> batchDates.forEach((planId, oldDate) ->
                oldDates.computeIfAbsent(planId, key -> new LinkedHashSet<>()).add(oldDate)));
        Map<Long, Date> clearableDates = new LinkedHashMap<>();
        oldDates.forEach((planId, dates) -> {
            if (!finalDates.containsKey(planId) && dates.size() == 1) {
                Date oldDate = dates.iterator().next();
                if (!retainedDateMap.getOrDefault(planId, Collections.emptySet()).contains(oldDate)) {
                    clearableDates.put(planId, oldDate);
                }
            }
        });
        return clearableDates;
    }

    /**
     * 应用单条正式安排或撤销事件，其他标题和缺少归属字段的记录保持不变。
     *
     * @param factoryCode 当前工厂
     * @param processLog 待处理的有效过程日志
     * @param batchDates 同批次累计的正式安排
     * @throws NumberFormatException 计划主键不在Long范围内
     * @throws DateTimeParseException 正式安排日期非法
     */
    private void applyLog(String factoryCode, LhScheduleProcessLog processLog, Map<Long, Date> batchDates) {
        if (ARRANGED_LOG_TITLE.equals(processLog.getTitle())) {
            Matcher matcher = ARRANGED_PATTERN.matcher(processLog.getLogDetail());
            if (matcher.find() && processLog.getBatchNo().equals(matcher.group(1))
                    && factoryCode.equals(matcher.group(2)) && StringUtils.isNotBlank(matcher.group(3))) {
                Date executeDate = Date.from(LocalDate.parse(matcher.group(5))
                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
                batchDates.put(Long.valueOf(matcher.group(4)), executeDate);
            }
        } else if (REVOKED_LOG_TITLE.equals(processLog.getTitle())) {
            Matcher matcher = REVOKED_PATTERN.matcher(processLog.getLogDetail());
            if (matcher.find()) {
                batchDates.remove(Long.valueOf(matcher.group(1)));
            }
        }
    }
}
