package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingBuildResult;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.common.core.enums.ThreeShiftEnum;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 将GDYY实际库存和班次计划转换为CD15大卷成熟流水。
 */
@Component
public class Cd15BigRollAgingStockBuilder {

    private static final String ACTUAL_STOCK = "ACTUAL_STOCK";
    private static final String PLAN_STOCK = "GDYY_PLAN";
    private static final String ESTIMATE_STOCK = "1";

    /**
     * 实际库存按入库时间精确释放，计划库存按班次结束时间保守释放。
     */
    public Cd15BigRollAgingBuildResult build(List<GdyyStock> actualStocks,
                                             List<GdyyScheduleResult> scheduleResults,
                                             List<Cd15ShiftConfig> shiftConfigs,
                                             int agingPeriodHours) {
        int agingHours = Math.max(0, agingPeriodHours);
        List<GdyyStock> actual = this.safe(actualStocks).stream()
                .filter(Objects::nonNull)
                .filter(item -> !ESTIMATE_STOCK.equals(item.getEstimateStockFlag()))
                .filter(item -> StringUtils.hasText(item.getBigRollCode()))
                .filter(item -> this.actualRollQuantity(item).signum() > 0)
                .collect(Collectors.toList());

        Set<String> missingCodes = actual.stream()
                .filter(this::isActualDataMissing)
                .map(GdyyStock::getBigRollCode)
                .collect(Collectors.toCollection(HashSet::new));

        List<Cd15BigRollAgingStock> actualAgingStocks = actual.stream()
                .filter(item -> !this.isActualDataMissing(item))
                .map(item -> this.actualStock(item, agingHours))
                .collect(Collectors.toList());

        Map<String, ShiftTiming> shiftTimings = this.shiftTimings(shiftConfigs);
        List<PlanShift> planShifts = this.safe(scheduleResults).stream()
                .filter(Objects::nonNull)
                .flatMap(result -> this.planShifts(result, shiftTimings))
                .filter(item -> StringUtils.hasText(item.bigRollCode))
                .filter(item -> item.quantity.signum() > 0)
                .collect(Collectors.toList());

        missingCodes.addAll(planShifts.stream()
                .filter(item -> item.shiftDate == null || item.shiftTiming == null)
                .map(item -> item.bigRollCode)
                .collect(Collectors.toSet()));

        List<Cd15BigRollAgingStock> planAgingStocks = planShifts.stream()
                .filter(item -> item.shiftDate != null && item.shiftTiming != null)
                .filter(item -> !this.coveredByActual(item, actual))
                .map(item -> this.planStock(item, agingHours))
                .collect(Collectors.toList());

        List<Cd15BigRollAgingStock> stocks = Stream.concat(actualAgingStocks.stream(), planAgingStocks.stream())
                .sorted(Comparator.comparing(Cd15BigRollAgingStock::getReleaseTime)
                        .thenComparing(item -> Objects.toString(item.getSourceId(), "")))
                .collect(Collectors.toList());
        return Cd15BigRollAgingBuildResult.builder()
                .stocks(stocks)
                .dataMissingBigRollCodes(missingCodes)
                .build();
    }

    private Cd15BigRollAgingStock actualStock(GdyyStock stock, int agingHours) {
        LocalDateTime stockInTime = this.toLocalDateTime(stock.getInboundTime());
        return Cd15BigRollAgingStock.builder()
                .sourceType(ACTUAL_STOCK)
                .sourceId(this.actualSourceId(stock))
                .bigRollCode(stock.getBigRollCode())
                .bigRollBarcode(stock.getBigRollBarcode())
                .availableQuantity(this.actualAvailableMeters(stock))
                .allocatedQuantity(BigDecimal.ZERO)
                .stockInTime(stockInTime)
                .releaseTime(stockInTime.plusHours(agingHours))
                .build();
    }

    private Cd15BigRollAgingStock planStock(PlanShift shift, int agingHours) {
        LocalDateTime estimatedInboundTime = shift.endTime();
        return Cd15BigRollAgingStock.builder()
                .sourceType(PLAN_STOCK)
                .sourceId("PLAN:" + shift.sourceId + ":" + shift.classField)
                .bigRollCode(shift.bigRollCode)
                .availableQuantity(shift.quantity)
                .allocatedQuantity(BigDecimal.ZERO)
                .stockInTime(estimatedInboundTime)
                .releaseTime(estimatedInboundTime.plusHours(agingHours))
                .build();
    }

    private boolean coveredByActual(PlanShift shift, List<GdyyStock> actualStocks) {
        LocalDateTime start = shift.startTime();
        LocalDateTime end = shift.endTime();
        return actualStocks.stream()
                .filter(item -> shift.bigRollCode.equals(item.getBigRollCode()))
                .map(GdyyStock::getInboundTime)
                .filter(Objects::nonNull)
                .map(this::toLocalDateTime)
                .anyMatch(time -> !time.isBefore(start) && time.isBefore(end));
    }

    private Stream<PlanShift> planShifts(GdyyScheduleResult result,
                                         Map<String, ShiftTiming> shiftTimings) {
        String sourceId = Objects.toString(result.getId(), "NO_ID");
        return Arrays.asList(
                this.planShift(sourceId, result.getBigRollCode(), "CLASS1", result.getClass1ScheduleDate(), result.getClass1PlanQty(), shiftTimings),
                this.planShift(sourceId, result.getBigRollCode(), "CLASS2", result.getClass2ScheduleDate(), result.getClass2PlanQty(), shiftTimings),
                this.planShift(sourceId, result.getBigRollCode(), "CLASS3", result.getClass3ScheduleDate(), result.getClass3PlanQty(), shiftTimings),
                this.planShift(sourceId, result.getBigRollCode(), "CLASS4", result.getClass4ScheduleDate(), result.getClass4PlanQty(), shiftTimings),
                this.planShift(sourceId, result.getBigRollCode(), "CLASS5", result.getClass5ScheduleDate(), result.getClass5PlanQty(), shiftTimings),
                this.planShift(sourceId, result.getBigRollCode(), "CLASS6", result.getClass6ScheduleDate(), result.getClass6PlanQty(), shiftTimings),
                this.planShift(sourceId, result.getBigRollCode(), "CLASS7", result.getClass7ScheduleDate(), result.getClass7PlanQty(), shiftTimings),
                this.planShift(sourceId, result.getBigRollCode(), "CLASS8", result.getClass8ScheduleDate(), result.getClass8PlanQty(), shiftTimings))
                .stream();
    }

    private PlanShift planShift(String sourceId, String bigRollCode, String classField,
                                Date shiftDate, Double quantity,
                                Map<String, ShiftTiming> shiftTimings) {
        return new PlanShift(sourceId, bigRollCode, classField,
                shiftDate == null ? null : this.toLocalDate(shiftDate),
                this.value(quantity), shiftTimings.get(classField));
    }

    /**
     * 按结果班次字段解析启用配置。重复、非标准编码或时间非法的配置不参与成熟时间计算。
     */
    private Map<String, ShiftTiming> shiftTimings(List<Cd15ShiftConfig> shiftConfigs) {
        Map<String, List<Cd15ShiftConfig>> configsByClass = this.safe(shiftConfigs).stream()
                .filter(Objects::nonNull)
                .filter(config -> Integer.valueOf(1).equals(config.getIsActive()))
                .filter(config -> StringUtils.hasText(config.getClassField()))
                .collect(Collectors.groupingBy(config ->
                        config.getClassField().trim().toUpperCase()));
        return configsByClass.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 1)
                .map(entry -> this.shiftTiming(entry.getKey(), entry.getValue().get(0)))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ShiftTiming::getClassField,
                        Function.identity()));
    }

    private ShiftTiming shiftTiming(String classField, Cd15ShiftConfig config) {
        if (!ThreeShiftEnum.isValidCode(this.trim(config.getShiftCode()))
                || !StringUtils.hasText(config.getStartTime())
                || !StringUtils.hasText(config.getEndTime())
                || (!Integer.valueOf(0).equals(config.getIsCrossDay())
                && !Integer.valueOf(1).equals(config.getIsCrossDay()))) {
            return null;
        }
        try {
            LocalTime startTime = LocalTime.parse(config.getStartTime().trim());
            LocalTime endTime = LocalTime.parse(config.getEndTime().trim());
            boolean crossDay = Integer.valueOf(1).equals(config.getIsCrossDay());
            if (!crossDay && !endTime.isAfter(startTime)) {
                return null;
            }
            return new ShiftTiming(classField, startTime, endTime, crossDay);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private boolean isActualDataMissing(GdyyStock stock) {
        return stock.getInboundTime() == null
                || stock.getStockMeters() == null
                || stock.getStockMeters().signum() <= 0;
    }

    private BigDecimal actualAvailableMeters(GdyyStock stock) {
        BigDecimal rollMeters = this.value(stock.getStockMeters());
        if (rollMeters.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return this.actualRollQuantity(stock).multiply(rollMeters);
    }

    private BigDecimal actualRollQuantity(GdyyStock stock) {
        return this.value(stock.getStockNum())
                .add(this.value(stock.getModifyNum()))
                .subtract(this.value(stock.getBadNum()))
                .max(BigDecimal.ZERO);
    }

    private String actualSourceId(GdyyStock stock) {
        if (StringUtils.hasText(stock.getBigRollBarcode())) {
            return "ACTUAL:" + stock.getBigRollBarcode();
        }
        return "ACTUAL:" + Objects.toString(stock.getId(), stock.getBigRollCode());
    }


    private LocalDateTime toLocalDateTime(Date value) {
        return LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

    private LocalDate toLocalDate(Date value) {
        return new java.sql.Date(value.getTime()).toLocalDate();
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal value(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** GDYY计划班次对应的CD15运行时间。 */
    private static final class ShiftTiming {
        private final String classField;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final boolean crossDay;

        private ShiftTiming(String classField, LocalTime startTime,
                            LocalTime endTime, boolean crossDay) {
            this.classField = classField;
            this.startTime = startTime;
            this.endTime = endTime;
            this.crossDay = crossDay;
        }

        private String getClassField() {
            return classField;
        }
    }

    /** 单条GDYY计划班次的窄模型。 */
    private static final class PlanShift {
        private final String sourceId;
        private final String bigRollCode;
        private final String classField;
        private final LocalDate shiftDate;
        private final BigDecimal quantity;
        private final ShiftTiming shiftTiming;

        private PlanShift(String sourceId, String bigRollCode, String classField,
                          LocalDate shiftDate, BigDecimal quantity,
                          ShiftTiming shiftTiming) {
            this.sourceId = sourceId;
            this.bigRollCode = bigRollCode;
            this.classField = classField;
            this.shiftDate = shiftDate;
            this.quantity = quantity;
            this.shiftTiming = shiftTiming;
        }

        private LocalDateTime startTime() {
            return shiftDate.atTime(shiftTiming.startTime);
        }

        private LocalDateTime endTime() {
            LocalDate endDate = shiftTiming.crossDay
                    ? shiftDate.plusDays(1) : shiftDate;
            return endDate.atTime(shiftTiming.endTime);
        }
    }
}