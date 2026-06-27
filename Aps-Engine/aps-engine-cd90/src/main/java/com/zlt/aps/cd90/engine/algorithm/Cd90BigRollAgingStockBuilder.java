package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90BigRollAgingBuildResult;
import com.zlt.aps.cd90.engine.model.Cd90BigRollAgingStock;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 将XWYY实际库存和班次计划转换为直裁大卷成熟流水。
 */
@Component
public class Cd90BigRollAgingStockBuilder {

    private static final String ACTUAL_STOCK = "ACTUAL_STOCK";
    private static final String PLAN_STOCK = "XWYY_PLAN";
    private static final String ESTIMATE_STOCK = "1";

    /**
     * 实际库存按入库时间精确释放，计划库存按班次结束时间保守释放。
     *
     * @param actualStocks XWYY实际库存
     * @param scheduleResults XWYY排程结果
     * @param agingPeriodHours 静置小时数
     * @return 成熟流水和数据缺失大卷集合
     */
    public Cd90BigRollAgingBuildResult build(List<XwyyStock> actualStocks,
                                             List<XwyyScheduleResult> scheduleResults,
                                             int agingPeriodHours) {
        int agingHours = Math.max(0, agingPeriodHours);
        List<XwyyStock> actual = safe(actualStocks).stream()
                .filter(Objects::nonNull)
                .filter(item -> !ESTIMATE_STOCK.equals(item.getEstimateStockFlag()))
                .filter(item -> StringUtils.hasText(item.getBigRollCode()))
                .filter(item -> this.actualRollQuantity(item).signum() > 0)
                .collect(Collectors.toList());

        Set<String> missingCodes = actual.stream()
                .filter(this::isActualDataMissing)
                .map(XwyyStock::getBigRollCode)
                .collect(Collectors.toCollection(HashSet::new));

        List<Cd90BigRollAgingStock> actualAgingStocks = actual.stream()
                .filter(item -> !this.isActualDataMissing(item))
                .map(item -> actualStock(item, agingHours))
                .collect(Collectors.toList());

        List<PlanShift> planShifts = safe(scheduleResults).stream()
                .filter(Objects::nonNull)
                .flatMap(this::planShifts)
                .filter(item -> StringUtils.hasText(item.bigRollCode))
                .filter(item -> item.quantity.signum() > 0)
                .collect(Collectors.toList());

        missingCodes.addAll(planShifts.stream()
                .filter(item -> item.shiftDate == null)
                .map(item -> item.bigRollCode)
                .collect(Collectors.toSet()));

        List<Cd90BigRollAgingStock> planAgingStocks = planShifts.stream()
                .filter(item -> item.shiftDate != null)
                .filter(item -> !coveredByActual(item, actual))
                .map(item -> planStock(item, agingHours))
                .collect(Collectors.toList());

        List<Cd90BigRollAgingStock> stocks = Stream.concat(
                        actualAgingStocks.stream(), planAgingStocks.stream())
                .sorted(Comparator.comparing(Cd90BigRollAgingStock::getReleaseTime)
                        .thenComparing(item -> Objects.toString(item.getSourceId(), "")))
                .collect(Collectors.toList());
        return Cd90BigRollAgingBuildResult.builder()
                .stocks(stocks)
                .dataMissingBigRollCodes(missingCodes)
                .build();
    }

    /** 将一条实际库存转换为精确成熟流水。 */
    private Cd90BigRollAgingStock actualStock(XwyyStock stock, int agingHours) {
        LocalDateTime stockInTime = toLocalDateTime(stock.getStockInTime());
        return Cd90BigRollAgingStock.builder()
                .sourceType(ACTUAL_STOCK)
                .sourceId(actualSourceId(stock))
                .bigRollCode(stock.getBigRollCode())
                .availableQuantity(this.actualAvailableMeters(stock))
                .allocatedQuantity(BigDecimal.ZERO)
                .stockInTime(stockInTime)
                .releaseTime(stockInTime.plusHours(agingHours))
                .build();
    }

    /** 将一条压延班次计划转换为保守成熟流水。 */
    private Cd90BigRollAgingStock planStock(PlanShift shift, int agingHours) {
        LocalDateTime estimatedInboundTime = shift.endTime();
        return Cd90BigRollAgingStock.builder()
                .sourceType(PLAN_STOCK)
                .sourceId("PLAN:" + shift.sourceId + ":" + shift.classField)
                .bigRollCode(shift.bigRollCode)
                .availableQuantity(shift.quantity)
                .allocatedQuantity(BigDecimal.ZERO)
                .stockInTime(estimatedInboundTime)
                .releaseTime(estimatedInboundTime.plusHours(agingHours))
                .build();
    }

    /** 实际入库落在同一计划班次时，计划量不再重复计入。 */
    private boolean coveredByActual(PlanShift shift, List<XwyyStock> actualStocks) {
        LocalDateTime start = shift.startTime();
        LocalDateTime end = shift.endTime();
        return actualStocks.stream()
                .filter(item -> shift.bigRollCode.equals(item.getBigRollCode()))
                .map(XwyyStock::getStockInTime)
                .filter(Objects::nonNull)
                .map(this::toLocalDateTime)
                .anyMatch(time -> !time.isBefore(start) && time.isBefore(end));
    }

    /** 展开CLASS1至CLASS8计划量及对应的自然班次时间。 */
    private Stream<PlanShift> planShifts(XwyyScheduleResult result) {
        String sourceId = Objects.toString(result.getId(), "NO_ID");
        return Arrays.asList(
                planShift(sourceId, result.getBigRollCode(), "CLASS1",
                        result.getClass1ScheduleDate(), result.getClass1PlanQty(), 14),
                planShift(sourceId, result.getBigRollCode(), "CLASS2",
                        result.getClass2ScheduleDate(), result.getClass2PlanQty(), 22),
                planShift(sourceId, result.getBigRollCode(), "CLASS3",
                        result.getClass3ScheduleDate(), result.getClass3PlanQty(), 6),
                planShift(sourceId, result.getBigRollCode(), "CLASS4",
                        result.getClass4ScheduleDate(), result.getClass4PlanQty(), 14),
                planShift(sourceId, result.getBigRollCode(), "CLASS5",
                        result.getClass5ScheduleDate(), result.getClass5PlanQty(), 22),
                planShift(sourceId, result.getBigRollCode(), "CLASS6",
                        result.getClass6ScheduleDate(), result.getClass6PlanQty(), 6),
                planShift(sourceId, result.getBigRollCode(), "CLASS7",
                        result.getClass7ScheduleDate(), result.getClass7PlanQty(), 14),
                planShift(sourceId, result.getBigRollCode(), "CLASS8",
                        result.getClass8ScheduleDate(), result.getClass8PlanQty(), 22))
                .stream();
    }

    private PlanShift planShift(String sourceId, String bigRollCode, String classField,
                                Date shiftDate, BigDecimal quantity, int startHour) {
        return new PlanShift(sourceId, bigRollCode, classField,
                shiftDate == null ? null : toLocalDate(shiftDate),
                value(quantity), startHour);
    }

    /** 判断实际库存是否缺少成熟时间或单卷米数。 */
    private boolean isActualDataMissing(XwyyStock stock) {
        return stock.getStockInTime() == null
                || stock.getStockMeters() == null
                || stock.getStockMeters().signum() <= 0;
    }

    /** 将实际库存净卷数换算为可参与直裁分配的米数。 */
    private BigDecimal actualAvailableMeters(XwyyStock stock) {
        return this.actualRollQuantity(stock).multiply(stock.getStockMeters());
    }

    /** 按库存、修正和不良数量计算实际库存净卷数。 */
    private BigDecimal actualRollQuantity(XwyyStock stock) {
        return this.value(stock.getStockNum())
                .add(this.value(stock.getModifyNum()))
                .subtract(this.value(stock.getBadNum()))
                .max(BigDecimal.ZERO);
    }

    private String actualSourceId(XwyyStock stock) {
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

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    /** 单条XWYY计划班次的窄模型。 */
    private static final class PlanShift {
        private final String sourceId;
        private final String bigRollCode;
        private final String classField;
        private final LocalDate shiftDate;
        private final BigDecimal quantity;
        private final int startHour;

        private PlanShift(String sourceId, String bigRollCode, String classField,
                          LocalDate shiftDate, BigDecimal quantity, int startHour) {
            this.sourceId = sourceId;
            this.bigRollCode = bigRollCode;
            this.classField = classField;
            this.shiftDate = shiftDate;
            this.quantity = quantity;
            this.startHour = startHour;
        }

        private LocalDateTime startTime() {
            return shiftDate.atTime(startHour, 0);
        }

        private LocalDateTime endTime() {
            return startTime().plusHours(8);
        }
    }
}
