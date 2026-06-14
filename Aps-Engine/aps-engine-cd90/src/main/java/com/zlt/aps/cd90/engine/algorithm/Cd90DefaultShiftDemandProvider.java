package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90InboundRecord;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDemandDecision;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;
import com.zlt.aps.cd90.engine.service.Cd90ShiftDemandProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于逐班需求、6点库存和滚动入库的默认需求提供器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd90DefaultShiftDemandProvider implements Cd90ShiftDemandProvider {

    private static final Pattern CLASS_FIELD_PATTERN = Pattern.compile("CLASS([1-8])");
    private static final LocalTime FIRST_FORMING_DEMAND_TIME = LocalTime.of(22, 0);

    private final Cd90DemandCalculator demandCalculator;
    private final Cd90InboundResolver inboundResolver;

    /**
     * 计算当前候选规格的窗口需求、库存缺口和前序有效计划抵扣。
     */
    @Override
    public Cd90ShiftDemandDecision resolve(Cd90AutoScheduleContext context,
                                           Cd90AutoScheduleInput input,
                                           Cd90ShiftDescriptor shift,
                                           Cd90ScheduleCandidate candidate,
                                           Cd90RollingScheduleContext rolling) {
        validate(context, input, shift, candidate);
        LocalDateTime demandStart = demandStart(context, shift.getClassField());
        List<Cd90DemandShift> clothShifts = safe(input.getDemandShifts()).stream()
                .filter(item -> item != null && candidate.getClothCode().equals(item.getClothCode()))
                .filter(item -> item.getStartTime() != null)
                .sorted(Comparator.comparing(Cd90DemandShift::getStartTime))
                .collect(Collectors.toList());
        List<Cd90DemandShift> window = clothShifts.stream()
                .filter(item -> !item.getStartTime().isBefore(demandStart))
                .limit(context.getParameters().getDemandWindow())
                .collect(Collectors.toList());
        BigDecimal demandQuantity = calculateWindowDemand(
                window, context.getParameters().getDemandCalcMode());
        BigDecimal consumedBeforeWindow = clothShifts.stream()
                .filter(item -> item.getStartTime().isBefore(demandStart))
                .map(item -> value(item.getClothDemandQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal stockAtSix = safe(input.getStocksAtSix()).stream()
                .filter(item -> item != null && candidate.getClothCode().equals(item.getClothCode()))
                .map(item -> value(item.getStockQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Cd90InboundRecord> effectiveInbound = effectiveInbound(rolling);
        BigDecimal inboundBeforeShift = inboundQuantity(effectiveInbound, candidate.getClothCode(),
                record -> record.getInboundTime() == null
                        || !record.getInboundTime().isAfter(shift.getStartTime()),
                context.getParameters().getRollCoilMeter());
        LocalDateTime demandDeadline = window.isEmpty()
                ? demandStart : window.get(window.size() - 1).getStartTime();
        BigDecimal futureEffectivePlan = inboundQuantity(effectiveInbound, candidate.getClothCode(),
                record -> record.getInboundTime() != null
                        && record.getInboundTime().isAfter(shift.getStartTime())
                        && !record.getInboundTime().isAfter(demandDeadline),
                context.getParameters().getRollCoilMeter());

        BigDecimal inventoryBalance = stockAtSix.add(inboundBeforeShift).subtract(consumedBeforeWindow);
        BigDecimal expectedStock = inventoryBalance.max(BigDecimal.ZERO);
        BigDecimal shortage = inventoryBalance.signum() < 0
                ? inventoryBalance.abs() : BigDecimal.ZERO;
        BigDecimal netDemand = demandCalculator.calculateNetDemand(
                demandQuantity, shortage, expectedStock, futureEffectivePlan);
        log.debug("[直裁自动排程] 当前班次净需求计算完成, classField={}, clothCode={}, "
                        + "demandQuantity={}, expectedStock={}, shortage={}, futurePlan={}, netDemand={}",
                shift.getClassField(), candidate.getClothCode(), demandQuantity,
                expectedStock, shortage, futureEffectivePlan, netDemand);
        return Cd90ShiftDemandDecision.builder()
                .netDemandQuantity(netDemand).planSurplusQuantity(null).build();
    }

    /**
     * 汇总6点至当前直裁班次开始前已经发生的成型帘布消耗。
     */
    @Override
    public BigDecimal cumulativeConsumptionBeforeShift(Cd90AutoScheduleContext context,
                                                       Cd90AutoScheduleInput input,
                                                       Cd90ShiftDescriptor shift) {
        if (context == null || context.getScheduleDate() == null || input == null || shift == null) {
            throw new IllegalArgumentException("累计消耗计算上下文、输入和班次不能为空");
        }
        LocalDateTime sixAtWindowStart = context.getScheduleDate().minusDays(1).atTime(6, 0);
        return safe(input.getDemandShifts()).stream()
                .filter(item -> item != null && item.getStartTime() != null)
                .filter(item -> !item.getStartTime().isBefore(sixAtWindowStart))
                .filter(item -> item.getStartTime().isBefore(shift.getStartTime()))
                .map(item -> value(item.getClothDemandQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateWindowDemand(List<Cd90DemandShift> window, String mode) {
        List<BigDecimal> effective = window.stream()
                .filter(Cd90DemandShift::isIncluded)
                .map(item -> value(item.getClothDemandQuantity()))
                .filter(item -> item.signum() > 0)
                .collect(Collectors.toList());
        if (effective.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = effective.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if ("AVERAGE".equals(mode)) {
            return total.divide(BigDecimal.valueOf(effective.size()), 10, RoundingMode.HALF_UP)
                    .stripTrailingZeros();
        }
        if (!"SUM".equals(mode)) {
            throw new IllegalArgumentException("需求计算方式只能取AVERAGE或SUM");
        }
        return total;
    }

    private List<Cd90InboundRecord> effectiveInbound(Cd90RollingScheduleContext rolling) {
        if (rolling == null) {
            return Collections.emptyList();
        }
        List<Cd90InboundRecord> records = new ArrayList<>();
        records.addAll(safe(rolling.getActualInboundRecords()));
        records.addAll(safe(rolling.getPlannedInboundRecords()));
        return inboundResolver.resolve(records);
    }

    private BigDecimal inboundQuantity(List<Cd90InboundRecord> records, String clothCode,
                                       java.util.function.Predicate<Cd90InboundRecord> predicate,
                                       BigDecimal coilMeter) {
        return records.stream()
                .filter(item -> clothCode.equals(item.getClothCode()))
                .filter(predicate)
                .map(item -> item.getInboundQuantity() == null
                        ? value(coilMeter).multiply(BigDecimal.valueOf(item.getVehicleCount()))
                        : item.getInboundQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDateTime demandStart(Cd90AutoScheduleContext context, String classField) {
        Matcher matcher = CLASS_FIELD_PATTERN.matcher(classField == null ? "" : classField);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("直裁班次字段只能取CLASS1至CLASS8");
        }
        int classIndex = Integer.parseInt(matcher.group(1));
        return context.getScheduleDate().minusDays(1).atTime(FIRST_FORMING_DEMAND_TIME)
                .plusHours((classIndex - 1L) * 8L);
    }

    private void validate(Cd90AutoScheduleContext context, Cd90AutoScheduleInput input,
                          Cd90ShiftDescriptor shift, Cd90ScheduleCandidate candidate) {
        if (context == null || context.getScheduleDate() == null || context.getParameters() == null
                || input == null || shift == null || candidate == null
                || candidate.getClothCode() == null) {
            throw new IllegalArgumentException("班次需求计算上下文、输入、班次和候选不能为空");
        }
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
