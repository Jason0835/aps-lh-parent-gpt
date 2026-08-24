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
        BigDecimal netDemand = this.sharedNetDemand(
                context, input, shift, candidate, rolling, demandStart);
        log.debug("[斜裁自动排程] 当前班次净需求计算完成, classField={}, steelStripCode={}, "
                        + "materialKey={}, demandQuantity={}, sharedNetDemand={}",
                shift.getClassField(), candidate.getSteelStripCode(),
                candidate.getMaterialKey(), demandQuantity, netDemand);
        return Cd15ShiftDemandDecision.builder()
                .netDemandQuantity(netDemand).planSurplusQuantity(null)
                .stopAffected(window.stream().anyMatch(Cd15DemandShift::isStopped))
                .build();
    }

    /**
     * 同一钢带的不同施工材料共用6点库存及有效入库，按首个需求时间和材料键稳定分配。
     */
    private BigDecimal sharedNetDemand(
            Cd15AutoScheduleContext context,
            Cd15AutoScheduleInput input,
            Cd15ShiftDescriptor shift,
            Cd15ScheduleCandidate candidate,
            Cd15RollingScheduleContext rolling,
            LocalDateTime demandStart) {
        String steelStripCode = candidate.getSteelStripCode();
        BigDecimal depthClassQty = this.requiredDepth(input, steelStripCode);
        List<MaterialWindowDemand> profiles = this.materialWindowDemands(
                context, input, steelStripCode, demandStart, depthClassQty);
        if (profiles.stream().noneMatch(
                profile -> candidate.getMaterialKey().equals(profile.materialKey))) {
            return BigDecimal.ZERO;
        }

        BigDecimal stockAtSix = this.safe(input.getStocksAtSix()).stream()
                .filter(item -> item != null
                        && steelStripCode.equals(item.getSteelStripCode()))
                .map(item -> this.value(item.getStockQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime stockBaselineTime = this.safe(input.getStocksAtSix()).stream()
                .filter(item -> item != null
                        && steelStripCode.equals(item.getSteelStripCode()))
                .map(Cd15StockSource::getSnapshotTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        BigDecimal consumedBeforeWindow = this.planningDemands(input).stream()
                .filter(item -> item != null
                        && steelStripCode.equals(item.getSteelStripCode()))
                .filter(item -> item.getStartTime() != null
                        && (stockBaselineTime == null
                        || !item.getStartTime().isBefore(stockBaselineTime))
                        && item.getStartTime().isBefore(demandStart))
                .map(item -> this.value(item.getSteelStripDemandQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Cd15InboundRecord> effectiveInbound = this.effectiveInbound(rolling);
        BigDecimal coilMeter = this.curlLength(
                input, steelStripCode, context.getParameters().getRollCoilMeter());
        BigDecimal inboundBeforeShift = this.inboundQuantity(
                effectiveInbound, steelStripCode,
                record -> record.getInboundTime() == null
                        || !record.getInboundTime().isAfter(shift.getStartTime()),
                coilMeter);
        List<Cd15InboundRecord> futureInbound = effectiveInbound.stream()
                .filter(item -> item != null
                        && steelStripCode.equals(item.getSteelStripCode()))
                .filter(item -> item.getInboundTime() != null
                        && item.getInboundTime().isAfter(shift.getStartTime()))
                .sorted(Comparator.comparing(Cd15InboundRecord::getInboundTime)
                        .thenComparing(item -> Objects.toString(item.getTaskKey(), ""))
                        .thenComparing(item -> Objects.toString(item.getLaneCode(), "")))
                .collect(Collectors.toList());

        BigDecimal inventoryBalance = stockAtSix.add(inboundBeforeShift)
                .subtract(consumedBeforeWindow);
        BigDecimal available = inventoryBalance.max(BigDecimal.ZERO);
        BigDecimal backlog = inventoryBalance.signum() < 0
                ? inventoryBalance.abs() : BigDecimal.ZERO;
        int inboundIndex = 0;
        for (MaterialWindowDemand profile : profiles) {
            while (inboundIndex < futureInbound.size()
                    && !futureInbound.get(inboundIndex).getInboundTime()
                            .isAfter(profile.deadline)) {
                available = available.add(this.inboundQuantity(
                        futureInbound.get(inboundIndex), coilMeter));
                inboundIndex++;
            }
            BigDecimal netDemand = demandCalculator.calculateNetDemand(
                    profile.demandQuantity, backlog, available, BigDecimal.ZERO);
            BigDecimal totalNeed = profile.demandQuantity.add(backlog);
            available = available.subtract(totalNeed).max(BigDecimal.ZERO);
            backlog = BigDecimal.ZERO;
            if (candidate.getMaterialKey().equals(profile.materialKey)) {
                return netDemand;
            }
        }
        return BigDecimal.ZERO;
    }

    /** 构建同一钢带下各材料的当前需求窗口。 */
    private List<MaterialWindowDemand> materialWindowDemands(
            Cd15AutoScheduleContext context,
            Cd15AutoScheduleInput input,
            String steelStripCode,
            LocalDateTime demandStart,
            BigDecimal depthClassQty) {
        return this.planningDemands(input).stream()
                .filter(item -> item != null
                        && steelStripCode.equals(item.getSteelStripCode()))
                .filter(item -> item.getStartTime() != null
                        && !item.getStartTime().isBefore(demandStart))
                .filter(item -> item.getMaterialKey() != null)
                .collect(Collectors.groupingBy(
                        Cd15DemandShift::getMaterialKey,
                        LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    List<Cd15DemandShift> shifts = entry.getValue().stream()
                            .sorted(Comparator.comparing(Cd15DemandShift::getStartTime))
                            .collect(Collectors.toList());
                    List<Cd15DemandShift> window =
                            demandWindowSelector.select(shifts, depthClassQty);
                    BigDecimal quantity = this.calculateWindowDemand(
                            window, context.getParameters().getDemandCalcMode(),
                            depthClassQty);
                    LocalDateTime firstStart = shifts.get(0).getStartTime();
                    LocalDateTime deadline = window.isEmpty()
                            ? firstStart : window.get(window.size() - 1).getStartTime();
                    return new MaterialWindowDemand(
                            entry.getKey(), firstStart, deadline, quantity);
                })
                .filter(profile -> profile.demandQuantity.signum() > 0)
                .sorted(Comparator
                        .comparing((MaterialWindowDemand profile) -> profile.firstStart)
                        .thenComparing(profile -> profile.materialKey))
                .collect(Collectors.toList());
    }

    /** 单条入库记录的精确米数；缺少精确数量时按车数和卷曲长度折算。 */
    private BigDecimal inboundQuantity(
            Cd15InboundRecord record, BigDecimal coilMeter) {
        return record.getInboundQuantity() == null
                ? this.value(coilMeter).multiply(
                        BigDecimal.valueOf(record.getVehicleCount()))
                : record.getInboundQuantity();
    }

    /** 材料级共享库存分配窗口。 */
    private static final class MaterialWindowDemand {
        private final String materialKey;
        private final LocalDateTime firstStart;
        private final LocalDateTime deadline;
        private final BigDecimal demandQuantity;

        private MaterialWindowDemand(
                String materialKey,
                LocalDateTime firstStart,
                LocalDateTime deadline,
                BigDecimal demandQuantity) {
            this.materialKey = materialKey;
            this.firstStart = firstStart;
            this.deadline = deadline;
            this.demandQuantity = demandQuantity;
        }
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
        // SUM窗口不足时，使用最后3个有正需求班次的平均值预测每个缺失窗口。
        int recentStartIndex = Math.max(0, effective.size() - 3);
        List<Cd15DemandShift> recentEffective = effective.subList(recentStartIndex, effective.size());
        BigDecimal recentTotal = recentEffective.stream()
                .map(item -> value(item.getSteelStripDemandQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recentAverage = recentTotal.divide(
                BigDecimal.valueOf(recentEffective.size()), 10, RoundingMode.HALF_UP);
        return this.normalize(total.add(recentAverage.multiply(missingWeight)));
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
                || candidate.getSteelStripCode() == null
                || candidate.getMaterialKey() == null) {
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
