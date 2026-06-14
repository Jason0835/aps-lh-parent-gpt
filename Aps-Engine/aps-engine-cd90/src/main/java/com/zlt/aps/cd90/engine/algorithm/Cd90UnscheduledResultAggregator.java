package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90ScheduleAttemptTrace;
import com.zlt.aps.cd90.engine.model.Cd90UnscheduledReason;
import com.zlt.aps.cd90.engine.model.Cd90UnscheduledResultModel;
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
public class Cd90UnscheduledResultAggregator {

    private final Cd90UnscheduledReasonResolver reasonResolver;

    /**
     * 将多班执行轨迹汇总为一规格一原因一行的未排内存结果。
     */
    public List<Cd90UnscheduledResultModel> aggregate(List<Cd90ScheduleAttemptTrace> traces) {
        Map<String, List<Cd90ScheduleAttemptTrace>> grouped = safe(traces).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getClothCode()))
                .sorted(Comparator.comparingInt(Cd90ScheduleAttemptTrace::getSequence))
                .collect(Collectors.groupingBy(Cd90ScheduleAttemptTrace::getClothCode,
                        LinkedHashMap::new, Collectors.toList()));
        List<Cd90UnscheduledResultModel> results = new ArrayList<>();
        grouped.forEach((clothCode, attempts) -> results.addAll(aggregateOne(clothCode, attempts)));
        log.info("[直裁自动排程] 未排结果内存汇总完成, traceCount={}, clothCount={}, reasonRowCount={}",
                safe(traces).size(), grouped.size(), results.size());
        return results;
    }

    private List<Cd90UnscheduledResultModel> aggregateOne(
            String clothCode, List<Cd90ScheduleAttemptTrace> attempts) {
        BigDecimal demand = attempts.stream()
                .map(Cd90ScheduleAttemptTrace::getNetDemandQuantity)
                .filter(value -> value != null && value.signum() > 0)
                .findFirst().orElse(BigDecimal.ZERO);
        BigDecimal scheduled = attempts.stream()
                .map(Cd90ScheduleAttemptTrace::getScheduledQuantity)
                .map(this::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unscheduled = demand.subtract(scheduled).max(BigDecimal.ZERO);
        if (unscheduled.signum() <= 0) {
            return Collections.emptyList();
        }

        LinkedHashMap<String, Cd90UnscheduledReason> reasons = new LinkedHashMap<>();
        attempts.stream().map(Cd90ScheduleAttemptTrace::getFailureReason)
                .filter(StringUtils::hasText)
                .map(reasonResolver::resolve)
                .forEach(reason -> reasons.putIfAbsent(reason.getReasonCode(), reason));
        Cd90UnscheduledReason windowLimit = reasonResolver.resolve("SCHEDULE_WINDOW_LIMIT");
        reasons.putIfAbsent(windowLimit.getReasonCode(), windowLimit);

        String firstStage = reasons.values().iterator().next().getFailStage();
        String bigRollCode = attempts.stream().map(Cd90ScheduleAttemptTrace::getBigRollCode)
                .filter(StringUtils::hasText).findFirst().orElse(null);
        List<Cd90UnscheduledResultModel> rows = new ArrayList<>();
        int order = 1;
        for (Cd90UnscheduledReason reason : reasons.values()) {
            rows.add(Cd90UnscheduledResultModel.builder()
                    .clothCode(clothCode).bigRollCode(bigRollCode)
                    .demandQuantity(demand).scheduledQuantity(scheduled)
                    .unscheduledQuantity(unscheduled).failStage(firstStage)
                    .reasonCode(reason.getReasonCode()).reasonOrder(order)
                    .primaryReason(order == 1).reasonDescription(reason.getReasonDescription())
                    .candidateMachineCodes(null).build());
            order++;
        }
        return rows;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
