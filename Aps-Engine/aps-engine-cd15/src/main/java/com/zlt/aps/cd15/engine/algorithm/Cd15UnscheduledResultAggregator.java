package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace;
import com.zlt.aps.cd15.engine.model.Cd15UnscheduledReason;
import com.zlt.aps.cd15.engine.model.Cd15UnscheduledResultModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 窗口结束后按规格汇总未排数量和有序原因。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd15UnscheduledResultAggregator {

    private final Cd15UnscheduledReasonResolver reasonResolver;

    /**
     * 将多班执行轨迹汇总为一规格一原因一行的未排内存结果。
     */
    public List<Cd15UnscheduledResultModel> aggregate(List<Cd15ScheduleAttemptTrace> traces) {
        // 先按全窗口执行序号排序再分组，确保同一规格的第一失败原因可稳定复现。
        Map<String, List<Cd15ScheduleAttemptTrace>> grouped = safe(traces).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getSteelStripCode()))
                .sorted(Comparator.comparingInt(Cd15ScheduleAttemptTrace::getSequence))
                .collect(Collectors.groupingBy(this::materialGroupKey,
                        LinkedHashMap::new, Collectors.toList()));
        List<Cd15UnscheduledResultModel> results = new ArrayList<>();
        grouped.values().forEach(attempts -> results.addAll(this.aggregateOne(attempts)));
        log.info("[斜裁自动排程] 未排结果内存汇总完成, traceCount={}, clothCount={}, reasonRowCount={}",
                safe(traces).size(), grouped.size(), results.size());
        return results;
    }

    private List<Cd15UnscheduledResultModel> aggregateOne(
            List<Cd15ScheduleAttemptTrace> attempts) {
        Cd15ScheduleAttemptTrace identity = attempts.get(0);
        String steelStripCode = identity.getSteelStripCode();
        // 总需求取该规格首次进入窗口时的正净需求，后续班次重算值不能重复累加。
        BigDecimal demand = attempts.stream()
                .map(Cd15ScheduleAttemptTrace::getNetDemandQuantity)
                .filter(value -> value != null && value.signum() > 0)
                .findFirst().orElse(BigDecimal.ZERO);
        // 已排数量按各班真实提交任务量求和，失败轨迹的已排量为0。
        BigDecimal scheduled = attempts.stream()
                .map(Cd15ScheduleAttemptTrace::getScheduledQuantity)
                .map(this::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unscheduled = demand.subtract(scheduled).max(BigDecimal.ZERO);
        if (unscheduled.signum() <= 0) {
            return Collections.emptyList();
        }

        // 原因按实际发生顺序去重；第一条原因作为主因，保留后续诊断原因。
        LinkedHashMap<String, Cd15UnscheduledReason> reasons = new LinkedHashMap<>();
        attempts.stream().map(Cd15ScheduleAttemptTrace::getFailureReason)
                .filter(StringUtils::hasText)
                .map(reasonResolver::resolve)
                .forEach(reason -> reasons.putIfAbsent(reason.getReasonCode(), reason));
        // 窗口结束仍有余量时必须追加窗口限制，即使此前没有资源失败原因。
        Cd15UnscheduledReason windowLimit = reasonResolver.resolve("SCHEDULE_WINDOW_LIMIT");
        reasons.putIfAbsent(windowLimit.getReasonCode(), windowLimit);

        String firstStage = reasons.values().iterator().next().getFailStage();
        String bigRollCode = attempts.stream().map(Cd15ScheduleAttemptTrace::getBigRollCode)
                .filter(StringUtils::hasText).findFirst().orElse(null);
        String cuttingAngle = attempts.stream().map(Cd15ScheduleAttemptTrace::getCuttingAngle)
                .filter(StringUtils::hasText).findFirst().orElse(null);
        List<Cd15UnscheduledResultModel> rows = new ArrayList<>();
        int order = 1;
        for (Cd15UnscheduledReason reason : reasons.values()) {
            rows.add(Cd15UnscheduledResultModel.builder()
                    .steelStripCode(steelStripCode).bigRollCode(bigRollCode)
                    .cuttingAngle(cuttingAngle)
                    .demandQuantity(demand).scheduledQuantity(scheduled)
                    .unscheduledQuantity(unscheduled).failStage(firstStage)
                    .reasonCode(reason.getReasonCode()).reasonOrder(order)
                    .primaryReason(order == 1).reasonDescription(reason.getReasonDescription())
                    .candidateMachineCodes(null).build());
            order++;
        }
        return rows;
    }

    private String materialGroupKey(Cd15ScheduleAttemptTrace trace) {
        return String.valueOf(trace.getSteelStripCode()) + "|"
                + String.valueOf(trace.getBigRollCode()) + "|"
                + String.valueOf(trace.getCuttingAngle());
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
