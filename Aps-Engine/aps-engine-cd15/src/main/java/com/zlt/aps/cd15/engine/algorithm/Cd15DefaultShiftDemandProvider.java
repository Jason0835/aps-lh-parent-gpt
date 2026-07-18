package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15InboundRecord;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDemandDecision;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15StockSource;
import com.zlt.aps.cd15.engine.service.Cd15ShiftDemandProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于逐班需求、6点库存和滚动入库的默认需求提供器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd15DefaultShiftDemandProvider implements Cd15ShiftDemandProvider {

    private static final Pattern CLASS_FIELD_PATTERN = Pattern.compile("CLASS([1-8])");
    private static final LocalTime FIRST_FORMING_DEMAND_TIME = LocalTime.of(22, 0);

    private final Cd15DemandCalculator demandCalculator;
    private final Cd15InboundResolver inboundResolver;
    private final Cd15FractionalDemandWindowSelector demandWindowSelector;

    /**
     * 计算当前候选规格的窗口需求、库存缺口和前序有效计划抵扣。
     */
    @Override
    public Cd15ShiftDemandDecision resolve(Cd15AutoScheduleContext context,
                                           Cd15AutoScheduleInput input,
                                           Cd15ShiftDescriptor shift,
                                           Cd15ScheduleCandidate candidate,
                                           Cd15RollingScheduleContext rolling) {
        validate(context, input, shift, candidate);
        if (candidate.isNewSpecAdvance()) {
            BigDecimal advanceRemaining = rolling == null
                    || rolling.getNewSpecAdvanceRemainingBySteelStrip() == null
                    ? BigDecimal.ZERO : rolling.getNewSpecAdvanceRemainingBySteelStrip()
                            .getOrDefault(candidate.getSteelStripCode(), BigDecimal.ZERO);
            log.info("[斜裁自动排程] 新增规格使用提前需求, classField={}, steelStripCode={}, advanceRemaining={}",
                    shift.getClassField(), candidate.getSteelStripCode(), advanceRemaining);
            return Cd15ShiftDemandDecision.builder()
                    .netDemandQuantity(advanceRemaining)
                    .planSurplusQuantity(null).build();
        }
        // 斜裁CLASS字段映射到其负责供应的首个成型自然班次。
        LocalDateTime demandStart = demandStart(context, shift.getClassField());
        List<Cd15DemandShift> materialShifts = this.planningDemands(input).stream()
                .filter(item -> item != null && candidate.getMaterialKey().equals(item.getMaterialKey()))
                .filter(item -> item.getStartTime() != null)
                .sorted(Comparator.comparing(Cd15DemandShift::getStartTime))
                .collect(Collectors.toList());
        // 当前班按该钢带动态深度承担成型需求，半班由窗口选择器按比例计入。
        List<Cd15DemandShift> availableShifts = materialShifts.stream()
                .filter(item -> !item.getStartTime().isBefore(demandStart))
                .collect(Collectors.toList());
        BigDecimal depthClassQty = this.requiredDepth(input, candidate.getSteelStripCode());
        List<Cd15DemandShift> window = demandWindowSelector.select(availableShifts, depthClassQty);
        BigDecimal demandQuantity = calculateWindowDemand(
                window, context.getParameters().getDemandCalcMode(), depthClassQty);
        // 窗口前成型消耗用于重算6点库存余额，不属于本次待排需求。
        BigDecimal consumedBeforeWindow = materialShifts.stream()
                .filter(item -> item.getStartTime().isBefore(demandStart))
                .map(item -> value(item.getSteelStripDemandQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal stockAtSix = safe(input.getStocksAtSix()).stream()
                .filter(item -> item != null && candidate.getSteelStripCode().equals(item.getSteelStripCode()))
                .map(item -> value(item.getStockQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 实际入库优先于同任务计划入库，解析器会消除互斥记录，避免重复抵扣需求。
        List<Cd15InboundRecord> effectiveInbound = effectiveInbound(rolling);
        // 班次开始前已入库数量进入当前可用库存；班次后但需求截止前入库用于抵扣未来需求。
        BigDecimal inboundBeforeShift = inboundQuantity(effectiveInbound, candidate.getSteelStripCode(),
                record -> record.getInboundTime() == null
                        || !record.getInboundTime().isAfter(shift.getStartTime()),
                curlLength(input, candidate.getSteelStripCode(), context.getParameters().getRollCoilMeter()));
        LocalDateTime demandDeadline = window.isEmpty()
                ? demandStart : window.get(window.size() - 1).getStartTime();
        BigDecimal futureEffectivePlan = inboundQuantity(effectiveInbound, candidate.getSteelStripCode(),
                record -> record.getInboundTime() != null
                        && record.getInboundTime().isAfter(shift.getStartTime())
                        && !record.getInboundTime().isAfter(demandDeadline),
                curlLength(input, candidate.getSteelStripCode(), context.getParameters().getRollCoilMeter()));

        // 余额为负时拆成历史缺口，余额为正时作为现有库存，两者不能同时重复参与净需求。
        BigDecimal inventoryBalance = stockAtSix.add(inboundBeforeShift).subtract(consumedBeforeWindow);
        BigDecimal expectedStock = inventoryBalance.max(BigDecimal.ZERO);
        BigDecimal shortage = inventoryBalance.signum() < 0
                ? inventoryBalance.abs() : BigDecimal.ZERO;
        // 净需求口径：窗口需求 + 历史缺口 - 可用库存 - 截止前有效计划入库，最低为0。
        BigDecimal netDemand = demandCalculator.calculateNetDemand(
                demandQuantity, shortage, expectedStock, futureEffectivePlan);
        log.debug("[斜裁自动排程] 当前班次净需求计算完成, classField={}, steelStripCode={}, "
                        + "demandQuantity={}, expectedStock={}, shortage={}, futurePlan={}, netDemand={}",
                shift.getClassField(), candidate.getSteelStripCode(), demandQuantity,
                expectedStock, shortage, futureEffectivePlan, netDemand);
        return Cd15ShiftDemandDecision.builder()
                .netDemandQuantity(netDemand).planSurplusQuantity(null).build();
    }

    /**
     * 汇总6点至当前斜裁班次开始前已经发生的成型钢带消耗，按钢带代号分组。
     */
    @Override
    public Map<String, BigDecimal> cumulativeConsumptionBySteelStripBeforeShift(
            Cd15AutoScheduleContext context, Cd15AutoScheduleInput input, Cd15ShiftDescriptor shift) {
        if (context == null || context.getScheduleDate() == null || input == null || shift == null) {
            throw new IllegalArgumentException("累计消耗计算上下文、输入和班次不能为空");
        }
        LocalDateTime sixAtWindowStart = context.getScheduleDate().minusDays(1).atTime(6, 0);
        // 库排释放必须按钢带独立扣减，避免某个的成型消耗释放其他已占库排。
        return safe(input.getDemandShifts()).stream()
                .filter(item -> item != null && item.getStartTime() != null)
                .filter(item -> item.getSteelStripCode() != null)
                .filter(item -> !item.getStartTime().isBefore(sixAtWindowStart))
                .filter(item -> item.getStartTime().isBefore(shift.getStartTime()))
                .collect(Collectors.groupingBy(Cd15DemandShift::getSteelStripCode,
                        Collectors.mapping(item -> value(item.getSteelStripDemandQuantity()),
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    private BigDecimal calculateWindowDemand(List<Cd15DemandShift> window,
                                             String mode,
                                             BigDecimal depthClassQty) {
        List<Cd15DemandShift> effective = window.stream()
                .filter(Cd15DemandShift::isIncluded)
                .filter(item -> value(item.getSteelStripDemandQuantity()).signum() > 0)
                .collect(Collectors.toList());
        if (effective.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = effective.stream()
                .map(item -> value(item.getSteelStripDemandQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWeight = effective.stream()
                .map(this::windowWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal missingWeight = depthClassQty == null
                ? BigDecimal.ZERO : depthClassQty.subtract(totalWeight).max(BigDecimal.ZERO);
        if ("AVERAGE".equals(mode)) {
            BigDecimal average = total.divide(totalWeight, 10, RoundingMode.HALF_UP);
            return missingWeight.signum() > 0
                    ? this.normalize(average.multiply(depthClassQty))
                    : this.normalize(average);
        }
        if (!"SUM".equals(mode)) {
            throw new IllegalArgumentException("需求计算方式只能取AVERAGE或SUM");
        }
        if (missingWeight.signum() <= 0) {
            return this.normalize(total);
        }
        BigDecimal maxShiftDemand = effective.stream()
                .map(item -> value(item.getSteelStripDemandQuantity()))
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return this.normalize(total.add(maxShiftDemand.multiply(missingWeight)));
    }
    private BigDecimal requiredDepth(Cd15AutoScheduleInput input, String steelStripCode) {
        BigDecimal depthClassQty = input.getDepthClassQtyBySteelStrip() == null
                ? null : input.getDepthClassQtyBySteelStrip().get(steelStripCode);
        if (depthClassQty == null || depthClassQty.signum() <= 0) {
            throw new IllegalArgumentException("钢带未匹配有效备库深度, steelStripCode=" + steelStripCode);
        }
        return depthClassQty;
    }

    private BigDecimal windowWeight(Cd15DemandShift shift) {
        return shift.getWindowWeight() == null ? BigDecimal.ONE : shift.getWindowWeight();
    }

    private List<Cd15InboundRecord> effectiveInbound(Cd15RollingScheduleContext rolling) {
        if (rolling == null) {
            return Collections.emptyList();
        }
        List<Cd15InboundRecord> records = new ArrayList<>();
        records.addAll(safe(rolling.getActualInboundRecords()));
        records.addAll(safe(rolling.getPlannedInboundRecords()));
        return inboundResolver.resolve(records);
    }

    private BigDecimal inboundQuantity(List<Cd15InboundRecord> records, String steelStripCode,
                                       java.util.function.Predicate<Cd15InboundRecord> predicate,
                                       BigDecimal coilMeter) {
        return records.stream()
                .filter(item -> steelStripCode.equals(item.getSteelStripCode()))
                .filter(predicate)
                .map(item -> item.getInboundQuantity() == null
                        ? value(coilMeter).multiply(BigDecimal.valueOf(item.getVehicleCount()))
                        : item.getInboundQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 入库记录只有车数没有精确数量时，按该钢带的标准卷曲长度折算；标准长度缺失时才使用CRIMP_LENGTH兜底。 */
    private BigDecimal curlLength(Cd15AutoScheduleInput input, String steelStripCode, BigDecimal fallback) {
        return safe(input.getConstructionMaterials()).stream()
                .filter(item -> item != null && steelStripCode.equals(item.getSteelStripCode()))
                .map(Cd15ConstructionMaterial::getCurlLength)
                .filter(item -> item != null && item.signum() > 0)
                .findFirst()
                .orElse(fallback);
    }

    private LocalDateTime demandStart(Cd15AutoScheduleContext context, String classField) {
        Matcher matcher = CLASS_FIELD_PATTERN.matcher(classField == null ? "" : classField);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("斜裁班次字段只能取CLASS1至CLASS8");
        }
        int classIndex = Integer.parseInt(matcher.group(1));
        return context.getScheduleDate().minusDays(1).atTime(FIRST_FORMING_DEMAND_TIME)
                .plusHours((classIndex - 1L) * 8L);
    }

    private void validate(Cd15AutoScheduleContext context, Cd15AutoScheduleInput input,
                          Cd15ShiftDescriptor shift, Cd15ScheduleCandidate candidate) {
        if (context == null || context.getScheduleDate() == null || context.getParameters() == null
                || input == null || shift == null || candidate == null
                || candidate.getSteelStripCode() == null) {
            throw new IllegalArgumentException("班次需求计算上下文、输入、班次和候选不能为空");
        }
    }

    private BigDecimal normalize(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
    }
    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }


    /** 优先使用去重计划需求；未建立新增规格快照时兼容原始输入。 */
    private List<Cd15DemandShift> planningDemands(Cd15AutoScheduleInput input) {
        return input.getPlanningDemandShifts() == null
                ? this.safe(input.getDemandShifts())
                : this.safe(input.getPlanningDemandShifts());
    }
    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
